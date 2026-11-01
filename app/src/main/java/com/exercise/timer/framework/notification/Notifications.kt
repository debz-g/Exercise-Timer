package com.exercise.timer.framework.notification

import android.app.Notification
import android.media.AudioAttributes
import android.net.Uri
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.os.Build
import com.exercise.timer.MainActivity
import com.exercise.timer.R
import com.exercise.timer.domain.model.TimerStatus
import com.exercise.timer.domain.reminder.ReminderNotifier
import com.exercise.timer.framework.timer.TimerService
import com.exercise.timer.core.format.formatInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The app's two notifications: the running timer, and the daily reminder. */
@Singleton
class Notifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderNotifier {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_TIMER,
                    context.getString(R.string.channel_timer),
                    // High (but silent) is the tested config for Live Update promotion on Samsung;
                    // LOW channels are silently left unpromoted.
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.channel_timer_description)
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_REMINDER,
                    context.getString(R.string.channel_reminder),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.channel_reminder_description)
                    setSound(
                        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.reminder_chime}"),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                },
            )
        )
        // A channel's sound can't change once created, so the chime lives on a new channel.
        OLD_CHANNELS.forEach(system::deleteNotificationChannel)
    }

    /** The ongoing notification while a session runs, with a live countdown and a Stop button. */
    fun timer(status: TimerStatus.Running): Notification {
        val interval = formatInterval(context, status.config.intervalSeconds)
        val text = buildString {
            append(context.getString(R.string.notification_timer_text, interval))
            status.sleepRemainingMillis?.let {
                append(" · ")
                append(context.getString(R.string.notification_timer_sleep, ((it + 59_999) / 60_000).toInt()))
            }
        }
        val stop = PendingIntent.getService(
            context,
            0,
            Intent(context, TimerService::class.java).setAction(TimerService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.notification_timer_title, status.round))
            .setContentText(text)
            .setContentIntent(openApp())
            .setOngoing(true)
            // Quiet via the soundless channel, not setSilent(): Samsung files SILENT-flagged
            // notifications in the silent section, which is never promoted to a Live Update.
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Accent for the progress bar; the notification is never colorized (that blocks promotion).
            .setColor(ContextCompat.getColor(context, R.color.launcher_foreground))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(R.drawable.ic_stop, context.getString(R.string.action_stop), stop)
            .apply {
                // A Live Update (lock screen / Now Bar, status bar chip). Promotion silently fails
                // unless BOTH the request and a ProgressStyle are present.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    setRequestPromotedOngoing(true)
                    setStyle(intervalProgress(status))
                }
            }
            .apply {
                // A live countdown to the next beep; too jumpy to be useful for very short intervals,
                // where the chip shows the round instead.
                if (status.config.intervalSeconds >= LIVE_COUNTDOWN_MIN_SECONDS) {
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                    setWhen(System.currentTimeMillis() + status.remainingMillis)
                    setShowWhen(true)
                } else {
                    setShortCriticalText(context.getString(R.string.notification_timer_chip, status.round))
                }
            }
            .build()
    }

    /** One segment per interval that drains toward the next beep. Must be styled-by-progress, or it draws full. */
    private fun intervalProgress(status: TimerStatus.Running): NotificationCompat.ProgressStyle {
        val total = status.config.intervalSeconds
        return NotificationCompat.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressSegments(
                listOf(
                    NotificationCompat.ProgressStyle.Segment(total)
                        .setColor(ContextCompat.getColor(context, R.color.launcher_foreground))
                )
            )
            .setProgress((((status.remainingMillis + 999) / 1_000).toInt()).coerceIn(0, total))
    }

    override fun showReminder() {
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_fitness_center)
            .setContentTitle(context.getString(R.string.notification_reminder_title))
            .setContentText(context.getString(R.string.notification_reminder_text))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(ID_REMINDER, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post.
        }
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_TIMER = "timer_live"
        const val CHANNEL_REMINDER = "reminder_chime"
        private val OLD_CHANNELS = listOf("reminder", "timer")
        const val ID_TIMER = 1
        const val ID_REMINDER = 2
        private const val LIVE_COUNTDOWN_MIN_SECONDS = 5
    }
}
