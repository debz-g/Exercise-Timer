package com.exercise.timer.domain.usecase

import com.exercise.timer.domain.model.ReminderSettings
import com.exercise.timer.domain.reminder.ReminderNotifier
import com.exercise.timer.domain.reminder.ReminderScheduler
import com.exercise.timer.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ObserveReminderUseCase @Inject constructor(private val repository: ReminderRepository) {
    operator fun invoke(): Flow<ReminderSettings> = repository.settings
}

/** Saves the reminder and schedules or cancels the alarm to match. */
class UpdateReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(settings: ReminderSettings) {
        repository.save(settings)
        if (settings.enabled) scheduler.scheduleNext(settings) else scheduler.cancel()
    }
}

/** Re-arms the alarm after it fires, after a reboot, or when the clock or time zone changes. */
class RescheduleReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke() {
        val settings = repository.settings.first()
        if (settings.enabled) scheduler.scheduleNext(settings) else scheduler.cancel()
    }
}

class ShowReminderUseCase @Inject constructor(private val notifier: ReminderNotifier) {
    operator fun invoke() = notifier.showReminder()
}
