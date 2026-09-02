package com.freegymtrainer.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra("type")) {
            "workout" -> {
                NotificationHelper.show(context, 1, "Time to train", "Don't forget your workout today.")
                // Re-schedule for tomorrow at the same time (simple daily-repeat pattern).
                val hour = intent.getIntExtra("hour", 17)
                val minute = intent.getIntExtra("minute", 0)
                ReminderScheduler.scheduleWorkoutReminder(context, hour, minute)
            }
            "water" -> {
                NotificationHelper.show(context, 2, "Stay hydrated", "Time to drink some water.")
                val intervalHours = intent.getIntExtra("intervalHours", 2)
                ReminderScheduler.scheduleWaterReminder(context, intervalHours)
            }
            "nutrition" -> {
                NotificationHelper.show(context, 3, "Nutrition check-in", "Your protein target may still be incomplete today.")
                // Re-schedule for tomorrow, same as the workout/water cases above.
                ReminderScheduler.scheduleNutritionReminder(context)
            }
        }
    }
}
