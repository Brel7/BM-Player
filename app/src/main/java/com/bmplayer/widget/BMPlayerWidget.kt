package com.bmplayer.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.unit.ColorProvider
import com.bmplayer.MainActivity
import androidx.compose.ui.graphics.Color

class BMPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetPreferences = context.getSharedPreferences("bm_player_widget", Context.MODE_PRIVATE)
        val trackTitle = widgetPreferences.getString("title", null)?.takeIf(String::isNotBlank)
        val artist = widgetPreferences.getString("artist", null).orEmpty()
        val launchIntent = Intent(context, MainActivity::class.java)
        provideContent { WidgetContent(trackTitle, artist, launchIntent) }
    }

    @Composable
    private fun WidgetContent(trackTitle: String?, artist: String, launchIntent: Intent) {
        Column(
            GlanceModifier.fillMaxSize()
                .clickable(actionStartActivity(launchIntent))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BM PLAYER", style = TextStyle(color = ColorProvider(Color(0xFFB9750F)), fontSize = 12.sp))
            Text(trackTitle ?: "Aucune lecture en cours", maxLines = 1, style = TextStyle(fontSize = 18.sp))
            if (artist.isNotBlank()) Text(artist, maxLines = 1, style = TextStyle(fontSize = 13.sp))
        }
    }
}
