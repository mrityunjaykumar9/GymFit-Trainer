package com.freegymtrainer.app

import android.app.Application
import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.repository.SeedRepository
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.notifications.NotificationHelper
import com.freegymtrainer.app.notifications.ReminderScheduler
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymTrainerApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        NotificationHelper.createChannel(this)
        MobileAds.initialize(this) {
            // AdMob SDK ready - banners created after this point will serve ads.
        }

        // Seed the exercise library, food database, and workout templates on first launch
        // so the app is fully usable offline immediately - no network/backend required.
        applicationScope.launch {
            container.seedRepository.seedIfNeeded()
        }
    }

    companion object {
        /** Called by BootReceiver after a device reboot to restore any enabled local reminders. */
        fun rescheduleRemindersFromSettings(context: android.content.Context) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                val db = AppDatabase.getInstance(context)
                val settings = db.reminderSettingsDao().get() ?: return@launch
                if (settings.workoutReminderEnabled) {
                    ReminderScheduler.scheduleWorkoutReminder(context, settings.workoutReminderHour, settings.workoutReminderMinute)
                }
                if (settings.waterReminderEnabled) {
                    ReminderScheduler.scheduleWaterReminder(context, settings.waterReminderIntervalHours)
                }
            }
        }
    }
}
