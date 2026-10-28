package com.freegymtrainer.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.freegymtrainer.app.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "gym_trainer_reminders"
    private const val CHANNEL_NAME = "Reminders"

    /** Separate channel so users can silence meal nudges without losing workout/water reminders. */
    const val MEAL_CHANNEL_ID = "gym_trainer_meal_reminders"
    private const val MEAL_CHANNEL_NAME = "Meal plan reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Workout and water reminders" }
            )
            manager.createNotificationChannel(
                NotificationChannel(MEAL_CHANNEL_ID, MEAL_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "A nudge at each meal time with what's on your plan" }
            )
        }
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun show(context: Context, id: Int, title: String, message: String, channelId: String = CHANNEL_ID) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(contentIntent(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted yet - skip rather than crash.
        }
    }

    /** Like [show] but with an expandable body (used for the meal item list). */
    fun showBigText(
        context: Context,
        id: Int,
        title: String,
        shortText: String,
        bigText: String,
        channelId: String = CHANNEL_ID
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(contentIntent(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        }
    }
}
