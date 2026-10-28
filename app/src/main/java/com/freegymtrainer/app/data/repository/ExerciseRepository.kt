package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.Exercise
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val db: AppDatabase) {
    fun getAll(): Flow<List<Exercise>> = db.exerciseDao().getAll()

    fun getAllMobility(): Flow<List<Exercise>> = db.exerciseDao().getAllMobility()

    fun filter(muscle: String?, equipment: String?, difficulty: String?, query: String?): Flow<List<Exercise>> =
        db.exerciseDao().filter(muscle, equipment, difficulty, query?.takeIf { it.isNotBlank() })

    suspend fun getById(id: Long): Exercise? = db.exerciseDao().getById(id)

    suspend fun getMuscleGroups(): List<String> = db.exerciseDao().getAllMuscleGroups()

    /** The exercises with these names, keyed by name (names that do not exist are simply absent). */
    suspend fun getByNames(names: Collection<String>): Map<String, Exercise> =
        db.exerciseDao().getByNames(names.toList()).associateBy { it.name }
}
