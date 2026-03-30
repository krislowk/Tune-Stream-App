package com.vynce.music.utils

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.vynce.music.data.model.Song
import com.vynce.vynceclient.models.SongItem

fun SongItem.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putString("album_id", album?.id)
        putString("artist_id", artists.firstOrNull()?.id)
    }
    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(thumbnail.toUri())
                .setExtras(extras)
                .build()
        )
        .build()
}

fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(thumbnail.toUri())
                .build()
        )
        .build()
}
