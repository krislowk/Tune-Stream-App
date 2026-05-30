package com.vynce.music.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "auth_session")
data class AuthSession(
    @PrimaryKey
    val id: String = "default_session",
    val visitorData: String? = null,
    val cookie: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)












