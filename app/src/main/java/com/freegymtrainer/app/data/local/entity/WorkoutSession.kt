package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val templateDayId: Long?,
    val dayTitle: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val durationSeconds: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val estimatedCalories: Int = 0,
    val completed: Boolean = false
)

@Entity(tableName = "workout_set_logs")
data class WorkoutSetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setIndex: Int,
    val targetReps: String,
    val actualReps: Int,
    val weightKg: Double,
    val completed: Boolean
)
