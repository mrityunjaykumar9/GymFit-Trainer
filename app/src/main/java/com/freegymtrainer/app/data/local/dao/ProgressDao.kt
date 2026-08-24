package com.freegymtrainer.app.data.local.dao

import androidx.room.*
import com.freegymtrainer.app.data.local.entity.WeightEntry
import com.freegymtrainer.app.data.local.entity.BodyMeasurement
import com.freegymtrainer.app.data.local.entity.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert
    suspend fun insert(entry: WeightEntry): Long

    @Query("SELECT * FROM weight_history ORDER BY dateEpochDay ASC")
    fun getAll(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_history ORDER BY dateEpochDay DESC LIMIT 1")
    suspend fun getLatest(): WeightEntry?
}

@Dao
interface BodyMeasurementDao {
    @Insert
    suspend fun insert(entry: BodyMeasurement): Long

    @Query("SELECT * FROM body_measurements ORDER BY dateEpochDay ASC")
    fun getAll(): Flow<List<BodyMeasurement>>
}

@Dao
interface WaterLogDao {
    @Insert
    suspend fun insert(entry: WaterLog): Long

    @Query("SELECT * FROM water_logs WHERE dateEpochDay = :day ORDER BY id ASC")
    fun getForDay(day: Long): Flow<List<WaterLog>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_logs WHERE dateEpochDay = :day")
    suspend fun getTotalForDay(day: Long): Int
}
