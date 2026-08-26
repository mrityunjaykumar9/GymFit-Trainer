package com.freegymtrainer.app.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

/** Decodes a static image bundled under `assets/<path>`, or null if it's missing/unreadable. */
@Composable
fun rememberAssetImageBitmap(path: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(path) {
        try {
            context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        } catch (e: IOException) {
            null
        }
    }
}
