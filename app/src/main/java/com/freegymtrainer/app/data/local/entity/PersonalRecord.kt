package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_records")
data class PersonalRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val estimatedOneRepMax: Double,
    val achievedDateEpochDay: Long
)
