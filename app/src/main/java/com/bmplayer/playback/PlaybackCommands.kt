package com.bmplayer.playback

object PlaybackCommands {
    const val SET_EFFECTS_ENABLED = "com.bmplayer.playback.SET_EFFECTS_ENABLED"
    const val ENABLED = "enabled"
    const val SET_CROSSFADE = "com.bmplayer.playback.SET_CROSSFADE"
    const val SECONDS = "seconds"
    const val SET_BASS_STRENGTH = "com.bmplayer.playback.SET_BASS_STRENGTH"
    const val SET_VIRTUALIZER_STRENGTH = "com.bmplayer.playback.SET_VIRTUALIZER_STRENGTH"
    const val STRENGTH = "strength"
    const val PLAY_NEXT = "com.bmplayer.playback.PLAY_NEXT"
    const val TRACK_ID = "track_id"
    const val INDEX = "index"
    val ALL = listOf(SET_EFFECTS_ENABLED, SET_CROSSFADE, SET_BASS_STRENGTH, SET_VIRTUALIZER_STRENGTH, PLAY_NEXT)
}

fun shouldStartCrossfade(isPlaying: Boolean, inProgress: Boolean, repeatOne: Boolean, crossfadeMs: Long, durationMs: Long, positionMs: Long, hasNext: Boolean): Boolean =
    isPlaying && !inProgress && !repeatOne && crossfadeMs > 0 && durationMs > 0 && hasNext && durationMs - positionMs <= crossfadeMs

// Cap at the queue size so repeat-all over unreadable files cannot loop forever.
fun shouldSkipAfterError(consecutiveErrors: Int, mediaItemCount: Int, hasNext: Boolean): Boolean =
    hasNext && consecutiveErrors < mediaItemCount
