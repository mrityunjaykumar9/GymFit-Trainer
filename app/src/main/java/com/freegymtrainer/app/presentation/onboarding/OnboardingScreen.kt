package com.freegymtrainer.app.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.data.local.entity.UserProfile
import com.freegymtrainer.app.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val goals = listOf("Build muscle", "Lose fat", "Lose weight", "Maintain weight", "Improve fitness", "Strength training")
private val experienceLevels = listOf("Beginner", "Intermediate", "Advanced")
private val activityLevels = listOf("Sedentary", "Lightly active", "Moderately active", "Very active", "Extremely active")
private val equipmentOptions = listOf("Full gym", "Dumbbells", "Barbell", "Machines", "Resistance bands", "Bodyweight only", "Home gym")
private val genders = listOf("Male", "Female", "Other")

@Composable
fun OnboardingScreen(container: AppContainer, onFinished: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("25") }
    var gender by remember { mutableStateOf(genders[0]) }
    var heightCm by remember { mutableStateOf("170") }
    var weightKg by remember { mutableStateOf("70") }
    var goal by remember { mutableStateOf(goals[0]) }
    var experience by remember { mutableStateOf(experienceLevels[0]) }
    var activity by remember { mutableStateOf(activityLevels[2]) }
    var daysPerWeek by remember { mutableIntStateOf(4) }
    var sessionDuration by remember { mutableIntStateOf(45) }
    val selectedEquipment = remember { mutableStateListOf("Bodyweight only") }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))

            when (step) {
                0 -> {
                    Text("Welcome to Free Gym Trainer 👋", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Let's set up your profile so your workouts and nutrition targets fit you.", style = MaterialTheme.typography.bodyMedium)
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
                }
                1 -> {
                    Text("Body basics", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(value = heightCm, onValueChange = { heightCm = it.filter { c -> c.isDigit() } }, label = { Text("Height (cm)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = weightKg, onValueChange = { weightKg = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Current weight (kg)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())
                }
                2 -> {
                    Text("What's your goal?", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    goals.forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            RadioButton(selected = goal == g, onClick = { goal = g })
                            Text(g)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Experience level", style = MaterialTheme.typography.labelLarge)
                    Row { experienceLevels.forEach { e ->
                        FilterChip(selected = experience == e, onClick = { experience = e }, label = { Text(e) }, modifier = Modifier.padding(end = 8.dp))
                    } }
                }
                3 -> {
                    Text("Training availability", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("Days per week you can train: $daysPerWeek", style = MaterialTheme.typography.bodyLarge)
                    Slider(value = daysPerWeek.toFloat(), onValueChange = { daysPerWeek = it.toInt() }, valueRange = 2f..6f, steps = 3)
                    Spacer(Modifier.height(16.dp))
                    Text("Approx. session duration: $sessionDuration min", style = MaterialTheme.typography.bodyLarge)
                    Slider(value = sessionDuration.toFloat(), onValueChange = { sessionDuration = it.toInt() }, valueRange = 20f..90f, steps = 6)
                    Spacer(Modifier.height(16.dp))
                    Text("Activity level (outside the gym)", style = MaterialTheme.typography.labelLarge)
                    activityLevels.forEach { a ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            RadioButton(selected = activity == a, onClick = { activity = a })
                            Text(a)
                        }
                    }
                }
                4 -> {
                    Text("Available equipment", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    equipmentOptions.forEach { eq ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Checkbox(
                                checked = selectedEquipment.contains(eq),
                                onCheckedChange = { checked ->
                                    if (checked) selectedEquipment.add(eq) else selectedEquipment.remove(eq)
                                }
                            )
                            Text(eq)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Free Gym Trainer provides general fitness and nutrition guidance, not medical advice. Consult a professional if you have a medical condition.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (step > 0) {
                    OutlinedButton(onClick = { step-- }) { Text("Back") }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Button(onClick = {
                    if (step < 4) {
                        step++
                    } else {
                        val profile = UserProfile(
                            name = name.ifBlank { "Athlete" },
                            dateOfBirth = "", // simplified: MVP stores age indirectly via BMR calc input at nutrition-calc time
                            gender = gender,
                            heightCm = heightCm.toFloatOrNull() ?: 170f,
                            weightKg = weightKg.toFloatOrNull() ?: 70f,
                            goal = goal,
                            activityLevel = activity,
                            experienceLevel = experience,
                            daysPerWeek = daysPerWeek,
                            sessionDurationMinutes = sessionDuration,
                            equipmentAvailable = selectedEquipment.toList(),
                            onboardingComplete = true
                        )
                        scope.launch {
                            container.userRepository.saveProfile(profile)
                            // Pick a starting template based on goal + days/week, then seed the weekly plan.
                            val recommendedTemplateName = recommendTemplate(goal, daysPerWeek)
                            val allTemplates = container.workoutRepository.getAllTemplates().first()
                            val chosen = allTemplates.firstOrNull { it.name == recommendedTemplateName } ?: allTemplates.firstOrNull()
                            chosen?.let { container.workoutRepository.initDefaultWeeklyPlanIfEmpty(it.id) }
                            onFinished()
                        }
                    }
                }) { Text(if (step < 4) "Next" else "Get Started") }
            }
        }
    }
}

private fun recommendTemplate(goal: String, daysPerWeek: Int): String = when {
    goal == "Lose fat" || goal == "Lose weight" -> "Fat Loss Circuit"
    daysPerWeek >= 6 -> "Push Pull Legs"
    daysPerWeek >= 4 -> "Upper Lower"
    else -> "Beginner Full Body"
}

