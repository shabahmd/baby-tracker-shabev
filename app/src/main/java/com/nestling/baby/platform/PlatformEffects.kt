package com.nestling.baby.platform

import android.content.Context
import com.nestling.baby.domain.EventType
import com.nestling.baby.timer.TimerService
import com.nestling.baby.widget.QuickLogWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Side effects that need a Context (foreground service, widget refresh). Keeping them
 * behind an interface is what lets the ViewModel be tested on the JVM with no Android.
 */
interface PlatformEffects {
    fun timerStarted(type: EventType)

    fun timerStopped()

    fun dataChanged()

    object None : PlatformEffects {
        override fun timerStarted(type: EventType) = Unit

        override fun timerStopped() = Unit

        override fun dataChanged() = Unit
    }
}

class AndroidPlatformEffects(private val context: Context) : PlatformEffects {

    override fun timerStarted(type: EventType) {
        TimerService.start(context)
        refreshWidget()
    }

    override fun timerStopped() {
        TimerService.stop(context)
        refreshWidget()
    }

    override fun dataChanged() = refreshWidget()

    private fun refreshWidget() {
        scope.launch {
            runCatching { QuickLogWidget().updateAll(context) }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
