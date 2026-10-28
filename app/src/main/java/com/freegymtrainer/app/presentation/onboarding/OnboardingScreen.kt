package com.freegymtrainer.app.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.data.local.entity.ReminderSettings
import com.freegymtrainer.app.data.local.entity.UserProfile
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val genders = listOf("Male", "Female", "Other")

// Everything beyond name/age/gender now just takes the entity's own sensible default (see
// UserProfile.kt) instead of asking - onboarding is a single quick step straight to Home.
@Composable
fun OnboardingScreen(container: AppContainer, onFinished: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("25") }
    var gender by remember { mutableStateOf(genders[0]) }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Welcome to GymFit Trainer 👋", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Just a few quick details to get started.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = age, onValueChange = { age = it.filter { c -> c.isDigit() } }, label = { Text("Age") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text("Gender", style = MaterialTheme.typography.labelLarge)
            Row { genders.forEach { g ->
                FilterChip(selected = gender == g, onClick = { gender = g }, label = { Text(g) }, modifier = Modifier.padding(end = 8.dp))
            } }
            Spacer(Modifier.height(16.dp))
            Text(
                "GymFit Trainer provides general fitness and nutrition guidance, not medical advice. Consult a professional if you have a medical condition.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
            Button(onClick = {
                val defaults = UserProfile()
                val profile = defaults.copy(
                    name = name.ifBlank { "Athlete" },
                    dateOfBirth = com.freegymtrainer.app.domain.usecase.NutritionCalculator.dateOfBirthForAge(age.toIntOrNull() ?: 25),
                    gender = gender,
                    onboardingComplete = true
                )
                scope.launch {
                    container.userRepository.saveProfile(profile)
                    // Pick a starting template based on the default goal + days/week, then seed the weekly plan.
                    val recommendedTemplateName = recommendTemplate(profile.goal, profile.daysPerWeek)
                    val allTemplates = container.workoutRepository.getAllTemplates().first()
                    val chosen = allTemplates.firstOrNull { it.name == recommendedTemplateName } ?: allTemplates.firstOrNull()
                    chosen?.let { container.workoutRepository.initDefaultWeeklyPlanIfEmpty(it.id) }

                    // ReminderSettings defaults to every reminder enabled, but nothing
                    // schedules an actual alarm until this row exists and the scheduler
                    // runs once - otherwise Settings shows reminders as "on" for a user
                    // who never opens that screen, while none are actually scheduled.
                    if (container.userRepository.getReminderSettings() == null) {
                        val reminderDefaults = ReminderSettings()
                        container.userRepository.saveReminderSettings(reminderDefaults)
                        if (reminderDefaults.workoutReminderEnabled) {
                            ReminderScheduler.scheduleWorkoutReminder(context, reminderDefaults.workoutReminderHour, reminderDefaults.workoutReminderMinute)
                        }
                        if (reminderDefaults.waterReminderEnabled) {
                            ReminderScheduler.scheduleWaterReminder(context, reminderDefaults.waterReminderIntervalHours)
                        }
                        if (reminderDefaults.nutritionReminderEnabled) {
                            ReminderScheduler.scheduleNutritionReminder(context)
                        }
                        if (reminderDefaults.mealRemindersEnabled) {
                            ReminderScheduler.scheduleMealReminders(context, reminderDefaults)
                        }
                    }

                    onFinished()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Get Started") }
        }
    }
}

private fun recommendTemplate(goal: String, daysPerWeek: Int): String = when {
    goal == "Lose fat" || goal == "Lose weight" -> "Fat Loss Circuit"
    daysPerWeek >= 6 -> "Push Pull Legs"
    daysPerWeek >= 4 -> "Upper Lower"
    else -> "Beginner Full Body"
}

