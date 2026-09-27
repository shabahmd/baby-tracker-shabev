package com.nestling.baby.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.nestling.baby.MainActivity
import com.nestling.baby.R
import com.nestling.baby.di.Graph
import com.nestling.baby.domain.ActiveTimer
import com.nestling.baby.domain.EventType
import com.nestling.baby.timer.TimerService
import com.nestling.baby.ui.QuickAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The three buttons, on the home screen.
 *
 * Deliberately plain [RemoteViews] rather than Glance: Glance drags in WorkManager,
 * which adds WAKE_LOCK and RECEIVE_BOOT_COMPLETED to the manifest. "Nestling asks for
 * three permissions" is a promise worth a hundred lines of RemoteViews.
 *
 * Bottle and Sleep toggle the timer without opening the app at all. Diaper opens the
 * app straight onto the picker, because it needs one more decision from the parent.
 */
class QuickLogWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        scope.launch {
            val running = runCatching { Graph.repository(context).currentTimer() }.getOrNull()
            val views = buildViews(context, running)
            runCatching {
                appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
            }
            pending.finish()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) {
            super.onReceive(context, intent)
            return
        }
        val type = EventType.fromStorage(intent.getStringExtra(EXTRA_TYPE))
        val pending = goAsync()
        scope.launch {
            runCatching { toggle(context, type) }
            refreshQuickLogWidget(context)
            pending.finish()
        }
    }

    /** Same rule as the in-app buttons: tap to start, tap again to stop and save. */
    private suspend fun toggle(context: Context, type: EventType) {
        val repository = Graph.repository(context)
        val running = repository.currentTimer()
        when {
            running == null -> {
                repository.startTimer(type)
                TimerService.start(context)
            }

            running.type == type -> {
                repository.stopTimer()
                TimerService.stop(context)
            }

            else -> {
                // Switching type saves the first timer before starting the second.
                repository.stopTimer()
                repository.startTimer(type)
                TimerService.start(context)
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

private const val ACTION_TOGGLE = "com.nestling.baby.action.WIDGET_TOGGLE"
private const val EXTRA_TYPE = "com.nestling.baby.extra.WIDGET_TYPE"

/** Redraw every placed widget. Cheap, and the only way state ever reaches them. */
fun refreshQuickLogWidget(context: Context) {
    val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
    val component = ComponentName(context, QuickLogWidgetReceiver::class.java)
    val ids = runCatching { manager.getAppWidgetIds(component) }.getOrNull() ?: return
    if (ids.isEmpty()) return
    val intent = Intent(context, QuickLogWidgetReceiver::class.java)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    runCatching { context.sendBroadcast(intent) }
}

private fun buildViews(context: Context, running: ActiveTimer?): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_quick_log)
    views.configure(context, R.id.widget_bottle, EventType.BOTTLE, running)
    views.configure(context, R.id.widget_sleep, EventType.SLEEP, running)
    views.configure(context, R.id.widget_diaper, EventType.DIAPER, running)
    return views
}

private fun RemoteViews.configure(
    context: Context,
    viewId: Int,
    type: EventType,
    running: ActiveTimer?,
) {
    val active = running?.type == type
    setInt(
        viewId,
        "setBackgroundResource",
        if (active) R.drawable.widget_button_background_active else R.drawable.widget_button_background,
    )
    setInt(
        viewId,
        "setColorFilter",
        ContextCompat.getColor(
            context,
            if (active) R.color.widget_icon_active else R.color.widget_icon,
        ),
    )
    setContentDescription(viewId, context.getString(type.description(active)))
    setOnClickPendingIntent(viewId, type.pendingIntent(context))
}

private fun EventType.description(active: Boolean): Int = when {
    active && this == EventType.SLEEP -> R.string.cd_stop_sleep
    active -> R.string.cd_stop_bottle
    this == EventType.BOTTLE -> R.string.cd_log_bottle
    this == EventType.SLEEP -> R.string.cd_log_sleep
    else -> R.string.cd_log_diaper
}

private fun EventType.pendingIntent(context: Context): PendingIntent {
    val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    return if (this == EventType.DIAPER) {
        PendingIntent.getActivity(
            context,
            ordinal,
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(QuickAction.EXTRA, QuickAction.DIAPER),
            flags,
        )
    } else {
        PendingIntent.getBroadcast(
            context,
            ordinal,
            Intent(context, QuickLogWidgetReceiver::class.java)
                .setAction(ACTION_TOGGLE)
                .putExtra(EXTRA_TYPE, name),
            flags,
        )
    }
}
