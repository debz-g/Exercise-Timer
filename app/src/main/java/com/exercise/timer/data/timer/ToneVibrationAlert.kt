package com.exercise.timer.data.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * A short double beep plus a matching buzz. Plays on the alarm stream so it is loud and still heard
 * when the ringer is silenced, which matters when the phone is across the room during exercise.
 */
@Singleton
class ToneVibrationAlert @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : IntervalAlert {

    private val pcm: ShortArray = buildDoubleBeep()

    private val alarmAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val track: AudioTrack? by lazy {
        runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(alarmAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * Short.SIZE_BYTES)
                .build()
                .also { it.write(pcm, 0, pcm.size) }
        }.getOrNull()
    }

    private val vibrator by lazy {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }

    override fun play() {
        track?.let { t ->
            runCatching {
                if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
                t.reloadStaticData()
                t.play()
            }
        }
        vibrator?.takeIf { it.hasVibrator() }?.let { v ->
            val buzz = VibrationEffect.createWaveform(longArrayOf(0, PIP_MS.toLong(), GAP_MS.toLong(), PIP_MS.toLong()), -1)
            // Tagged as an alarm: some phones (Samsung) mute "media" vibration by default.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                v.vibrate(buzz, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(buzz, alarmAttributes)
            }
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val FREQUENCY_HZ = 1_000.0
        const val PIP_MS = 160
        const val GAP_MS = 90
        const val RAMP_MS = 8

        /** Two sine pips with short fades, so they sound clean instead of clicking. */
        fun buildDoubleBeep(): ShortArray {
            val pip = SAMPLE_RATE * PIP_MS / 1_000
            val gap = SAMPLE_RATE * GAP_MS / 1_000
            val ramp = SAMPLE_RATE * RAMP_MS / 1_000
            val out = ShortArray(pip * 2 + gap)
            for (start in intArrayOf(0, pip + gap)) {
                for (i in 0 until pip) {
                    val envelope = min(1.0, min(i, pip - 1 - i) / ramp.toDouble())
                    val sample = sin(2 * PI * FREQUENCY_HZ * i / SAMPLE_RATE) * envelope * 0.9
                    out[start + i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
            }
            return out
        }
    }
}
