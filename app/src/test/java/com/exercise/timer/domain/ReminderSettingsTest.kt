package com.exercise.timer.domain

import com.exercise.timer.domain.model.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderSettingsTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val reminder = ReminderSettings(enabled = true, time = LocalTime.of(7, 30))

    @Test
    fun `later today when the time hasn't passed`() {
        val now = ZonedDateTime.of(2026, 10, 31, 6, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 10, 31, 7, 30, 0, 0, zone), reminder.nextTrigger(now))
    }

    @Test
    fun `tomorrow when the time has passed or is now`() {
        val tomorrow = ZonedDateTime.of(2026, 11, 1, 7, 30, 0, 0, zone)
        assertEquals(tomorrow, reminder.nextTrigger(ZonedDateTime.of(2026, 10, 31, 7, 30, 0, 0, zone)))
        assertEquals(tomorrow, reminder.nextTrigger(ZonedDateTime.of(2026, 10, 31, 22, 0, 0, 0, zone)))
    }
}
