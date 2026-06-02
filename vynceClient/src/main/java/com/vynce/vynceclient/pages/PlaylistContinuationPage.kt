package com.vynce.vynceclient.pages

import com.vynce.vynceclient.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)















