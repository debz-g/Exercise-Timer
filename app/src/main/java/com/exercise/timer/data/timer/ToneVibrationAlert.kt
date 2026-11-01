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

/**
 * A soft single beep plus a matching buzz.
 *
 * The beep plays on the media stream, so the phone's volume buttons set how loud it is. The buzz is
 * tagged as an alarm instead: some phones (Samsung) mute media vibration by default, and with the
 * volume turned down the buzz is the only signal left.
 */
@Singleton
class ToneVibrationAlert @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : IntervalAlert {

    private val pcm: ShortArray = SoftBeep.samples()

    private val beepAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val track: AudioTrack? by lazy {
        runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(beepAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SoftBeep.SAMPLE_RATE)
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
            val buzz = VibrationEffect.createOneShot(BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                v.vibrate(buzz, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(buzz, beepAttributes)
            }
        }
    }

    private companion object {
        /** Long enough to read as one buzz, short enough not to feel like a second signal. */
        const val BUZZ_MS = 220L
    }
}
