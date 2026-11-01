package com.exercise.timer.presentation.reminder

import androidx.lifecycle.viewModelScope
import com.exercise.timer.core.mvi.MviViewModel
import com.exercise.timer.domain.model.ReminderSettings
import com.exercise.timer.domain.usecase.ObserveReminderUseCase
import com.exercise.timer.domain.usecase.ShowReminderUseCase
import com.exercise.timer.domain.usecase.UpdateReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    observeReminder: ObserveReminderUseCase,
    private val updateReminder: UpdateReminderUseCase,
    private val showReminder: ShowReminderUseCase,
) : MviViewModel<ReminderUiState, ReminderIntent, ReminderEffect>(ReminderUiState()) {

    init {
        viewModelScope.launch {
            observeReminder().collect { setState { copy(enabled = it.enabled, time = it.time) } }
        }
    }

    override fun handleIntent(intent: ReminderIntent) {
        when (intent) {
            is ReminderIntent.SetEnabled -> {
                save(currentState.copy(enabled = intent.enabled))
                if (intent.enabled) {
                    if (!currentState.notificationsAllowed) sendEffect(ReminderEffect.RequestNotificationPermission)
                    sendEffect(ReminderEffect.ReminderSaved(currentState.time))
                }
            }
            ReminderIntent.OpenTimePicker -> setState { copy(timeSheetOpen = true) }
            ReminderIntent.DismissTimePicker -> setState { copy(timeSheetOpen = false) }
            is ReminderIntent.PickTime -> {
                // Picking a time is a clear "remind me then", so it also turns the reminder on.
                setState { copy(timeSheetOpen = false) }
                save(currentState.copy(enabled = true, time = intent.time))
                if (!currentState.notificationsAllowed) sendEffect(ReminderEffect.RequestNotificationPermission)
                sendEffect(ReminderEffect.ReminderSaved(intent.time))
            }
            is ReminderIntent.NotificationPermissionChecked -> setState { copy(notificationsAllowed = intent.granted) }
            ReminderIntent.AllowNotifications -> sendEffect(ReminderEffect.RequestNotificationPermission)
            ReminderIntent.SendTestReminder -> showReminder()
        }
    }

    private fun save(next: ReminderUiState) {
        setState { copy(enabled = next.enabled, time = next.time) }
        viewModelScope.launch { updateReminder(ReminderSettings(next.enabled, next.time)) }
    }
}
