package com.freegymtrainer.app.data.local.dao

import androidx.room.*
import com.freegymtrainer.app.data.local.entity.UserProfile
import com.freegymtrainer.app.data.local.entity.ReminderSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)
}

@Dao
interface ReminderSettingsDao {
    @Query("SELECT * FROM reminder_settings WHERE id = 1")
    fun observe(): Flow<ReminderSettings?>

    @Query("SELECT * FROM reminder_settings WHERE id = 1")
    suspend fun get(): ReminderSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ReminderSettings)
}
