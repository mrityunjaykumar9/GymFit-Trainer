package com.freegymtrainer.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.freegymtrainer.app.data.local.entity.ReminderSettings
import java.util.Calendar

/**
 * Schedules local-only reminders with AlarmManager. Uses inexact, wake-up alarms
 * (setAndAllowWhileIdle) so no special "exact alarm" permission dialog is needed for an MVP.
 */
object ReminderScheduler {

    private const val REQUEST_WORKOUT = 1001
    private const val REQUEST_WATER = 1002
    private const val REQUEST_NUTRITION = 1003

    // One request code per meal so the four alarms don't overwrite each other.
    private val MEAL_REQUEST_CODES = mapOf(
        "Breakfast" to 1010, "Lunch" to 1011, "Snack" to 1012, "Dinner" to 1013
    )

    // Nutrition reminders have no user-configurable time (unlike workout's hour/minute or
    // water's interval) - a fixed evening check-in is enough for an MVP.
    private const val NUTRITION_REMINDER_HOUR = 20
    private const val NUTRITION_REMINDER_MINUTE = 0

    fun scheduleWorkoutReminder(context: Context, hour: Int, minute: Int) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("type", "workout")
            // Must be carried on the intent so the receiver can re-read the user's chosen
            // time when it reschedules tomorrow's alarm - without this, every reschedule
            // silently fell back to the receiver's hardcoded defaults instead of what was
            // actually configured in Settings.
            putExtra("hour", hour)
            putExtra("minute", minute)
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
            // See the comment in scheduleWorkoutReminder - same reason this is needed.
            putExtra("intervalHours", intervalHours)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_WATER, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + intervalHours * 60 * 60 * 1000L
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun scheduleNutritionReminder(context: Context) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("type", "nutrition")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_NUTRITION, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = nextTriggerTimeMillis(NUTRITION_REMINDER_HOUR, NUTRITION_REMINDER_MINUTE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    /** Schedules a nudge at each meal time carrying that meal's slot ("Breakfast" etc). */
    fun scheduleMealReminders(context: Context, settings: ReminderSettings) {
        val times = mapOf(
            "Breakfast" to (settings.breakfastHour to settings.breakfastMinute),
            "Lunch" to (settings.lunchHour to settings.lunchMinute),
            "Snack" to (settings.snackHour to settings.snackMinute),
            "Dinner" to (settings.dinnerHour to settings.dinnerMinute)
        )
        times.forEach { (meal, hm) -> scheduleMealReminder(context, meal, hm.first, hm.second) }
    }

    fun scheduleMealReminder(context: Context, mealType: String, hour: Int, minute: Int) {
        val requestCode = MEAL_REQUEST_CODES[mealType] ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("type", "meal")
            putExtra("meal", mealType)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTimeMillis(hour, minute), pendingIntent)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCodes = listOf(REQUEST_WORKOUT, REQUEST_WATER, REQUEST_NUTRITION) + MEAL_REQUEST_CODES.values
        requestCodes.forEach { requestCode ->
            // PendingIntent matching ignores extras, so a bare intent to the same receiver
            // with the same request code cancels whatever alarm that code scheduled.
            val intent = Intent(context, ReminderBroadcastReceiver::class.java)
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
