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

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Query("""
        SELECT * FROM exercises WHERE
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

    @Query("SELECT DISTINCT primaryMuscle FROM exercises ORDER BY primaryMuscle ASC")
    suspend fun getAllMuscleGroups(): List<String>

    @Query("SELECT * FROM exercises WHERE name IN (:names)")
    suspend fun getByNames(names: List<String>): List<Exercise>
}
