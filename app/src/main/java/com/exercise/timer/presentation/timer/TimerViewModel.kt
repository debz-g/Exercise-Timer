package com.exercise.timer.presentation.timer

import androidx.lifecycle.viewModelScope
import com.exercise.timer.core.mvi.MviViewModel
import com.exercise.timer.domain.model.TimerConfig
import com.exercise.timer.domain.model.TimerStatus
import com.exercise.timer.domain.usecase.ObserveTimerConfigUseCase
import com.exercise.timer.domain.usecase.ObserveTimerStatusUseCase
import com.exercise.timer.domain.usecase.SaveTimerConfigUseCase
import com.exercise.timer.domain.usecase.SetSleepTimerUseCase
import com.exercise.timer.domain.usecase.StartTimerUseCase
import com.exercise.timer.domain.usecase.StopTimerUseCase
import com.exercise.timer.presentation.timer.TimerUiState.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    observeConfig: ObserveTimerConfigUseCase,
    observeStatus: ObserveTimerStatusUseCase,
    private val saveConfig: SaveTimerConfigUseCase,
    private val startTimer: StartTimerUseCase,
    private val stopTimer: StopTimerUseCase,
    private val setSleepTimer: SetSleepTimerUseCase,
) : MviViewModel<TimerUiState, TimerIntent, TimerEffect>(TimerUiState()) {

    init {
        viewModelScope.launch {
            observeConfig().collect { config ->
                // While running, the session's own config is what's on screen.
                if (!currentState.isRunning) setState { copy(config = config) }
            }
        }
        viewModelScope.launch {
            var wasRunning = false
            observeStatus().collect { status ->
                when (status) {
                    is TimerStatus.Running -> {
                        wasRunning = true
                        setState { copy(config = status.config, session = status.toSession()) }
                    }
                    is TimerStatus.Idle -> {
                        setState { copy(session = Session.Idle) }
                        if (wasRunning && status.endedBySleepTimer) sendEffect(TimerEffect.SleepTimerEndedSession)
                        wasRunning = false
                    }
                }
            }
        }
    }

    override fun handleIntent(intent: TimerIntent) {
        when (intent) {
            is TimerIntent.SelectInterval -> chooseInterval(intent.seconds)
            TimerIntent.OpenCustomInterval -> setState { copy(customSheetOpen = true) }
            TimerIntent.DismissCustomInterval -> setState { copy(customSheetOpen = false) }
            is TimerIntent.ConfirmCustomInterval -> {
                setState { copy(customSheetOpen = false) }
                chooseInterval(intent.seconds)
            }
            is TimerIntent.SetSleepTimer -> {
                val config = currentState.config
                setState { copy(config = config.copy(sleepTimerEnabled = intent.enabled)) }
                viewModelScope.launch { setSleepTimer(config, intent.enabled) }
            }
            TimerIntent.Start -> viewModelScope.launch { startTimer(currentState.config) }
            TimerIntent.Stop -> stopTimer()
            is TimerIntent.LiveUpdatesChecked -> setState { copy(liveUpdatesAllowed = intent.allowed) }
            TimerIntent.AllowLiveUpdates -> sendEffect(TimerEffect.OpenLiveUpdateSettings)
        }
    }

    private fun chooseInterval(seconds: Int) {
        if (currentState.isRunning) return
        val clamped = seconds.coerceIn(TimerConfig.MIN_INTERVAL_SECONDS, TimerConfig.MAX_INTERVAL_SECONDS)
        val config = currentState.config.copy(intervalSeconds = clamped)
        setState { copy(config = config) }
        viewModelScope.launch { saveConfig(config) }
    }

    private fun TimerStatus.Running.toSession() = Session.Running(
        intervalSeconds = config.intervalSeconds,
        round = round,
        remainingSeconds = (remainingMillis + 999) / 1_000,
        roundEndsAt = roundEndsAt,
        sleepMinutesLeft = sleepRemainingMillis?.let { ((it + 59_999) / 60_000).toInt() },
    )
}
