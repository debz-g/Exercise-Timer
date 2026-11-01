package com.exercise.timer.domain

import com.exercise.timer.domain.model.IntervalProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalProgressTest {

    @Test
    fun `at the start the whole interval is left`() {
        val p = IntervalProgress.at(0, 10_000)
        assertEquals(0, p.completedIntervals)
        assertEquals(1, p.round)
        assertEquals(10_000, p.remainingMillis)
        assertEquals(10, p.displaySeconds)
    }

    @Test
    fun `display rounds up so it never reads zero before the beep`() {
        assertEquals(10, IntervalProgress.at(1, 10_000).displaySeconds)
        assertEquals(1, IntervalProgress.at(9_999, 10_000).displaySeconds)
    }

    @Test
    fun `an interval boundary starts the next round at full time`() {
        val p = IntervalProgress.at(20_000, 10_000)
        assertEquals(2, p.completedIntervals)
        assertEquals(3, p.round)
        assertEquals(10_000, p.remainingMillis)
    }

    @Test
    fun `next display change lands on the next whole second`() {
        assertEquals(1_000, IntervalProgress.at(0, 10_000).millisUntilDisplayChange)
        assertEquals(750, IntervalProgress.at(250, 10_000).millisUntilDisplayChange)
    }
}
