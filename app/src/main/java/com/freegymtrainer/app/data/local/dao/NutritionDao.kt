package com.freegymtrainer.app.data.local.dao

import androidx.room.*
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import com.freegymtrainer.app.data.local.entity.MealPlanDay
import com.freegymtrainer.app.data.local.entity.MealPlanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<Food>)

    @Query("SELECT * FROM foods WHERE (:query IS NULL OR name LIKE '%' || :query || '%') ORDER BY name ASC")
    fun search(query: String?): Flow<List<Food>>

    @Query("SELECT name FROM foods")
    suspend fun allNames(): List<String>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): Food?

    @Query("SELECT * FROM foods WHERE name IN (:names)")
    suspend fun getByNames(names: List<String>): List<Food>

    @Query("SELECT * FROM foods WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Food>
}

@Dao
interface MealLogDao {
    @Insert
    suspend fun insert(entry: MealLogEntry): Long

    @Insert
    suspend fun insertAll(entries: List<MealLogEntry>)

    @Delete
    suspend fun delete(entry: MealLogEntry)

    @Query("SELECT * FROM meal_log_entries WHERE dateEpochDay = :day ORDER BY id ASC")
    fun getForDay(day: Long): Flow<List<MealLogEntry>>

    @Query("SELECT * FROM meal_log_entries WHERE dateEpochDay = :day ORDER BY id ASC")
    suspend fun getForDayOnce(day: Long): List<MealLogEntry>

    @Query("SELECT * FROM meal_log_entries WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay ASC, id ASC")
    fun getRange(from: Long, to: Long): Flow<List<MealLogEntry>>

    /** The foods eaten most recently (one row per food), newest first - for the quick-add chips. */
    @Query("SELECT * FROM foods WHERE id IN (SELECT foodId FROM meal_log_entries WHERE foodId > 0 GROUP BY foodId ORDER BY MAX(id) DESC LIMIT :limit)")
    fun recentFoodsUnordered(limit: Int): Flow<List<Food>>

    @Query("SELECT foodId FROM meal_log_entries WHERE foodId > 0 GROUP BY foodId ORDER BY MAX(id) DESC LIMIT :limit")
    fun recentFoodIds(limit: Int): Flow<List<Long>>
}

@Dao
interface MealPlanDao {
    @Query("SELECT COUNT(*) FROM meal_plan_days")
    suspend fun countDays(): Int

    @Insert
    suspend fun insertDay(day: MealPlanDay): Long

    @Insert
    suspend fun insertItems(items: List<MealPlanItem>)

    @Query("SELECT * FROM meal_plan_days WHERE dietType = :dietType AND dayOfWeek = :dayOfWeek LIMIT 1")
    suspend fun getDay(dietType: String, dayOfWeek: Int): MealPlanDay?

    @Query("SELECT * FROM meal_plan_days WHERE dietType = :dietType ORDER BY dayOfWeek ASC")
    suspend fun getDaysForDiet(dietType: String): List<MealPlanDay>

    @Query("SELECT * FROM meal_plan_items WHERE planDayId IN (:planDayIds) ORDER BY orderIndex ASC")
    suspend fun getItemsForDays(planDayIds: List<Long>): List<MealPlanItem>

    @Query("SELECT * FROM meal_plan_items WHERE planDayId = :planDayId ORDER BY orderIndex ASC")
    suspend fun getItemsForDay(planDayId: Long): List<MealPlanItem>
}
