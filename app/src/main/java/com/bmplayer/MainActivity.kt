package com.bmplayer

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.bmplayer.data.local.FavoriteDao
import com.bmplayer.data.local.FavoriteEntity
import com.bmplayer.data.local.HistoryDao
import com.bmplayer.data.local.HistoryEntity
import com.bmplayer.data.local.PlaylistDao
import com.bmplayer.data.local.PlaylistEntity
import com.bmplayer.data.local.PlaylistTrackEntity
import com.bmplayer.data.preferences.UserPreferences
import com.bmplayer.data.preferences.UserPreferencesStore
import com.bmplayer.domain.model.Track
import com.bmplayer.playback.PlaybackCommands
import com.bmplayer.playback.PlaybackService
import com.bmplayer.ui.LibraryViewModel
import com.bmplayer.ui.theme.BMPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferencesStore: UserPreferencesStore
    @Inject lateinit var historyDao: HistoryDao
    @Inject lateinit var playlistDao: PlaylistDao
        @Inject lateinit var favoriteDao: FavoriteDao
    private val libraryViewModel by viewModels<LibraryViewModel>()
    private var controller: MediaController? = null
    private var sleepTimerJob: Job? = null
    private var playbackPositionMs by mutableStateOf(0L)
    private var playbackDurationMs by mutableStateOf(0L)
    private var playbackPlaying by mutableStateOf(false)
    private var shuffleEnabledState by mutableStateOf(false)
    private var repeatModeState by mutableStateOf(androidx.media3.common.Player.REPEAT_MODE_OFF)
    private var playbackTicker: Job? = null
    private var currentTrackId by mutableStateOf<Long?>(null)
    private var sleepTimerRemainingMs by mutableStateOf(0L)
    private var playlistTrackIds by mutableStateOf<Set<Long>>(emptySet())
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        libraryViewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAudioPermissionIfNeeded()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            lifecycleScope.launch {
                val savedCrossfade = preferencesStore.preferences.first().crossfadeSeconds
                controller?.sendCustomCommand(
                    SessionCommand(PlaybackCommands.SET_CROSSFADE, Bundle().apply { putInt(PlaybackCommands.SECONDS, savedCrossfade) }),
                    Bundle.EMPTY
                )
            }
            playbackTicker = lifecycleScope.launch {
                while (true) {
                    controller?.let { mediaController ->
                        playbackPositionMs = mediaController.currentPosition.coerceAtLeast(0L)
                        playbackDurationMs = mediaController.duration.coerceAtLeast(0L)
                        playbackPlaying = mediaController.isPlaying
                        currentTrackId = mediaController.currentMediaItem?.mediaId?.toLongOrNull()
                        shuffleEnabledState = mediaController.shuffleModeEnabled
                        repeatModeState = mediaController.repeatMode
                    }
                    delay(250)
                }
            }
        }, ContextCompat.getMainExecutor(this))
        setContent {
            BMPlayerTheme(
                darkTheme = preferencesStore.preferences.collectAsStateWithLifecycle(UserPreferences()).value.darkTheme,
                accent = preferencesStore.preferences.collectAsStateWithLifecycle(UserPreferences()).value.accentColor()
            ) {
                BMPlayerApp(
                    tracks = libraryViewModel.tracks.collectAsStateWithLifecycle().value,
                    favoriteIds = favoriteDao.observeIds().collectAsStateWithLifecycle(emptyList()).value.toSet(),
                    playlists = playlistDao.observePlaylists().collectAsStateWithLifecycle(emptyList()).value,
                    playlistTrackIds = playlistTrackIds,
                    preferences = preferencesStore.preferences.collectAsStateWithLifecycle(UserPreferences()).value,
                    onRefresh = libraryViewModel::refresh,
                    onPlay = ::playTrack,
                    onToggleFavorite = ::toggleFavorite,
                    onPlayAll = ::playAll,
                    isPlaying = playbackPlaying,
                    playbackPositionMs = playbackPositionMs,
                    playbackDurationMs = playbackDurationMs,
                    currentTrackId = currentTrackId,
                    onSeek = { position -> controller?.seekTo(position) },
                    onPause = { controller?.pause() },
                    onResume = { controller?.play() },
                    onNext = { controller?.seekToNext() },
                    onPrevious = { controller?.seekToPrevious() },
                    shuffleEnabled = shuffleEnabledState,
                    repeatMode = repeatModeState,
                    onToggleShuffle = { controller?.shuffleModeEnabled = !(controller?.shuffleModeEnabled ?: false) },
                    onCycleRepeat = { cycleRepeatMode() },
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                    onToggleDarkTheme = { enabled -> lifecycleScope.launch { preferencesStore.setDarkTheme(enabled) } },
                    onAccentChange = { accent -> lifecycleScope.launch { preferencesStore.setAccent(accent, accent) } },
                    onResetTheme = { lifecycleScope.launch { preferencesStore.resetTheme() } },
                    onSleepTimer = ::startSleepTimer,
                    onEffectsEnabled = ::setEffectsEnabled,
                    onCreatePlaylist = { name -> lifecycleScope.launch { playlistDao.create(PlaylistEntity(name = name)) } },
                    onDeletePlaylist = { id -> lifecycleScope.launch { playlistDao.delete(id) } },
                    onAddToPlaylist = { playlistId, trackId ->
                        lifecycleScope.launch {
                            playlistDao.addTrack(PlaylistTrackEntity(playlistId, trackId, playlistDao.nextPosition(playlistId)))
                            playlistTrackIds = playlistTrackIds + trackId
                        }
                    },
                    onPlayPlaylist = ::playPlaylist,
                    onSelectPlaylist = { id -> lifecycleScope.launch { playlistTrackIds = playlistDao.tracks(id).map { it.trackId }.toSet() } },
                    onCrossfadeChange = { seconds ->
                        lifecycleScope.launch {
                            preferencesStore.setCrossfadeSeconds(seconds)
                            controller?.sendCustomCommand(
                                SessionCommand(PlaybackCommands.SET_CROSSFADE, Bundle().apply { putInt(PlaybackCommands.SECONDS, seconds) }),
                                Bundle.EMPTY
                            )
                        }
                    },
                    onArtworkShapeChange = { shape -> lifecycleScope.launch { preferencesStore.setArtworkShape(shape) } },
                    onLanguageChange = { language -> lifecycleScope.launch { preferencesStore.setLanguage(language) } }
                )
            }
        }
    }

    private fun requestAudioPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(arrayOf(permission))
        else libraryViewModel.refresh()
    }

    private fun playTrack(track: Track) {
        lifecycleScope.launch { historyDao.record(HistoryEntity(trackId = track.id)) }
        val library = libraryViewModel.tracks.value
        val mediaItems = library.map { it.toMediaItem() }
        controller?.setMediaItems(mediaItems)
        controller?.seekTo(library.indexOfFirst { it.id == track.id }.coerceAtLeast(0), 0L)
        controller?.prepare()
        controller?.play()
    }

    private fun playAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        controller?.setMediaItems(tracks.map { it.toMediaItem() })
        controller?.prepare()
        controller?.play()
    }

    private fun playPlaylist(playlist: PlaylistEntity) {
        lifecycleScope.launch {
            val ids = playlistDao.tracks(playlist.id).map { it.trackId }.toSet()
            playAll(libraryViewModel.tracks.value.filter { it.id in ids })
        }
    }

    private fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            sleepTimerRemainingMs = 0L
            return
        }
        sleepTimerRemainingMs = minutes * 60_000L
        sleepTimerJob = lifecycleScope.launch {
            while (sleepTimerRemainingMs > 0) {
                delay(1_000L)
                sleepTimerRemainingMs = (sleepTimerRemainingMs - 1_000L).coerceAtLeast(0L)
            }
            controller?.pause()
        }
    }

    private fun setEffectsEnabled(enabled: Boolean) {
        controller?.sendCustomCommand(
            SessionCommand(PlaybackCommands.SET_EFFECTS_ENABLED, Bundle.EMPTY),
            Bundle().apply { putBoolean(PlaybackCommands.ENABLED, enabled) }
        )
    }

    private fun toggleFavorite(track: Track) {
        lifecycleScope.launch {
            if (favoriteDao.contains(track.id)) favoriteDao.remove(track.id) else favoriteDao.add(FavoriteEntity(track.id))
        }
    }

    private fun cycleRepeatMode() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
            androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
            else -> androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }

    override fun onDestroy() {
        playbackTicker?.cancel()
        sleepTimerJob?.cancel()
        controller?.release()
        super.onDestroy()
    }
}

private fun Track.toMediaItem() = MediaItem.Builder()
    .setUri(uri)
    .setMediaId(id.toString())
    .setTag(this)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setArtworkUri(artworkUri).build())
    .build()

private fun UserPreferences.accentColor(): androidx.compose.ui.graphics.Color = runCatching {
    androidx.compose.ui.graphics.Color(AndroidColor.parseColor(if (darkTheme) accentDark else accentLight))
}.getOrDefault(androidx.compose.ui.graphics.Color(0xFFB9750F))

    private fun artworkShape(value: String): RoundedCornerShape = when (value) {
        "round" -> RoundedCornerShape(50)
        "square" -> RoundedCornerShape(0)
        else -> RoundedCornerShape(12)
    }

@Composable
private fun BMPlayerApp(
    tracks: List<Track>,
    favoriteIds: Set<Long>,
    playlists: List<PlaylistEntity>,
    preferences: UserPreferences,
    onRefresh: () -> Unit,
    onPlay: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    isPlaying: Boolean,
    currentTrackId: Long?,
    playbackPositionMs: Long,
    playbackDurationMs: Long,
    onSeek: (Long) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    sleepTimerRemainingMs: Long,
    onToggleDarkTheme: (Boolean) -> Unit,
    onAccentChange: (String) -> Unit,
    onResetTheme: () -> Unit,
    onSleepTimer: (Int) -> Unit,
    onEffectsEnabled: (Boolean) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    playlistTrackIds: Set<Long>,
    onDeletePlaylist: (Long) -> Unit,
    onAddToPlaylist: (Long, Long) -> Unit
    ,onPlayPlaylist: (PlaylistEntity) -> Unit
    ,onSelectPlaylist: (Long) -> Unit
    ,onCrossfadeChange: (Int) -> Unit
    ,onArtworkShapeChange: (String) -> Unit
    ,onLanguageChange: (String) -> Unit
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    var tab by remember { mutableStateOf(0) }
    val activeTrack = tracks.firstOrNull { it.id == currentTrackId } ?: selectedTrack
    val english = preferences.language == "en"
    Scaffold(bottomBar = {
        Surface(Modifier.padding(12.dp), shape = RoundedCornerShape(28.dp), tonalElevation = 8.dp, shadowElevation = 8.dp) {
        NavigationBar {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text(if (english) "Library" else "Bibliothèque") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Album, null) }, label = { Text(if (english) "Playing" else "Lecture") })
            NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(if (english) "Settings" else "Réglages") })
            NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text(if (english) "Favorites" else "Favoris") })
            NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, icon = { Icon(Icons.Default.Album, null) }, label = { Text("Playlists") })
        }
        }
    }) { padding ->
        if (tab == 0) LibraryScreen(tracks, favoriteIds, currentTrackId, selectedTrack, preferences.artworkShape, onRefresh, onPlay, onToggleFavorite, onPlayAll, { selectedTrack = it }, Modifier.padding(padding))
        else if (tab == 1) NowPlayingScreen(activeTrack, preferences.artworkShape, isPlaying, playbackPositionMs, playbackDurationMs, onSeek, onPause, onResume, onNext, onPrevious, shuffleEnabled, repeatMode, onToggleShuffle, onCycleRepeat, { tab = 0 }, Modifier.padding(padding))
        else if (tab == 2) SettingsScreen(preferences, onToggleDarkTheme, onAccentChange, onResetTheme, onEffectsEnabled, sleepTimerRemainingMs, onSleepTimer, onCrossfadeChange, onArtworkShapeChange, onLanguageChange, Modifier.padding(padding))
        else if (tab == 3) FavoritesScreen(tracks.filter { it.id in favoriteIds }, currentTrackId, preferences.artworkShape, { selectedTrack = it; onPlay(it) }, { onToggleFavorite(it) }, Modifier.padding(padding))
        else if (tab == 4) PlaylistScreen(playlists, tracks, playlistTrackIds, preferences.artworkShape, onCreatePlaylist, onDeletePlaylist, onAddToPlaylist, onPlayPlaylist, onSelectPlaylist, Modifier.padding(padding))
        else PlaylistScreen(playlists, tracks, playlistTrackIds, preferences.artworkShape, onCreatePlaylist, onDeletePlaylist, onAddToPlaylist, onPlayPlaylist, onSelectPlaylist, Modifier.padding(padding))
    }
}

@Composable
private fun LibraryScreen(tracks: List<Track>, favoriteIds: Set<Long>, currentTrackId: Long?, selected: Track?, artworkShapeValue: String, onRefresh: () -> Unit, onPlay: (Track) -> Unit, onToggleFavorite: (Track) -> Unit, onPlayAll: (List<Track>) -> Unit, onSelect: (Track) -> Unit, modifier: Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    val matchingTracks = remember(tracks, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) tracks else tracks.filter { track ->
            listOf(track.title, track.artist, track.album, track.genre)
                .any { value -> value.contains(query, ignoreCase = true) }
        }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = com.bmplayer.R.drawable.bm_player_logo, contentDescription = "Logo BM Player", modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Column { Text("BM Player", style = MaterialTheme.typography.headlineLarge); Text("Votre musique, simplement.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Row {
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualiser") }
                IconButton(onClick = { onPlayAll(matchingTracks) }, enabled = matchingTracks.isNotEmpty()) { Icon(Icons.Default.PlayArrow, if (searchQuery.isBlank()) "Lire toute la bibliothèque" else "Lire tous les résultats") }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Effacer la recherche")
                }
            },
            placeholder = { Text("Titre, artiste, album ou genre") }
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (searchQuery.isBlank()) "${tracks.size} morceaux locaux" else "${matchingTracks.size} résultat(s) sur ${tracks.size}",
            style = MaterialTheme.typography.titleMedium
        )
        if (searchQuery.isNotBlank() && matchingTracks.isNotEmpty()) {
            Button(onClick = { onPlayAll(matchingTracks) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Lire tout (${matchingTracks.size})")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (tracks.isEmpty()) EmptyLibrary()
        else if (matchingTracks.isEmpty()) {
            Text("Aucun morceau ne correspond à « $searchQuery ».", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matchingTracks, key = { it.id }) { track -> TrackRow(track, selected == track || currentTrackId == track.id, favoriteIds.contains(track.id), artworkShapeValue, { onSelect(track); onPlay(track) }, { onToggleFavorite(track) }) }
        }
    }
}

@Composable
private fun FavoritesScreen(tracks: List<Track>, currentTrackId: Long?, artworkShapeValue: String, onPlay: (Track) -> Unit, onRemove: (Track) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Favoris", style = MaterialTheme.typography.headlineLarge)
        Text("${tracks.size} morceaux que vous aimez", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        if (tracks.isEmpty()) {
            EmptyLibrary()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(track, currentTrackId == track.id, true, artworkShapeValue, { onPlay(track) }, { onRemove(track) })
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LibraryMusic, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Aucune musique trouvée", style = MaterialTheme.typography.titleLarge)
        Text("Ajoutez des fichiers audio sur cet appareil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrackRow(track: Track, selected: Boolean, favorite: Boolean, artworkShapeValue: String, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .12f) else Color.Transparent).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(artworkShape(artworkShapeValue)).background(MaterialTheme.colorScheme.primary.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
            ArtworkImage(track, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${track.artist}  ·  ${track.album}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        IconButton(onClick = onToggleFavorite) { Icon(if (favorite) androidx.compose.material.icons.Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favori", tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun NowPlayingScreen(track: Track?, artworkShapeValue: String, isPlaying: Boolean, playbackPositionMs: Long, playbackDurationMs: Long, onSeek: (Long) -> Unit, onPause: () -> Unit, onResume: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, shuffleEnabled: Boolean, repeatMode: Int, onToggleShuffle: () -> Unit, onCycleRepeat: () -> Unit, onOpenLibrary: () -> Unit, modifier: Modifier) {
    var dragOffset by remember { mutableStateOf(0f) }
    Column(modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("LECTURE EN COURS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(34.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(330.dp)
                .offset { IntOffset(dragOffset.roundToInt(), 0) }
                .clip(artworkShape(artworkShapeValue))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .18f))
                .pointerInput(Unit) {
                    var totalDrag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { totalDrag = Offset.Zero },
                        onDrag = { change, amount ->
                            change.consume()
                            totalDrag += amount
                            dragOffset = (totalDrag.x * 0.35f).coerceIn(-120f, 120f)
                        },
                        onDragEnd = {
                            when {
                                totalDrag.x < -80f -> onNext()
                                totalDrag.x > 80f -> onPrevious()
                                totalDrag.y > 100f -> onOpenLibrary()
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (track != null) ArtworkImage(track, Modifier.fillMaxSize()) else Icon(Icons.Default.LibraryMusic, null, Modifier.size(90.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))
        Text(track?.title ?: "Sélectionnez un morceau", style = MaterialTheme.typography.titleLarge)
        Text(track?.artist ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(25.dp))
        Slider(
            value = playbackPositionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..playbackDurationMs.coerceAtLeast(1L).toFloat(),
            onValueChangeFinished = { }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatDuration(playbackPositionMs), style = MaterialTheme.typography.labelMedium); Text(formatDuration(if (playbackDurationMs > 0) playbackDurationMs else track?.durationMs ?: 0), style = MaterialTheme.typography.labelMedium) }
        AnimatedWaveform(isPlaying)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            IconButton(onClick = onToggleShuffle) { Icon(Icons.Default.Shuffle, "Aléatoire", tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Précédent", Modifier.size(32.dp)) }
            Surface(Modifier.size(68.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary) { IconButton(onClick = if (isPlaying) onPause else onResume) { Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Lecture", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp)) } }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Suivant", Modifier.size(32.dp)) }
            IconButton(onClick = onCycleRepeat) { Icon(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Répétition", tint = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun SettingsScreen(
    preferences: UserPreferences,
    onToggleDarkTheme: (Boolean) -> Unit,
    onAccentChange: (String) -> Unit,
    onResetTheme: () -> Unit,
    onEffectsEnabled: (Boolean) -> Unit,
    sleepTimerRemainingMs: Long,
    onSleepTimer: (Int) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onArtworkShapeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier
) {
    var effectsEnabled by remember { mutableStateOf(false) }
    var customTimerMinutes by remember { mutableStateOf("") }
    var showAppearance by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val english = preferences.language == "en"
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(if (english) "Settings" else "Réglages", style = MaterialTheme.typography.headlineLarge)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(if (english) "Dark mode" else "Mode sombre"); Text(if (english) "Follow the selected theme" else "Suivre le thème choisi", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = preferences.darkTheme, onCheckedChange = onToggleDarkTheme)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(if (english) "Audio effects" else "Effets audio"); Text(if (english) "Equalizer, bass boost and spatial audio" else "Égaliseur, bass boost et spatialisation", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = effectsEnabled, onCheckedChange = { effectsEnabled = it; onEffectsEnabled(it) })
        }
        Row(Modifier.fillMaxWidth().clickable { showAppearance = !showAppearance }, verticalAlignment = Alignment.CenterVertically) { Text(if (english) "Appearance" else "Apparence", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text(if (showAppearance) "−" else "+", style = MaterialTheme.typography.titleLarge) }
        if (showAppearance) {
            Text(if (english) "Accent color" else "Couleur d'accent", style = MaterialTheme.typography.titleMedium)
            ColorWheel(onColorSelected = { color -> onAccentChange("#${color.toArgb().and(0xFFFFFF).toString(16).padStart(6, '0').uppercase()}") })
            Text(if (english) "Artwork shape" else "Forme des pochettes", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("round" to if (english) "Round" else "Ronde", "rounded" to if (english) "Rounded" else "Arrondie", "square" to if (english) "Square" else "Carrée").forEach { (value, label) -> Button(onClick = { onArtworkShapeChange(value) }) { Text(label) } }
            }
            Text("Fondu enchaîné : ${preferences.crossfadeSeconds}s", style = MaterialTheme.typography.titleMedium)
            Slider(value = preferences.crossfadeSeconds.toFloat(), onValueChange = { onCrossfadeChange(it.roundToInt()) }, valueRange = 0f..15f, steps = 14)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("0s"); Text("15s") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onLanguageChange("fr") }) { Text("Français") }; Button(onClick = { onLanguageChange("en") }) { Text("English") } }
            Button(onClick = onResetTheme) { Text(if (english) "Reset default theme" else "Réinitialiser au thème par défaut") }
        }
        Text(if (english) "Sleep timer" else "Minuteur", style = MaterialTheme.typography.titleMedium)
        Text(if (sleepTimerRemainingMs > 0) (if (english) "Active: ${formatDuration(sleepTimerRemainingMs)} remaining" else "Actif : ${formatDuration(sleepTimerRemainingMs)} restantes") else (if (english) "No active timer" else "Aucun minuteur actif"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 60).forEach { minutes -> Button(onClick = { onSleepTimer(minutes) }) { Text("${minutes} min") } }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = customTimerMinutes, onValueChange = { customTimerMinutes = it.filter(Char::isDigit) }, label = { Text(if (english) "Custom duration" else "Durée personnalisée") }, suffix = { Text("min") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = { customTimerMinutes.toIntOrNull()?.takeIf { it > 0 }?.let(onSleepTimer) }) { Text(if (english) "Start" else "Démarrer") }
        }
        if (sleepTimerRemainingMs > 0) Button(onClick = { onSleepTimer(0) }) { Text(if (english) "Stop timer" else "Arrêter le minuteur") }
        Row(Modifier.fillMaxWidth().clickable { showAbout = !showAbout }, verticalAlignment = Alignment.CenterVertically) { Text(if (english) "About" else "À propos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text("Version 1.0.0 · 2026") }
        if (showAbout) {
            Text("BM Player · lecteur local sans publicité ni suivi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Politique de confidentialité\nAucune donnée personnelle, aucun tracker et aucune publicité. La musique reste sur votre appareil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Conditions d'utilisation\nBM Player est fourni pour la lecture de contenus audio que vous possédez ou êtes autorisé à utiliser.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Licence open source\nLe code de BM Player est distribué sous licence MIT.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ColorWheel(onColorSelected: (Color) -> Unit) {
    Canvas(
        Modifier
            .size(220.dp)
            .pointerInput(Unit) {
                fun updateColor(point: Offset) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = point.x - center.x
                    val dy = point.y - center.y
                    val radius = sqrt(dx * dx + dy * dy).coerceAtMost(center.x)
                    val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                    val saturation = (radius / center.x).coerceIn(0f, 1f)
                    val hsv = floatArrayOf(hue, saturation, 1f)
                    onColorSelected(Color(AndroidColor.HSVToColor(hsv)))
                }
                detectDragGestures(
                    onDragStart = ::updateColor,
                    onDrag = { change, _ -> change.consume(); updateColor(change.position) }
                )
            }
    ) {
        drawCircle(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)), radius = size.minDimension / 2f)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .95f), Color.Transparent), radius = size.minDimension / 2f), radius = size.minDimension / 2f)
        drawCircle(Color.White.copy(alpha = .95f), radius = size.minDimension * .12f)
    }
}

@Composable
private fun PlaylistScreen(
    playlists: List<PlaylistEntity>,
    tracks: List<Track>,
    playlistTrackIds: Set<Long>,
    artworkShapeValue: String,
    onCreate: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onAddToPlaylist: (Long, Long) -> Unit,
    onPlayPlaylist: (PlaylistEntity) -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    modifier: Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var deleteCandidate by remember { mutableStateOf<PlaylistEntity?>(null) }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Playlists", style = MaterialTheme.typography.headlineLarge)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Nouvelle playlist") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (name.isNotBlank()) { onCreate(name.trim()); name = "" } }) { Text("Créer") }
        }
        if (playlists.isEmpty()) {
            Text("Aucune playlist personnalisée", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.weight(if (selectedPlaylistId == null) 1f else 0.35f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { selectedPlaylistId = playlist.id; onSelectPlaylist(playlist.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(playlist.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onPlayPlaylist(playlist) }) { Icon(Icons.Default.PlayArrow, "Lire la playlist") }
                        Button(onClick = { deleteCandidate = playlist }) { Text("Supprimer") }
                    }
                }
            }
        }
        val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }
        if (selectedPlaylist != null) {
            Text("Ajouter à « ${selectedPlaylist.name} »", style = MaterialTheme.typography.titleMedium)
            val selectedTracks = tracks.filter { it.id in playlistTrackIds }
            if (selectedTracks.isNotEmpty()) {
                Text("${selectedTracks.size} morceaux dans cette playlist", color = MaterialTheme.colorScheme.primary)
                LazyColumn(Modifier.weight(0.35f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(selectedTracks, key = { "selected-${it.id}" }) { track -> TrackRow(track, false, false, artworkShapeValue, { onPlayPlaylist(selectedPlaylist) }, {}) }
                }
            }
            LazyColumn(Modifier.weight(0.65f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tracks, key = { it.id }) { track ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(track.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Button(onClick = { onAddToPlaylist(selectedPlaylist.id, track.id) }, enabled = track.id !in playlistTrackIds) { Text(if (track.id in playlistTrackIds) "Ajoutée" else "Ajouter") }
                    }
                }
            }
        }
        deleteCandidate?.let { playlist ->
            AlertDialog(
                onDismissRequest = { deleteCandidate = null },
                title = { Text("Supprimer la playlist ?") },
                text = { Text("Cette action supprimera « ${playlist.name} » et ses morceaux.") },
                confirmButton = { Button(onClick = { onDelete(playlist.id); if (selectedPlaylistId == playlist.id) selectedPlaylistId = null; deleteCandidate = null }) { Text("Supprimer") } },
                dismissButton = { Button(onClick = { deleteCandidate = null }) { Text("Annuler") } }
            )
        }
    }
}

@Composable
private fun ArtworkImage(track: Track, modifier: Modifier) {
    val context = LocalContext.current
    val embeddedArtwork by produceState<Bitmap?>(initialValue = null, track.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, track.uri)
                    retriever.embeddedPicture?.let { bytes -> android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                }
            }.getOrNull()
        }
    }
    if (embeddedArtwork != null) {
        Image(embeddedArtwork!!.asImageBitmap(), contentDescription = "Pochette de ${track.title}", modifier = modifier, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    } else {
        AsyncImage(model = track.artworkUri, contentDescription = "Pochette de ${track.title}", modifier = modifier, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}

@Composable
private fun AnimatedWaveform(isPlaying: Boolean) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "waveform")
    Row(Modifier.fillMaxWidth().height(34.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(32) { index ->
            val animatedHeight by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(320 + index * 18),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "bar-$index"
            )
            val height = if (isPlaying) animatedHeight else 0.45f
            Box(Modifier.weight(1f).height((8 + height * 26).dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)))
        }
    }
}

private fun formatDuration(milliseconds: Long): String { val seconds = milliseconds / 1000; return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}" }
