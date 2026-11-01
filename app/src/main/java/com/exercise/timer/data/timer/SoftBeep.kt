package com.exercise.timer.data.timer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * PCM for the end-of-interval signal: one soft, bell-like sine beep.
 *
 * A pure sine has no harsh overtones, a raised-cosine attack and a long exponential tail keep the edges
 * smooth, and the peak sits well below full scale. Pure Kotlin so the shape can be unit-tested without
 * a device.
 */
internal object SoftBeep {

    const val SAMPLE_RATE = 44_100
    const val DURATION_MS = 300

    private const val FREQUENCY_HZ = 660.0
    private const val PEAK = 0.5
    private const val ATTACK_MS = 20.0
    private const val RELEASE_MS = 15.0
    private const val DECAY_MS = 110.0

    fun samples(): ShortArray {
        val total = SAMPLE_RATE * DURATION_MS / 1_000
        val attack = (SAMPLE_RATE * ATTACK_MS / 1_000).toInt()
        val release = (SAMPLE_RATE * RELEASE_MS / 1_000).toInt()
        val decay = SAMPLE_RATE * DECAY_MS / 1_000

        return ShortArray(total) { i ->
            val fadeIn = if (i < attack) 0.5 - 0.5 * cos(PI * i / attack) else 1.0
            val fadeOut = ((total - 1 - i) / release.toDouble()).coerceIn(0.0, 1.0)
            val tail = exp(-(i - attack).coerceAtLeast(0) / decay)
            val envelope = fadeIn * fadeOut * tail * PEAK
            val sample = sin(2 * PI * FREQUENCY_HZ * i / SAMPLE_RATE) * envelope
            (sample * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
