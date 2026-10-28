package com.freegymtrainer.app.domain.training

/** How much training experience the routine is sized for. */
enum class TrainingLevel(val label: String, val blurb: String) {
    BEGINNER("Beginner", "Simple, joint-friendly lifts. 3 sets, lighter loads, learn the movements."),
    INTERMEDIATE("Intermediate", "More exercises and heavier compound lifts. 3-4 sets."),
    ADVANCED("Advanced", "Full volume with the hardest variations. 4 sets, push the last reps.");

    companion object {
        /** Maps the experience picked during onboarding. */
        fun fromProfile(experience: String): TrainingLevel =
            values().firstOrNull { it.label.equals(experience, ignoreCase = true) } ?: BEGINNER
    }
}

/** One exercise in a routine: [exercise] must match an exercise name in the library. */
data class PlanStep(val exercise: String, val sets: Int, val reps: String, val restSeconds: Int, val tip: String)

/**
 * A body part (or group) the person can choose to train, with how to train it and a ready-made
 * routine for each level. [front] says which side of the body the muscle map shows; [regions] are
 * the muscle-map areas to light up.
 */
class BodyPart(
    val id: String,
    val title: String,
    val subtitle: String,
    val front: Boolean,
    val regions: Set<String>,
    val howToTrain: List<String>,
    val plans: Map<TrainingLevel, List<PlanStep>>
) {
    fun plan(level: TrainingLevel): List<PlanStep> = plans.getValue(level)
    val allExerciseNames: Set<String> get() = plans.values.flatten().map { it.exercise }.toSet()
}

/** Ready-made "I am training X today" routines built from the exercises in the library. */
object BodyPartPlans {

    private fun s(name: String, sets: Int, reps: String, rest: Int, tip: String) = PlanStep(name, sets, reps, rest, tip)

    private fun levels(beginner: List<PlanStep>, intermediate: List<PlanStep>, advanced: List<PlanStep>) = mapOf(
        TrainingLevel.BEGINNER to beginner,
        TrainingLevel.INTERMEDIATE to intermediate,
        TrainingLevel.ADVANCED to advanced
    )

    val all: List<BodyPart> = listOf(
        BodyPart(
            "chest", "Chest", "Pecs and upper chest", true, setOf("chest"),
            listOf(
                "Train chest 1-2 times a week with at least 48 hours between sessions.",
                "Start with a heavy press, then move to fly and push-up variations for the squeeze.",
                "Keep shoulder blades pinched back and down on every press - it protects your shoulders.",
                "Lower the weight for 2-3 seconds and press up smoothly. No bouncing."
            ),
            levels(
                listOf(
                    s("Dumbbell Bench Press", 3, "10-12", 90, "Lower until your elbows are just below the bench, elbows about 45 degrees from your body."),
                    s("Incline Dumbbell Press", 3, "10-12", 90, "Bench at a low incline; this builds the upper chest."),
                    s("Cable Fly", 3, "12-15", 60, "Soft bend in the elbows, hug a big tree."),
                    s("Push Ups", 2, "As many as you can", 60, "Body in one straight line. Drop to your knees if you must.")
                ),
                listOf(
                    s("Barbell Bench Press", 4, "6-10", 120, "Your heavy lift. Feet planted, shoulder blades pinched, bar to mid-chest."),
                    s("Incline Dumbbell Press", 3, "8-12", 90, "Control the way down."),
                    s("Cable Fly", 3, "12-15", 60, "Squeeze hard when your hands meet."),
                    s("Pec Deck (Machine Fly)", 3, "12-15", 60, "Pause for a second at the squeeze."),
                    s("Push Ups", 2, "To failure", 60, "A finisher to burn out the last bit of chest.")
                ),
                listOf(
                    s("Barbell Bench Press", 4, "5-8", 150, "Add weight only when every rep is clean."),
                    s("Incline Barbell Bench Press", 4, "6-10", 120, "Bar to the upper chest."),
                    s("Dumbbell Bench Press", 3, "8-12", 90, "A deeper stretch than the barbell."),
                    s("Cable Fly", 3, "12-15", 60, "Slow and controlled."),
                    s("Pec Deck (Machine Fly)", 3, "12-15", 60, "Last set: add a 10-second hold at the squeeze."),
                    s("Push Ups", 2, "To failure", 60, "Finish with a burnout.")
                )
            )
        ),
        BodyPart(
            "back", "Back", "Lats, mid-back and spinal erectors", false, setOf("lats", "traps"),
            listOf(
                "Train back 1-2 times a week. Use both vertical pulls (pull-ups, pulldowns) and rows.",
                "Think about driving your elbows toward your back pockets, not about pulling with your hands.",
                "Keep your chest up and spine neutral on every row and deadlift.",
                "Pause for a moment with the shoulder blades squeezed together."
            ),
            levels(
                listOf(
                    s("Lat Pulldown", 3, "10-12", 90, "Pull the bar to your upper chest and lead with your elbows."),
                    s("Seated Cable Row", 3, "10-12", 90, "Sit tall, squeeze your shoulder blades together."),
                    s("Face Pull", 3, "12-15", 60, "Rope to your face with elbows high - great for posture.")
                ),
                listOf(
                    s("Pull Ups", 3, "5-10", 120, "Use an assisted machine or a band if you cannot do five."),
                    s("Barbell Row", 4, "8-10", 90, "Hinge forward with a flat back, row to your lower ribs."),
                    s("Lat Pulldown", 3, "10-12", 90, "A slower, controlled lowering phase."),
                    s("Seated Cable Row", 3, "10-12", 90, "Do not swing your torso."),
                    s("Face Pull", 3, "12-15", 60, "Finish with rear-shoulder and upper-back work.")
                ),
                listOf(
                    s("Deadlift", 4, "4-6", 180, "The king of back lifts. Neutral spine, bar close to your shins."),
                    s("Pull Ups", 4, "6-10", 120, "Add weight when you can do 10 clean reps."),
                    s("Barbell Row", 4, "6-10", 120, "Row hard, lower slowly."),
                    s("T-Bar Row", 3, "8-12", 90, "Chest supported if your lower back is tired."),
                    s("Seated Cable Row", 3, "10-12", 90, "Squeeze at the end of every rep."),
                    s("Face Pull", 3, "12-15", 60, "Keep shoulders healthy.")
                )
            )
        ),
        BodyPart(
            "shoulders", "Shoulders", "Front, side and rear delts", true, setOf("shoulders"),
            listOf(
                "Train shoulders 1-2 times a week. Side and rear delts need as much work as the front.",
                "Lateral raises work best with light weight and strict form - no swinging.",
                "Warm up the shoulder joint with a few light sets before pressing.",
                "Never press behind your neck; keep the bar in front of your face."
            ),
            levels(
                listOf(
                    s("Dumbbell Shoulder Press", 3, "10-12", 90, "Press straight up, wrists stacked over elbows."),
                    s("Lateral Raise", 3, "12-15", 60, "Lift to shoulder height, lead with your elbows."),
                    s("Rear Delt Fly", 3, "12-15", 60, "Hinge forward and open your arms like wings.")
                ),
                listOf(
                    s("Overhead Press", 4, "6-10", 120, "Squeeze your glutes and keep your ribs down."),
                    s("Lateral Raise", 4, "12-15", 60, "Light weight, perfect form."),
                    s("Rear Delt Fly", 3, "12-15", 60, "Squeeze your shoulder blades."),
                    s("Face Pull", 3, "12-15", 60, "Great for rear delts and posture.")
                ),
                listOf(
                    s("Overhead Press", 4, "5-8", 150, "Heavy, controlled and strict."),
                    s("Dumbbell Shoulder Press", 3, "8-12", 90, "A bigger range of motion than the barbell."),
                    s("Lateral Raise", 4, "12-15", 60, "Last set: drop the weight and keep going."),
                    s("Front Raise", 3, "10-12", 60, "Stop at eye level."),
                    s("Rear Delt Fly", 3, "12-15", 60, "Slow and controlled."),
                    s("Face Pull", 3, "12-15", 60, "Finish with shoulder health work.")
                )
            )
        ),
        BodyPart(
            "biceps", "Biceps", "Front of the upper arm", true, setOf("biceps"),
            listOf(
                "Train biceps 2 times a week, after your back work (they already get hit on rows and pulldowns).",
                "Keep your elbows pinned to your sides - do not swing your body.",
                "Control the lowering phase; it builds as much muscle as the lift.",
                "Vary the grip: palms up (curl), neutral (hammer) and a cable for constant tension."
            ),
            levels(
                listOf(
                    s("Dumbbell Curl", 3, "10-12", 60, "Palms up, elbows still, squeeze at the top."),
                    s("Hammer Curl", 3, "10-12", 60, "Thumbs up - hits the forearm and the outer biceps."),
                    s("Cable Curl", 2, "12-15", 60, "Constant tension from the cable.")
                ),
                listOf(
                    s("Barbell Curl", 3, "8-12", 75, "Do not lean back."),
                    s("Preacher Curl", 3, "10-12", 75, "Keep your upper arms flat on the pad."),
                    s("Hammer Curl", 3, "10-12", 60, "Alternate arms if you like."),
                    s("Cable Curl", 3, "12-15", 60, "Squeeze hard at the top.")
                ),
                listOf(
                    s("Barbell Curl", 4, "6-10", 90, "Heavy but strict."),
                    s("Preacher Curl", 4, "8-12", 75, "Stretch fully at the bottom."),
                    s("Dumbbell Curl", 3, "8-12", 60, "Twist your wrist up as you lift."),
                    s("Hammer Curl", 3, "10-12", 60, "Neutral grip."),
                    s("Cable Curl", 3, "12-15", 60, "Finish with a burnout.")
                )
            )
        ),
        BodyPart(
            "triceps", "Triceps", "Back of the upper arm", false, setOf("triceps"),
            listOf(
                "The triceps are about two-thirds of your upper arm - train them as hard as your biceps.",
                "Press and extend movements both matter: one heavy compound, then isolation.",
                "Keep your elbows pointing forward and close to your head or sides.",
                "Lock out fully to squeeze the muscle at the end of each rep."
            ),
            levels(
                listOf(
                    s("Tricep Pushdown", 3, "10-12", 60, "Elbows pinned to your sides, press all the way down."),
                    s("Overhead Tricep Extension", 3, "10-12", 60, "Lower behind your head, elbows pointing forward."),
                    s("Push Ups", 2, "As many as you can", 60, "Hands close together to emphasise the triceps.")
                ),
                listOf(
                    s("Close Grip Bench Press", 4, "6-10", 90, "Hands shoulder-width apart, elbows tucked."),
                    s("Tricep Pushdown", 3, "10-12", 60, "Squeeze at the bottom."),
                    s("Overhead Tricep Extension", 3, "10-12", 60, "Big stretch at the bottom."),
                    s("Skull Crushers", 3, "10-12", 75, "Lower to your forehead, only your forearms move.")
                ),
                listOf(
                    s("Close Grip Bench Press", 4, "5-8", 120, "Heavy and controlled."),
                    s("Skull Crushers", 4, "8-12", 75, "Keep your upper arms vertical."),
                    s("Overhead Tricep Extension", 3, "10-12", 60, "Full stretch."),
                    s("Tricep Pushdown", 3, "12-15", 60, "Finish with a burnout.")
                )
            )
        ),
        BodyPart(
            "legs", "Legs", "Quads, hamstrings and calves", true, setOf("quads", "hamstrings", "calves"),
            listOf(
                "Train legs 1-2 times a week - they are your biggest muscles and respond to heavy work.",
                "Squat or press to at least parallel, knees tracking over your toes.",
                "Include a hamstring movement (Romanian deadlift) as well as quads.",
                "Do not skip calves: straight-leg and bent-knee raises with a full stretch."
            ),
            levels(
                listOf(
                    s("Leg Press", 3, "10-12", 90, "Feet shoulder-width, do not lock your knees at the top."),
                    s("Walking Lunges", 3, "10 each leg", 75, "Long steps, torso upright."),
                    s("Leg Extension", 3, "12-15", 60, "Squeeze at the top for a second."),
                    s("Romanian Deadlift", 3, "10-12", 75, "Hips back, soft knees, feel the hamstring stretch."),
                    s("Calf Raise", 3, "15-20", 45, "Full stretch at the bottom, pause at the top.")
                ),
                listOf(
                    s("Barbell Back Squat", 4, "6-10", 150, "Brace your core, sit between your hips, drive up through the mid-foot."),
                    s("Romanian Deadlift", 3, "8-10", 120, "Hips back, soft knees, feel the hamstring stretch."),
                    s("Leg Press", 3, "10-12", 90, "Do not let your lower back round."),
                    s("Leg Extension", 3, "12-15", 60, "Pause at the top."),
                    s("Calf Raise", 4, "12-20", 45, "Slow and with a full range.")
                ),
                listOf(
                    s("Barbell Back Squat", 5, "5-8", 180, "Heavy and deep. Use safety bars."),
                    s("Front Squat", 3, "6-10", 120, "Elbows high, upright torso."),
                    s("Romanian Deadlift", 4, "6-10", 120, "Keep the bar close to your legs."),
                    s("Bulgarian Split Squat", 3, "8-10 each leg", 90, "Stay tall and control the descent."),
                    s("Calf Raise", 4, "12-20", 45, "Finish with burning calves.")
                )
            )
        ),
        BodyPart(
            "glutes", "Glutes", "Hips and rear", false, setOf("glutes"),
            listOf(
                "Train glutes 2 times a week - hip hinges and thrusts work best.",
                "Squeeze your glutes hard at the top of every rep.",
                "Keep your ribs down so the work comes from the hips, not the lower back.",
                "Progress by adding weight or slowing the lowering phase."
            ),
            levels(
                listOf(
                    s("Glute Bridge", 3, "12-15", 60, "Drive through your heels and squeeze at the top."),
                    s("Walking Lunges", 3, "10 each leg", 75, "Take a long step to bring in the glutes."),
                    s("Leg Press", 3, "12-15", 90, "Feet high on the platform to emphasise the glutes.")
                ),
                listOf(
                    s("Hip Thrust", 4, "8-12", 90, "Chin tucked, ribs down, pause at the top."),
                    s("Romanian Deadlift", 3, "8-10", 120, "Push your hips back."),
                    s("Bulgarian Split Squat", 3, "8-10 each leg", 90, "Lean slightly forward."),
                    s("Glute Bridge", 2, "15-20", 45, "Finish with a burnout.")
                ),
                listOf(
                    s("Hip Thrust", 4, "6-10", 120, "Your heavy glute lift."),
                    s("Barbell Back Squat", 4, "6-10", 150, "Sit deep to bring in the glutes."),
                    s("Romanian Deadlift", 4, "6-10", 120, "Deep hamstring and glute stretch."),
                    s("Bulgarian Split Squat", 3, "8-10 each leg", 90, "Control every rep."),
                    s("Glute Bridge", 3, "15-20", 45, "Pump finisher.")
                )
            )
        ),
        BodyPart(
            "abs", "Abs & Core", "Abs, obliques and deep core", true, setOf("abs"),
            listOf(
                "Train abs 2-3 times a week. Abs are visible only when your body fat is low - diet matters most.",
                "Quality over quantity: slow reps with a hard squeeze beat fast ones.",
                "Breathe out as you crunch to engage the deep core muscles.",
                "Keep your lower back protected - do not pull on your neck."
            ),
            levels(
                listOf(
                    s("Crunch", 3, "15-20", 45, "Curl your ribs toward your hips, do not pull your neck."),
                    s("Plank", 3, "30-45 sec", 45, "Body in a straight line, squeeze glutes and abs."),
                    s("Mountain Climbers", 3, "30 sec", 45, "Drive your knees to your chest quickly with hips low.")
                ),
                listOf(
                    s("Cable Crunch", 4, "12-15", 60, "Round your spine and bring elbows toward thighs."),
                    s("Plank", 3, "45-60 sec", 45, "Do not let your hips sag."),
                    s("Crunch", 3, "15-20", 45, "Pause at the top."),
                    s("Mountain Climbers", 3, "40 sec", 45, "Fast and controlled.")
                ),
                listOf(
                    s("Hanging Leg Raise", 4, "10-15", 60, "No swinging - curl your pelvis up."),
                    s("Cable Crunch", 4, "12-15", 60, "Add weight gradually."),
                    s("Plank", 3, "60 sec", 45, "A longer hold with perfect form."),
                    s("Mountain Climbers", 3, "45 sec", 45, "Finish with a conditioning burst.")
                )
            )
        ),
        BodyPart(
            "forearms", "Traps & Forearms", "Neck, upper traps and grip", true, setOf("traps", "forearms"),
            listOf(
                "Traps and forearms grow with heavy carrying and shrugging - 1-2 times a week is enough.",
                "Shrug straight up toward your ears; do not roll your shoulders.",
                "Strong forearms help every pulling exercise, so grip strength carries over.",
                "Use straps only on your heaviest sets so your grip keeps improving."
            ),
            levels(
                listOf(
                    s("Barbell Shrug", 3, "12-15", 60, "Lift straight up and hold for a second."),
                    s("Farmer's Carry", 3, "30-40 m", 60, "Shoulders back, walk tall with a firm grip."),
                    s("Wrist Curl", 3, "15-20", 45, "Forearms on your thighs, only your wrists move.")
                ),
                listOf(
                    s("Barbell Shrug", 4, "10-15", 60, "Slow and controlled."),
                    s("Farmer's Carry", 4, "40 m", 60, "Heavier each week."),
                    s("Wrist Curl", 3, "15-20", 45, "Let the bar roll to your fingertips and curl up.")
                ),
                listOf(
                    s("Deadlift", 3, "4-6", 180, "Heavy pulls build the traps and grip."),
                    s("Barbell Shrug", 4, "8-12", 75, "Heavy with a pause at the top."),
                    s("Farmer's Carry", 4, "40-50 m", 60, "As heavy as you can hold."),
                    s("Wrist Curl", 3, "15-20", 45, "Finish with a forearm burn.")
                )
            )
        ),
        BodyPart(
            "full", "Full Body", "Train everything in one session", true, setOf("chest", "shoulders", "abs", "quads", "biceps"),
            listOf(
                "Full-body sessions are ideal 2-3 times a week if you cannot train every day.",
                "Pick one lift for legs, one push, one pull and one core move.",
                "Rest 48 hours between full-body days.",
                "Keep every lift 1-2 reps short of failure so you can repeat the session."
            ),
            levels(
                listOf(
                    s("Leg Press", 3, "10-12", 90, "A safe way to train the legs hard."),
                    s("Dumbbell Bench Press", 3, "10-12", 90, "Controlled press."),
                    s("Lat Pulldown", 3, "10-12", 90, "Lead with your elbows."),
                    s("Dumbbell Shoulder Press", 2, "10-12", 90, "Do not arch your lower back."),
                    s("Plank", 2, "30-45 sec", 45, "Finish with a core hold.")
                ),
                listOf(
                    s("Barbell Back Squat", 4, "6-10", 150, "Your leg lift for the day."),
                    s("Dumbbell Bench Press", 3, "8-12", 90, "Push."),
                    s("Seated Cable Row", 3, "10-12", 90, "Pull."),
                    s("Overhead Press", 3, "8-10", 90, "Press overhead."),
                    s("Plank", 3, "45 sec", 45, "Core.")
                ),
                listOf(
                    s("Deadlift", 4, "4-6", 180, "Pull from the floor."),
                    s("Barbell Back Squat", 4, "5-8", 150, "Squat deep."),
                    s("Barbell Bench Press", 4, "5-8", 150, "Press."),
                    s("Pull Ups", 3, "6-10", 120, "Pull."),
                    s("Overhead Press", 3, "6-10", 120, "Press overhead."),
                    s("Hanging Leg Raise", 3, "10-12", 60, "Core finisher.")
                )
            )
        )
    )

    fun byId(id: String): BodyPart? = all.firstOrNull { it.id == id }
}
