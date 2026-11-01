package com.exercise.timer.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.exercise.timer.domain.model.ReminderSettings
import com.exercise.timer.domain.model.TimerConfig
import com.exercise.timer.domain.repository.ReminderRepository
import com.exercise.timer.domain.repository.TimerPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private object Keys {
    val IntervalSeconds = intPreferencesKey("interval_seconds")
    val SleepTimer = booleanPreferencesKey("sleep_timer")
    val ReminderEnabled = booleanPreferencesKey("reminder_enabled")
    val ReminderHour = intPreferencesKey("reminder_hour")
    val ReminderMinute = intPreferencesKey("reminder_minute")
}

@Singleton
class DataStoreTimerPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : TimerPreferencesRepository {

    override val config: Flow<TimerConfig> = dataStore.data
        .map { prefs ->
            val seconds = (prefs[Keys.IntervalSeconds] ?: TimerConfig.DEFAULT_INTERVAL_SECONDS)
                .coerceIn(TimerConfig.MIN_INTERVAL_SECONDS, TimerConfig.MAX_INTERVAL_SECONDS)
            TimerConfig(seconds, prefs[Keys.SleepTimer] ?: false)
        }
        .distinctUntilChanged()

    override suspend fun save(config: TimerConfig) {
        dataStore.edit {
            it[Keys.IntervalSeconds] = config.intervalSeconds
            it[Keys.SleepTimer] = config.sleepTimerEnabled
        }
    }
}

@Singleton
class DataStoreReminderRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ReminderRepository {

    override val settings: Flow<ReminderSettings> = dataStore.data
        .map { prefs ->
            val default = ReminderSettings.Default
            ReminderSettings(
                enabled = prefs[Keys.ReminderEnabled] ?: default.enabled,
                time = LocalTime.of(
                    (prefs[Keys.ReminderHour] ?: default.time.hour).coerceIn(0, 23),
                    (prefs[Keys.ReminderMinute] ?: default.time.minute).coerceIn(0, 59),
                ),
            )
        }
        .distinctUntilChanged()

    override suspend fun save(settings: ReminderSettings) {
        dataStore.edit {
            it[Keys.ReminderEnabled] = settings.enabled
            it[Keys.ReminderHour] = settings.time.hour
            it[Keys.ReminderMinute] = settings.time.minute
        }
    }
}
