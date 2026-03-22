package com.vynce.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable


@Serializable
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long, // String for display like "3:45"
    val durationMs: Long = 0,
    val contentUri: String= "",
    val thumbnail: String = "",
    val isYoutube: Boolean = false,
    val isLiked: Boolean = false,
)