package com.exercise.timer.presentation.reminder

import com.exercise.timer.core.mvi.UiEffect
import com.exercise.timer.core.mvi.UiIntent
import com.exercise.timer.core.mvi.UiState
import com.exercise.timer.domain.model.ReminderSettings
import java.time.LocalTime

data class ReminderUiState(
    val enabled: Boolean = ReminderSettings.Default.enabled,
    val time: LocalTime = ReminderSettings.Default.time,
    /** Whether the app may post notifications at all; the reminder is silent without it. */
    val notificationsAllowed: Boolean = true,
    val timeSheetOpen: Boolean = false,
) : UiState

sealed interface ReminderIntent : UiIntent {
    data class SetEnabled(val enabled: Boolean) : ReminderIntent
    data object OpenTimePicker : ReminderIntent
    data object DismissTimePicker : ReminderIntent
    data class PickTime(val time: LocalTime) : ReminderIntent
    /** The screen checked the notification permission (on resume, or after asking). */
    data class NotificationPermissionChecked(val granted: Boolean) : ReminderIntent
    data object AllowNotifications : ReminderIntent
    data object SendTestReminder : ReminderIntent
}

sealed interface ReminderEffect : UiEffect {
    data object RequestNotificationPermission : ReminderEffect
    data class ReminderSaved(val time: LocalTime) : ReminderEffect
}
