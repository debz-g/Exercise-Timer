package com.exercise.timer.domain.model

import java.time.LocalTime
import java.time.ZonedDateTime

/** The daily "time to exercise" notification. */
data class ReminderSettings(
    val enabled: Boolean,
    val time: LocalTime,
) {
    /** The next time the reminder should fire, strictly after [now]. */
    fun nextTrigger(now: ZonedDateTime): ZonedDateTime {
        val today = now.with(time.withSecond(0).withNano(0))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    companion object {
        val Default = ReminderSettings(enabled = false, time = LocalTime.of(8, 0))
    }
}
