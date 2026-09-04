package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.Exercise
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val db: AppDatabase) {
    fun getAll(): Flow<List<Exercise>> = db.exerciseDao().getAll()

    fun getAllYoga(): Flow<List<Exercise>> = db.exerciseDao().getAllYoga()

    fun filter(muscle: String?, equipment: String?, difficulty: String?, query: String?): Flow<List<Exercise>> =
        db.exerciseDao().filter(muscle, equipment, difficulty, query?.takeIf { it.isNotBlank() })

    suspend fun getById(id: Long): Exercise? = db.exerciseDao().getById(id)

    suspend fun getMuscleGroups(): List<String> = db.exerciseDao().getAllMuscleGroups()
}
