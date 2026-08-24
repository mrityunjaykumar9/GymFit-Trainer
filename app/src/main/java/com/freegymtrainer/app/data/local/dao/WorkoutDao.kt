package com.freegymtrainer.app.data.local.dao

import androidx.room.*
import com.freegymtrainer.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {
    @Query("SELECT COUNT(*) FROM workout_templates")
    suspend fun countTemplates(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateDay(day: WorkoutTemplateDay): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(exercises: List<WorkoutTemplateExercise>)

    @Query("SELECT * FROM workout_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplate(id: Long): WorkoutTemplate?

    @Query("SELECT * FROM workout_template_days WHERE templateId = :templateId ORDER BY orderIndex ASC")
    suspend fun getDaysForTemplate(templateId: Long): List<WorkoutTemplateDay>

    @Query("SELECT * FROM workout_template_days WHERE id = :dayId")
    suspend fun getTemplateDay(dayId: Long): WorkoutTemplateDay?

    @Query("SELECT * FROM workout_template_exercises WHERE templateDayId = :dayId ORDER BY orderIndex ASC")
    suspend fun getExercisesForDay(dayId: Long): List<WorkoutTemplateExercise>
}

@Dao
interface WeeklyPlanDao {
    @Query("SELECT COUNT(*) FROM weekly_plan")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeeklyPlanEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<WeeklyPlanEntry>)

    @Query("SELECT * FROM weekly_plan ORDER BY dayOfWeek ASC")
    fun getWeeklyPlan(): Flow<List<WeeklyPlanEntry>>

    @Query("SELECT * FROM weekly_plan WHERE dayOfWeek = :dayOfWeek")
    suspend fun getForDay(dayOfWeek: Int): WeeklyPlanEntry?
}

@Dao
interface WorkoutSessionDao {
    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSession?

    @Query("SELECT * FROM workout_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE completed = 1 AND startTimeMillis >= :sinceMillis ORDER BY startTimeMillis DESC")
    suspend fun getCompletedSince(sinceMillis: Long): List<WorkoutSession>

    @Insert
    suspend fun insertSetLog(setLog: WorkoutSetLog): Long

    @Insert
    suspend fun insertSetLogs(setLogs: List<WorkoutSetLog>)

    @Query("SELECT * FROM workout_set_logs WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getSetLogsForSession(sessionId: Long): List<WorkoutSetLog>

    @Query("SELECT * FROM workout_set_logs WHERE exerciseId = :exerciseId ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentSetLogsForExercise(exerciseId: Long, limit: Int = 50): List<WorkoutSetLog>
}

@Dao
interface PersonalRecordDao {
    @Query("SELECT * FROM personal_records ORDER BY achievedDateEpochDay DESC")
    fun getAll(): Flow<List<PersonalRecord>>

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId ORDER BY estimatedOneRepMax DESC LIMIT 1")
    suspend fun getBestForExercise(exerciseId: Long): PersonalRecord?

    @Insert
    suspend fun insert(record: PersonalRecord): Long

    @Delete
    suspend fun delete(record: PersonalRecord)
}
