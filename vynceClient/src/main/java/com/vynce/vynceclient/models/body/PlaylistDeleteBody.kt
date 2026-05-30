package com.vynce.vynceclient.models.body

import com.vynce.vynceclient.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)












