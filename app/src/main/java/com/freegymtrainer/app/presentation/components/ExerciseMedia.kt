package com.freegymtrainer.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Shows an exercise's demonstration clip when [mediaUrl] is set, otherwise falls back to a
 * muscle-group icon placeholder. No exercise ships with a bundled clip today (see
 * README -> "Adding exercise animations") - this is the playback plumbing so that adding a
 * real, licensed clip later is just setting [com.freegymtrainer.app.data.local.entity.Exercise.mediaUrl].
 */
@Composable
fun ExerciseMediaPlayer(mediaUrl: String?, primaryMuscle: String, modifier: Modifier = Modifier) {
    if (mediaUrl.isNullOrBlank()) {
        MuscleIconPlaceholder(primaryMuscle, modifier)
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

@Composable
private fun MuscleIconPlaceholder(primaryMuscle: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                muscleGroupIcon(primaryMuscle),
                contentDescription = primaryMuscle,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text("$primaryMuscle demonstration", style = MaterialTheme.typography.titleMedium)
        }
    }
}
