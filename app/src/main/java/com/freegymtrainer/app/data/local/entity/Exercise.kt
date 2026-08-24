package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single exercise in the library. [instructions] and [commonMistakes] are stored as
 * ";;"-delimited strings (see Converters) to avoid pulling in a JSON library for an MVP.
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryMuscle: String,        // e.g. "Chest", "Back", "Legs"
    val secondaryMuscles: List<String>,
    val equipment: String,            // e.g. "Barbell", "Dumbbell", "Bodyweight"
    val difficulty: String,           // Beginner / Intermediate / Advanced
    val category: String,             // Push / Pull / Legs / Core / Cardio / Full Body
    val goalTags: List<String>,       // Muscle gain / Strength / Fat loss / Endurance / Mobility
    val instructions: List<String>,
    val commonMistakes: List<String>,
    val safetyNotes: String,
    // Null until a real demonstration clip is hosted somewhere and wired up per exercise -
    // the UI falls back to a muscle-group icon placeholder when this is null. See
    // ExerciseMediaPlayer and README -> "Adding exercise animations".
    val mediaUrl: String? = null
)
