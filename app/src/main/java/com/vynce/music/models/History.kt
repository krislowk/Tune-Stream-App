package com.vynce.music.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "history",
    indices = [Index(value = ["timestamp"])]
)
data class History(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: String,
    val timestamp: Long = System.currentTimeMillis()
)
















