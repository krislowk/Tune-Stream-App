package com.vynce.music.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "default_user",
    val email: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val visitorData: String? = null,
    val cookie: String? = null
)















