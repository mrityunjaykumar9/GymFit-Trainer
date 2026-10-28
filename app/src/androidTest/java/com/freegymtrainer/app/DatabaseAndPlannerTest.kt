package com.freegymtrainer.app

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.UserProfile
import com.freegymtrainer.app.data.repository.NutritionPlanner
import com.freegymtrainer.app.data.repository.SeedRepository
import com.freegymtrainer.app.di.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a device/emulator: proves that updating from the version that is live (database v7) keeps
 * the user's data, and that the diet planner works end-to-end on a real Room database.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseAndPlannerTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrationFrom7KeepsEveryUserRowAndAddsMealReminderDefaults() {
        helper.createDatabase("migration-test", 7).apply {
            execSQL(
                "INSERT INTO user_profile VALUES (1, 'Keep Me', '1998-01-01', 'Male', 175.0, 72.5, 'Lose fat', " +
                    "'Moderately active', 'Beginner', 4, 45, 'Bodyweight', 1)"
            )
            execSQL("INSERT INTO weight_history (dateEpochDay, weightKg) VALUES (20000, 73.0), (20007, 72.5)")
            execSQL(
                "INSERT INTO meal_log_entries (dateEpochDay, mealType, foodId, foodName, servingGrams, calories, protein, carbs, fat) " +
                    "VALUES (20007, 'Lunch', 5, 'Dal', 150, 174, 13.5, 30, 0.6)"
            )
            execSQL(
                "INSERT INTO reminder_settings VALUES (1, 1, 18, 15, 1, 3, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate("migration-test", 8, true, AppDatabase.MIGRATION_7_8)

        db.query("SELECT name, weightKg, onboardingComplete FROM user_profile").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Keep Me", c.getString(0))
            assertEquals(72.5f, c.getFloat(1), 0.001f)
            assertEquals(1, c.getInt(2))
        }
        db.query("SELECT COUNT(*) FROM weight_history").use { c -> c.moveToFirst(); assertEquals(2, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM meal_log_entries").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.query(
            "SELECT workoutReminderHour, workoutReminderMinute, nutritionReminderEnabled, mealRemindersEnabled, " +
                "breakfastHour, lunchHour, snackMinute, dinnerHour, dinnerMinute FROM reminder_settings"
        ).use { c ->
            assertTrue(c.moveToFirst())
            // existing choices are untouched ...
            assertEquals(18, c.getInt(0)); assertEquals(15, c.getInt(1)); assertEquals(0, c.getInt(2))
            // ... and the new meal-reminder columns get the app defaults
            assertEquals(1, c.getInt(3)); assertEquals(8, c.getInt(4)); assertEquals(13, c.getInt(5))
            assertEquals(30, c.getInt(6)); assertEquals(20, c.getInt(7)); assertEquals(30, c.getInt(8))
        }
    }

    @Test
    fun migrationFrom9DropsArmCirclesAndTreePoseButKeepsEverythingElse() {
        helper.createDatabase("migration-test-9-10", 9).apply {
            execSQL(
                "INSERT INTO exercises (id, name, primaryMuscle, secondaryMuscles, equipment, difficulty, category, goalTags, instructions, commonMistakes, safetyNotes, mediaUrl, isHeldPose) VALUES " +
                    "(1, 'Arm Circles', 'Shoulders', '', 'Bodyweight', 'Beginner', 'Mobility', 'Mobility', 'Stand tall', 'Too fast', 'Stay controlled', NULL, 0), " +
                    "(2, 'Tree Pose', 'Core', 'Legs', 'Bodyweight', 'Beginner', 'Mobility', 'Mobility', 'Balance', 'Hips tilt', 'Use a wall', NULL, 1), " +
                    "(3, 'Keep Me', 'Chest', '', 'Barbell', 'Beginner', 'Push', 'Strength', 'Press', 'Flare', 'Spot', NULL, 0)"
            )
            execSQL(
                "INSERT INTO workout_templates (id, name, description, daysPerWeek, level, goalTag, isCustom, isYoga) VALUES " +
                    "(1, 'Warm-Up & Mobility', 'desc', 5, 'Beginner', 'Mobility', 0, 1)"
            )
            execSQL(
                "INSERT INTO workout_template_days (id, templateId, orderIndex, title, muscleGroupsSummary) VALUES " +
                    "(1, 1, 1, 'Pre-Lifting Warm-Up', 'Full Body')"
            )
            execSQL(
                "INSERT INTO workout_template_exercises (id, templateDayId, exerciseId, orderIndex, sets, reps, restSeconds) VALUES " +
                    "(1, 1, 1, 1, 1, '10', 10), (2, 1, 2, 2, 1, '10', 10), (3, 1, 3, 3, 1, '10', 10)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate("migration-test-9-10", 10, true, AppDatabase.MIGRATION_9_10)

        db.query("SELECT name FROM exercises ORDER BY name").use { c ->
            val names = generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList()
            assertEquals(listOf("Keep Me"), names)
        }
        db.query("SELECT COUNT(*) FROM workout_template_exercises").use { c ->
            c.moveToFirst(); assertEquals(1, c.getInt(0))
        }
        db.query("SELECT exerciseId FROM workout_template_exercises").use { c ->
            c.moveToFirst(); assertEquals(3, c.getInt(0))
        }
    }

    @Test
    fun migrationFrom10DropsTrianglePoseAndShoulderRollsButKeepsEverythingElse() {
        helper.createDatabase("migration-test-10-11", 10).apply {
            execSQL(
                "INSERT INTO exercises (id, name, primaryMuscle, secondaryMuscles, equipment, difficulty, category, goalTags, instructions, commonMistakes, safetyNotes, mediaUrl, isHeldPose) VALUES " +
                    "(1, 'Triangle Pose', 'Balance', 'Legs;;Back', 'Bodyweight', 'Intermediate', 'Mobility', 'Mobility', 'Stand wide', 'Collapsing', 'Stay long', NULL, 1), " +
                    "(2, 'Shoulder Rolls', 'Shoulders', '', 'Bodyweight', 'Beginner', 'Mobility', 'Mobility', 'Roll shoulders', 'Rushing', 'Go slow', NULL, 0), " +
                    "(3, 'Keep Me', 'Chest', '', 'Barbell', 'Beginner', 'Push', 'Strength', 'Press', 'Flare', 'Spot', NULL, 0)"
            )
            execSQL(
                "INSERT INTO workout_templates (id, name, description, daysPerWeek, level, goalTag, isCustom, isYoga) VALUES " +
                    "(1, 'Warm-Up & Mobility', 'desc', 5, 'Beginner', 'Mobility', 0, 1)"
            )
            execSQL(
                "INSERT INTO workout_template_days (id, templateId, orderIndex, title, muscleGroupsSummary) VALUES " +
                    "(1, 1, 1, 'Cool-Down & Recovery', 'Flexibility')"
            )
            execSQL(
                "INSERT INTO workout_template_exercises (id, templateDayId, exerciseId, orderIndex, sets, reps, restSeconds) VALUES " +
                    "(1, 1, 1, 1, 1, '10', 10), (2, 1, 2, 2, 1, '10', 10), (3, 1, 3, 3, 1, '10', 10)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate("migration-test-10-11", 11, true, AppDatabase.MIGRATION_10_11)

        db.query("SELECT name FROM exercises ORDER BY name").use { c ->
            val names = generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList()
            assertEquals(listOf("Keep Me"), names)
        }
        db.query("SELECT COUNT(*) FROM workout_template_exercises").use { c ->
            c.moveToFirst(); assertEquals(1, c.getInt(0))
        }
        db.query("SELECT exerciseId FROM workout_template_exercises").use { c ->
            c.moveToFirst(); assertEquals(3, c.getInt(0))
        }
    }

    @Test
    fun plannerBuildsTargetsAPlanAndAReminderMeal() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            SeedRepository(db).seedIfNeeded()
            db.userProfileDao().upsert(
                UserProfile(
                    name = "Planner", dateOfBirth = NutritionCalculatorDob, gender = "Female", heightCm = 165f,
                    weightKg = 62f, goal = "Lose fat", activityLevel = "Lightly active", onboardingComplete = true
                )
            )
            val planner = NutritionPlanner(db, SettingsDataStore(context))
            val resolved = planner.resolve()
            assertNotNull(resolved)
            val targets = resolved!!.targets
            assertTrue("calorie floor for women", targets.calorieTarget >= 1200)
            assertTrue(targets.proteinGrams in 90..140)

            val plan = planner.buildPlan(resolved)
            assertEquals(7, plan.days.size)
            plan.days.forEach { assertTrue("day ${it.dayOfWeek} has meals", it.meals.isNotEmpty()) }

            // the meal reminder reads today's lunch from this same plan
            val lunch = planner.plannedMealToday("Lunch")
            assertNotNull(lunch)
            assertTrue(lunch!!.items.isNotEmpty())
            assertTrue(lunch.calories > 100f)
        } finally {
            db.close()
        }
    }

    private companion object {
        val NutritionCalculatorDob: String = com.freegymtrainer.app.domain.usecase.NutritionCalculator.dateOfBirthForAge(30)
    }
}
