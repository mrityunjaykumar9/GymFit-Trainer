package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_history")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val weightKg: Float
)

@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val chestCm: Float? = null,
    val waistCm: Float? = null,
    val hipsCm: Float? = null,
    val armsCm: Float? = null,
    val thighsCm: Float? = null,
    val neckCm: Float? = null,
    val bodyFatPercent: Float? = null
)

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val amountMl: Int
)
