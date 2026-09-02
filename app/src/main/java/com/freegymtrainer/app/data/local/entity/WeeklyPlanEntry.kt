package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The user's customizable 7-day schedule. dayOfWeek: 1=Monday .. 7=Sunday */
@Entity(tableName = "weekly_plan")
data class WeeklyPlanEntry(
    @PrimaryKey val dayOfWeek: Int,
    val templateDayId: Long? = null,
    val isRestDay: Boolean = true
)

/**
 * A one-off override for a single calendar date, e.g. "skip today's scheduled workout" -
 * without touching the recurring [WeeklyPlanEntry] for that day-of-week (which would affect
 * every future week, not just today).
 */
@Entity(tableName = "daily_overrides")
data class DailyOverride(
    @PrimaryKey val dateEpochDay: Long,
    val skipWorkout: Boolean = true
)
