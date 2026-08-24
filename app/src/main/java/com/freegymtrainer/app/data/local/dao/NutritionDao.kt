package com.freegymtrainer.app.data.local.dao

import androidx.room.*
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<Food>)

    @Query("SELECT * FROM foods WHERE (:query IS NULL OR name LIKE '%' || :query || '%') ORDER BY name ASC")
    fun search(query: String?): Flow<List<Food>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): Food?
}

@Dao
interface MealLogDao {
    @Insert
    suspend fun insert(entry: MealLogEntry): Long

    @Delete
    suspend fun delete(entry: MealLogEntry)

    @Query("SELECT * FROM meal_log_entries WHERE dateEpochDay = :day ORDER BY id ASC")
    fun getForDay(day: Long): Flow<List<MealLogEntry>>
}
