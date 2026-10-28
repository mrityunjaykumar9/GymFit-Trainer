package com.freegymtrainer.app.presentation.components

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.data.local.entity.Exercise

/**
 * Turns an exercise name into the file name it is looked up by under `assets/`, e.g. "Barbell Bench Press" ->
 * "barbell_bench_press". Parenthesized suffixes and apostrophes are dropped first (so "Pec Deck (Machine Fly)" ->
 * "pec_deck" and "Farmer's Carry" -> "farmers_carry"), then it's lowercased with any other non-alphanumeric run
 * collapsed to a single underscore.
 */
fun exerciseSlug(name: String): String {
    val withoutParens = name.replace(Regex("\\([^)]*\\)"), "")
    val withoutApostrophes = withoutParens.replace("'", "").replace("’", "")
    return withoutApostrophes.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
}

/** Which picture of the 3D athlete: the start of a rep, its peak, or a zoomed-in thumbnail. Files: assets/exercise_pics/<slug>_a|b|t.webp */
enum class ExercisePicKind(val suffix: String) { START("a"), PEAK("b"), THUMB("t") }

private val picCache = LruCache<String, ImageBitmap>(48)

/** The pre-rendered picture of [exerciseName] (a small WebP made from the 3D athlete), or null if there is none. */
@Composable
fun rememberExercisePic(exerciseName: String, kind: ExercisePicKind, reqPx: Int = 320): ImageBitmap? {
    val context = LocalContext.current
    return remember(exerciseName, kind, reqPx) {
        val path = "exercise_pics/${exerciseSlug(exerciseName)}_${kind.suffix}.webp"
        picCache.get(path) ?: decodeSampledAssetBitmap(context, path, reqPx, reqPx)?.also { picCache.put(path, it) }
    }
}

/** Square thumbnail of the exercise (the athlete at the peak of the movement) with a tidy icon fallback. */
@Composable
fun ExerciseThumb(exercise: Exercise, modifier: Modifier = Modifier, kind: ExercisePicKind = ExercisePicKind.THUMB) {
    val pic = rememberExercisePic(exercise.name, kind, reqPx = 240)
    val cs = MaterialTheme.colorScheme
    Box(
        modifier.background(Brush.verticalGradient(listOf(cs.surfaceVariant, cs.surface))),
        contentAlignment = Alignment.Center
    ) {
        if (pic != null) {
            Image(bitmap = pic, contentDescription = exercise.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(muscleGroupIcon(exercise.primaryMuscle), contentDescription = null, tint = cs.primary, modifier = Modifier.size(28.dp))
        }
    }
}
