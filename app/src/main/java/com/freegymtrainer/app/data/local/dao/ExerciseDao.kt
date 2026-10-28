package com.freegymtrainer.app.data.local.dao

import androidx.room.*
import com.freegymtrainer.app.data.local.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT name FROM exercises")
    suspend fun allNames(): List<String>

    // Excludes Warm-Up & Mobility content - every current caller (custom workout builder's
    // picker, exercise-swap candidates, Home's recommendations) is a gym-context list; mobility
    // has its own pool (see getAllMobility below) so a swap or a builder search never mixes in a
    // stretch or warm-up drill.
    @Query("SELECT * FROM exercises WHERE category != 'Mobility' ORDER BY name ASC")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    // Excludes Warm-Up & Mobility content - the Exercise Library is the gym-focused browsing
    // surface; mobility has its own dedicated tab (see getAllMobility below) so the two pools
    // don't get mixed into one undifferentiated list.
    @Query("""
        SELECT * FROM exercises WHERE
        category != 'Mobility' AND
        (:muscle IS NULL OR primaryMuscle = :muscle) AND
        (:equipment IS NULL OR equipment = :equipment) AND
        (:difficulty IS NULL OR difficulty = :difficulty) AND
        (:query IS NULL OR name LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun filter(
        muscle: String?,
        equipment: String?,
        difficulty: String?,
        query: String?
    ): Flow<List<Exercise>>

    @Query("SELECT DISTINCT primaryMuscle FROM exercises WHERE category != 'Mobility' ORDER BY primaryMuscle ASC")
    suspend fun getAllMuscleGroups(): List<String>

    @Query("SELECT * FROM exercises WHERE name IN (:names)")
    suspend fun getByNames(names: List<String>): List<Exercise>

    @Query("SELECT * FROM exercises WHERE category = 'Mobility' ORDER BY name ASC")
    fun getAllMobility(): Flow<List<Exercise>>
}
