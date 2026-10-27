package com.bmplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.Crossfade
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import com.bmplayer.ui.formatTimerRemaining
import com.bmplayer.ui.sleepTimerProgress
import kotlinx.coroutines.android.awaitFrame
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import com.bmplayer.ui.backgroundFile
import com.bmplayer.ui.backDestination
import com.bmplayer.ui.barCollapseIntent
import com.bmplayer.ui.QueueSheet
import com.bmplayer.ui.queueEntries
import com.bmplayer.ui.previousRestartsTrack
import androidx.compose.runtime.rememberUpdatedState
import com.bmplayer.ui.CollectionsScreen
import com.bmplayer.ui.animationScale
import com.bmplayer.ui.motionMs
import com.bmplayer.ui.isForwardTransition
import androidx.activity.compose.BackHandler
import com.bmplayer.ui.dominantAccent
import com.bmplayer.ui.loadArtworkBitmap
import com.bmplayer.ui.cachedArtwork
import com.bmplayer.ui.loadCachedArtwork
import com.bmplayer.ui.saveBackgroundImage
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.updateAll
import com.bmplayer.widget.BMPlayerWidget
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.bmplayer.data.local.FavoriteDao
import com.bmplayer.data.local.FavoriteEntity
import com.bmplayer.data.local.HistoryDao
import com.bmplayer.data.local.HistoryEntity
import com.bmplayer.data.local.PlaylistDao
import com.bmplayer.data.local.PlaylistEntity
import com.bmplayer.data.local.PlaylistTrackEntity
import com.bmplayer.data.mediastore.MediaStoreObserver
import com.bmplayer.data.preferences.UserPreferences
import com.bmplayer.data.preferences.UserPreferencesStore
import com.bmplayer.domain.model.Track
import com.bmplayer.playback.PlaybackCommands
import com.bmplayer.playback.PlaybackService
import com.bmplayer.playback.planResume
import com.bmplayer.playback.toMediaItem
import com.bmplayer.playback.advancePending
import com.bmplayer.ui.searchTracks
import com.bmplayer.ui.LibraryViewModel
import com.bmplayer.ui.sortTracks
import com.bmplayer.ui.theme.BMPlayerTheme
import com.bmplayer.ui.theme.LocalLiquidGlass
import com.bmplayer.ui.theme.LocalAppearance
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI
import com.bmplayer.ui.theme.liquidGlassSurface
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
    private var effectsSupported by mutableStateOf<Boolean?>(null)
    private var playbackTicker: Job? = null
    private var currentTrackId by mutableStateOf<Long?>(null)
    private var queueTrackIds by mutableStateOf(emptyList<Long>())
    private var sleepTimerRemainingMs by mutableStateOf(0L)
    private var sleepTimerTotalMs by mutableStateOf(0L)
    private var pendingNextTrackIds = emptyList<Long>()
    private var lastObservedTrackId: Long? = null
    private var playbackRestorationFinished = false
    private var mediaScanJob: Job? = null
    private var mediaObserverRegistered = false
    private val mediaStoreObserver: ContentObserver by lazy {
        MediaStoreObserver(Handler(Looper.getMainLooper())) { scheduleMediaStoreRefresh() }
    }
    private var backgroundVersion by mutableStateOf(0L)
    private val backgroundPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) lifecycleScope.launch {
            if (saveBackgroundImage(this@MainActivity, uri)) backgroundVersion = System.currentTimeMillis()
            else Toast.makeText(this@MainActivity, if (preferencesStore.preferences.first().language == "en") "Unreadable image" else "Image illisible", Toast.LENGTH_SHORT).show()
        }
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        libraryViewModel.refresh()
        registerMediaStoreObserver()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAudioPermissionIfNeeded()
        backgroundVersion = backgroundFile(this).takeIf { it.exists() }?.lastModified() ?: 0L
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            queueTrackIds = readQueueIds()
            controller?.addListener(object : androidx.media3.common.Player.Listener {
                override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                    queueTrackIds = readQueueIds()
                }
            })
            lifecycleScope.launch {
                val savedCrossfade = preferencesStore.preferences.first().crossfadeSeconds
                val savedPreferences = preferencesStore.preferences.first()
                controller?.sendCustomCommand(
                    SessionCommand(PlaybackCommands.SET_CROSSFADE, Bundle().apply { putInt(PlaybackCommands.SECONDS, savedCrossfade) }),
                    Bundle.EMPTY
                )
                val effectsFuture = controller?.sendCustomCommand(
                    SessionCommand(PlaybackCommands.SET_EFFECTS_ENABLED, Bundle.EMPTY),
                    Bundle().apply { putBoolean(PlaybackCommands.ENABLED, savedPreferences.effectsEnabled) }
                )
                effectsFuture?.addListener({
                    effectsSupported = runCatching { effectsFuture.get().resultCode == SessionResult.RESULT_SUCCESS }.getOrDefault(false)
                }, ContextCompat.getMainExecutor(this@MainActivity))
                setEffectStrength(PlaybackCommands.SET_BASS_STRENGTH, savedPreferences.bassStrength)
                setEffectStrength(PlaybackCommands.SET_VIRTUALIZER_STRENGTH, savedPreferences.virtualizerStrength)
            }
            restorePlaybackState()
            playbackTicker = lifecycleScope.launch {
                var lastPersistAtMs = 0L
                while (true) {
                    controller?.let { mediaController ->
                        playbackPositionMs = mediaController.currentPosition.coerceAtLeast(0L)
                        playbackDurationMs = mediaController.duration.coerceAtLeast(0L)
                        playbackPlaying = mediaController.isPlaying
                        val observedTrackId = mediaController.currentMediaItem?.mediaId?.toLongOrNull()
                        if (observedTrackId != lastObservedTrackId) {
                            pendingNextTrackIds = advancePending(pendingNextTrackIds, observedTrackId)
                            lastObservedTrackId = observedTrackId
                        }
                        currentTrackId = observedTrackId
                        shuffleEnabledState = mediaController.shuffleModeEnabled
                        repeatModeState = mediaController.repeatMode
                        val now = System.currentTimeMillis()
                        if (playbackRestorationFinished && now - lastPersistAtMs >= 2_000L) {
                            persistPlaybackState(mediaController)
                            lastPersistAtMs = now
                        }
                    }
                    delay(250)
                }
            }
        }, ContextCompat.getMainExecutor(this))
        setContent {
            val preferences = preferencesStore.preferences.collectAsStateWithLifecycle(UserPreferences()).value
            val libraryTracks = libraryViewModel.tracks.collectAsStateWithLifecycle().value
            val playingArtworkTrack = libraryTracks.firstOrNull { it.id == currentTrackId }
                val albumAccent by produceState(preferences.defaultAccent(), playingArtworkTrack?.id) {
                value = playingArtworkTrack?.let { extractAlbumAccent(this@MainActivity, it) } ?: preferences.defaultAccent()
            }
            BMPlayerTheme(
                darkTheme = preferences.darkTheme,
                accent = albumAccent,
                appearance = preferences.appearanceStyle,
                animationScale = animationScale(preferences.animationSpeed)
            ) {
                BMPlayerApp(
                    tracks = libraryTracks,
                    favoriteIds = remember { favoriteDao.observeIds() }.collectAsStateWithLifecycle(emptyList()).value.toSet(),
                    playlists = remember { playlistDao.observePlaylists() }.collectAsStateWithLifecycle(emptyList()).value,
                    playlistTracks = remember { playlistDao.observeAllTracks() }.collectAsStateWithLifecycle(emptyList()).value.let { rows -> remember(rows) { rows.groupBy({ it.playlistId }, { it.trackId }) } },
                    preferences = preferences,
                    onRefresh = libraryViewModel::refresh,
                    onPlay = ::playTrack,
                    onPlayNext = ::playNext,
                    onSetSortOrder = { value -> lifecycleScope.launch { preferencesStore.setSortOrder(value) } },
                    onToggleFavorite = ::toggleFavorite,
                    onPlayAll = ::playAll,
                    isPlaying = playbackPlaying,
                    playbackPositionMs = playbackPositionMs,
                    playbackDurationMs = playbackDurationMs,
                    currentTrackId = currentTrackId,
                    queueTrackIds = queueTrackIds,
                    onQueueMove = { from, to ->
                        // Media3 keeps the shuffled order private, so the order shown only becomes the order played once shuffle is off.
                        controller?.shuffleModeEnabled = false
                        controller?.moveMediaItem(from, to)
                        // playNext places a track from the pending list, whose positions a manual move invalidates.
                        pendingNextTrackIds = emptyList()
                        controller?.let(::persistPlaybackState)
                    },
                    onPlayQueueIndex = { index -> controller?.seekTo(index, 0L); controller?.play() },
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
                    sleepTimerTotalMs = sleepTimerTotalMs,
                    onToggleDarkTheme = { enabled -> lifecycleScope.launch { preferencesStore.setDarkTheme(enabled) } },
                    onSleepTimer = ::startSleepTimer,
                    onEffectsEnabled = ::setEffectsEnabled,
                    effectsSupported = effectsSupported,
                    onBassStrength = { value -> lifecycleScope.launch { preferencesStore.setBassStrength(value); setEffectStrength(PlaybackCommands.SET_BASS_STRENGTH, value) } },
                    onVirtualizerStrength = { value -> lifecycleScope.launch { preferencesStore.setVirtualizerStrength(value); setEffectStrength(PlaybackCommands.SET_VIRTUALIZER_STRENGTH, value) } },
                    onCreatePlaylist = { name, onCreated -> lifecycleScope.launch { onCreated(playlistDao.create(PlaylistEntity(name = name))) } },
                    onDeletePlaylist = { id -> lifecycleScope.launch { playlistDao.deleteWithTracks(id) } },
                    onAddTracks = { playlistId, trackIds ->
                        lifecycleScope.launch {
                            val first = playlistDao.nextPosition(playlistId)
                            playlistDao.setTracks(trackIds.mapIndexed { index, trackId -> PlaylistTrackEntity(playlistId, trackId, first + index) })
                        }
                    },
                    onRemoveTrack = { playlistId, trackId -> lifecycleScope.launch { playlistDao.removeTrack(playlistId, trackId) } },
                    onPlayGroup = { list, shuffle -> controller?.shuffleModeEnabled = shuffle; playAll(list) },
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
                    onAppearanceStyleChange = { style -> lifecycleScope.launch { preferencesStore.setAppearanceStyle(style) } },
                    backgroundVersion = backgroundVersion,
                    onPickBackground = { backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onBackgroundDimChange = { dim -> lifecycleScope.launch { preferencesStore.setBackgroundDim(dim) } },
                    onAnimationSpeedChange = { speed -> lifecycleScope.launch { preferencesStore.setAnimationSpeed(speed) } },
                    onWidgetStyleChange = { style -> lifecycleScope.launch { preferencesStore.setWidgetStyle(style); BMPlayerWidget().updateAll(this@MainActivity) } },
                    onLanguageChange = { language -> lifecycleScope.launch { preferencesStore.setLanguage(language) } }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerMediaStoreObserver()
    }

    override fun onStop() {
        controller?.let(::persistPlaybackState)
        unregisterMediaStoreObserver()
        super.onStop()
    }

    private fun registerMediaStoreObserver() {
        if (mediaObserverRegistered) return
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) return
        runCatching {
            contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            mediaObserverRegistered = true
        }
    }

    private fun unregisterMediaStoreObserver() {
        if (!mediaObserverRegistered) return
        contentResolver.unregisterContentObserver(mediaStoreObserver)
        mediaObserverRegistered = false
        mediaScanJob?.cancel()
    }

    private fun scheduleMediaStoreRefresh() {
        mediaScanJob?.cancel()
        mediaScanJob = lifecycleScope.launch {
            delay(500L)
            libraryViewModel.refresh()
        }
    }

    private fun restorePlaybackState() {
        lifecycleScope.launch {
            val activePlayer = controller
            if (activePlayer != null && activePlayer.mediaItemCount > 0) {
                playbackRestorationFinished = true
                return@launch
            }
            val savedState = preferencesStore.preferences.first()
            val library = withTimeoutOrNull(15_000L) {
                libraryViewModel.tracks.first { it.isNotEmpty() }
            }
            if (library == null) {
                playbackRestorationFinished = true
                return@launch
            }
            val plan = planResume(savedState.playbackQueueIds, savedState.playbackTrackId, savedState.playbackPositionMs, library.associateBy { it.id })
            if (plan != null) {
                val player = controller ?: return@launch
                player.setMediaItems(plan.items.map { it.toMediaItem() }, plan.startIndex, plan.startPositionMs)
                player.shuffleModeEnabled = savedState.shuffleEnabled
                player.repeatMode = savedState.repeatMode
                player.prepare()
                if (savedState.playbackWasPlaying) player.play()
            }
            playbackRestorationFinished = true
        }
    }

    private fun readQueueIds(): List<Long> = controller?.let { player ->
        (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
    } ?: emptyList()

    private fun persistPlaybackState(player: MediaController) {
        if (player.mediaItemCount == 0) return
        lifecycleScope.launch {
            preferencesStore.savePlaybackState(
                queueIds = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() },
                trackId = player.currentMediaItem?.mediaId?.toLongOrNull(),
                positionMs = player.currentPosition,
                wasPlaying = player.isPlaying,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode
            )
        }
    }

    private fun requestAudioPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(arrayOf(permission))
        else libraryViewModel.refresh()
    }

    private fun playTrack(track: Track, queue: List<Track>) {
        lifecycleScope.launch { historyDao.record(HistoryEntity(trackId = track.id)) }
        pendingNextTrackIds = emptyList()
        controller?.setMediaItems(queue.map { it.toMediaItem() }, queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0), 0L)
        controller?.prepare()
        controller?.play()
    }

    private fun playAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        pendingNextTrackIds = emptyList()
        controller?.setMediaItems(tracks.map { it.toMediaItem() })
        controller?.prepare()
        controller?.play()
    }

    private fun playNext(track: Track) {
        val player = controller ?: return
        if (player.mediaItemCount == 0) {
            playTrack(track, libraryViewModel.tracks.value)
            return
        }
        val insertAt = (player.currentMediaItemIndex + 1 + pendingNextTrackIds.size).coerceIn(0, player.mediaItemCount)
        // The service inserts it, so it can also place it next in the shuffled order.
        player.sendCustomCommand(
            SessionCommand(PlaybackCommands.PLAY_NEXT, Bundle.EMPTY),
            Bundle().apply { putLong(PlaybackCommands.TRACK_ID, track.id); putInt(PlaybackCommands.INDEX, insertAt) }
        )
        pendingNextTrackIds = pendingNextTrackIds + track.id
        lifecycleScope.launch { persistPlaybackState(player) }
    }


    private fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            sleepTimerRemainingMs = 0L
            sleepTimerTotalMs = 0L
            return
        }
        sleepTimerRemainingMs = minutes * 60_000L
        sleepTimerTotalMs = sleepTimerRemainingMs
        sleepTimerJob = lifecycleScope.launch {
            while (sleepTimerRemainingMs > 0) {
                delay(1_000L)
                sleepTimerRemainingMs = (sleepTimerRemainingMs - 1_000L).coerceAtLeast(0L)
            }
            controller?.pause()
        }
    }

    private fun setEffectsEnabled(enabled: Boolean) {
        lifecycleScope.launch { preferencesStore.setEffectsEnabled(enabled) }
        val future = controller?.sendCustomCommand(
            SessionCommand(PlaybackCommands.SET_EFFECTS_ENABLED, Bundle.EMPTY),
            Bundle().apply { putBoolean(PlaybackCommands.ENABLED, enabled) }
        )
        future?.addListener({
            effectsSupported = runCatching { future.get().resultCode == SessionResult.RESULT_SUCCESS }.getOrDefault(false)
            if (effectsSupported != true) lifecycleScope.launch { preferencesStore.setEffectsEnabled(false) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setEffectStrength(command: String, strength: Int) {
        controller?.sendCustomCommand(
            SessionCommand(command, Bundle.EMPTY),
            Bundle().apply { putInt(PlaybackCommands.STRENGTH, strength) }
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

private fun UserPreferences.defaultAccent(): Color =
    if (darkTheme) Color(0xFFE8A33D) else Color(0xFFB9750F)

private suspend fun extractAlbumAccent(context: Context, track: Track): Color? =
    loadArtworkBitmap(context, track.uri, track.artworkUri, 256)?.let(::dominantAccent)?.let(::Color)

// Dragging the bar is the alternative to the chevron, for when a faulty key or the keyboard makes the button unreachable.
@Composable
private fun Modifier.barDragCollapse(onIntent: (Boolean) -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    return this.pointerInput(Unit) {
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = { barCollapseIntent(total, threshold)?.let(onIntent) },
            onVerticalDrag = { _, amount -> total += amount }
        )
    }
}

internal fun artworkShape(value: String): RoundedCornerShape = when (value) {
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
    onPlay: (track: Track, queue: List<Track>) -> Unit,
    onPlayNext: (Track) -> Unit,
    onSetSortOrder: (String) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    isPlaying: Boolean,
    currentTrackId: Long?,
    queueTrackIds: List<Long>,
    onQueueMove: (Int, Int) -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
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
    sleepTimerTotalMs: Long,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSleepTimer: (Int) -> Unit,
    onEffectsEnabled: (Boolean) -> Unit,
    effectsSupported: Boolean?,
    onBassStrength: (Int) -> Unit,
    onVirtualizerStrength: (Int) -> Unit,
    onCreatePlaylist: (name: String, onCreated: (Long) -> Unit) -> Unit,
    playlistTracks: Map<Long, List<Long>>,
    onDeletePlaylist: (Long) -> Unit,
    onAddTracks: (playlistId: Long, trackIds: List<Long>) -> Unit,
    onRemoveTrack: (playlistId: Long, trackId: Long) -> Unit,
    onPlayGroup: (tracks: List<Track>, shuffle: Boolean) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onArtworkShapeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onAppearanceStyleChange: (String) -> Unit,
    backgroundVersion: Long,
    onPickBackground: () -> Unit,
    onBackgroundDimChange: (Float) -> Unit,
    onAnimationSpeedChange: (String) -> Unit,
    onWidgetStyleChange: (String) -> Unit
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    var tab by remember { mutableStateOf(0) }
    var bottomBarCollapsed by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    val queueRows = remember(tracks, queueTrackIds) { queueEntries(tracks, queueTrackIds) }
    val backTarget = backDestination(tab, currentTrackId != null)
    BackHandler(enabled = backTarget != null) { backTarget?.let { tab = it } }
    val activeTrack = tracks.firstOrNull { it.id == currentTrackId } ?: selectedTrack
    val english = preferences.language == "en"
    val appBackdrop = if (preferences.appearanceStyle == "liquid_glass") {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                MaterialTheme.colorScheme.background,
                Color(0xFF70C1B3).copy(alpha = 0.13f),
                MaterialTheme.colorScheme.background
            )
        )
    } else {
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
    }
    val context = LocalContext.current
    val photoBackdrop = if (preferences.appearanceStyle == "transparent" && backgroundVersion > 0L) {
        Modifier.clipToBounds()
            .paint(rememberAsyncImagePainter(backgroundRequest(context, backgroundVersion)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            .background(MaterialTheme.colorScheme.background.copy(alpha = preferences.backgroundDim))
    } else Modifier.background(appBackdrop)
    Scaffold(
        modifier = Modifier.fillMaxSize().then(photoBackdrop),
        containerColor = Color.Transparent,
        bottomBar = {
        val barIn = motionMs(220)
        val barOut = motionMs(180)
        AnimatedContent(
            targetState = bottomBarCollapsed,
            transitionSpec = {
                (slideInVertically(tween(barIn)) { it / 2 } + fadeIn(tween(barIn))) togetherWith
                    (slideOutVertically(tween(barOut)) { it / 2 } + fadeOut(tween(barOut))) using SizeTransform { _, _ -> tween(barIn) }
            },
            label = "bottom-bar-collapse"
        ) { collapsed ->
            if (collapsed) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CollapsedPill(preferences.appearanceStyle, { bottomBarCollapsed = it }, { queueOpen = true }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, if (english) "Queue" else "File d'attente")
                    }
                    Spacer(Modifier.width(10.dp))
                    CollapsedPill(preferences.appearanceStyle, { bottomBarCollapsed = it }, { bottomBarCollapsed = false }) {
                        Icon(Icons.Default.KeyboardArrowUp, if (english) "Show navigation" else "Rouvrir la barre")
                    }
                }
            } else if (currentTrackId != null && tab != 1 && activeTrack != null) {
                MiniPlayerDock(
                    track = activeTrack,
                    isPlaying = isPlaying,
                    artworkShapeValue = preferences.artworkShape,
                    english = english,
                    onOpen = { tab = 1 },
                    onNavigate = { tab = it },
                    onPrevious = onPrevious,
                    onTogglePlayback = if (isPlaying) onPause else onResume,
                    onNext = onNext,
                    onCollapse = { bottomBarCollapsed = true }
                )
            } else {
                Surface(
                    Modifier.padding(12.dp).liquidGlassSurface(RoundedCornerShape(28.dp)).barDragCollapse { bottomBarCollapsed = it },
                    shape = RoundedCornerShape(28.dp),
                    color = if (preferences.appearanceStyle != "standard") Color.Transparent else MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NavigationBar {
                            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text(if (english) "Library" else "Bibliothèque") })
                            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Album, null) }, label = { Text(if (english) "Playing" else "Lecture") })
                            NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text(if (english) "Favorites" else "Favoris") })
                            NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, icon = { Icon(Icons.Default.Album, null) }, label = { Text("Playlists") })
                        }
                        IconButton(onClick = { bottomBarCollapsed = true }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, if (english) "Hide navigation" else "Réduire la barre", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }) { padding ->
        val screenMs = motionMs(320)
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val direction = if (isForwardTransition(initialState, targetState)) 1 else -1
                (slideInHorizontally(tween(screenMs)) { direction * it / 3 } + fadeIn(tween(screenMs))) togetherWith
                    (slideOutHorizontally(tween(screenMs)) { -direction * it / 3 } + fadeOut(tween(screenMs)))
            },
            label = "screen"
        ) { screen ->
            if (screen == 0) LibraryScreen(tracks, favoriteIds, currentTrackId, preferences.artworkShape, preferences.sortOrder, onSetSortOrder, onRefresh, { tab = 2 }, onPlay, onPlayNext, onToggleFavorite, onPlayAll, { selectedTrack = it }, Modifier.padding(padding), active = screen == tab)
            else if (screen == 1) NowPlayingScreen(activeTrack, activeTrack?.id in favoriteIds, preferences.artworkShape, isPlaying, playbackPositionMs, playbackDurationMs, onSeek, onPause, onResume, onNext, onPrevious, shuffleEnabled, repeatMode, onToggleShuffle, onCycleRepeat, { tab = 0 }, { activeTrack?.let(onToggleFavorite) }, Modifier.padding(padding))
            else if (screen == 2) SettingsScreen(preferences, onToggleDarkTheme, onEffectsEnabled, effectsSupported, onBassStrength, onVirtualizerStrength, sleepTimerRemainingMs, sleepTimerTotalMs, onSleepTimer, onCrossfadeChange, onArtworkShapeChange, onLanguageChange, onAppearanceStyleChange, backgroundVersion, onPickBackground, onBackgroundDimChange, onAnimationSpeedChange, onWidgetStyleChange, Modifier.padding(padding))
            else if (screen == 3) { val favorites = tracks.filter { it.id in favoriteIds }; FavoritesScreen(favorites, currentTrackId, preferences.artworkShape, { selectedTrack = it; onPlay(it, favorites) }, { onToggleFavorite(it) }, Modifier.padding(padding)) }
            else CollectionsScreen(tracks, playlists, playlistTracks, currentTrackId, preferences.artworkShape, english, onPlay, onPlayGroup, onCreatePlaylist, onDeletePlaylist, onAddTracks, onRemoveTrack, Modifier.padding(padding), active = screen == tab)
        }
        if (queueOpen) {
            QueueSheet(queueRows, currentTrackId, preferences.artworkShape, shuffleEnabled, english, onQueueMove, onPlayQueueIndex) { queueOpen = false }
        }
    }
}

@Composable
private fun CollapsedPill(appearanceStyle: String, onDragIntent: (Boolean) -> Unit, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        Modifier.size(width = 64.dp, height = 30.dp)
            .liquidGlassSurface(RoundedCornerShape(50))
            .barDragCollapse(onDragIntent),
        shape = RoundedCornerShape(50),
        color = if (appearanceStyle != "standard") Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        IconButton(onClick = onClick) { content() }
    }
}

@Composable
private fun MiniPlayerDock(
    track: Track,
    isPlaying: Boolean,
    artworkShapeValue: String,
    english: Boolean,
    onOpen: () -> Unit,
    onNavigate: (Int) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onCollapse: () -> Unit
) {
    val dockShape = RoundedCornerShape(22.dp)
    var destinationsExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .liquidGlassSurface(dockShape)
            .barDragCollapse { if (it) onCollapse() }
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (LocalAppearance.current != "standard") 0.62f else 0.98f), dockShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val miniMs = motionMs(240)
        AnimatedContent(
            targetState = track,
            contentKey = { it.id },
            transitionSpec = {
                (slideInVertically(tween(miniMs)) { it / 2 } + fadeIn(tween(miniMs))) togetherWith
                    (slideOutVertically(tween(miniMs)) { -it / 2 } + fadeOut(tween(miniMs)))
            },
            modifier = Modifier.weight(1f).clickable(onClick = onOpen),
            label = "mini-player-track"
        ) { shown ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(artworkShape(artworkShapeValue))) {
                    ArtworkImage(shown, Modifier.fillMaxSize(), maxSide = 160)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(shown.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(shown.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Précédent") }
        IconButton(onClick = onTogglePlayback) { Crossfade(targetState = isPlaying, animationSpec = tween(motionMs(150)), label = "mini-play-pause") { playing -> Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Lecture") } }
        IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Suivant") }
        IconButton(onClick = onCollapse) { Icon(Icons.Default.KeyboardArrowDown, if (english) "Hide player bar" else "Réduire le lecteur") }
        Box {
            IconButton(onClick = { destinationsExpanded = true }) { Icon(Icons.Default.MoreVert, "Navigation") }
            DropdownMenu(expanded = destinationsExpanded, onDismissRequest = { destinationsExpanded = false }) {
                listOf(
                    0 to if (english) "Library" else "Bibliothèque",
                    2 to if (english) "Settings" else "Réglages",
                    3 to if (english) "Favorites" else "Favoris",
                    4 to "Playlists"
                ).forEach { (destination, title) ->
                    DropdownMenuItem(text = { Text(title) }, onClick = { destinationsExpanded = false; onNavigate(destination) })
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(tracks: List<Track>, favoriteIds: Set<Long>, currentTrackId: Long?, artworkShapeValue: String, sortOrder: String, onSortOrderChange: (String) -> Unit, onRefresh: () -> Unit, onOpenSettings: () -> Unit, onPlay: (track: Track, queue: List<Track>) -> Unit, onPlayNext: (Track) -> Unit, onToggleFavorite: (Track) -> Unit, onPlayAll: (List<Track>) -> Unit, onSelect: (Track) -> Unit, modifier: Modifier, active: Boolean = true) {
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var activeRailLetter by remember { mutableStateOf<Char?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val searchMs = motionMs(250)
    BackHandler(enabled = active && searchExpanded) { searchExpanded = false; searchQuery = "" }
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            awaitFrame() // the field only enters composition with AnimatedVisibility
            searchFocus.requestFocus()
            keyboard?.show()
        }
    }
    val orderedTracks = remember(tracks, sortOrder) { sortTracks(tracks, sortOrder) }
    val matchingTracks = remember(orderedTracks, searchQuery) { searchTracks(orderedTracks, searchQuery) }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = com.bmplayer.R.drawable.bm_player_logo, contentDescription = "Logo BM Player", modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Spacer(Modifier.width(8.dp))
                Text("BM Player", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row {
                IconButton(onClick = { searchExpanded = !searchExpanded; if (searchExpanded.not()) searchQuery = "" }, modifier = Modifier.size(36.dp)) {
                    Icon(if (searchExpanded) Icons.Default.Clear else Icons.Default.Search, if (searchExpanded) "Masquer la recherche" else "Rechercher")
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Settings, "Réglages") }
                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Refresh, "Actualiser") }
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.Sort, "Trier la bibliothèque") }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Ordre alphabétique") }, onClick = { onSortOrderChange("title"); sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Date d’ajout") }, onClick = { onSortOrderChange("date"); sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Aléatoire") }, onClick = { onSortOrderChange("random"); sortMenuExpanded = false })
                    }
                }
                IconButton(onClick = { onPlayAll(matchingTracks) }, enabled = matchingTracks.isNotEmpty(), modifier = Modifier.size(36.dp)) { Icon(Icons.Default.PlayArrow, if (searchQuery.isBlank()) "Lire toute la bibliothèque" else "Lire tous les résultats") }
            }
        }
        Spacer(Modifier.height(20.dp))
        AnimatedVisibility(
            visible = searchExpanded,
            enter = expandVertically(tween(searchMs)) + fadeIn(tween(searchMs)),
            exit = shrinkVertically(tween(searchMs)) + fadeOut(tween(searchMs))
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer la recherche")
                    }
                },
                placeholder = { Text("Titre, artiste, album ou genre") }
            )
        }
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
        } else Row(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(matchingTracks, key = { it.id }) { track -> TrackRow(track, currentTrackId == track.id, favoriteIds.contains(track.id), artworkShapeValue, { onSelect(track); onPlay(track, matchingTracks) }, { onToggleFavorite(track) }, { onPlayNext(track) }) }
            }
            if (searchQuery.isBlank() && matchingTracks.size > 20) {
                Column(
                    Modifier.width(30.dp).fillMaxHeight().pointerInput(matchingTracks) {
                        fun selectAt(y: Float) {
                            val letterIndex = (y / size.height * 26).toInt().coerceIn(0, 25)
                            val letter = ('A'.code + letterIndex).toChar()
                            activeRailLetter = letter
                            val targetIndex = matchingTracks.indexOfFirst { it.title.firstOrNull()?.uppercaseChar() == letter }
                            if (targetIndex >= 0) scope.launch { listState.scrollToItem(targetIndex) }
                        }
                        detectDragGestures(
                            onDragStart = { selectAt(it.y) },
                            onDrag = { change, _ -> change.consume(); selectAt(change.position.y) },
                            onDragEnd = { activeRailLetter = null },
                            onDragCancel = { activeRailLetter = null }
                        )
                    },
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ('A'..'Z').forEachIndexed { index, letter ->
                        val distance = activeRailLetter?.let { abs(index - (it - 'A')) } ?: Int.MAX_VALUE
                        val curve = if (distance <= 5) (1f - distance / 6f) else 0f
                        Box(
                            Modifier.weight(1f).fillMaxWidth().clickable {
                                activeRailLetter = letter
                                val targetIndex = matchingTracks.indexOfFirst { it.title.firstOrNull()?.uppercaseChar() == letter }
                                if (targetIndex >= 0) scope.launch {
                                    listState.animateScrollToItem(targetIndex)
                                    delay(350L)
                                    if (activeRailLetter == letter) activeRailLetter = null
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                letter.toString(),
                                modifier = Modifier.offset(x = (-curve * 9f).dp),
                                color = if (activeRailLetter == letter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = if (activeRailLetter == letter) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
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
private fun TrackRow(track: Track, selected: Boolean, favorite: Boolean, artworkShapeValue: String, onClick: () -> Unit, onToggleFavorite: () -> Unit, onPlayNext: () -> Unit = {}) {
    val rowShape = RoundedCornerShape(if (LocalLiquidGlass.current) 14.dp else 4.dp)
    Row(Modifier.fillMaxWidth().clip(rowShape).background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .14f) else Color.Transparent).liquidGlassSurface(rowShape).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(artworkShape(artworkShapeValue)).background(MaterialTheme.colorScheme.primary.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
            ArtworkImage(track, Modifier.fillMaxSize(), maxSide = 160)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(if (selected) "EN COURS DE LECTURE" else "${track.artist}  ·  ${track.album}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onPlayNext) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Lire ensuite") }
        IconButton(onClick = onToggleFavorite) { Icon(if (favorite) androidx.compose.material.icons.Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favori", tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun NowPlayingScreen(track: Track?, isFavorite: Boolean, artworkShapeValue: String, isPlaying: Boolean, playbackPositionMs: Long, playbackDurationMs: Long, onSeek: (Long) -> Unit, onPause: () -> Unit, onResume: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, shuffleEnabled: Boolean, repeatMode: Int, onToggleShuffle: () -> Unit, onCycleRepeat: () -> Unit, onOpenLibrary: () -> Unit, onToggleFavorite: () -> Unit, modifier: Modifier) {
    val dragX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var artworkWidthPx by remember { mutableIntStateOf(1) }
    var skipDirection by remember { mutableIntStateOf(1) }
    var gestureSwap by remember { mutableStateOf(false) }
    val flyMs = motionMs(220)
    val swapMs = motionMs(360)
    // Read at call time: the gesture handler below is created once and would otherwise see a stale position.
    val positionMs by rememberUpdatedState(playbackPositionMs)
    val nextAction by rememberUpdatedState(onNext)
    val previousAction by rememberUpdatedState(onPrevious)
    val openLibrary by rememberUpdatedState(onOpenLibrary)
    var directionReset by remember { mutableStateOf<Job?>(null) }
    val skipPrevious = {
        skipDirection = if (previousRestartsTrack(positionMs)) 1 else -1
        previousAction()
        // First track: Previous restarts it without a track change, so the next automatic swap must go forward again.
        directionReset?.cancel()
        directionReset = scope.launch { delay(1_000L); skipDirection = 1 }
    }
    val skipNext = { skipDirection = 1; nextAction() }
    LaunchedEffect(track?.id) {
        // The new artwork is now entering from the side; bring the flown-out container back without a visible jump.
        if (dragX.value != 0f) dragX.snapTo(0f)
        gestureSwap = false
        skipDirection = 1
    }
        Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("LECTURE EN COURS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(34.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(330.dp)
                .onSizeChanged { artworkWidthPx = it.width.coerceAtLeast(1) }
                .offset { IntOffset(dragX.value.roundToInt(), 0) }
                .graphicsLayer { rotationZ = (dragX.value * 0.02f).coerceIn(-8f, 8f) }
                .clip(artworkShape(artworkShapeValue))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .18f))
                .liquidGlassSurface(artworkShape(artworkShapeValue))
                .pointerInput(flyMs) {
                    var totalDrag = Offset.Zero
                    fun settle() = scope.launch { if (flyMs == 0) dragX.snapTo(0f) else dragX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    var returnJob: Job? = null
                    fun flyOut(direction: Int) {
                        returnJob?.cancel()
                        returnJob = scope.launch {
                            try {
                                dragX.animateTo(-direction * artworkWidthPx.toFloat(), tween(flyMs))
                            } finally {
                                // A new touch cancels the fly-out animation; the swipe must still change the track.
                                gestureSwap = true
                                if (direction > 0) skipNext() else skipPrevious()
                            }
                            // No track change (e.g. last track without repeat): bring the artwork back instead of leaving it off-screen.
                            delay(700L)
                            if (dragX.value != 0f) { gestureSwap = false; settle() }
                        }
                    }
                    detectDragGestures(
                        onDragStart = { totalDrag = Offset.Zero; returnJob?.cancel() },
                        onDrag = { change, amount ->
                            change.consume()
                            totalDrag += amount
                            scope.launch { dragX.snapTo(totalDrag.x) }
                        },
                        onDragEnd = {
                            val threshold = artworkWidthPx * 0.3f
                            when {
                                // Animations off: no fly-out, so no empty gap while waiting for the track change.
                                flyMs == 0 && totalDrag.x < -threshold -> { skipNext(); settle() }
                                flyMs == 0 && totalDrag.x > threshold -> { skipPrevious(); settle() }
                                totalDrag.x < -threshold -> flyOut(1)
                                // Past 3 s Previous only restarts the track: no fly-out, which would leave an empty gap.
                                totalDrag.x > threshold -> if (previousRestartsTrack(positionMs)) { skipPrevious(); settle() } else flyOut(-1)
                                else -> {
                                    if (totalDrag.y > 100f) openLibrary()
                                    settle()
                                }
                            }
                        },
                        onDragCancel = { settle() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = track,
                transitionSpec = {
                    val s = skipDirection
                    val enter = slideInHorizontally(tween(swapMs)) { s * it } + fadeIn(tween(swapMs)) + scaleIn(tween(swapMs), initialScale = 0.92f)
                    // After a swipe the old artwork already flew away, so it leaves instantly.
                    val exit = if (gestureSwap) fadeOut(tween(0)) else slideOutHorizontally(tween(swapMs)) { -s * it } + fadeOut(tween(swapMs))
                    enter togetherWith exit
                },
                label = "album-art-transition"
            ) { displayedTrack ->
                if (displayedTrack != null) ArtworkImage(displayedTrack, Modifier.fillMaxSize(), maxSide = 1024)
                else Icon(Icons.Default.LibraryMusic, null, Modifier.size(90.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().liquidGlassSurface(RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track?.title ?: "Sélectionnez un morceau", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track?.artist ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onToggleFavorite, enabled = track != null) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favori", tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onToggleShuffle) { Icon(Icons.Default.Shuffle, "Aléatoire", tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = skipPrevious) { Icon(Icons.Default.SkipPrevious, "Précédent", Modifier.size(28.dp)) }
            Surface(Modifier.size(60.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary) { IconButton(onClick = if (isPlaying) onPause else onResume) { Crossfade(targetState = isPlaying, animationSpec = tween(motionMs(150)), label = "play-pause") { playing -> Icon(imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (playing) "Pause" else "Lecture", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) } } }
            IconButton(onClick = skipNext) { Icon(Icons.Default.SkipNext, "Suivant", Modifier.size(28.dp)) }
            IconButton(onClick = onCycleRepeat) { Icon(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Répétition", tint = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    preferences: UserPreferences,
    onToggleDarkTheme: (Boolean) -> Unit,
    onEffectsEnabled: (Boolean) -> Unit,
    effectsSupported: Boolean?,
    onBassStrength: (Int) -> Unit,
    onVirtualizerStrength: (Int) -> Unit,
    sleepTimerRemainingMs: Long,
    sleepTimerTotalMs: Long,
    onSleepTimer: (Int) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onArtworkShapeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onAppearanceStyleChange: (String) -> Unit,
    backgroundVersion: Long,
    onPickBackground: () -> Unit,
    onBackgroundDimChange: (Float) -> Unit,
    onAnimationSpeedChange: (String) -> Unit,
    onWidgetStyleChange: (String) -> Unit,
    modifier: Modifier
) {
    var bassStrength by remember(preferences.bassStrength) { mutableFloatStateOf(preferences.bassStrength.toFloat()) }
    var virtualizerStrength by remember(preferences.virtualizerStrength) { mutableFloatStateOf(preferences.virtualizerStrength.toFloat()) }
    var customTimerMinutes by remember { mutableStateOf(40) }
    var showAppearance by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val english = preferences.language == "en"
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(if (english) "Settings" else "Réglages", style = MaterialTheme.typography.headlineLarge)
        Row(Modifier.fillMaxWidth().liquidGlassSurface(RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(if (english) "Dark mode" else "Mode sombre"); Text(if (english) "Follow the selected theme" else "Suivre le thème choisi", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = preferences.darkTheme, onCheckedChange = onToggleDarkTheme)
        }
        Row(Modifier.fillMaxWidth().liquidGlassSurface(RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (english) "Audio effects" else "Effets audio")
                Text(
                    when (effectsSupported) {
                        true -> if (english) "Supported on this device" else "Disponibles sur cet appareil"
                        false -> if (english) "No compatible effects found" else "Aucun effet compatible détecté"
                        null -> if (english) "Checking device support…" else "Vérification de l’appareil…"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = preferences.effectsEnabled, onCheckedChange = onEffectsEnabled, enabled = effectsSupported != false)
        }
        if (preferences.effectsEnabled && effectsSupported == true) {
            Text(if (english) "Bass boost" else "Amplification des basses")
            Slider(value = bassStrength, onValueChange = { bassStrength = it }, valueRange = 0f..1000f, onValueChangeFinished = { onBassStrength(bassStrength.roundToInt()) })
            Text(if (english) "Virtualizer" else "Spatialisation")
            Slider(value = virtualizerStrength, onValueChange = { virtualizerStrength = it }, valueRange = 0f..1000f, onValueChangeFinished = { onVirtualizerStrength(virtualizerStrength.roundToInt()) })
        }
        Row(Modifier.fillMaxWidth().liquidGlassSurface(RoundedCornerShape(16.dp)).clickable { showAppearance = !showAppearance }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (english) "Appearance" else "Apparence", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text(if (showAppearance) "−" else "+", style = MaterialTheme.typography.titleLarge) }
        if (showAppearance) {
            Text("Style", style = MaterialTheme.typography.titleMedium)
            val styles = listOf("standard" to "Standard", "liquid_glass" to "Liquid Glass", "transparent" to "Transparent")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                styles.forEachIndexed { index, (value, label) ->
                    SegmentedButton(selected = preferences.appearanceStyle == value, onClick = { onAppearanceStyleChange(value) }, shape = SegmentedButtonDefaults.itemShape(index, styles.size)) { Text(label, maxLines = 1) }
                }
            }
            Text(if (english) "Animation speed" else "Vitesse des animations", style = MaterialTheme.typography.titleMedium)
            val speeds = listOf("slow" to if (english) "Slow" else "Lente", "normal" to if (english) "Normal" else "Normale", "fast" to if (english) "Fast" else "Rapide", "off" to if (english) "Off" else "Désactivées")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                speeds.forEachIndexed { index, (value, label) ->
                    SegmentedButton(selected = preferences.animationSpeed == value, onClick = { onAnimationSpeedChange(value) }, shape = SegmentedButtonDefaults.itemShape(index, speeds.size)) { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                }
            }
            if (preferences.appearanceStyle == "transparent") {
                val context = LocalContext.current
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (backgroundVersion > 0L) AsyncImage(model = backgroundRequest(context, backgroundVersion), contentDescription = if (english) "Background photo" else "Photo de fond", modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    else Text(if (english) "Choose a photo" else "Choisissez une photo", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Button(onClick = onPickBackground) { Text(if (backgroundVersion > 0L) (if (english) "Change" else "Changer") else (if (english) "Choose" else "Choisir")) }
                }
                var dim by remember(preferences.backgroundDim) { mutableStateOf(preferences.backgroundDim) }
                Text(if (english) "Overlay: ${(dim * 100).roundToInt()} %" else "Voile : ${(dim * 100).roundToInt()} %")
                Slider(value = dim, onValueChange = { dim = it }, valueRange = 0f..0.8f, onValueChangeFinished = { onBackgroundDimChange(dim) })
            }
            Text("Widget", style = MaterialTheme.typography.titleMedium)
            val widgetStyles = listOf("accent" to if (english) "Artwork color" else "Couleur de la pochette", "glass" to "Liquid Glass")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                widgetStyles.forEachIndexed { index, (value, label) ->
                    SegmentedButton(selected = preferences.widgetStyle == value, onClick = { onWidgetStyleChange(value) }, shape = SegmentedButtonDefaults.itemShape(index, widgetStyles.size)) { Text(label, maxLines = 1) }
                }
            }
            Text(if (english) "Artwork shape" else "Forme des pochettes", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("round" to if (english) "Round" else "Ronde", "rounded" to if (english) "Rounded" else "Arrondie", "square" to if (english) "Square" else "Carrée").forEach { (value, label) -> Button(onClick = { onArtworkShapeChange(value) }) { Text(label) } }
            }
            Text("Fondu enchaîné : ${preferences.crossfadeSeconds}s", style = MaterialTheme.typography.titleMedium)
            Slider(value = preferences.crossfadeSeconds.toFloat(), onValueChange = { onCrossfadeChange(it.roundToInt()) }, valueRange = 0f..15f, steps = 14)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("0s"); Text("15s") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onLanguageChange("fr") }) { Text("Français") }; Button(onClick = { onLanguageChange("en") }) { Text("English") } }
        }
        Text(if (english) "Sleep timer" else "Minuteur", style = MaterialTheme.typography.titleMedium)
        if (sleepTimerRemainingMs > 0) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { sleepTimerProgress(sleepTimerRemainingMs, sleepTimerTotalMs) }, modifier = Modifier.fillMaxSize(), strokeWidth = 8.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatTimerRemaining(sleepTimerRemainingMs), style = MaterialTheme.typography.headlineLarge)
                        Text(if (english) "remaining" else "restantes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { onSleepTimer(0) }) { Text(if (english) "Cancel" else "Annuler") }
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60, 90).forEach { minutes -> FilterChip(selected = false, onClick = { onSleepTimer(minutes) }, label = { Text("$minutes min") }) }
            }
            Text(if (english) "Custom: $customTimerMinutes min" else "Personnalisé : $customTimerMinutes min")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Slider(value = customTimerMinutes.toFloat(), onValueChange = { customTimerMinutes = (it / 5).roundToInt() * 5 }, valueRange = 5f..180f, steps = 34, modifier = Modifier.weight(1f))
                Button(onClick = { onSleepTimer(customTimerMinutes) }) { Text(if (english) "Start" else "Démarrer") }
            }
        }
        val aboutContext = LocalContext.current
        val appVersion = remember(aboutContext) { appVersionName(aboutContext) }
        Row(Modifier.fillMaxWidth().liquidGlassSurface(RoundedCornerShape(16.dp)).clickable { showAbout = !showAbout }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (english) "About" else "À propos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text("Version $appVersion · 2026") }
        if (showAbout) {
            Text("BM Player · lecteur local sans publicité ni suivi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Politique de confidentialité\nAucune donnée personnelle, aucun tracker et aucune publicité. La musique reste sur votre appareil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Conditions d'utilisation\nBM Player est fourni pour la lecture de contenus audio que vous possédez ou êtes autorisé à utiliser.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Licence open source\nLe code de BM Player est distribué sous licence MIT.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ArtworkImage(track: Track, modifier: Modifier, maxSide: Int = 256) {
    val context = LocalContext.current
    // The cached bitmap is the initial value, so a cover already decoded comes back without a blank frame.
    // key() gives a reused row a fresh state holder, otherwise it keeps showing the cover of the track it held before.
    val artwork by key(track.id, maxSide) {
        produceState(cachedArtwork(track.id, maxSide), track.id, maxSide) {
            value = loadCachedArtwork(context, track, maxSide)
        }
    }
    Crossfade(artwork, modifier, tween(motionMs(220)), label = "artwork") { bitmap ->
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), "Pochette de ${track.title}", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, Modifier.fillMaxSize(0.4f), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AnimatedWaveform(isPlaying: Boolean) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "waveform")
    val waveformColor = MaterialTheme.colorScheme.primary
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2_400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "waveform-phase"
    )
    Canvas(Modifier.fillMaxWidth().height(22.dp)) {
        val barCount = 52
        val cellWidth = size.width / barCount
        val strokeWidth = 2.dp.toPx()
        repeat(barCount) { index ->
            val base = sin(index * 0.48f)
            val movement = if (isPlaying) sin(index * 0.72f + phase) else 0f
            val normalized = (0.42f + base * 0.12f + movement * 0.18f).coerceIn(0.14f, 0.78f)
            val barHeight = size.height * normalized
            val centerX = cellWidth * (index + 0.5f)
            drawLine(
                color = waveformColor.copy(alpha = 0.32f + normalized * 0.36f),
                start = Offset(centerX, (size.height - barHeight) / 2f),
                end = Offset(centerX, (size.height + barHeight) / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

// Read from the installed package: the version used to be typed here by hand and drifted behind the build.
private fun appVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""

private fun formatDuration(milliseconds: Long): String { val seconds = milliseconds / 1000; return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}" }

private fun backgroundRequest(context: Context, version: Long): ImageRequest = ImageRequest.Builder(context)
    .data(backgroundFile(context))
    .memoryCacheKey("app_background_$version")
    .diskCachePolicy(CachePolicy.DISABLED)
    .build()
