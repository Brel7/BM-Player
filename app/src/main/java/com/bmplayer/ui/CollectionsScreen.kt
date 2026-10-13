package com.bmplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bmplayer.ArtworkImage
import com.bmplayer.artworkShape
import com.bmplayer.data.local.PlaylistEntity
import com.bmplayer.domain.model.Track
import com.bmplayer.ui.theme.liquidGlassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionsScreen(
    tracks: List<Track>,
    playlists: List<PlaylistEntity>,
    playlistTracks: Map<Long, List<Long>>,
    currentTrackId: Long?,
    artworkShapeValue: String,
    english: Boolean,
    onPlay: (Track, List<Track>) -> Unit,
    onPlayGroup: (tracks: List<Track>, shuffle: Boolean) -> Unit,
    onCreatePlaylist: (name: String, onCreated: (Long) -> Unit) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddTracks: (playlistId: Long, trackIds: List<Long>) -> Unit,
    onRemoveTrack: (playlistId: Long, trackId: Long) -> Unit,
    modifier: Modifier,
    active: Boolean = true
) {
    val artists = remember(tracks, english) { groupByArtist(tracks, if (english) "Unknown artist" else "Artiste inconnu") }
    val albums = remember(tracks, english) { groupByAlbum(tracks, if (english) "Unknown" else "Inconnu") }
    val mixes = remember(tracks, playlists, playlistTracks) {
        playlists.map { TrackGroup("mix:${it.id}", it.name, "", orderByIds(tracks, playlistTracks[it.id].orEmpty())) }
    }
    var section by rememberSaveable { mutableIntStateOf(0) }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    // A just-created mix resolves once the database emits it; until then the grid stays visible.
    val openGroup = remember(openKey, artists, albums, mixes) { openKey?.let { key -> (artists + albums + mixes).firstOrNull { it.key == key } } }
    // Only when a detail page is really shown, and not while this screen slides out.
    BackHandler(enabled = active && openGroup != null) { openKey = null }
    val screenMs = motionMs(320)
    AnimatedContent(
        targetState = openGroup,
        contentKey = { it?.key },
        transitionSpec = {
            val direction = if (targetState != null) 1 else -1
            (slideInHorizontally(tween(screenMs)) { direction * it / 3 } + fadeIn(tween(screenMs))) togetherWith
                (slideOutHorizontally(tween(screenMs)) { -direction * it / 3 } + fadeOut(tween(screenMs)))
        },
        modifier = modifier.fillMaxSize(),
        label = "collections"
    ) { group ->
        if (group == null) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Playlists", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                    if (section == 2) IconButton(onClick = { creating = true }) { Icon(Icons.Default.Add, if (english) "New mix" else "Nouvelle compile") }
                }
                PrimaryTabRow(selectedTabIndex = section, containerColor = Color.Transparent) {
                    listOf(if (english) "Artists" else "Artistes", "Albums", if (english) "My mixes" else "Mes compiles").forEachIndexed { index, label ->
                        Tab(selected = section == index, onClick = { section = index }, text = { Text(label, maxLines = 1) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                val groups = when (section) { 0 -> artists; 1 -> albums; else -> mixes }
                if (groups.isEmpty()) {
                    Text(
                        when {
                            section == 2 && english -> "No mix yet. Tap + to create one."
                            section == 2 -> "Aucune compile. Touchez + pour en créer une."
                            english -> "No tracks in the library."
                            else -> "Aucun titre dans la bibliothèque."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(groups, key = { it.key }) { item -> GroupCard(item, mosaic = section == 2, english) { openKey = item.key } }
                    }
                }
            }
        } else {
            GroupDetail(
                group = group,
                mixId = group.key.takeIf { it.startsWith("mix:") }?.removePrefix("mix:")?.toLongOrNull(),
                tracks = tracks,
                currentTrackId = currentTrackId,
                artworkShapeValue = artworkShapeValue,
                english = english,
                onBack = { openKey = null },
                onPlay = onPlay,
                onPlayGroup = onPlayGroup,
                onDeletePlaylist = { id -> onDeletePlaylist(id); openKey = null },
                onAddTracks = onAddTracks,
                onRemoveTrack = onRemoveTrack
            )
        }
    }
    if (creating) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(if (english) "New mix" else "Nouvelle compile") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(if (english) "Mix name" else "Nom de la compile") }) },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = {
                    section = 2
                    onCreatePlaylist(name.trim()) { id -> openKey = "mix:$id" }
                    creating = false
                }) { Text(if (english) "Create" else "Créer") }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(if (english) "Cancel" else "Annuler") } }
        )
    }
}

@Composable
private fun GroupCover(group: TrackGroup, mosaic: Boolean) {
    val tracks = group.tracks
    when {
        tracks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        }
        mosaic && tracks.size >= 4 -> Column(Modifier.fillMaxSize()) {
            listOf(0, 2).forEach { start ->
                Row(Modifier.weight(1f)) {
                    ArtworkImage(tracks[start], Modifier.weight(1f).fillMaxSize())
                    ArtworkImage(tracks[start + 1], Modifier.weight(1f).fillMaxSize())
                }
            }
        }
        else -> ArtworkImage(tracks.first(), Modifier.fillMaxSize())
    }
}

@Composable
private fun GroupCard(group: TrackGroup, mosaic: Boolean, english: Boolean, onOpen: () -> Unit) {
    Column(Modifier.clickable(onClick = onOpen)) {
        val shape = RoundedCornerShape(16.dp)
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(MaterialTheme.colorScheme.primary.copy(alpha = .18f)).liquidGlassSurface(shape)) {
            GroupCover(group, mosaic)
        }
        Spacer(Modifier.height(6.dp))
        Text(group.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val count = trackCount(group.tracks.size, english)
        Text(if (group.subtitle.isBlank()) count else "${group.subtitle} · $count", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// French uses the singular for 0 and 1, English only for 1.
internal fun trackCount(count: Int, english: Boolean) =
    if (english) (if (count == 1) "1 track" else "$count tracks") else (if (count <= 1) "$count titre" else "$count titres")

@Composable
private fun GroupDetail(
    group: TrackGroup,
    mixId: Long?,
    tracks: List<Track>,
    currentTrackId: Long?,
    artworkShapeValue: String,
    english: Boolean,
    onBack: () -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onPlayGroup: (tracks: List<Track>, shuffle: Boolean) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddTracks: (Long, List<Long>) -> Unit,
    onRemoveTrack: (Long, Long) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var headerMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, if (english) "Back" else "Retour") }
                Spacer(Modifier.weight(1f))
                if (mixId != null) Box {
                    IconButton(onClick = { headerMenu = true }) { Icon(Icons.Default.MoreVert, if (english) "Mix options" else "Options de la compile") }
                    DropdownMenu(expanded = headerMenu, onDismissRequest = { headerMenu = false }) {
                        DropdownMenuItem(text = { Text(if (english) "Delete mix" else "Supprimer la compile") }, onClick = { headerMenu = false; confirmDelete = true })
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val shape = RoundedCornerShape(16.dp)
                Box(Modifier.size(120.dp).clip(shape).background(MaterialTheme.colorScheme.primary.copy(alpha = .18f))) { GroupCover(group, mosaic = mixId != null) }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(group.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (group.subtitle.isNotBlank()) Text(group.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text("${trackCount(group.tracks.size, english)} · ${formatTimerRemaining(group.tracks.sumOf { it.durationMs })}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onPlayGroup(group.tracks, false) }, enabled = group.tracks.isNotEmpty()) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (english) "Play" else "Lire")
                }
                OutlinedButton(onClick = { onPlayGroup(group.tracks, true) }, enabled = group.tracks.isNotEmpty()) {
                    Icon(Icons.Default.Shuffle, null); Spacer(Modifier.width(6.dp)); Text(if (english) "Shuffle" else "Aléatoire")
                }
            }
            if (mixId != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(if (english) "Add tracks" else "Ajouter des titres")
                }
            }
        }
        if (group.tracks.isEmpty()) item {
            Text(if (english) "This mix is empty." else "Cette compile est vide.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(group.tracks, key = { it.id }) { track ->
            GroupTrackRow(
                track = track,
                playing = track.id == currentTrackId,
                artworkShapeValue = artworkShapeValue,
                english = english,
                onClick = { onPlay(track, group.tracks) },
                onRemove = mixId?.let { id -> { onRemoveTrack(id, track.id) } }
            )
        }
    }
    if (adding && mixId != null) {
        AddTracksSheet(tracks, group.tracks.map { it.id }.toSet(), english, onAdd = { ids -> onAddTracks(mixId, ids) }, onDismiss = { adding = false })
    }
    if (confirmDelete && mixId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (english) "Delete this mix?" else "Supprimer la compile ?") },
            text = { Text(if (english) "« ${group.title} » will be deleted. Your music files are kept." else "« ${group.title} » sera supprimée. Vos fichiers musicaux sont conservés.") },
            confirmButton = { Button(onClick = { confirmDelete = false; onDeletePlaylist(mixId) }) { Text(if (english) "Delete" else "Supprimer") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(if (english) "Cancel" else "Annuler") } }
        )
    }
}

@Composable
private fun GroupTrackRow(track: Track, playing: Boolean, artworkShapeValue: String, english: Boolean, onClick: () -> Unit, onRemove: (() -> Unit)?) {
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (playing) MaterialTheme.colorScheme.primary.copy(alpha = .14f) else Color.Transparent)
            .liquidGlassSurface(shape).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(artworkShape(artworkShapeValue))) { ArtworkImage(track, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (onRemove != null) Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, if (english) "Track options" else "Options du titre") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(if (english) "Remove from mix" else "Retirer de la compile") }, onClick = { menu = false; onRemove() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTracksSheet(tracks: List<Track>, existing: Set<Long>, english: Boolean, onAdd: (List<Long>) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val checked = remember { mutableStateListOf<Long>() }
    val shown = remember(tracks, query) {
        val q = query.trim()
        if (q.isEmpty()) tracks else tracks.filter { t -> listOf(t.title, t.artist, t.album).any { it.contains(q, ignoreCase = true) } }
    }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(if (english) "Add tracks" else "Ajouter des titres", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text(if (english) "Title, artist or album" else "Titre, artiste ou album") },
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(shown, key = { it.id }) { track ->
                    val already = track.id in existing
                    val toggle = { if (track.id in checked) checked.remove(track.id) else checked.add(track.id) }
                    Row(
                        Modifier.fillMaxWidth()
                            .toggleable(value = already || track.id in checked, enabled = !already, role = Role.Checkbox) { toggle() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = already || track.id in checked, onCheckedChange = null, enabled = !already)
                        Column(Modifier.weight(1f)) {
                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onAdd(tracks.filter { it.id in checked }.map { it.id })
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                enabled = checked.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (english) "Add (${checked.size})" else "Ajouter (${checked.size})") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
