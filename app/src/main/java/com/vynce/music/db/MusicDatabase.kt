package com.vynce.music.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vynce.music.db.daos.DatabaseDao
import com.vynce.music.db.entities.AlbumArtistMap
import com.vynce.music.db.entities.AlbumEntity
import com.vynce.music.db.entities.ArtistEntity
import com.vynce.music.db.entities.Event
import com.vynce.music.db.entities.FormatEntity
import com.vynce.music.db.entities.LyricsEntity
import com.vynce.music.db.entities.PlayCountEntity
import com.vynce.music.db.entities.PlaylistEntity
import com.vynce.music.db.entities.PlaylistSongMap
import com.vynce.music.db.entities.PlaylistSongMapPreview
import com.vynce.music.db.entities.PodcastEntity
import com.vynce.music.db.entities.RecognitionHistory
import com.vynce.music.db.entities.RelatedSongMap
import com.vynce.music.db.entities.SearchHistory
import com.vynce.music.db.entities.SetVideoIdEntity
import com.vynce.music.db.entities.SongAlbumMap
import com.vynce.music.db.entities.SongArtistMap
import com.vynce.music.db.entities.SongEntity
import com.vynce.music.db.entities.SortedSongAlbumMap
import com.vynce.music.db.entities.SortedSongArtistMap
import com.vynce.music.db.entities.SpeedDialItem
import com.vynce.music.models.AuthSession
import com.vynce.music.models.History
import com.vynce.music.models.Song
import com.vynce.music.models.StreamCache
import com.vynce.music.models.User

@Database(
    entities = [
        Song::class,
        History::class,
        StreamCache::class,
        User::class,
        SpeedDialItem::class,
        AuthSession::class,
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        FormatEntity::class,
        PodcastEntity::class,
        Event::class,
        SearchHistory::class,
        SongArtistMap::class,
        SongAlbumMap::class,
        AlbumArtistMap::class,
        PlaylistEntity::class,
        PlaylistSongMap::class,
        SetVideoIdEntity::class,
        RecognitionHistory::class,
        PlayCountEntity::class,
        RelatedSongMap::class,
        LyricsEntity::class
    ],
    views = [
        PlaylistSongMapPreview::class,
        SortedSongAlbumMap::class,
        SortedSongArtistMap::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract val databaseDao: DatabaseDao

    companion object {
        private const val DB_NAME = "vynce_db"

        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
















