package com.nestling.baby.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import com.nestling.baby.MainActivity
import com.nestling.baby.R
import com.nestling.baby.di.Graph
import com.nestling.baby.domain.ActiveTimer
import com.nestling.baby.domain.EventType
import com.nestling.baby.timer.TimerService
import com.nestling.baby.ui.QuickAction
import androidx.compose.ui.unit.dp

/**
 * A 1x1 home-screen widget with the three event buttons. Bottle and Sleep start and
 * stop the timer without opening the app at all; Diaper opens straight onto the picker.
 */
class QuickLogWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val running = runCatching { Graph.repository(context).currentTimer() }.getOrNull()
        provideContent {
            GlanceTheme {
                WidgetBody(running)
            }
        }
    }
}

@Composable
private fun WidgetBody(running: ActiveTimer?) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(4.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        EventType.entries.forEach { type ->
            val active = running?.type == type
            val background = when {
                active -> GlanceTheme.colors.primary
                else -> GlanceTheme.colors.secondaryContainer
            }
            val tint = when {
                active -> GlanceTheme.colors.onPrimary
                else -> GlanceTheme.colors.onSecondaryContainer
            }
            val action = if (type == EventType.DIAPER) {
                actionStartActivity(
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra(QuickAction.EXTRA, QuickAction.DIAPER),
                )
            } else {
                actionRunCallback<ToggleTimerAction>(
                    actionParametersOf(ToggleTimerAction.typeKey to type.name),
                )
            }
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(2.dp)
                    .clickable(action),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(background)
                        .cornerRadius(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(type.widgetIcon()),
                        contentDescription = context.getString(type.widgetLabel()),
                        colorFilter = ColorFilter.tint(tint),
                        modifier = GlanceModifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun EventType.widgetIcon(): Int = when (this) {
    EventType.BOTTLE -> R.drawable.ic_bottle
    EventType.SLEEP -> R.drawable.ic_sleep
    EventType.DIAPER -> R.drawable.ic_diaper
}

private fun EventType.widgetLabel(): Int = when (this) {
    EventType.BOTTLE -> R.string.cd_log_bottle
    EventType.SLEEP -> R.string.cd_log_sleep
    EventType.DIAPER -> R.string.cd_log_diaper
}

/** Start or stop a timer straight from the home screen. */
class ToggleTimerAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val type = EventType.fromStorage(parameters[typeKey])
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
                repository.stopTimer()
                repository.startTimer(type)
                TimerService.start(context)
            }
        }
        QuickLogWidget().updateAll(context)
    }

    companion object {
        val typeKey = ActionParameters.Key<String>("event_type")
    }
}

class QuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickLogWidget()
}
