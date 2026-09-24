package com.bmplayer.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val trackIds: List<Long> = emptyList()
)
