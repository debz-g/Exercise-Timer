package com.exercise.timer.domain.usecase

import com.exercise.timer.domain.model.TimerConfig
import com.exercise.timer.domain.model.TimerStatus
import com.exercise.timer.domain.repository.TimerPreferencesRepository
import com.exercise.timer.domain.repository.TimerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveTimerStatusUseCase @Inject constructor(private val timer: TimerRepository) {
    operator fun invoke(): StateFlow<TimerStatus> = timer.status
}

class ObserveTimerConfigUseCase @Inject constructor(private val preferences: TimerPreferencesRepository) {
    operator fun invoke(): Flow<TimerConfig> = preferences.config
}

/** Remembers Dad's choice without starting anything. */
class SaveTimerConfigUseCase @Inject constructor(private val preferences: TimerPreferencesRepository) {
    suspend operator fun invoke(config: TimerConfig) = preferences.save(config)
}

class StartTimerUseCase @Inject constructor(
    private val timer: TimerRepository,
    private val preferences: TimerPreferencesRepository,
) {
    suspend operator fun invoke(config: TimerConfig) {
        timer.start(config)
        preferences.save(config)
    }
}

class StopTimerUseCase @Inject constructor(private val timer: TimerRepository) {
    operator fun invoke() = timer.stop()
}

/** Changes the sleep timer: remembered for next time, and applied to a running session. */
class SetSleepTimerUseCase @Inject constructor(
    private val timer: TimerRepository,
    private val preferences: TimerPreferencesRepository,
) {
    suspend operator fun invoke(current: TimerConfig, enabled: Boolean) {
        if (timer.status.value is TimerStatus.Running) timer.setSleepTimer(enabled)
        preferences.save(current.copy(sleepTimerEnabled = enabled))
    }
}
