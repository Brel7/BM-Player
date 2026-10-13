package com.bmplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private const val BACKGROUND_MAX_SIDE = 1440

fun fitWithin(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
    val longest = maxOf(width, height)
    if (longest <= maxSide) return width to height
    val scale = maxSide.toFloat() / longest
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}

fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    val longest = maxOf(width, height)
    var sample = 1
    while (longest / (sample * 2) >= maxSide) sample *= 2
    return sample
}

fun backgroundFile(context: Context): File = File(context.filesDir, "app_background.jpg")

suspend fun saveBackgroundImage(context: Context, source: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, BACKGROUND_MAX_SIDE) }
        val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return@runCatching false
        val (width, height) = fitWithin(decoded.width, decoded.height, BACKGROUND_MAX_SIDE)
        val scaled = Bitmap.createScaledBitmap(decoded, width, height, true)
        // Camera photos store their rotation in EXIF; without it portrait shots render sideways.
        val degrees = resolver.openInputStream(source)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val upright = if (degrees == 0f) scaled else Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, Matrix().apply { postRotate(degrees) }, true)
        val target = backgroundFile(context)
        val temp = File(context.filesDir, "app_background.tmp")
        temp.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        temp.renameTo(target) || run { temp.copyTo(target, overwrite = true); temp.delete() }
    }.getOrDefault(false)
}
