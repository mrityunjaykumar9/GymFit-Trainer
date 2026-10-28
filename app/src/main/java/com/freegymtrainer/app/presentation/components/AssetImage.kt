package com.freegymtrainer.app.presentation.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

/**
 * Decodes a static image bundled under `assets/<path>` downsampled to roughly [reqWidth]x[reqHeight]
 * instead of full source resolution, or null if it's missing/unreadable.
 *
 * Decoding at full resolution and letting Compose scale it down on draw (what a plain
 * `BitmapFactory.decodeStream(it)` does) still allocates the whole uncompressed bitmap in
 * memory - e.g. a 1024x1024 source is ~4MB as ARGB_8888 even when it's only ever shown at
 * thumbnail size. [calculateInSampleSize] picks the largest power-of-two downsample that still
 * covers the requested size, so memory use roughly matches what's actually displayed. This is
 * also what Play Console's "bitmap downsampling" technical-quality check looks for.
 */
@Composable
fun rememberAssetImageBitmap(path: String, reqWidth: Int = 1080, reqHeight: Int = 1080): ImageBitmap? {
    val context = LocalContext.current
    return remember(path, reqWidth, reqHeight) {
        decodeSampledAssetBitmap(context, path, reqWidth, reqHeight)
    }
}

/** Decodes `assets/<path>` downsampled to roughly [reqWidth]x[reqHeight]; null if missing/unreadable. */
fun decodeSampledAssetBitmap(context: Context, path: String, reqWidth: Int, reqHeight: Int): ImageBitmap? =
    try {
        // First pass: inJustDecodeBounds reads only the header (width/height), no pixel data.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
        }
        // Second pass: actually decode, subsampled - a fresh stream since the first was consumed.
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }
    } catch (e: IOException) {
        null
    }

/** Largest power-of-two [BitmapFactory.Options.inSampleSize] whose output still covers reqWidth x reqHeight. */
fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (rawWidth <= 0 || rawHeight <= 0) return inSampleSize
    if (rawHeight > reqHeight || rawWidth > reqWidth) {
        var halfHeight = rawHeight / 2
        var halfWidth = rawWidth / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
