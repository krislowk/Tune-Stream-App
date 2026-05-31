package com.vynce.music.utils

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.vynce.music.db.entities.Album
import com.vynce.music.db.entities.Artist
import com.vynce.music.db.entities.Playlist
import com.vynce.music.models.Song
import com.vynce.vynceclient.models.SongItem

fun SongItem.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putString("album_id", album?.id)
        putString("artist_id", artists.firstOrNull()?.id)
        putString("share_link", shareLink)
        putInt("lyrics_offset", 0)
    }
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(android.net.Uri.EMPTY)
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
    val extras = Bundle().apply {
        putInt("lyrics_offset", lyricsOffset)
    }
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(if (!isYoutube && contentUri.isNotEmpty()) contentUri.toUri() else android.net.Uri.EMPTY)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(thumbnail.toUri())
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setExtras(extras)
                .build()
        )
        .build()
}

fun Playlist.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setArtworkUri(thumbnails.firstOrNull()?.toUri())
                .build()
        )
        .build()
}

fun Album.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artists.joinToString { it.name })
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setArtworkUri(thumbnailUrl?.toUri())
                .build()
        )
        .build()
}

fun Artist.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setArtworkUri(thumbnailUrl?.toUri())
                .build()
        )
        .build()
}












