package com.vynce.vynceclient.models.body

import com.vynce.vynceclient.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)












