package com.vynce.vynceclient.models.body

import com.vynce.vynceclient.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)












