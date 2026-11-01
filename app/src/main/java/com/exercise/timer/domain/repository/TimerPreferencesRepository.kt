package com.exercise.timer.domain.repository

import com.exercise.timer.domain.model.TimerConfig
import kotlinx.coroutines.flow.Flow

/** The last interval and sleep-timer choice, so the app opens the way Dad left it. */
interface TimerPreferencesRepository {
    val config: Flow<TimerConfig>

    suspend fun save(config: TimerConfig)
}
