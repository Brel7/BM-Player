package com.bmplayer.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bmplayer.ArtworkImage
import com.bmplayer.artworkShape

private val QueueRowHeight = 64.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    entries: List<QueueEntry>,
    currentTrackId: Long?,
    artworkShapeValue: String,
    shuffleEnabled: Boolean,
    english: Boolean,
    onMove: (fromTimelineIndex: Int, toTimelineIndex: Int) -> Unit,
    onPlayAt: (timelineIndex: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // The drag reorders this copy on the spot; the timeline is told once, on release, so the controller cannot
    // report an order back mid gesture and restart the pointer handler under the finger.
    var rows by remember(entries) { mutableStateOf(entries) }
    var draggedSlot by remember(entries) { mutableStateOf<Int?>(null) }
    var dragStartSlot by remember(entries) { mutableIntStateOf(-1) }
    // The order as the finger took hold: a second drag started before the controller reports the first one back
    // would otherwise read its timeline indices from a list the screen has already left behind.
    var orderAtDragStart by remember(entries) { mutableStateOf(emptyList<QueueEntry>()) }
    var dragOffsetPx by remember(entries) { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { QueueRowHeight.roundToPx() }
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    LaunchedEffect(entries) {
        val playing = rows.indexOfFirst { it.track.id == currentTrackId }
        if (playing > 0) listState.scrollToItem(playing)
    }

    fun dropDraggedRow() {
        val to = draggedSlot
        if (to != null && to != dragStartSlot && dragStartSlot in orderAtDragStart.indices && to in orderAtDragStart.indices) {
            onMove(orderAtDragStart[dragStartSlot].index, orderAtDragStart[to].index)
        }
        draggedSlot = null
        dragStartSlot = -1
        dragOffsetPx = 0f
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text(if (english) "Queue" else "File d'attente", style = MaterialTheme.typography.titleLarge)
            Text(
                trackCount(rows.size, english),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            if (shuffleEnabled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (english) "Shuffle is on. Moving a track fixes the order and turns shuffle off."
                    else "Lecture aléatoire active. Déplacer un titre fixera l'ordre et désactivera l'aléatoire.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(12.dp))
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (english) "The queue is empty." else "La file d'attente est vide.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                    itemsIndexed(rows, key = { _, entry -> entry.index }) { position, entry ->
                        val dragging = draggedSlot == position
                        val playing = entry.track.id == currentTrackId
                        Row(
                            Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = if (dragging) null else tween(motionMs(180))
                                )
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer {
                                    if (dragging) {
                                        translationY = dragOffsetPx
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                        shadowElevation = 12f
                                    }
                                }
                                .fillMaxWidth()
                                .height(QueueRowHeight)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (dragging) MaterialTheme.colorScheme.surfaceVariant
                                    else if (playing) MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { onPlayAt(entry.index) }
                                .semantics {
                                    customActions = listOf(
                                        CustomAccessibilityAction(if (english) "Move up" else "Monter") {
                                            if (position > 0) onMove(rows[position].index, rows[position - 1].index)
                                            true
                                        },
                                        CustomAccessibilityAction(if (english) "Move down" else "Descendre") {
                                            if (position < rows.lastIndex) onMove(rows[position].index, rows[position + 1].index)
                                            true
                                        }
                                    )
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(48.dp).clip(artworkShape(artworkShapeValue))) {
                                ArtworkImage(entry.track, Modifier.fillMaxSize(), maxSide = 160)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    entry.track.artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                Modifier.size(34.dp).pointerInput(entries) {
                                    detectDragGestures(
                                        onDragStart = {
                                            orderAtDragStart = rows
                                            draggedSlot = position
                                            dragStartSlot = position
                                            dragOffsetPx = 0f
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            val from = draggedSlot ?: return@detectDragGestures
                                            dragOffsetPx += amount.y
                                            val target = dragTargetIndex(from, dragOffsetPx, rowHeightPx, rows.size)
                                            if (target != from) {
                                                rows = moveItem(rows, from, target)
                                                dragOffsetPx -= (target - from) * rowHeightPx
                                                draggedSlot = target
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        },
                                        onDragEnd = { dropDraggedRow() },
                                        onDragCancel = { dropDraggedRow() }
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                color = if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Crossfade(dragging, animationSpec = tween(motionMs(150)), label = "queue-handle") { held ->
                                        if (held) {
                                            Icon(
                                                Icons.Default.DragHandle,
                                                if (english) "Move track" else "Déplacer le titre",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text(
                                                "${position + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
