package com.vynce.music.data.model

import android.net.Uri
import kotlinx.serialization.Serializable


@Serializable
data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long, // String for display like "3:45"
    val durationMs: Long = 0,
    val contentUri: Uri,
    val thumbnail: String = "",
    val isYoutube: Boolean = false,
)