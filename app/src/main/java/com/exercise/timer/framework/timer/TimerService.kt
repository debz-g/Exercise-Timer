package com.exercise.timer.framework.timer

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.exercise.timer.data.timer.TimerKeepAlive
import com.exercise.timer.domain.model.TimerStatus
import com.exercise.timer.domain.repository.TimerRepository
import com.exercise.timer.framework.notification.Notifications
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

/**
 * Keeps the timer beeping with the screen off or the app closed: a foreground service with the
 * ongoing notification, plus a partial wake lock so the CPU stays awake between beeps. It stops
 * itself as soon as the session ends.
 */
@AndroidEntryPoint
class TimerService : LifecycleService() {

    @Inject lateinit var timer: TimerRepository
    @Inject lateinit var notifications: Notifications

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            timer.stop()
            stopSelf()
            return START_NOT_STICKY
        }
        val running = timer.status.value as? TimerStatus.Running
        if (running == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Must be called promptly after startForegroundService, before anything else.
        goForeground(notifications.timer(running))
        acquireWakeLock()
        observe()
        return START_NOT_STICKY
    }

    private var observing = false

    private fun observe() {
        if (observing) return
        observing = true
        lifecycleScope.launch {
            timer.status
                // Repost often enough for the Live Update's progress bar (~30 steps per interval),
                // not on every tick of a long interval. The countdown chip ticks by itself.
                .distinctUntilChangedBy { status ->
                    (status as? TimerStatus.Running)?.let {
                        val step = (it.config.intervalSeconds / 30).coerceAtLeast(1)
                        listOf(it.round, it.config, it.sleepRemainingMillis?.div(60_000), (it.remainingMillis + 999) / 1_000 / step)
                    }
                }
                .collect { status ->
                    when (status) {
                        is TimerStatus.Running -> notify(notifications.timer(status))
                        is TimerStatus.Idle -> {
                            ServiceCompat.stopForeground(this@TimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    }
                }
        }
    }

    private fun goForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, Notifications.ID_TIMER, notification, type)
    }

    private fun notify(notification: Notification) {
        try {
            NotificationManagerCompat.from(this).notify(Notifications.ID_TIMER, notification)
        } catch (_: SecurityException) {
            // No notification permission: the service still runs, the notification just isn't shown.
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ExerciseTimer:session")
            .apply { acquire(MAX_WAKE_LOCK.inWholeMilliseconds) }
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.exercise.timer.action.STOP"

        /** A safety net in case a session is never stopped. */
        private val MAX_WAKE_LOCK = 12.hours
    }
}

@Singleton
class ServiceTimerKeepAlive @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TimerKeepAlive {
    override fun sessionStarted() {
        ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
    }
}
