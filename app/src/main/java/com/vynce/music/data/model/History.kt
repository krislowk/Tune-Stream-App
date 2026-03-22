package com.vynce.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "history")
data class History(
    @PrimaryKey
    val videoId: String,
    val timestamp: Long = System.currentTimeMillis()
)
