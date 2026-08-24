package com.freegymtrainer.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Shows an exercise's demonstration clip when [mediaUrl] is set, otherwise falls back to a
 * looping animated stick-figure of the exercise's motion pattern ([AnimatedExerciseFigure]).
 * No exercise ships with a bundled clip today (see README -> "Adding exercise animations") -
 * this is the playback plumbing so that adding a real, licensed clip later is just setting
 * [com.freegymtrainer.app.data.local.entity.Exercise.mediaUrl].
 */
@Composable
fun ExerciseMediaPlayer(mediaUrl: String?, category: String, modifier: Modifier = Modifier) {
    if (mediaUrl.isNullOrBlank()) {
        AnimatedExerciseFigure(category = category, modifier = modifier)
        return
    }

    val context = LocalContext.current
    val exoPlayer = remember(mediaUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}
