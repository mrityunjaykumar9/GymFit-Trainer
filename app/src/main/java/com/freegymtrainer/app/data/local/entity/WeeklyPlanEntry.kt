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
