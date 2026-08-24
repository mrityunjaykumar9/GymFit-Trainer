package com.freegymtrainer.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** AlarmManager alarms are cleared on reboot - this restores them if reminders are enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Reminder re-scheduling on boot reads saved settings from Room; wired up via
        // GymTrainerApp's applicationScope so this receiver stays a thin trigger.
        com.freegymtrainer.app.GymTrainerApp.rescheduleRemindersFromSettings(context)
    }
}
