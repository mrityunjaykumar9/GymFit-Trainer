package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.dao.DayOption
import com.freegymtrainer.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class TemplateDayDetail(
    val day: WorkoutTemplateDay,
    val exercises: List<Pair<WorkoutTemplateExercise, Exercise>>
)

/** One exercise slot as specified by the workout builder - not yet persisted. */
data class CustomExerciseInput(val exerciseId: Long, val sets: Int, val reps: String, val restSeconds: Int)

class WorkoutRepository(private val db: AppDatabase) {

    fun getAllTemplates(): Flow<List<WorkoutTemplate>> = db.workoutTemplateDao().getAllTemplates()

    /** The single seeded "Warm-Up & Mobility" program - excluded from [getAllTemplates] since
     * that's the gym-focused Workouts tab. Backs the dedicated Warm-Up tab instead. */
    fun getMobilityTemplate(): Flow<WorkoutTemplate?> = db.workoutTemplateDao().getMobilityTemplate()

    suspend fun getTemplateDays(templateId: Long): List<WorkoutTemplateDay> =
        db.workoutTemplateDao().getDaysForTemplate(templateId)

    suspend fun countExercisesForDay(dayId: Long): Int = db.workoutTemplateDao().countExercisesForDay(dayId)

    /** Substitutes the exercise at one plan slot (e.g. no equipment for the original pick). */
    suspend fun swapExercise(templateExerciseId: Long, newExerciseId: Long) =
        db.workoutTemplateDao().swapExercise(templateExerciseId, newExerciseId)

    /** Every workout day across every template (seeded and user-built), flattened for the
     * Home "Quick Start" picker - lets someone start any day, not just the one the weekly
     * plan/override machinery computed for today. */
    fun getAllDayOptions(): Flow<List<DayOption>> = db.workoutTemplateDao().getAllDayOptions()

    /** The single auto-created "My Workouts" template that holds every day the user builds
     * themselves - created lazily on first save so a fresh install never has an empty program
     * sitting in the list before the user has actually built anything. */
    private suspend fun getOrCreateCustomTemplate(): WorkoutTemplate {
        db.workoutTemplateDao().getCustomTemplate()?.let { return it }
        val template = WorkoutTemplate(
            name = "My Workouts",
            description = "Workouts you built yourself",
            daysPerWeek = 0,
            level = "Custom",
            goalTag = "Personal",
            isCustom = true
        )
        val id = db.workoutTemplateDao().insertTemplate(template)
        return template.copy(id = id)
    }

    /** Saves a new day under "My Workouts" and returns its id so the caller can jump straight
     * into viewing (or starting) it. */
    suspend fun createCustomDay(title: String, muscleGroupsSummary: String, exercises: List<CustomExerciseInput>): Long {
        val template = getOrCreateCustomTemplate()
        val orderIndex = db.workoutTemplateDao().countDaysForTemplate(template.id) + 1
        val dayId = db.workoutTemplateDao().insertTemplateDay(
            WorkoutTemplateDay(templateId = template.id, orderIndex = orderIndex, title = title, muscleGroupsSummary = muscleGroupsSummary)
        )
        db.workoutTemplateDao().insertTemplateExercises(
            exercises.mapIndexed { index, input ->
                WorkoutTemplateExercise(
                    templateDayId = dayId, exerciseId = input.exerciseId, orderIndex = index + 1,
                    sets = input.sets, reps = input.reps, restSeconds = input.restSeconds
                )
            }
        )
        return dayId
    }

    /**
     * Saves a ready-made routine (for example "Back - Beginner") under "My Workouts", replacing the
     * earlier copy with the same title so starting the same routine twice does not pile up duplicates.
     */
    suspend fun createOrReplaceCustomDay(title: String, muscleGroupsSummary: String, exercises: List<CustomExerciseInput>): Long {
        val template = getOrCreateCustomTemplate()
        db.workoutTemplateDao().findDayByTitle(template.id, title)?.let { deleteCustomDay(it.id) }
        return createCustomDay(title, muscleGroupsSummary, exercises)
    }

    /** Removes a custom day and its exercise slots. Only meaningful for days under "My
     * Workouts" - the UI only ever offers this action there. */
    suspend fun deleteCustomDay(dayId: Long) {
        db.workoutTemplateDao().deleteExercisesForDay(dayId)
        db.workoutTemplateDao().deleteTemplateDay(dayId)
    }

    /** Whether [dayId] belongs to the user-built "My Workouts" template - gates whether the
     * day-detail screen offers a delete action. */
    suspend fun isCustomDay(dayId: Long): Boolean {
        val day = db.workoutTemplateDao().getTemplateDay(dayId) ?: return false
        return db.workoutTemplateDao().getTemplate(day.templateId)?.isCustom == true
    }

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

    /** A one-off "skip today" override, distinct from the recurring weekly plan (see DailyOverride). */
    fun observeTodayOverride(): Flow<DailyOverride?> = db.dailyOverrideDao().observeForDay(LocalDate.now().toEpochDay())

    suspend fun setSkipToday(skip: Boolean) {
        val today = LocalDate.now().toEpochDay()
        if (skip) db.dailyOverrideDao().upsert(DailyOverride(today, true))
        else db.dailyOverrideDao().deleteForDay(today)
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
