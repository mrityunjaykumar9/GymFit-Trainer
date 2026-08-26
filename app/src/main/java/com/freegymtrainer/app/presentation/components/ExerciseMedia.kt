package com.freegymtrainer.app.presentation.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.freegymtrainer.app.data.local.entity.Exercise
import kotlinx.coroutines.delay
import java.io.IOException

private val PHOTO_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

/**
 * Turns an exercise name into the file/folder name it's looked up by under `assets/`, e.g.
 * "Barbell Bench Press" -> "barbell_bench_press". Parenthesized suffixes and apostrophes are
 * dropped first (so "Pec Deck (Machine Fly)" -> "pec_deck" and "Farmer's Carry" ->
 * "farmers_carry" - matching how someone would naturally shorten the name for a filename),
 * then it's lowercased with any other non-alphanumeric run collapsed to a single underscore.
 */
fun exerciseSlug(name: String): String {
    val withoutParens = name.replace(Regex("\\([^)]*\\)"), "")
    val withoutApostrophes = withoutParens.replace("'", "").replace("’", "")
    return withoutApostrophes.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
}

private fun listAssetsSafely(context: Context, path: String): List<String> =
    try {
        context.assets.list(path)?.toList() ?: emptyList()
    } catch (e: IOException) {
        emptyList()
    }

private fun decodeAsset(context: Context, path: String): ImageBitmap? =
    try {
        context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    } catch (e: IOException) {
        null
    }

@Composable
private fun rememberPhotoFrames(slug: String): List<ImageBitmap> {
    val context = LocalContext.current
    return remember(slug) {
        // A single flat file (exercise_photos/<slug>.jpg) - the common case: one photo per
        // exercise, named exactly like the exercise.
        val flatFile = listAssetsSafely(context, "exercise_photos")
            .firstOrNull { it.substringBeforeLast('.') == slug && it.substringAfterLast('.', "").lowercase() in PHOTO_EXTENSIONS }
        if (flatFile != null) {
            return@remember listOfNotNull(decodeAsset(context, "exercise_photos/$flatFile"))
        }

        // Or a numbered sequence in its own folder (exercise_photos/<slug>/1.jpg, 2.jpg, ...)
        // for a multi-frame slideshow instead of a single still.
        val dir = "exercise_photos/$slug"
        listAssetsSafely(context, dir)
            .filter { it.substringAfterLast('.', "").lowercase() in PHOTO_EXTENSIONS }
            .sorted()
            .mapNotNull { fileName -> decodeAsset(context, "$dir/$fileName") }
    }
}

@Composable
private fun PhotoSlideshow(frames: List<ImageBitmap>, modifier: Modifier = Modifier) {
    var index by remember(frames) { mutableIntStateOf(0) }
    LaunchedEffect(frames) {
        if (frames.size <= 1) return@LaunchedEffect
        while (true) {
            delay(700)
            index = (index + 1) % frames.size
        }
    }
    Image(bitmap = frames[index], contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
}

/** Plays a video from a remote URL or a local `asset:///...` uri, looping with no controls. */
@Composable
private fun ExerciseVideoPlayback(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    AndroidView(
        factory = { PlayerView(it).apply { player = exoPlayer; useController = true } },
        modifier = modifier
    )
}

/**
 * Shows demonstration media for [exercise], in priority order:
 * 1. [Exercise.mediaUrl] - a remote-hosted clip, if you've set one.
 * 2. `assets/exercise_photos/<slug>.jpg` - a single photo named exactly like the exercise.
 *    `<slug>` is [exerciseSlug] of the exercise name, e.g. "Barbell Bench Press" ->
 *    `assets/exercise_photos/barbell_bench_press.jpg`.
 * 3. `assets/exercise_photos/<slug>/1.jpg, 2.jpg, ...` - or a numbered sequence in its own
 *    folder instead of a single file, auto-cycled as a slideshow.
 * 4. `assets/exercise_videos/<slug>.mp4` - a local video file, same slug convention.
 * 5. [AnimatedExerciseFigure] - the built-in animated placeholder, if none of the above exist.
 *
 * Photos are recommended over bundling videos: a handful of small photos per exercise adds
 * only a few hundred KB each, while 50 exercises x a 10s video would add tens to a few hundred
 * MB to the app depending on resolution/bitrate - a real difference for a free, ad-supported app.
 */
@Composable
fun ExerciseAutoMedia(exercise: Exercise, modifier: Modifier = Modifier) {
    val slug = remember(exercise.name) { exerciseSlug(exercise.name) }
    val context = LocalContext.current

    val remoteUrl = exercise.mediaUrl
    if (!remoteUrl.isNullOrBlank()) {
        ExerciseVideoPlayback(uri = remoteUrl, modifier = modifier)
        return
    }

    val photoFrames = rememberPhotoFrames(slug)
    if (photoFrames.isNotEmpty()) {
        PhotoSlideshow(photoFrames, modifier)
        return
    }

    val localVideoName = remember(slug) {
        val name = "$slug.mp4"
        if (listAssetsSafely(context, "exercise_videos").contains(name)) name else null
    }
    if (localVideoName != null) {
        ExerciseVideoPlayback(uri = "asset:///exercise_videos/$localVideoName", modifier = modifier)
        return
    }

    AnimatedExerciseFigure(category = exercise.category, modifier = modifier)
}
