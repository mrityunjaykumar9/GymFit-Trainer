package com.freegymtrainer.app

import android.app.Application
import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.repository.SeedRepository
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.notifications.NotificationHelper
import com.freegymtrainer.app.notifications.ReminderScheduler
import com.freegymtrainer.app.presentation.components.motion.ExerciseMotionLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class GymTrainerApp : Application() {

    lateinit var container: AppContainer
        private set

    /**
     * Awaitable handle on first-launch DB seeding (exercises/foods/workout templates).
     * Screens that read seeded data before showing the UI (see MainActivity.AppRoot) must
     * await this first - without it, a user who races through onboarding before seeding
     * finishes can end up with an empty weekly plan and a Home screen stuck on "Rest Day"
     * forever, since seeding only ever runs once per install.
     */
    lateinit var seedingReady: Deferred<Unit>
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        NotificationHelper.createChannel(this)
        // AdMob's MobileAds.initialize() is deliberately NOT called here - it must wait for the
        // EEA/UK consent flow (ConsentManager), which needs an Activity to show its dialog over.
        // MainActivity.onCreate runs that flow and initializes ads once it resolves.

        // Parse the bundled exercise animations off the main thread so the first exercise screen opens instantly.
        applicationScope.launch { ExerciseMotionLibrary.preload(this@GymTrainerApp) }

        seedingReady = applicationScope.async {
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
                if (settings.nutritionReminderEnabled) {
                    ReminderScheduler.scheduleNutritionReminder(context)
                }
                if (settings.mealRemindersEnabled) {
                    ReminderScheduler.scheduleMealReminders(context, settings)
                }
            }
        }
    }
}
