package com.vynce.music.provider

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.vynce.music.models.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalProvider @Inject constructor(@ApplicationContext private val context: Context) {
    fun fetchAudioFiles(): List<Song> {
        val songList = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
        )

        val cursor = context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            while (it.moveToNext()) {
                val mediaIdLong = it.getLong(idCol)
                val durationMs = it.getLong(durationCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    mediaIdLong
                ).toString()

                songList.add(
                    Song(
                        id = 0,
                        mediaId = "local_$mediaIdLong",
                        title = it.getString(titleCol) ?: "Unknown",
                        artist = it.getString(artistCol) ?: "Unknown",
                        album = it.getString(albumCol),
                        duration = durationMs / 1000,
                        durationMs = durationMs,
                        contentUri = contentUri,
                        thumbnail = "",
                        isYoutube = false
                    )
                )
            }
        }

        return songList
    }
}















