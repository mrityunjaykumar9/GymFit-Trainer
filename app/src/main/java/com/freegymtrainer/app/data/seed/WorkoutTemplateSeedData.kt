package com.freegymtrainer.app.data.seed

/** Plain seed models (not Room entities) - resolved to real IDs by SeedRepository on first launch. */
data class SeedTemplateExercise(val exerciseName: String, val sets: Int, val reps: String, val restSeconds: Int)
data class SeedTemplateDay(val title: String, val muscleGroupsSummary: String, val exercises: List<SeedTemplateExercise>)
data class SeedTemplate(
    val name: String, val description: String, val daysPerWeek: Int,
    val level: String, val goalTag: String, val days: List<SeedTemplateDay>,
    val isYoga: Boolean = false
)

object WorkoutTemplateSeedData {

    private fun te(name: String, sets: Int, reps: String, rest: Int) = SeedTemplateExercise(name, sets, reps, rest)

    val all: List<SeedTemplate> = listOf(

        SeedTemplate(
            "Beginner Full Body", "A simple 3-day full-body split, ideal for building a foundation.",
            3, "Beginner", "General fitness",
            listOf(
                SeedTemplateDay("Full Body A", "Chest, Back, Legs, Core", listOf(
                    te("Barbell Back Squat", 3, "8-10", 90), te("Dumbbell Bench Press", 3, "8-10", 90),
                    te("Seated Cable Row", 3, "10-12", 75), te("Plank", 3, "30-45s", 45)
                )),
                SeedTemplateDay("Full Body B", "Legs, Shoulders, Back, Core", listOf(
                    te("Romanian Deadlift", 3, "8-10", 90), te("Dumbbell Shoulder Press", 3, "8-10", 75),
                    te("Lat Pulldown", 3, "10-12", 75), te("Crunch", 3, "15-20", 45)
                )),
                SeedTemplateDay("Full Body C", "Legs, Chest, Back, Arms", listOf(
                    te("Leg Press", 3, "10-12", 90), te("Push Ups", 3, "AMRAP", 75),
                    te("Barbell Row", 3, "8-10", 90), te("Dumbbell Curl", 2, "10-12", 60),
                    te("Tricep Pushdown", 2, "10-12", 60)
                ))
            )
        ),

        SeedTemplate(
            "Push Pull Legs", "A 6-day intermediate split for maximizing muscle gain and strength.",
            6, "Intermediate", "Muscle gain",
            listOf(
                SeedTemplateDay("Push Day", "Chest, Shoulders, Triceps", listOf(
                    te("Barbell Bench Press", 4, "6-8", 120), te("Incline Dumbbell Press", 3, "8-10", 90),
                    te("Overhead Press", 3, "8-10", 90), te("Lateral Raise", 3, "12-15", 60),
                    te("Tricep Pushdown", 3, "10-12", 60)
                )),
                SeedTemplateDay("Pull Day", "Back, Biceps, Rear Delts", listOf(
                    te("Deadlift", 3, "5-6", 150), te("Pull Ups", 3, "AMRAP", 90),
                    te("Barbell Row", 3, "8-10", 90), te("Face Pull", 3, "12-15", 60),
                    te("Barbell Curl", 3, "10-12", 60)
                )),
                SeedTemplateDay("Leg Day", "Quadriceps, Hamstrings, Glutes, Calves", listOf(
                    te("Barbell Back Squat", 4, "6-8", 120), te("Romanian Deadlift", 3, "8-10", 90),
                    te("Leg Press", 3, "10-12", 90), te("Walking Lunges", 3, "10 each leg", 75),
                    te("Calf Raise", 4, "12-15", 45)
                ))
            )
        ),

        SeedTemplate(
            "Upper Lower", "A balanced 4-day split alternating upper and lower body sessions.",
            4, "Intermediate", "Strength",
            listOf(
                SeedTemplateDay("Upper Body", "Chest, Back, Shoulders, Arms", listOf(
                    te("Barbell Bench Press", 4, "6-8", 120), te("Barbell Row", 4, "6-8", 120),
                    te("Overhead Press", 3, "8-10", 90), te("Lat Pulldown", 3, "10-12", 75),
                    te("Dumbbell Curl", 2, "10-12", 60), te("Tricep Pushdown", 2, "10-12", 60)
                )),
                SeedTemplateDay("Lower Body", "Quadriceps, Hamstrings, Glutes, Core", listOf(
                    te("Barbell Back Squat", 4, "6-8", 120), te("Romanian Deadlift", 3, "8-10", 90),
                    te("Bulgarian Split Squat", 3, "10-12", 75), te("Calf Raise", 3, "12-15", 45),
                    te("Plank", 3, "30-45s", 45)
                ))
            )
        ),

        SeedTemplate(
            "Fat Loss Circuit", "High-intensity, full-body training designed to maximize calorie burn.",
            5, "Intermediate", "Fat loss",
            listOf(
                SeedTemplateDay("Full Body HIIT", "Full Body, Cardio", listOf(
                    te("Burpees", 4, "12-15", 45), te("Mountain Climbers", 4, "20", 45),
                    te("Walking Lunges", 3, "12/leg", 60), te("Push Ups", 3, "AMRAP", 45),
                    te("Jump Rope", 3, "60s", 45)
                )),
                SeedTemplateDay("Cardio + Core", "Cardio, Abs", listOf(
                    te("Running", 1, "20-25 min", 0), te("Plank", 3, "30-45s", 30),
                    te("Cable Crunch", 3, "15-20", 45), te("Hanging Leg Raise", 3, "10-12", 60)
                )),
                SeedTemplateDay("Lower Body Circuit", "Legs, Glutes", listOf(
                    te("Barbell Back Squat", 3, "12-15", 60), te("Glute Bridge", 3, "15-20", 45),
                    te("Walking Lunges", 3, "12/leg", 45), te("Stair Climbing", 1, "15 min", 0)
                ))
            )
        ),

        SeedTemplate(
            "Home Workout (No Equipment)", "A bodyweight-only routine for training anywhere.",
            4, "Beginner", "General fitness",
            listOf(
                SeedTemplateDay("Upper Body", "Chest, Back, Arms, Core", listOf(
                    te("Push Ups", 4, "AMRAP", 60), te("Plank", 3, "30-45s", 45),
                    te("Mountain Climbers", 3, "20", 45)
                )),
                SeedTemplateDay("Lower Body", "Legs, Glutes", listOf(
                    te("Walking Lunges", 4, "12/leg", 60), te("Glute Bridge", 3, "15-20", 45),
                    te("Calf Raise", 3, "15-20", 30)
                )),
                SeedTemplateDay("Full Body Conditioning", "Full Body, Cardio", listOf(
                    te("Burpees", 4, "12-15", 45), te("Mountain Climbers", 3, "20", 45),
                    te("Push Ups", 3, "AMRAP", 45), te("Plank", 3, "30-45s", 30)
                ))
            )
        ),

        SeedTemplate(
            "Warm-Up & Mobility", "Dynamic drills to prep for a lifting session, plus static stretches for flexibility and recovery.",
            5, "Beginner", "Mobility",
            listOf(
                SeedTemplateDay("Pre-Lifting Warm-Up", "Full Body, Mobility", listOf(
                    te("Leg Swings", 1, "10-12/leg", 10),
                    te("High Knees", 1, "20-30s", 15),
                    te("Jumping Jacks", 1, "30-45s", 15),
                    te("Inchworm", 1, "6-8", 15),
                    te("Standing Toe Touch", 1, "8-10", 10)
                )),
                SeedTemplateDay("Full-Body Mobility Flow", "Balance, Flexibility, Full Body", listOf(
                    te("Mountain Pose", 1, "5-8 breaths", 10),
                    te("Cat-Cow Stretch", 1, "8-10 rounds", 10),
                    te("Downward-Facing Dog", 2, "5-8 breaths", 10),
                    te("Warrior I", 2, "5 breaths/side", 10),
                    te("Warrior II", 2, "5 breaths/side", 10),
                    te("Cobra Pose", 2, "3-5 breaths", 10)
                )),
                SeedTemplateDay("Cool-Down & Recovery", "Flexibility, Full Body", listOf(
                    te("Seated Forward Bend", 2, "5-8 breaths", 10),
                    te("Bridge Pose", 2, "5 breaths", 10),
                    te("Standing Side Bend", 1, "3 each side", 10),
                    te("Child's Pose", 1, "5-10 breaths", 10),
                    te("Relaxation Pose", 1, "1-5 min", 0)
                ))
            ),
            isYoga = true
        )
    )
}
