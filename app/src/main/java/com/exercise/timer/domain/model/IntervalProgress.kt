package com.exercise.timer.domain.model

/**
 * Where a repeating countdown is after [elapsedMillis]. Computed from the start time rather than
 * by counting ticks, so a late tick never makes the timer drift.
 */
data class IntervalProgress(
    /** How many intervals have finished (and beeped). */
    val completedIntervals: Long,
    /** Time left in the current interval, in 1..intervalMillis. */
    val remainingMillis: Long,
) {
    /** 1-based number of the interval counting down now. */
    val round: Long get() = completedIntervals + 1

    /** Whole seconds to show; rounds up so the display never reads 0 before the beep. */
    val displaySeconds: Long get() = (remainingMillis + 999) / 1_000

    /** Time until [displaySeconds] changes (always also lands on the next beep). */
    val millisUntilDisplayChange: Long
        get() = (remainingMillis % 1_000).let { if (it == 0L) 1_000L else it }

    companion object {
        fun at(elapsedMillis: Long, intervalMillis: Long): IntervalProgress {
            require(intervalMillis > 0) { "intervalMillis must be positive" }
            val elapsed = elapsedMillis.coerceAtLeast(0)
            return IntervalProgress(
                completedIntervals = elapsed / intervalMillis,
                remainingMillis = intervalMillis - elapsed % intervalMillis,
            )
        }
    }
}
