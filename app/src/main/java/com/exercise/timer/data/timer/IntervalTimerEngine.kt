package com.exercise.timer.data.timer

import com.exercise.timer.di.ApplicationScope
import com.exercise.timer.domain.model.IntervalProgress
import com.exercise.timer.domain.model.TimerConfig
import com.exercise.timer.domain.model.TimerStatus
import com.exercise.timer.domain.repository.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the repeating countdown: beeps at the end of every interval and starts the next one, until
 * [stop] or the sleep timer. Lives as long as the app process; [TimerKeepAlive] keeps that process
 * running with the screen off.
 *
 * Confined to the main thread (see [ApplicationScope]): calls and the loop never interleave, so a
 * cancelled session can't publish over a new one.
 */
@Singleton
class IntervalTimerEngine @Inject constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    private val clock: ElapsedClock,
    private val alert: IntervalAlert,
    private val keepAlive: TimerKeepAlive,
) : TimerRepository {

    private val _status = MutableStateFlow<TimerStatus>(TimerStatus.Idle())
    override val status: StateFlow<TimerStatus> = _status.asStateFlow()

    private var job: Job? = null

    /** When the sleep timer ends the current session; read by the loop on every tick. */
    private var sleepEndsAt: Long? = null

    override fun start(config: TimerConfig) {
        job?.cancel()
        val startedAt = clock.now()
        sleepEndsAt = if (config.sleepTimerEnabled) startedAt + TimerConfig.SLEEP_TIMER.inWholeMilliseconds else null
        // Publish Running before the service starts, so it never sees a stale Idle and quits.
        publish(config, startedAt, startedAt)
        keepAlive.sessionStarted()
        job = scope.launch {
            var beeps = 0L
            while (isActive) {
                val now = clock.now()
                val progress = IntervalProgress.at(now - startedAt, config.intervalMillis)
                // Several intervals may have passed if the tick was late; beep once, not for each.
                if (progress.completedIntervals > beeps) {
                    beeps = progress.completedIntervals
                    alert.play()
                }
                val sleepLeft = sleepEndsAt?.let { it - now }
                if (sleepLeft != null && sleepLeft <= 0) {
                    end(endedBySleepTimer = true)
                    break
                }
                publish(config, startedAt, now)
                val wait = minOf(progress.millisUntilDisplayChange, sleepLeft ?: Long.MAX_VALUE)
                delay(wait.coerceAtLeast(1))
            }
        }
    }

    override fun stop() {
        job?.cancel()
        end(endedBySleepTimer = false)
    }

    override fun setSleepTimer(enabled: Boolean) {
        val running = _status.value as? TimerStatus.Running ?: return
        val now = clock.now()
        sleepEndsAt = if (enabled) now + TimerConfig.SLEEP_TIMER.inWholeMilliseconds else null
        _status.value = running.copy(
            config = running.config.copy(sleepTimerEnabled = enabled),
            sleepEndsAt = sleepEndsAt,
            sleepRemainingMillis = sleepEndsAt?.let { it - now },
        )
    }

    private fun publish(config: TimerConfig, startedAt: Long, now: Long) {
        val progress = IntervalProgress.at(now - startedAt, config.intervalMillis)
        val sleepEnd = sleepEndsAt
        _status.value = TimerStatus.Running(
            config = config.copy(sleepTimerEnabled = sleepEnd != null),
            round = progress.round,
            remainingMillis = progress.remainingMillis,
            roundEndsAt = now + progress.remainingMillis,
            sleepEndsAt = sleepEnd,
            sleepRemainingMillis = sleepEnd?.let { (it - now).coerceAtLeast(0) },
        )
    }

    private fun end(endedBySleepTimer: Boolean) {
        job = null
        sleepEndsAt = null
        _status.value = TimerStatus.Idle(endedBySleepTimer)
    }
}
