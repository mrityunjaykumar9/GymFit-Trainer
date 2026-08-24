package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class TemplateDayDetail(
    val day: WorkoutTemplateDay,
    val exercises: List<Pair<WorkoutTemplateExercise, Exercise>>
)

class WorkoutRepository(private val db: AppDatabase) {

    fun getAllTemplates(): Flow<List<WorkoutTemplate>> = db.workoutTemplateDao().getAllTemplates()

    suspend fun getTemplateDays(templateId: Long): List<WorkoutTemplateDay> =
        db.workoutTemplateDao().getDaysForTemplate(templateId)

    suspend fun getTemplateDayDetail(dayId: Long): TemplateDayDetail? {
        val day = db.workoutTemplateDao().getTemplateDay(dayId) ?: return null
        val exercises = db.workoutTemplateDao().getExercisesForDay(dayId).mapNotNull { te ->
            db.exerciseDao().getById(te.exerciseId)?.let { te to it }
        }
        return TemplateDayDetail(day, exercises)
    }

    fun getWeeklyPlan(): Flow<List<WeeklyPlanEntry>> = db.weeklyPlanDao().getWeeklyPlan()

    suspend fun setWeeklyPlanDay(entry: WeeklyPlanEntry) = db.weeklyPlanDao().upsert(entry)

    suspend fun initDefaultWeeklyPlanIfEmpty(templateId: Long) {
        if (db.weeklyPlanDao().count() > 0) return
        val days = getTemplateDays(templateId)
        if (days.isEmpty()) return
        // Simple default: cycle through template days Mon-Sat, rest on Sunday (day 7).
        val entries = (1..7).map { dayOfWeek ->
            if (dayOfWeek == 7 || days.isEmpty()) {
                WeeklyPlanEntry(dayOfWeek = dayOfWeek, templateDayId = null, isRestDay = true)
            } else {
                val templateDay = days[(dayOfWeek - 1) % days.size]
                WeeklyPlanEntry(dayOfWeek = dayOfWeek, templateDayId = templateDay.id, isRestDay = false)
            }
        }
        db.weeklyPlanDao().upsertAll(entries)
    }

    suspend fun startSession(templateDayId: Long?, dayTitle: String): Long =
        db.workoutSessionDao().insertSession(
            WorkoutSession(
                dateEpochDay = java.time.LocalDate.now().toEpochDay(),
                templateDayId = templateDayId,
                dayTitle = dayTitle,
                startTimeMillis = System.currentTimeMillis()
            )
        )

    suspend fun logSet(setLog: WorkoutSetLog) = db.workoutSessionDao().insertSetLog(setLog)

    suspend fun completeSession(session: WorkoutSession) = db.workoutSessionDao().updateSession(session)

    suspend fun getSetLogsForSession(sessionId: Long) = db.workoutSessionDao().getSetLogsForSession(sessionId)

    fun getAllSessions(): Flow<List<WorkoutSession>> = db.workoutSessionDao().getAllSessions()

    suspend fun getCompletedSince(sinceMillis: Long) = db.workoutSessionDao().getCompletedSince(sinceMillis)

    fun getPersonalRecords(): Flow<List<PersonalRecord>> = db.personalRecordDao().getAll()

    suspend fun maybeRecordPersonalRecord(exerciseId: Long, exerciseName: String, weightKg: Double, reps: Int): Boolean {
        if (weightKg <= 0 || reps <= 0) return false
        // Epley formula estimated 1RM
        val estimated1RM = weightKg * (1 + reps / 30.0)
        val current = db.personalRecordDao().getBestForExercise(exerciseId)
        if (current == null || estimated1RM > current.estimatedOneRepMax) {
            db.personalRecordDao().insert(
                PersonalRecord(
                    exerciseId = exerciseId, exerciseName = exerciseName, weightKg = weightKg,
                    reps = reps, estimatedOneRepMax = estimated1RM,
                    achievedDateEpochDay = java.time.LocalDate.now().toEpochDay()
                )
            )
            return true
        }
        return false
    }
}
