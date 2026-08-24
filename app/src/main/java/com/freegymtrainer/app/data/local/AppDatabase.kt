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
        ReminderSettings::class
    ],
    version = 1,
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

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "free_gym_trainer.db"
                ).build().also { INSTANCE = it }
            }
    }
}
