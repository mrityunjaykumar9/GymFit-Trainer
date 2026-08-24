package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.BodyMeasurement
import com.freegymtrainer.app.data.local.entity.WaterLog
import com.freegymtrainer.app.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ProgressRepository(private val db: AppDatabase) {
    fun getWeightHistory(): Flow<List<WeightEntry>> = db.weightDao().getAll()
    suspend fun addWeightEntry(weightKg: Float, date: LocalDate = LocalDate.now()) =
        db.weightDao().insert(WeightEntry(dateEpochDay = date.toEpochDay(), weightKg = weightKg))
    suspend fun getLatestWeight(): WeightEntry? = db.weightDao().getLatest()

    fun getMeasurements(): Flow<List<BodyMeasurement>> = db.bodyMeasurementDao().getAll()
    suspend fun addMeasurement(measurement: BodyMeasurement) = db.bodyMeasurementDao().insert(measurement)

    fun getWaterForToday(): Flow<List<WaterLog>> = db.waterLogDao().getForDay(LocalDate.now().toEpochDay())
    suspend fun addWater(amountMl: Int) =
        db.waterLogDao().insert(WaterLog(dateEpochDay = LocalDate.now().toEpochDay(), amountMl = amountMl))
    suspend fun getTotalWaterToday(): Int = db.waterLogDao().getTotalForDay(LocalDate.now().toEpochDay())
}
