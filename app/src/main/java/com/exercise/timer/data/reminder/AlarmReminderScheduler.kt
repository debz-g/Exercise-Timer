package com.exercise.timer.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.exercise.timer.domain.model.ReminderSettings
import com.exercise.timer.domain.reminder.ReminderScheduler
import com.exercise.timer.framework.reminder.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * One alarm at a time: when it fires, [ReminderReceiver] shows the notification and schedules the
 * next day's. Exact when the system allows it, otherwise within a few minutes.
 */
@Singleton
class AlarmReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderScheduler {

    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun scheduleNext(settings: ReminderSettings) {
        val trigger = settings.nextTrigger(ZonedDateTime.now()).toInstant().toEpochMilli()
        val intent = pendingIntent()
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        } else {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, trigger, INEXACT_WINDOW, intent)
        }
    }

    override fun cancel() {
        alarms.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 1
        val INEXACT_WINDOW = 5.minutes.inWholeMilliseconds
    }
}
