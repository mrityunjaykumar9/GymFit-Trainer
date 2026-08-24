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
    val nutritionReminderEnabled: Boolean = true
)
