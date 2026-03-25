package com.vynce.vynceclient.models

import kotlinx.serialization.Serializable

@Serializable
sealed class YTItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnail: String
    abstract val explicit: Boolean
    abstract val shareLink: String
}

@Serializable
data class Artist(
    val name: String,
    val id: String?,
)

@Serializable
data class Album(
    val name: String,
    val id: String,
)

@Serializable
data class SongItem(
    override val id: String,
    override val title: String,
    val artists: List<Artist>,
    val album: Album? = null,
    val duration: Int? = null,
    override val thumbnail: String,
    override val explicit: Boolean = false,
    val endpoint: WatchEndpoint? = null,
) : YTItem() {
    override val shareLink: String
        get() = "https://music.youtube.com/watch?v=$id"
}

@Serializable
data class AlbumItem(
    val browseId: String,
    val playlistId: String,
    override val id: String = browseId,
    override val title: String,
    val artists: List<Artist>?,
    val year: Int? = null,
    override val thumbnail: String,
    override val explicit: Boolean = false,
) : YTItem() {
    override val shareLink: String
        get() = "https://music.youtube.com/playlist?list=$playlistId"
}

@Serializable
data class PlaylistItem(
    override val id: String,
    override val title: String,
    val author: Artist?,
    val songCountText: String?,
    override val thumbnail: String,
    val playEndpoint: WatchEndpoint?,
    val shuffleEndpoint: WatchEndpoint?,
    val radioEndpoint: WatchEndpoint?,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = "https://music.youtube.com/playlist?list=$id"
}

@Serializable
data class ArtistItem(
    override val id: String,
    override val title: String,
    override val thumbnail: String,
    val shuffleEndpoint: WatchEndpoint?,
    val radioEndpoint: WatchEndpoint?,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = "https://music.youtube.com/channel/$id"
}

fun <T : YTItem> List<T>.filterExplicit(enabled: Boolean = true) =
    if (enabled) filter { !it.explicit }
    else this