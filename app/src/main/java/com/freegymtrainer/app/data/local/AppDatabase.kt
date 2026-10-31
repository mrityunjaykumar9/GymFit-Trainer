package com.freegymtrainer.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.freegymtrainer.app.data.local.dao.*
import com.freegymtrainer.app.data.local.entity.*

@Database(
    entities = [
        Exercise::class,
        UserProfile::class,
        WorkoutTemplate::class,
        WorkoutTemplateDay::class,
        WorkoutTemplateExercise::class,
        WeeklyPlanEntry::class,
        WorkoutSession::class,
        WorkoutSetLog::class,
        PersonalRecord::class,
        WeightEntry::class,
        BodyMeasurement::class,
        WaterLog::class,
        Food::class,
        MealLogEntry::class,
        MealPlanDay::class,
        MealPlanItem::class,
        ReminderSettings::class,
        DailyOverride::class
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun weeklyPlanDao(): WeeklyPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun reminderSettingsDao(): ReminderSettingsDao
    abstract fun weightDao(): WeightDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun foodDao(): FoodDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun dailyOverrideDao(): DailyOverrideDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * v7 -> v8: per-meal reminder times on reminder_settings. Real migrations (not a wipe) so
         * updating the app keeps every user's workouts, weights, personal records and profile.
         * New columns carry the same defaults as the entity.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminder_settings ADD COLUMN mealRemindersEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE reminder_settings ADD COLUMN breakfastHour INTEGER NOT NULL DEFAULT 8")
                db.execSQL("ALTER TABLE reminder_settings ADD COLUMN breakfastMinute INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminder_settings ADD COLUMN lunchHour INTEGER NOT NULL DEFAULT 13")
                db.execSQL("ALTER TABLE reminder_settings ADD COLUMN lunchMinute INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminder_settings ADD COLUMN snackHour INTEGER NOT NULL DEFAULT 17")
                db.execSQL("ALTER TABLE reminder_settings ADD COLUMN snackMinute INTEGER NOT NULL DEFAULT 30")
                db.execSQL("ALTER TABLE reminder_settings ADD COLUMN dinnerHour INTEGER NOT NULL DEFAULT 20")
                db.execSQL("ALTER TABLE reminder_settings ADD COLUMN dinnerMinute INTEGER NOT NULL DEFAULT 30")
            }
        }

        /**
         * v8 -> v9: the Yoga tab was reframed as "Warm-Up & Mobility" - exercises.category
         * 'Yoga' becomes 'Mobility', the new isHeldPose column marks which of them are held
         * stretches (true) vs rep-counted drills (false, the column default - correct for every
         * pre-existing row, since the old Yoga pool was all held poses), and the 12 pose names
         * drop their Sanskrit parenthetical for the new framing. exerciseSlug() strips that
         * parenthetical anyway, so the rename doesn't touch which photo/motion data each exercise
         * resolves to. The brand-new dynamic warm-up drills (Arm Circles, Jumping Jacks, ...)
         * aren't inserted here - SeedRepository's by-name merge (same mechanism already used for
         * newly shipped foods) adds them on next launch, same as any future exercise addition.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN isHeldPose INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE exercises SET category = 'Mobility', isHeldPose = 1 WHERE category = 'Yoga'")
                val renames = listOf(
                    "Mountain Pose (Tadasana)" to "Mountain Pose",
                    "Downward-Facing Dog (Adho Mukha Svanasana)" to "Downward-Facing Dog",
                    "Child's Pose (Balasana)" to "Child's Pose",
                    "Cat-Cow Stretch (Marjaryasana-Bitilasana)" to "Cat-Cow Stretch",
                    "Cobra Pose (Bhujangasana)" to "Cobra Pose",
                    "Warrior I (Virabhadrasana I)" to "Warrior I",
                    "Warrior II (Virabhadrasana II)" to "Warrior II",
                    "Tree Pose (Vrikshasana)" to "Tree Pose",
                    "Bridge Pose (Setu Bandhasana)" to "Bridge Pose",
                    "Seated Forward Bend (Paschimottanasana)" to "Seated Forward Bend",
                    "Triangle Pose (Trikonasana)" to "Triangle Pose",
                    "Corpse Pose (Savasana)" to "Relaxation Pose"
                )
                for ((old, new) in renames) {
                    db.execSQL("UPDATE exercises SET name = ? WHERE name = ?", arrayOf(new, old))
                }
            }
        }

        /**
         * v9 -> v10: a posture audit found Arm Circles and Tree Pose couldn't be made to render
         * correctly in 3D (the free-swing depth model can't represent a full overhead circle
         * without the arm appearing to cross through the torso, and the single-leg balance pose
         * has similar issues), so both are dropped rather than shipped looking wrong. Their rows
         * (and any routine-day lines referencing them) are deleted outright; past workout
         * sessions/personal records that logged them are left as historical data, same as any
         * other data this app never retroactively edits.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM workout_template_exercises WHERE exerciseId IN " +
                        "(SELECT id FROM exercises WHERE name IN ('Arm Circles', 'Tree Pose'))"
                )
                db.execSQL("DELETE FROM exercises WHERE name IN ('Arm Circles', 'Tree Pose')")
            }
        }

        /**
         * v10 -> v11: same situation as v9->v10, this time for Triangle Pose (the IK targets for
         * its reaching arms sat past the model's max arm length, which distorts the elbow) and
         * Shoulder Rolls (a straight-down-hanging arm has no way to clear the hip in every camera
         * angle in this engine) - both dropped rather than shipped looking wrong.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM workout_template_exercises WHERE exerciseId IN " +
                        "(SELECT id FROM exercises WHERE name IN ('Triangle Pose', 'Shoulder Rolls'))"
                )
                db.execSQL("DELETE FROM exercises WHERE name IN ('Triangle Pose', 'Shoulder Rolls')")
            }
        }

        /**
         * v11 -> v12: same situation as v9->v10 and v10->v11. Leg Curl (ankle/pad-clipping issues
         * that several rounds of pose fixes couldn't resolve) and Jump Rope (its own pose issues)
         * were dropped from the seed data in earlier commits, but - unlike those two prior removals -
         * without a matching migration, so an already-seeded device kept the stale rows instead of
         * losing them. This migration is the cleanup those two commits should have shipped with.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM workout_template_exercises WHERE exerciseId IN " +
                        "(SELECT id FROM exercises WHERE name IN ('Leg Curl', 'Jump Rope'))"
                )
                db.execSQL("DELETE FROM exercises WHERE name IN ('Leg Curl', 'Jump Rope')")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "free_gym_trainer.db"
                )
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    // Only installs older than v7 (before schema export began) may still be wiped.
                    // Every newer schema change MUST ship a real Migration above - a missing one
                    // fails loudly in testing instead of silently deleting users' data.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)
                    .build().also { INSTANCE = it }
            }
    }
}
