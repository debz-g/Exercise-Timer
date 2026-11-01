package com.exercise.timer.domain.model

/**
 * The repeating countdown. Timestamps are on the monotonic clock (`SystemClock.elapsedRealtime`),
 * so the UI can animate between updates without drifting.
 */
sealed interface TimerStatus {

    /** Nothing running. [endedBySleepTimer] is true when the last session was ended by the sleep timer. */
    data class Idle(val endedBySleepTimer: Boolean = false) : TimerStatus

    data class Running(
        val config: TimerConfig,
        /** 1-based: the interval currently counting down. */
        val round: Long,
        val remainingMillis: Long,
        /** When the current interval ends (and beeps). */
        val roundEndsAt: Long,
        /** When the sleep timer ends the session, or null when it is off. */
        val sleepEndsAt: Long?,
        /** Time left on the sleep timer, or null when it is off. */
        val sleepRemainingMillis: Long?,
    ) : TimerStatus
}
