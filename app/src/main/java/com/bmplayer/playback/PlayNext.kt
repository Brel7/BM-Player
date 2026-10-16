package com.bmplayer.playback

/** Shuffled play order with [item] moved to play right after [after]; Media3 inserts new items at a random shuffled spot. */
fun moveAfter(order: List<Int>, item: Int, after: Int): List<Int> {
    val rest = order - item
    val at = rest.indexOf(after)
    return if (at < 0) order else rest.take(at + 1) + item + rest.drop(at + 1)
}

/** "Play next" tracks still waiting after the current one: the first starts playing, or the user went elsewhere. */
fun advancePending(pending: List<Long>, currentId: Long?): List<Long> =
    if (currentId != null && pending.firstOrNull() == currentId) pending.drop(1) else emptyList()
