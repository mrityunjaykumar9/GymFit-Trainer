package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // single-row table, local-only, no auth needed
    val name: String = "",
    val dateOfBirth: String = "",   // ISO yyyy-MM-dd
    val gender: String = "Male",    // Male / Female / Other (affects BMR formula constant)
    val heightCm: Float = 170f,
    val weightKg: Float = 70f,
    val goal: String = "Build muscle",
    val activityLevel: String = "Moderately active",
    val experienceLevel: String = "Beginner",
    val daysPerWeek: Int = 4,
    val sessionDurationMinutes: Int = 45,
    val equipmentAvailable: List<String> = listOf("Bodyweight"),
    val onboardingComplete: Boolean = false
)
