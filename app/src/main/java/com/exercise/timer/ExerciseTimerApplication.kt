package com.exercise.timer

import android.app.Application
import com.exercise.timer.domain.usecase.RescheduleReminderUseCase
import com.exercise.timer.framework.notification.Notifications
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ExerciseTimerApplication : Application() {

    @Inject lateinit var notifications: Notifications
    @Inject lateinit var rescheduleReminder: RescheduleReminderUseCase

    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
        // Belt and braces: make sure the daily alarm is armed whenever the app starts.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { rescheduleReminder() }
    }
}
