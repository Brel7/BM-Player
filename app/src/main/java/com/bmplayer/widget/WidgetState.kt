package com.bmplayer.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow

// WCAG 2 relative luminance, in plain arithmetic so it runs in JVM unit tests.
fun relativeLuminance(argb: Int): Double {
    fun channel(shift: Int): Double {
        val c = ((argb shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

// Darkens until luminance <= 0.18, which keeps white text at a contrast of at least 4.5:1.
fun widgetAccentBackground(argb: Int): Int {
    var color = argb or (0xFF shl 24)
    while (relativeLuminance(color) > 0.18) {
        val r = ((color shr 16) and 0xFF) * 9 / 10
        val g = ((color shr 8) and 0xFF) * 9 / 10
        val b = (color and 0xFF) * 9 / 10
        color = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    return color
}

data class WidgetSnapshot(val title: String?, val artist: String, val isPlaying: Boolean, val accent: Int?, val artwork: Bitmap?)

object WidgetState {
    private const val PREFS = "bm_player_widget"

    private fun artworkFile(context: Context) = File(context.filesDir, "widget_artwork.png")

    // A running Glance session is not re-created by updateAll(), so the widget observes this flow instead of reading once.
    @Volatile private var flow: MutableStateFlow<WidgetSnapshot>? = null

    fun snapshots(context: Context): StateFlow<WidgetSnapshot> =
        flow ?: synchronized(this) { flow ?: MutableStateFlow(load(context)).also { flow = it } }

    fun save(context: Context, title: String?, artist: String, isPlaying: Boolean, accent: Int?, artwork: Bitmap?) {
        val file = artworkFile(context)
        if (artwork == null) file.delete() else file.outputStream().use { artwork.compress(Bitmap.CompressFormat.PNG, 100, it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("title", title)
            .putString("artist", artist)
            .putBoolean("isPlaying", isPlaying)
            .apply { if (accent == null) remove("accent") else putInt("accent", accent) }
            .apply()
        flow?.value = WidgetSnapshot(title?.takeIf(String::isNotBlank), artist, isPlaying, accent, artwork)
    }

    fun load(context: Context): WidgetSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val file = artworkFile(context)
        return WidgetSnapshot(
            title = prefs.getString("title", null)?.takeIf(String::isNotBlank),
            artist = prefs.getString("artist", null).orEmpty(),
            isPlaying = prefs.getBoolean("isPlaying", false),
            accent = if (prefs.contains("accent")) prefs.getInt("accent", 0) else null,
            artwork = if (file.exists()) BitmapFactory.decodeFile(file.path) else null
        )
    }
}
