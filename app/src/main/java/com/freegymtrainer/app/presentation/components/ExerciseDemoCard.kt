package com.freegymtrainer.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.presentation.components.motion.ExerciseMotionLibrary
import com.freegymtrainer.app.presentation.components.motion.ExerciseMotionPlayer

/**
 * The big "how do I do this" panel on the exercise detail and workout screens: the 3D athlete performing the
 * movement (rotate, slow down, read the form cue for each step). An exercise with no animation falls back to its
 * picture.
 */
@Composable
fun ExerciseDemoCard(exercise: Exercise, modifier: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
    val motion = remember(exercise.name) { ExerciseMotionLibrary.forExerciseName(context, exercise.name) }

    if (motion == null) {
        ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
            ExerciseThumb(exercise = exercise, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
        }
        return
    }

    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            ExerciseMotionPlayer(exercise = exercise, motion = motion, compact = compact)
        }
    }
}
