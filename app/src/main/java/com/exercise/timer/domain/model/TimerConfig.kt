package com.exercise.timer.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/** What Dad picked: beep every [intervalSeconds], optionally stopping on its own after [SLEEP_TIMER]. */
data class TimerConfig(
    val intervalSeconds: Int,
    val sleepTimerEnabled: Boolean,
) {
    init {
        require(intervalSeconds in MIN_INTERVAL_SECONDS..MAX_INTERVAL_SECONDS) {
            "Interval must be $MIN_INTERVAL_SECONDS..$MAX_INTERVAL_SECONDS seconds, was $intervalSeconds"
        }
    }

    val intervalMillis: Long get() = intervalSeconds * 1_000L

    companion object {
        const val MIN_INTERVAL_SECONDS = 1
        const val MAX_INTERVAL_SECONDS = 99 * 60 + 59
        const val DEFAULT_INTERVAL_SECONDS = 10

        /** How long a session runs before the sleep timer ends it. */
        val SLEEP_TIMER: Duration = 1.hours

        val Default = TimerConfig(DEFAULT_INTERVAL_SECONDS, sleepTimerEnabled = false)
    }
}
