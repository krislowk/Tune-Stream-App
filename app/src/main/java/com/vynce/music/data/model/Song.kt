package com.vynce.music.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["mediaId"], unique = true),
        Index(value = ["isLiked"]),
        Index(value = ["artist"]),
        Index(value = ["album"])
    ]
)
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: String, // Global unique ID (YouTube videoId or Local file path)
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long = 0, // In seconds for display
    val durationMs: Long = 0,
    val contentUri: String = "",
    val thumbnail: String = "",
    val isYoutube: Boolean = false,
    val isLiked: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

/**
 * Partial data class for UI performance (Projections)
 * Reduces memory overhead and avoids unnecessary recompositions when
 * non-UI fields of the Song change.
 */
data class SongItem(
    val mediaId: String,
    val title: String,
    val artist: String,
    val thumbnail: String,
    val isLiked: Boolean
)
