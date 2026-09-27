package com.nestling.baby.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nestling.baby.R
import com.nestling.baby.data.CsvExporter
import com.nestling.baby.ui.theme.NestlingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Quick actions the home-screen widget can hand to the activity. */
object QuickAction {
    const val EXTRA = "com.nestling.baby.extra.QUICK_ACTION"
    const val DIAPER = "diaper"
}

/**
 * Composition root: theme, state, snackbars, CSV export, and the one second tick that
 * drives the live timer.
 */
@Composable
fun NestlingApp(
    viewModel: NestlingViewModel,
    quickAction: String? = null,
    onQuickActionHandled: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val sheets = rememberSheetController()
    val scope = rememberCoroutineScope()
    val nowMillis by rememberNowMillis(fast = state.activeTimer != null)

    NestlingTheme(
        nightMode = state.settings.nightMode,
        dynamicColor = state.settings.dynamicColor,
    ) {
        NestlingScaffold(
            state = state,
            nowMillis = nowMillis,
            snackbarHostState = snackbarHostState,
            sheets = sheets,
            actions = NestlingActions(
                onToggleTimer = viewModel::toggleTimer,
                onStopTimer = viewModel::stopTimer,
                onDiscardTimer = viewModel::discardTimer,
                onLogDiaper = viewModel::logDiaper,
                onSaveAmount = viewModel::saveAmount,
                onDeleteEvent = viewModel::deleteEvent,
                onQueryChange = viewModel::setQuery,
                onShiftDay = viewModel::shiftDay,
                onSelectDate = viewModel::selectDate,
                onToggleNightMode = viewModel::setNightMode,
                onSetUnit = viewModel::setUnit,
                onRename = viewModel::setBabyName,
                onExport = {
                    scope.launch { exportCsv(context, viewModel, snackbarHostState) }
                },
            ),
        )
    }

    // Ask for notifications only when a timer actually starts — never on first launch,
    // and never as a gate. A denied prompt just means no ongoing notification; the
    // timer itself lives in Room either way.
    NotificationPermissionOnFirstTimer(running = state.activeTimer != null)

    // Widget "diaper" tap opens straight onto the picker.
    LaunchedEffect(quickAction) {
        if (quickAction == QuickAction.DIAPER) {
            sheets.openDiaper()
            onQuickActionHandled()
        }
    }

    // Saved / deleted / error messages. Never a dialog, never a crash.
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val text = buildString {
                append(context.getString(message.text))
                message.detail?.let { append(" · ").append(it) }
            }
            val editId = message.editAmountForEventId
            val result = snackbarHostState.showSnackbar(
                message = text,
                actionLabel = editId?.let { context.getString(R.string.action_edit_amount) },
                withDismissAction = false,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && editId != null) {
                val amount = viewModel.eventById(editId)?.amountMl ?: 0
                sheets.openAmount(editId, amount)
            }
        }
    }

    // After 10pm, offer the dim red theme once. Ignoring it means "not tonight".
    LaunchedEffect(state.suggestNightMode) {
        if (!state.suggestNightMode) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.msg_night_suggestion),
            actionLabel = context.getString(R.string.msg_night_suggestion_action),
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.setNightMode(true)
        } else {
            viewModel.dismissNightSuggestion()
        }
    }
}

@Composable
private fun NotificationPermissionOnFirstTimer(running: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or not, the timer keeps running */ }

    LaunchedEffect(running) {
        if (!running || asked) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Ticks once a second while a timer runs, once a minute otherwise (so "25 min ago"
 * stays true). Nothing else in the app animates on a clock.
 */
@Composable
fun rememberNowMillis(fast: Boolean): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(fast) {
        val interval = if (fast) 1_000L else 60_000L
        while (true) {
            now.longValue = System.currentTimeMillis()
            delay(interval)
        }
    }
    return now
}

/** Write the whole log to cacheDir and hand it to the share sheet. Nothing uploads. */
private suspend fun exportCsv(
    context: Context,
    viewModel: NestlingViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val outcome = runCatching {
        val events = viewModel.allEventsForExport()
        val file = withContext(Dispatchers.IO) {
            CsvExporter.write(context, CsvExporter.toCsv(events))
        }
        context.startActivity(CsvExporter.shareIntent(context, file))
    }
    if (outcome.isFailure) {
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.msg_export_failed),
            duration = SnackbarDuration.Short,
        )
    }
}
