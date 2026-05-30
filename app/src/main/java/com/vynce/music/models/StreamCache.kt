package com.vynce.music.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "stream_cache")
data class StreamCache(
    @PrimaryKey
    val videoId: String,
    val url: String,
    val bitrate: Int,
    val mimeType: String,
    val timestamp: Long = System.currentTimeMillis()
)












