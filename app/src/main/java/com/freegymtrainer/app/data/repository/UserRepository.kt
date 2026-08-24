package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.ReminderSettings
import com.freegymtrainer.app.data.local.entity.UserProfile
import kotlinx.coroutines.flow.Flow

class UserRepository(private val db: AppDatabase) {
    fun observeProfile(): Flow<UserProfile?> = db.userProfileDao().observe()
    suspend fun getProfile(): UserProfile? = db.userProfileDao().get()
    suspend fun saveProfile(profile: UserProfile) = db.userProfileDao().upsert(profile)

    fun observeReminderSettings(): Flow<ReminderSettings?> = db.reminderSettingsDao().observe()
    suspend fun getReminderSettings(): ReminderSettings? = db.reminderSettingsDao().get()
    suspend fun saveReminderSettings(settings: ReminderSettings) = db.reminderSettingsDao().upsert(settings)
}
