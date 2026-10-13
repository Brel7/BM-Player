package com.bmplayer.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bmplayer.MainActivity
import com.bmplayer.R
import com.bmplayer.data.preferences.UserPreferencesStore
import kotlinx.coroutines.flow.first

class BMPlayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = UserPreferencesStore(context)
        val initialPreferences = store.preferences.first()
        val snapshots = WidgetState.snapshots(context)
        provideContent {
            val snapshot by snapshots.collectAsState()
            val preferences by store.preferences.collectAsState(initialPreferences)
            WidgetContent(snapshot, preferences.widgetStyle, preferences.language == "en")
        }
    }

    @Composable
    private fun WidgetContent(snapshot: WidgetSnapshot, style: String, english: Boolean) {
        val large = LocalSize.current == LARGE
        val background = if (style == "glass") {
            GlanceModifier.background(ImageProvider(R.drawable.widget_glass_background))
        } else {
            GlanceModifier.background(Color(widgetAccentBackground(snapshot.accent ?: 0xFF1E1E1E.toInt()))).cornerRadius(24.dp)
        }
        // The card hugs its one-line content; a taller cell leaves transparent space instead of an oversized card.
        Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                GlanceModifier.fillMaxWidth().then(background).clickable(actionStartActivity<MainActivity>()).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = snapshot.artwork?.let(::ImageProvider) ?: ImageProvider(R.drawable.bm_player_logo),
                    contentDescription = null,
                    modifier = GlanceModifier.size(48.dp).cornerRadius(12.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(GlanceModifier.width(12.dp))
                if (large) {
                    // Text and controls share one row so a 4x1 widget (often < 80 dp tall) still gets the full layout.
                    Column(GlanceModifier.defaultWeight()) {
                        Text(
                            snapshot.title ?: if (english) "Nothing playing" else "Aucune lecture en cours",
                            maxLines = 1,
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                        if (snapshot.artist.isNotBlank()) {
                            Text(snapshot.artist, maxLines = 1, style = TextStyle(color = ColorProvider(Color(0xCCFFFFFF)), fontSize = 12.sp))
                        }
                    }
                    ControlButton(R.drawable.ic_widget_previous, if (english) "Previous" else "Précédent", command("previous"))
                    ToggleButton(snapshot, english)
                    ControlButton(R.drawable.ic_widget_next, if (english) "Next" else "Suivant", command("next"))
                } else {
                    Spacer(GlanceModifier.defaultWeight())
                    ToggleButton(snapshot, english)
                }
            }
        }
    }

    @Composable
    private fun ToggleButton(snapshot: WidgetSnapshot, english: Boolean) {
        val icon = if (snapshot.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        val label = if (snapshot.isPlaying) "Pause" else if (english) "Play" else "Lecture"
        // An empty queue has nothing to resume, so the button opens the app instead.
        ControlButton(icon, label, if (snapshot.title == null) actionStartActivity<MainActivity>() else command("toggle"))
    }

    @Composable
    private fun ControlButton(icon: Int, description: String, action: Action) {
        Image(ImageProvider(icon), contentDescription = description, modifier = GlanceModifier.size(36.dp).padding(4.dp).clickable(action))
    }

    private fun command(name: String): Action = actionRunCallback<WidgetControlCallback>(actionParametersOf(WidgetControlCallback.COMMAND to name))

    private companion object {
        val SMALL = DpSize(100.dp, 48.dp)
        val LARGE = DpSize(240.dp, 64.dp)
    }
}
