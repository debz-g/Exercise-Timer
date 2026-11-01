package com.exercise.timer.presentation.timer

import com.exercise.timer.core.mvi.UiEffect
import com.exercise.timer.core.mvi.UiIntent
import com.exercise.timer.core.mvi.UiState
import com.exercise.timer.domain.model.TimerConfig

data class TimerUiState(
    val config: TimerConfig = TimerConfig.Default,
    val session: Session = Session.Idle,
    val customSheetOpen: Boolean = false,
    /** Whether the running timer may show as a Live Update (lock screen, Now Bar, status bar chip). */
    val liveUpdatesAllowed: Boolean = true,
) : UiState {

    val isRunning: Boolean get() = session is Session.Running

    /** The custom interval, when the current one isn't a preset. */
    val customSeconds: Int? get() = config.intervalSeconds.takeIf { it !in IntervalPresets.all }

    sealed interface Session {
        data object Idle : Session

        data class Running(
            val intervalSeconds: Int,
            val round: Long,
            val remainingSeconds: Long,
            /** When this interval ends, on `SystemClock.elapsedRealtime`, for the smooth progress ring. */
            val roundEndsAt: Long,
            /** Whole minutes left on the sleep timer (rounded up), or null when it's off. */
            val sleepMinutesLeft: Int?,
        ) : Session
    }
}

sealed interface TimerIntent : UiIntent {
    data class SelectInterval(val seconds: Int) : TimerIntent
    data object OpenCustomInterval : TimerIntent
    data object DismissCustomInterval : TimerIntent
    data class ConfirmCustomInterval(val seconds: Int) : TimerIntent
    data class SetSleepTimer(val enabled: Boolean) : TimerIntent
    data object Start : TimerIntent
    data object Stop : TimerIntent
    /** The screen checked the Live Updates setting (on every resume). */
    data class LiveUpdatesChecked(val allowed: Boolean) : TimerIntent
    data object AllowLiveUpdates : TimerIntent
}

sealed interface TimerEffect : UiEffect {
    data object SleepTimerEndedSession : TimerEffect
    /** Live Updates are a per-app switch in Settings, not a pop-up permission. */
    data object OpenLiveUpdateSettings : TimerEffect
}

/** The quick picks. Anything else is a custom interval. */
object IntervalPresets {
    val all = listOf(10, 20, 30, 60)
}
