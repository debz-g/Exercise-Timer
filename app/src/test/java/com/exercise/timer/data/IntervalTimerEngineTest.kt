package com.exercise.timer.data

import com.exercise.timer.data.timer.IntervalTimerEngine
import com.exercise.timer.domain.model.TimerConfig
import com.exercise.timer.domain.model.TimerStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class IntervalTimerEngineTest {

    private var beeps = 0
    private var keepAliveStarts = 0

    private fun TestScope.engine() = IntervalTimerEngine(
        scope = backgroundScope,
        clock = { testScheduler.currentTime },
        alert = { beeps++ },
        keepAlive = { keepAliveStarts++ },
    )

    @Test
    fun `beeps at the end of every interval and keeps going`() = runTest {
        val engine = engine()
        engine.start(TimerConfig(intervalSeconds = 10, sleepTimerEnabled = false))
        runCurrent()
        assertEquals(1, keepAliveStarts)

        advanceTimeBy(9_999); runCurrent()
        assertEquals(0, beeps)
        advanceTimeBy(1); runCurrent()
        assertEquals(1, beeps)
        val status = engine.status.value as TimerStatus.Running
        assertEquals(2, status.round)
        assertEquals(10_000, status.remainingMillis)

        advanceTimeBy(30_000); runCurrent()
        assertEquals(4, beeps)
    }

    @Test
    fun `stop ends the session and the beeping`() = runTest {
        val engine = engine()
        engine.start(TimerConfig(1, sleepTimerEnabled = false))
        advanceTimeBy(3_000); runCurrent()
        engine.stop()
        val before = beeps
        advanceTimeBy(10_000); runCurrent()
        assertEquals(before, beeps)
        assertEquals(TimerStatus.Idle(endedBySleepTimer = false), engine.status.value)
    }

    @Test
    fun `sleep timer ends the session after an hour`() = runTest {
        val engine = engine()
        engine.start(TimerConfig(60, sleepTimerEnabled = true))
        advanceTimeBy(59.minutes.inWholeMilliseconds); runCurrent()
        assertTrue(engine.status.value is TimerStatus.Running)
        advanceTimeBy(1.minutes.inWholeMilliseconds); runCurrent()
        assertEquals(TimerStatus.Idle(endedBySleepTimer = true), engine.status.value)
        assertEquals(60, beeps)
        advanceTimeBy(10.minutes.inWholeMilliseconds); runCurrent()
        assertEquals(60, beeps)
    }

    @Test
    fun `turning the sleep timer on mid-session gives a fresh hour`() = runTest {
        val engine = engine()
        engine.start(TimerConfig(30, sleepTimerEnabled = false))
        advanceTimeBy(30.minutes.inWholeMilliseconds); runCurrent()
        engine.setSleepTimer(true)
        advanceTimeBy(59.minutes.inWholeMilliseconds); runCurrent()
        assertTrue(engine.status.value is TimerStatus.Running)
        advanceTimeBy(1.minutes.inWholeMilliseconds); runCurrent()
        assertEquals(TimerStatus.Idle(endedBySleepTimer = true), engine.status.value)
    }
}
