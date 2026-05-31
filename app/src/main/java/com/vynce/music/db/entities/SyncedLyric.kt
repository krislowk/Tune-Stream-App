package com.vynce.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Entity model representing a successfully synchronized lyric document.
 */
@Entity(tableName = "synced_lyrics")
data class SyncedLyric(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val rawLyrics: String,
    val lrcContent: String,
    val durationSec: Int,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
