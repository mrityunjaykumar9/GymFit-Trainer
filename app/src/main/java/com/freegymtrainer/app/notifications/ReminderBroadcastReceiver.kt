package com.freegymtrainer.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.repository.NutritionPlanner
import com.freegymtrainer.app.domain.nutrition.PlanMeal
import com.freegymtrainer.app.domain.nutrition.Portions
import com.freegymtrainer.app.di.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderBroadcastReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra("type")) {
            "workout" -> {
                NotificationHelper.show(context, 1, "Time to train", "Don't forget your workout today.")
                val hour = intent.getIntExtra("hour", 17)
                val minute = intent.getIntExtra("minute", 0)
                ReminderScheduler.scheduleWorkoutReminder(context, hour, minute)
            }
            "water" -> {
                NotificationHelper.show(context, 2, "Stay hydrated", "Time to drink some water.")
                val intervalHours = intent.getIntExtra("intervalHours", 2)
                ReminderScheduler.scheduleWaterReminder(context, intervalHours)
            }
            "nutrition" -> {
                NotificationHelper.show(context, 3, "Nutrition check-in", "Your protein target may still be incomplete today.")
                ReminderScheduler.scheduleNutritionReminder(context)
            }
            "meal" -> {
                val meal = intent.getStringExtra("meal") ?: "Breakfast"
                val hour = intent.getIntExtra("hour", 8)
                val minute = intent.getIntExtra("minute", 0)
                val pending = goAsync()
                val appContext = context.applicationContext
                scope.launch {
                    try {
                        showMealNotification(appContext, meal)
                    } catch (e: Exception) {
                        // Never let a reminder crash the app - log it and fall through to rescheduling.
                        android.util.Log.w("MealReminder", "Could not build the $meal reminder", e)
                    } finally {
                        ReminderScheduler.scheduleMealReminder(appContext, meal, hour, minute)
                        pending.finish()
                    }
                }
            }
        }
    }

    private suspend fun showMealNotification(context: Context, mealType: String) {
        val planned = NutritionPlanner(AppDatabase.getInstance(context), SettingsDataStore(context)).plannedMealToday(mealType)

        val title = mealTitle(mealType)
        val (short, big) = if (planned != null && planned.items.isNotEmpty()) {
            plannedText(planned)
        } else {
            genericFallback(mealType)
        }
        NotificationHelper.showBigText(
            context = context,
            id = mealNotificationId(mealType),
            title = title,
            shortText = short,
            bigText = big,
            channelId = NotificationHelper.MEAL_CHANNEL_ID
        )
    }

    private fun plannedText(meal: PlanMeal): Pair<String, String> {
        val items = meal.items.joinToString(", ") { "${it.food.name} (${Portions.describe(it.food.name, it.grams, Portions.isLiquid(it.food.name, it.food.category))})" }
        val kcal = meal.calories.toInt()
        val protein = meal.protein.toInt()
        val short = "≈ $kcal kcal · $protein g protein — tap to see your plan"
        val big = "On your plan for this meal:\n$items\n\n≈ $kcal kcal · $protein g protein. " +
            "Keep a protein source on every plate and drink a glass of water with your meal."
        return short to big
    }

    private fun genericFallback(mealType: String): Pair<String, String> {
        val advice = when (mealType) {
            "Breakfast" -> "Aim for 20–30 g protein — eggs, Greek yogurt, paneer, or a protein shake — plus a whole grain or fruit."
            "Lunch" -> "Build the plate: a palm of protein (dal, chicken, paneer, fish), a fist of carbs, and vegetables."
            "Snack" -> "Keep it protein-forward — curd, roasted chana, nuts, or a scoop of whey — not just biscuits or chips."
            "Dinner" -> "Go lighter on carbs, higher on protein and vegetables so you digest well before bed."
            else -> "Include a protein source and some vegetables."
        }
        return "Time to eat well" to advice
    }

    private fun mealTitle(mealType: String): String = when (mealType) {
        "Breakfast" -> "Breakfast time"
        "Lunch" -> "Lunch time"
        "Snack" -> "Snack time"
        "Dinner" -> "Dinner time"
        else -> "$mealType time"
    }

    private fun mealNotificationId(mealType: String): Int = when (mealType) {
        "Breakfast" -> 10
        "Lunch" -> 11
        "Snack" -> 12
        "Dinner" -> 13
        else -> 14
    }
}
