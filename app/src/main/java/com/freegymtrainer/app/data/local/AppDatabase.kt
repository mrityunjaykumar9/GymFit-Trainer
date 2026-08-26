package com.freegymtrainer.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        ReminderSettings::class
    ],
    version = 3,
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

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "free_gym_trainer.db"
                )
                    // Pre-release app with no cloud backup of local data - destructive
                    // migration just wipes and reseeds (SeedRepository.seedIfNeeded runs on
                    // every launch) rather than requiring hand-written Migration objects for
                    // every schema bump during active development.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
