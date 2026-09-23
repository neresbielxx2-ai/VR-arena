package com.lunarvr.music

data class MusicTrack(
    val id: String,
    var name: String,
    val filePath: String,
    val durationMs: Long = 0L
)
