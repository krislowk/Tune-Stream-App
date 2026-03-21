package com.vynce.vynceclient.models

import kotlinx.serialization.Serializable

@Serializable
data class AccountInfo(
    val name: String,
    val email: String?,
    val channelHandle: String?,
    val thumbnail: String? = null,
)
