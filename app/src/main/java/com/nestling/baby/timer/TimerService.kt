package com.nestling.baby.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nestling.baby.MainActivity
import com.nestling.baby.R
import com.nestling.baby.di.Graph
import com.nestling.baby.domain.ActiveTimer
import com.nestling.baby.domain.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps a running feed or sleep alive and visible. The service is a convenience — the
 * truth is in Room, written on start and on every minute boundary — but it means the
 * system will not quietly kill a 40 minute nap timer, and the parent can stop the timer
 * from the notification without unlocking.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        // Promote immediately: Android gives us ~5 seconds, and the database read below
        // is async.
        promote(buildNotification(null))

        if (intent?.action == ACTION_STOP) {
            scope.launch {
                runCatching { Graph.repository(applicationContext).stopTimer() }
                stopSelf()
            }
            return START_NOT_STICKY
        }

        if (tickJob?.isActive != true) {
            tickJob = scope.launch { runTimerLoop() }
        }
        return START_STICKY
    }

    private suspend fun runTimerLoop() {
        val repository = Graph.repository(applicationContext)
        while (scope.isActive) {
            val timer = runCatching { repository.currentTimer() }.getOrNull()
            if (timer == null) {
                stopSelf()
                return
            }
            promote(buildNotification(timer))
            // The minute boundary write: a killed process still knows this feed was live.
            runCatching { repository.tick() }
            delay(TICK_INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun promote(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        }
    }

    private fun buildNotification(timer: ActiveTimer?): Notification {
        val title = when (timer?.type) {
            EventType.SLEEP -> getString(R.string.timer_running_sleep)
            else -> getString(R.string.timer_running_bottle)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(timer != null)
            .setWhen(timer?.startedAt ?: System.currentTimeMillis())
            .setUsesChronometer(timer != null)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.timer_stop), stopIntent)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_timer),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_timer_description)
            setShowBadge(false)
        }
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "nestling_timer"
        private const val NOTIFICATION_ID = 1001
        private const val TICK_INTERVAL_MS = 60_000L
        const val ACTION_STOP = "com.nestling.baby.action.STOP_TIMER"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, TimerService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, TimerService::class.java)) }
        }
    }
}
