package com.vynce.vynceclient.models.body

import com.vynce.vynceclient.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String? = null,
    val params: String? = null
)















