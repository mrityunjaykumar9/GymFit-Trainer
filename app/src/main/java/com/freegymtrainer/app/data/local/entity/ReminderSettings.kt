package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_settings")
data class ReminderSettings(
    @PrimaryKey val id: Int = 1,
    val workoutReminderEnabled: Boolean = true,
    val workoutReminderHour: Int = 17,
    val workoutReminderMinute: Int = 0,
    val waterReminderEnabled: Boolean = true,
    val waterReminderIntervalHours: Int = 2,
    val nutritionReminderEnabled: Boolean = true,
    // Meal-plan reminders: one nudge per meal, at the times below, carrying that meal's
    // items from the user's active weekly plan (see ReminderBroadcastReceiver). Times are
    // fixed sensible defaults for now - the Settings screen exposes a single on/off switch.
    val mealRemindersEnabled: Boolean = true,
    val breakfastHour: Int = 8,
    val breakfastMinute: Int = 0,
    val lunchHour: Int = 13,
    val lunchMinute: Int = 0,
    val snackHour: Int = 17,
    val snackMinute: Int = 30,
    val dinnerHour: Int = 20,
    val dinnerMinute: Int = 30
)
