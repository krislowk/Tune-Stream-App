package com.vynce.vynceclient.models

import kotlinx.serialization.Serializable

@Serializable
data class Context(
    val client: Client,
    val thirdParty: ThirdParty? = null,
) {
    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val osVersion: String? = null,
        val gl: String,
        val hl: String,
        val visitorData: String? = null,
        val androidSdkVersion: Int? = null,
        val platform: String? = null,
    )

    @Serializable
    data class ThirdParty(
        val embedUrl: String,
    )
}
