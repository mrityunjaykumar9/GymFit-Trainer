package com.freegymtrainer.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Schedules local-only reminders with AlarmManager. Uses inexact, wake-up alarms
 * (setAndAllowWhileIdle) so no special "exact alarm" permission dialog is needed for an MVP.
 */
object ReminderScheduler {

    private const val REQUEST_WORKOUT = 1001
    private const val REQUEST_WATER = 1002
    private const val REQUEST_NUTRITION = 1003

    fun scheduleWorkoutReminder(context: Context, hour: Int, minute: Int) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("type", "workout")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_WORKOUT, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = nextTriggerTimeMillis(hour, minute)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun scheduleWaterReminder(context: Context, intervalHours: Int) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("type", "water")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_WATER, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + intervalHours * 60 * 60 * 1000L
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(REQUEST_WORKOUT to "workout", REQUEST_WATER to "water", REQUEST_NUTRITION to "nutrition")
            .forEach { (requestCode, type) ->
                val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply { putExtra("type", type) }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
    }

    private fun nextTriggerTimeMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }
}
