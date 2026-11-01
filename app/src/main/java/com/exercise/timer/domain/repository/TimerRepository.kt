package com.exercise.timer.domain.repository

import com.exercise.timer.domain.model.TimerConfig
import com.exercise.timer.domain.model.TimerStatus
import kotlinx.coroutines.flow.StateFlow

/** The running interval timer. It keeps going in the background until [stop] or the sleep timer. */
interface TimerRepository {
    val status: StateFlow<TimerStatus>

    /** Starts (or restarts) a session with [config]. */
    fun start(config: TimerConfig)

    fun stop()

    /** Turns the sleep timer on or off mid-session; turning it on gives the session a fresh hour. */
    fun setSleepTimer(enabled: Boolean)
}
