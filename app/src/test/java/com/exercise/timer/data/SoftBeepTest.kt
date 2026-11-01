package com.exercise.timer.data.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SoftBeepTest {

    @Test
    fun `is one short beep that is audible but far from full scale`() {
        val samples = SoftBeep.samples()
        assertEquals(SoftBeep.SAMPLE_RATE * SoftBeep.DURATION_MS / 1_000, samples.size)

        val peak = samples.maxOf { abs(it.toInt()) }
        assertTrue("peak $peak should stay below 60% of full scale", peak < Short.MAX_VALUE * 0.6)
        assertTrue("peak $peak should still be clearly audible", peak > Short.MAX_VALUE * 0.3)
    }

    @Test
    fun `fades in and out so the beep cannot click`() {
        val samples = SoftBeep.samples()

        assertEquals(0, samples.first().toInt())
        assertEquals(0, samples.last().toInt())
        // Two samples in, the attack has barely opened; a jump here would be an audible click.
        assertTrue(abs(samples[2].toInt()) < Short.MAX_VALUE / 20)
    }

    @Test
    fun `tone is the intended pitch`() {
        val samples = SoftBeep.samples()
        val crossings = (1 until samples.size).count { (samples[it - 1] < 0) != (samples[it] < 0) }

        // 660 Hz over 300 ms is 396 sign changes.
        assertTrue("expected about 396 sign changes, got $crossings", abs(crossings - 396) <= 2)
    }
}
