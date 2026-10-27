package com.bmplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap
import com.bmplayer.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

suspend fun loadArtworkBitmap(context: Context, trackUri: Uri, artworkUri: Uri?, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
    val embedded = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, trackUri)
            retriever.embeddedPicture?.let { data ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide) }
                BitmapFactory.decodeByteArray(data, 0, data.size, options)
            }
        }
    }.getOrNull()
    val artwork = embedded ?: artworkUri?.let { uri ->
        runCatching { context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) }.getOrNull()
    } ?: return@withContext null
    val (width, height) = fitWithin(artwork.width, artwork.height, maxSide)
    if (width == artwork.width && height == artwork.height) artwork else Bitmap.createScaledBitmap(artwork, width, height, true)
}

// Decoding a cover at full size on every bind made lists stutter and covers blink on the way back to a screen.
private val artworkCache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap) = value.byteCount
}

fun cachedArtwork(trackId: Long, maxSide: Int): Bitmap? = artworkCache.get("$trackId@$maxSide")

// A track with no cover at all is remembered too, otherwise every bind re-opened the file for nothing.
private val artworkMisses = ConcurrentHashMap.newKeySet<String>()

suspend fun loadCachedArtwork(context: Context, track: Track, maxSide: Int): Bitmap? {
    val key = "${track.id}@$maxSide"
    cachedArtwork(track.id, maxSide)?.let { return it }
    if (key in artworkMisses) return null
    val artwork = loadArtworkBitmap(context, track.uri, track.artworkUri, maxSide)
    if (artwork == null) artworkMisses += key else artworkCache.put(key, artwork)
    return artwork
}

fun dominantAccent(bitmap: Bitmap): Int? {
    val scale = 32f / maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
    val sample = Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true
    )
    val pixels = IntArray(sample.width * sample.height)
    sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
    var hueX = 0.0
    var hueY = 0.0
    var saturationTotal = 0.0
    var valueTotal = 0.0
    var weightTotal = 0.0
    val hsv = FloatArray(3)
    pixels.forEach { pixel ->
        Color.colorToHSV(pixel, hsv)
        if (hsv[1] > 0.18f && hsv[2] in 0.16f..0.96f) {
            val weight = hsv[1] * (1f - abs(hsv[2] - 0.62f))
            val angle = Math.toRadians(hsv[0].toDouble())
            hueX += cos(angle) * weight
            hueY += sin(angle) * weight
            saturationTotal += hsv[1] * weight
            valueTotal += hsv[2] * weight
            weightTotal += weight
        }
    }
    if (weightTotal <= 0.0) return null
    val hue = ((Math.toDegrees(atan2(hueY, hueX)) + 360.0) % 360.0).toFloat()
    val saturation = (saturationTotal / weightTotal).toFloat().coerceIn(0.45f, 0.85f)
    val value = (valueTotal / weightTotal).toFloat().coerceIn(0.55f, 0.88f)
    return Color.HSVToColor(floatArrayOf(hue, saturation, value))
}
