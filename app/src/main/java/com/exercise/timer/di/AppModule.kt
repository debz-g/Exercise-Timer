package com.exercise.timer.di

import android.content.Context
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.exercise.timer.data.preferences.DataStoreReminderRepository
import com.exercise.timer.data.preferences.DataStoreTimerPreferencesRepository
import com.exercise.timer.data.reminder.AlarmReminderScheduler
import com.exercise.timer.data.timer.ElapsedClock
import com.exercise.timer.data.timer.IntervalAlert
import com.exercise.timer.data.timer.IntervalTimerEngine
import com.exercise.timer.data.timer.TimerKeepAlive
import com.exercise.timer.data.timer.ToneVibrationAlert
import com.exercise.timer.domain.reminder.ReminderNotifier
import com.exercise.timer.domain.reminder.ReminderScheduler
import com.exercise.timer.domain.repository.ReminderRepository
import com.exercise.timer.domain.repository.TimerPreferencesRepository
import com.exercise.timer.domain.repository.TimerRepository
import com.exercise.timer.framework.notification.Notifications
import com.exercise.timer.framework.timer.ServiceTimerKeepAlive
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** App-wide scope on the main thread, for work that outlives any screen (the running timer). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides
    fun elapsedClock(): ElapsedClock = ElapsedClock { SystemClock.elapsedRealtime() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun timerRepository(impl: IntervalTimerEngine): TimerRepository
    @Binds abstract fun timerPreferences(impl: DataStoreTimerPreferencesRepository): TimerPreferencesRepository
    @Binds abstract fun reminderRepository(impl: DataStoreReminderRepository): ReminderRepository
    @Binds abstract fun reminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler
    @Binds abstract fun reminderNotifier(impl: Notifications): ReminderNotifier
    @Binds abstract fun intervalAlert(impl: ToneVibrationAlert): IntervalAlert
    @Binds abstract fun timerKeepAlive(impl: ServiceTimerKeepAlive): TimerKeepAlive
}
