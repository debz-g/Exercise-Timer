package com.exercise.timer.domain.repository

import com.exercise.timer.domain.model.ReminderSettings
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    val settings: Flow<ReminderSettings>

    suspend fun save(settings: ReminderSettings)
}
