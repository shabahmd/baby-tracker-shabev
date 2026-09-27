package com.nestling.baby.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nestling.baby.R
import com.nestling.baby.domain.BabyEvent
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.domain.Side
import com.nestling.baby.domain.VolumeUnit
import com.nestling.baby.ui.components.AmountSheet
import com.nestling.baby.ui.components.DiaperSheet
import com.nestling.baby.ui.components.QuickLogSheet
import com.nestling.baby.ui.history.HistoryScreen
import com.nestling.baby.ui.theme.Motion
import com.nestling.baby.ui.today.TodayScreen
import com.nestling.baby.util.TimeFormat
import java.time.LocalDate

/** Everything the UI can ask the app to do. Defaults make previews and tests cheap. */
data class NestlingActions(
    val onToggleTimer: (EventType) -> Unit = {},
    val onStopTimer: () -> Unit = {},
    val onDiscardTimer: () -> Unit = {},
    val onLogDiaper: (DiaperKind) -> Unit = {},
    val onSaveAmount: (Long, Int, Side?) -> Unit = { _, _, _ -> },
    val onDeleteEvent: (Long) -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onShiftDay: (Long) -> Unit = {},
    val onSelectDate: (LocalDate) -> Unit = {},
    val onToggleNightMode: (Boolean) -> Unit = {},
    val onSetUnit: (VolumeUnit) -> Unit = {},
    val onRename: (String) -> Unit = {},
    val onExport: () -> Unit = {},
)

/** Which bottom sheet, if any, is on screen. Survives rotation. */
class SheetController(
    kindState: MutableState<String>,
    eventState: MutableState<Long>,
    amountState: MutableState<Int>,
) {
    var kind by kindState
        private set
    var eventId by eventState
        private set
    var amountMl by amountState
        private set

    val isOpen: Boolean get() = kind != NONE

    fun openQuickLog() {
        kind = QUICK_LOG
    }

    fun openDiaper() {
        kind = DIAPER
    }

    fun openAmount(eventId: Long, amountMl: Int) {
        this.eventId = eventId
        this.amountMl = amountMl
        kind = AMOUNT
    }

    fun close() {
        kind = NONE
    }

    companion object {
        const val NONE = "none"
        const val QUICK_LOG = "quick_log"
        const val DIAPER = "diaper"
        const val AMOUNT = "amount"
    }
}

@Composable
fun rememberSheetController(): SheetController {
    val kind = rememberSaveable { mutableStateOf(SheetController.NONE) }
    val eventId = rememberSaveable { mutableStateOf(-1L) }
    val amount = rememberSaveable { mutableStateOf(0) }
    return remember { SheetController(kind, eventId, amount) }
}

/**
 * The whole app chrome: one top app bar, two bottom destinations, one centre FAB.
 * Stateless on purpose — tests drive it with a plain [NestlingUiState].
 */
@Composable
fun NestlingScaffold(
    state: NestlingUiState,
    nowMillis: Long,
    snackbarHostState: SnackbarHostState,
    actions: NestlingActions,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sheets: SheetController = rememberSheetController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = NestlingDestination.fromRoute(backStackEntry?.destination?.route)
    var renaming by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NestlingTopBar(
                state = state,
                destination = current,
                onToggleNightMode = actions.onToggleNightMode,
                onExport = actions.onExport,
                onRename = { renaming = true },
                onSetUnit = actions.onSetUnit,
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag(TestTags.BOTTOM_NAV)) {
                NavigationItem(NestlingDestination.TODAY, current, navController)
                // The gap the FAB is docked into.
                Spacer(Modifier.weight(1f))
                NavigationItem(NestlingDestination.HISTORY, current, navController)
            }
        },
        floatingActionButton = {
            val description = stringResource(R.string.cd_quick_log_fab)
            val rotation by animateFloatAsState(
                targetValue = if (sheets.isOpen) 45f else 0f,
                animationSpec = tween(Motion.DURATION_MEDIUM_MS, easing = Motion.emphasized),
                label = "fab-morph",
            )
            FloatingActionButton(
                onClick = { if (sheets.isOpen) sheets.close() else sheets.openQuickLog() },
                modifier = Modifier
                    .semantics { contentDescription = description }
                    .testTag(TestTags.FAB),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NestlingDestination.TODAY.route,
            enterTransition = { fadeIn(tween(Motion.DURATION_SHORT_MS, easing = Motion.emphasized)) },
            exitTransition = { fadeOut(tween(Motion.DURATION_SHORT_MS, easing = Motion.emphasized)) },
            popEnterTransition = { fadeIn(tween(Motion.DURATION_SHORT_MS, easing = Motion.emphasized)) },
            popExitTransition = { fadeOut(tween(Motion.DURATION_SHORT_MS, easing = Motion.emphasized)) },
        ) {
            composable(NestlingDestination.TODAY.route) {
                TodayScreen(
                    state = state,
                    nowMillis = nowMillis,
                    contentPadding = padding,
                    onEvent = { type ->
                        if (type == EventType.DIAPER) sheets.openDiaper() else actions.onToggleTimer(type)
                    },
                    onStopTimer = actions.onStopTimer,
                    onDiscardTimer = actions.onDiscardTimer,
                    onEventClick = { event -> openDetail(event, sheets, state) },
                )
            }
            composable(NestlingDestination.HISTORY.route) {
                HistoryScreen(
                    state = state,
                    nowMillis = nowMillis,
                    contentPadding = padding,
                    onQueryChange = actions.onQueryChange,
                    onShiftDay = actions.onShiftDay,
                    onJumpToToday = { actions.onSelectDate(state.today) },
                    onEventClick = { event -> openDetail(event, sheets, state) },
                )
            }
        }
    }

    when (sheets.kind) {
        SheetController.QUICK_LOG -> QuickLogSheet(
            onDismiss = { sheets.close() },
            onEvent = { type ->
                if (type == EventType.DIAPER) {
                    sheets.openDiaper()
                } else {
                    sheets.close()
                    actions.onToggleTimer(type)
                }
            },
        )

        SheetController.DIAPER -> DiaperSheet(
            onDismiss = { sheets.close() },
            onPick = { kind ->
                sheets.close()
                actions.onLogDiaper(kind)
            },
        )

        SheetController.AMOUNT -> AmountSheet(
            initialAmountMl = sheets.amountMl.takeIf { it > 0 } ?: state.settings.lastAmountMl,
            initialSide = state.settings.lastSide,
            unit = state.unit,
            onDismiss = { sheets.close() },
            onSave = { amount, side ->
                val id = sheets.eventId
                sheets.close()
                if (id > 0) actions.onSaveAmount(id, amount, side)
            },
            onDelete = {
                val id = sheets.eventId
                sheets.close()
                if (id > 0) actions.onDeleteEvent(id)
            },
        )

        else -> Unit
    }

    if (renaming) {
        RenameDialog(
            initialName = state.settings.babyName,
            onDismiss = { renaming = false },
            onConfirm = { name ->
                renaming = false
                actions.onRename(name)
            },
        )
    }
}

private fun openDetail(event: BabyEvent, sheets: SheetController, state: NestlingUiState) {
    sheets.openAmount(event.id, event.amountMl ?: state.settings.lastAmountMl)
}

@Composable
private fun RowScope.NavigationItem(
    destination: NestlingDestination,
    current: NestlingDestination,
    navController: NavHostController,
) {
    NavigationBarItem(
        selected = current == destination,
        onClick = {
            if (current != destination) {
                navController.navigate(destination.route) {
                    popUpTo(NestlingDestination.TODAY.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        },
        icon = {
            Icon(
                painter = painterResource(destination.icon),
                contentDescription = null,
            )
        },
        label = { Text(stringResource(destination.label)) },
    )
}

@Composable
private fun NestlingTopBar(
    state: NestlingUiState,
    destination: NestlingDestination,
    onToggleNightMode: (Boolean) -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onSetUnit: (VolumeUnit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val night = state.settings.nightMode
    val subtitle = when (destination) {
        NestlingDestination.TODAY -> stringResource(R.string.timeline_today) +
            " · " + TimeFormat.day(state.today, state.today)

        NestlingDestination.HISTORY -> stringResource(R.string.nav_history)
    }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = state.settings.babyName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
        actions = {
            IconButton(
                onClick = { onToggleNightMode(!night) },
                modifier = Modifier.testTag(TestTags.NIGHT_TOGGLE),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_night),
                    contentDescription = stringResource(
                        if (night) R.string.cd_night_mode_off else R.string.cd_night_mode_on,
                    ),
                    tint = if (night) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_export)) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onExport()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_rename)) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.unit == VolumeUnit.ML) {
                                        R.string.menu_units_oz
                                    } else {
                                        R.string.menu_units_ml
                                    },
                                ),
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onSetUnit(if (state.unit == VolumeUnit.ML) VolumeUnit.OZ else VolumeUnit.ML)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
