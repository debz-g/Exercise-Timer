package com.exercise.timer.domain.reminder

import com.exercise.timer.domain.model.ReminderSettings

/** Wakes the app at the reminder time. */
interface ReminderScheduler {
    /** Schedules the next reminder for [settings]; replaces any earlier one. */
    fun scheduleNext(settings: ReminderSettings)

    fun cancel()
}
