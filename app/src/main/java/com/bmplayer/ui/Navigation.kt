package com.bmplayer.ui

// Tabs: 0 Library, 1 Now playing, 2 Settings, 3 Favorites, 4 Playlists. Order used for slide direction.
val TAB_ORDER: List<Int> = listOf(0, 1, 3, 4, 2)

/** Where Back leads from [tab]; null lets Android close the app. */
fun backDestination(tab: Int, hasTrack: Boolean): Int? = when (tab) {
    0 -> null
    1 -> 0
    else -> if (hasTrack) 1 else 0
}

fun isForwardTransition(from: Int, to: Int): Boolean = TAB_ORDER.indexOf(to) > TAB_ORDER.indexOf(from)
