package com.vynce.music.db.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.vynce.music.db.entities.Album
import com.vynce.music.db.entities.AlbumArtistMap
import com.vynce.music.db.entities.AlbumEntity
import com.vynce.music.db.entities.Artist
import com.vynce.music.db.entities.ArtistEntity
import com.vynce.music.db.entities.Playlist
import com.vynce.music.db.entities.PlaylistEntity
import com.vynce.music.db.entities.PlaylistSong
import com.vynce.music.db.entities.PlaylistSongMap
import com.vynce.music.db.entities.PodcastEntity
import com.vynce.music.db.entities.SetVideoIdEntity
import com.vynce.music.db.entities.Song
import com.vynce.music.db.entities.SongAlbumMap
import com.vynce.music.db.entities.SongArtistMap
import com.vynce.music.db.entities.SongEntity
import com.vynce.music.db.entities.SpeedDialItem
import com.vynce.music.models.AuthSession
import com.vynce.music.models.History
import com.vynce.music.models.MediaMetadata
import com.vynce.music.models.SongItem
import com.vynce.music.models.StreamCache
import com.vynce.music.models.User
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.pages.AlbumPage
import kotlinx.coroutines.flow.Flow
import com.vynce.music.models.Song as SongModel

@Dao
abstract class DatabaseDao {
    // --- Songs (Entity) ---
    @Transaction
    @Query("SELECT * FROM song WHERE liked = 1 ORDER BY title ASC")
    abstract fun likedSongsByNameAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY title ASC")
    abstract fun songsByNameAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isUploaded = 1 ORDER BY title ASC")
    abstract fun uploadedSongsByNameAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE id = :id")
    abstract fun getSongById(id: String): Flow<Song?>

    @Transaction
    @Query("SELECT * FROM song WHERE id = :id")
    abstract suspend fun getSongByIdBlocking(id: String): Song?

    @Upsert
    abstract suspend fun upsert(song: SongEntity)

    @Upsert
    abstract suspend fun upsertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(song: SongEntity)

    @Update
    abstract suspend fun update(song: SongEntity)

    @Delete
    abstract suspend fun delete(song: SongEntity)

    // --- Albums ---
    @Transaction
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY title ASC")
    abstract fun albumsLikedByNameAsc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT * FROM album WHERE isUploaded = 1 ORDER BY title ASC")
    abstract fun albumsUploadedByNameAsc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT * FROM album WHERE id = :id")
    abstract fun getAlbumById(id: String): Flow<Album?>

    @Transaction
    @Query("SELECT * FROM album WHERE id = :id")
    abstract suspend fun getAlbumByIdBlocking(id: String): Album?

    @Upsert
    abstract suspend fun upsert(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(album: AlbumEntity)

    @Update
    abstract suspend fun update(album: AlbumEntity)

    // --- Artists ---
    @Query("""
        SELECT a.*, (SELECT COUNT(*) FROM song_artist_map WHERE artistId = a.id) as songCount 
        FROM artist a 
        WHERE a.bookmarkedAt IS NOT NULL 
        ORDER BY a.name ASC
    """)
    abstract fun artistsBookmarkedByNameAsc(): Flow<List<Artist>>

    @Query("""
        SELECT a.*, (SELECT COUNT(*) FROM song_artist_map WHERE artistId = a.id) as songCount 
        FROM artist a 
        WHERE a.id = :id
    """)
    abstract fun getArtistById(id: String): Flow<Artist?>

    @Query("""
        SELECT a.*, (SELECT COUNT(*) FROM song_artist_map WHERE artistId = a.id) as songCount 
        FROM artist a 
        WHERE a.id = :id
    """)
    abstract suspend fun getArtistByIdBlocking(id: String): Artist?

    @Upsert
    abstract suspend fun upsert(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(artist: ArtistEntity)

    @Update
    abstract suspend fun update(artist: ArtistEntity)

    // --- Podcasts ---
    @Query("SELECT * FROM podcast WHERE bookmarkedAt IS NOT NULL")
    abstract fun subscribedPodcasts(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcast WHERE id = :id")
    abstract fun getPodcastById(id: String): Flow<PodcastEntity?>

    @Query("SELECT * FROM podcast WHERE id = :id")
    abstract suspend fun getPodcastByIdBlocking(id: String): PodcastEntity?

    @Transaction
    @Query("SELECT * FROM song WHERE isEpisode = 1 ORDER BY inLibrary ASC")
    abstract fun podcastEpisodesByCreateDateAsc(): Flow<List<Song>>

    @Upsert
    abstract suspend fun upsert(podcast: PodcastEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(podcast: PodcastEntity)

    @Update
    abstract suspend fun update(podcast: PodcastEntity)

    // --- Playlists ---
    @Transaction
    @Query("""
        SELECT p.*, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p.id) as songCount
        FROM playlist p 
        ORDER BY p.name ASC
    """)
    abstract fun playlistsByNameAsc(): Flow<List<Playlist>>

    @Transaction
    @Query("""
        SELECT p.*, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p.id) as songCount
        FROM playlist p 
        WHERE p.id = :id
    """)
    abstract fun getPlaylistById(id: String): Flow<Playlist?>

    @Query("SELECT * FROM playlist WHERE id = :id")
    abstract suspend fun playlistBlocking(id: String): PlaylistEntity?

    @Upsert
    abstract suspend fun upsert(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(playlist: PlaylistEntity)

    @Update
    abstract suspend fun update(playlist: PlaylistEntity)

    @Delete
    abstract suspend fun delete(playlist: PlaylistEntity)

    // --- Playlist Maps ---
    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE playlistId = :playlistId ORDER BY position ASC")
    abstract fun playlistSongs(playlistId: String): Flow<List<PlaylistSong>>

    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE playlistId = :playlistId ORDER BY position ASC")
    abstract suspend fun playlistSongsBlocking(playlistId: String): List<PlaylistSong>

    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId")
    abstract suspend fun clearPlaylist(playlistId: String)

    @Upsert
    abstract suspend fun upsert(map: PlaylistSongMap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(map: PlaylistSongMap)

    // --- Songs (Model) ---
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    abstract fun getAllSongsFlow(): Flow<List<SongModel>>

    @Query("SELECT mediaId, title, artist, thumbnail, isLiked FROM songs ORDER BY dateAdded DESC")
    abstract fun getAllSongItemsFlow(): Flow<List<SongItem>>

    @Query("SELECT * FROM songs WHERE mediaId = :mediaId")
    abstract suspend fun getSongByMediaId(mediaId: String): SongModel?

    @Query("SELECT * FROM songs WHERE isYoutube = 0 ORDER BY dateAdded DESC")
    abstract fun getLocalSongsFlow(): Flow<List<SongModel>>

    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY dateAdded DESC")
    abstract fun getLikedSongsFlow(): Flow<List<SongModel>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, title")
    abstract fun getSongsByArtist(artist: String): Flow<List<SongModel>>

    @Query("SELECT * FROM songs WHERE mediaId IN (:mediaIds)")
    abstract fun getSongsByMediaIds(mediaIds: List<String>): Flow<List<SongModel>>

    @Upsert
    abstract suspend fun upsert(song: SongModel)

    @Upsert
    abstract suspend fun upsertSongsModel(songs: List<SongModel>)

    @Query("UPDATE songs SET isLiked = NOT isLiked WHERE mediaId = :mediaId")
    abstract suspend fun toggleLike(mediaId: String)

    @Query("UPDATE songs SET duration = :duration, durationMs = :durationMs WHERE mediaId = :mediaId")
    abstract suspend fun updateDuration(mediaId: String, duration: Long, durationMs: Long)

    @Query("DELETE FROM songs")
    abstract suspend fun clearAllSongs()

    @Query("SELECT * FROM songs ORDER BY title ASC")
    abstract fun getPagedSongs(): PagingSource<Int, SongModel>

    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
        OR artist LIKE '%' || :query || '%'
        ORDER BY CASE 
            WHEN title LIKE :query || '%' THEN 1
            WHEN artist LIKE :query || '%' THEN 2
            ELSE 3
        END
        LIMIT 50
    """)
    abstract fun searchSongs(query: String): Flow<List<SongModel>>

    // --- History ---
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    abstract fun getHistory(): Flow<List<History>>

    @Upsert
    abstract suspend fun upsertHistory(history: History)

    @Query("DELETE FROM history WHERE mediaId = :mediaId")
    abstract suspend fun deleteHistory(mediaId: String)

    @Query("DELETE FROM history")
    abstract suspend fun clearHistory()

    @Query("DELETE FROM history WHERE timestamp < :timestamp")
    abstract suspend fun deleteOldHistory(timestamp: Long)

    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN (
            SELECT mediaId, MAX(timestamp) as latest_play 
            FROM history 
            GROUP BY mediaId
        ) h ON s.mediaId = h.mediaId
        ORDER BY h.latest_play DESC
        LIMIT :limit
    """)
    abstract fun getRecentlyPlayedSongs(limit: Int = 20): Flow<List<SongModel>>

    @Query("DELETE FROM history")
    abstract suspend fun clearListenHistory()

    @Query("DELETE FROM search_history")
    abstract suspend fun clearSearchHistory()

    // --- User ---
    @Query("SELECT * FROM users WHERE id = :userId")
    abstract fun getUser(userId: String = "default_user"): Flow<User?>

    @Upsert
    abstract suspend fun upsertUser(user: User)

    @Delete
    abstract suspend fun deleteUser(user: User)

    // --- Auth ---
    @Query("SELECT * FROM auth_session WHERE id = :id")
    abstract fun getSession(id: String = "default_session"): Flow<AuthSession?>

    @Upsert
    abstract suspend fun upsertSession(session: AuthSession)

    @Query("DELETE FROM auth_session")
    abstract suspend fun clearSession()

    // --- Speed Dial ---
    @Query("SELECT * FROM speed_dial_item ORDER BY createDate ASC")
    abstract fun getAllSpeedDialItems(): Flow<List<SpeedDialItem>>

    @Upsert
    abstract suspend fun upsertSpeedDialItem(item: SpeedDialItem)

    @Query("DELETE FROM speed_dial_item WHERE id = :id")
    abstract suspend fun deleteSpeedDialItem(id: String)

    @Query("SELECT EXISTS(SELECT * FROM speed_dial_item WHERE id = :id)")
    abstract fun isPinned(id: String): Flow<Boolean>

    // --- Stream Cache ---
    @Query("SELECT * FROM stream_cache WHERE videoId = :videoId")
    abstract suspend fun getStream(videoId: String): StreamCache?

    @Upsert
    abstract suspend fun upsertStream(stream: StreamCache)

    @Query("DELETE FROM stream_cache WHERE videoId = :videoId")
    abstract suspend fun deleteStream(videoId: String)

    @Query("DELETE FROM stream_cache WHERE timestamp < :expiryTime")
    abstract suspend fun clearExpiredStreams(expiryTime: Long)

    @Query("DELETE FROM stream_cache")
    abstract suspend fun clearAllStreams()

    // --- Related & Maps ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(map: SongArtistMap)

    @Upsert
    abstract suspend fun upsert(map: SongArtistMap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(map: SongAlbumMap)

    @Upsert
    abstract suspend fun upsert(map: SongAlbumMap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(map: AlbumArtistMap)

    @Upsert
    abstract suspend fun upsert(map: AlbumArtistMap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: SetVideoIdEntity)

    @Upsert
    abstract suspend fun upsert(entity: SetVideoIdEntity)

    // --- Helpers ---

    @Transaction
    open suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return block()
    }

    @Transaction
    open suspend fun insert(mediaMetadata: MediaMetadata, block: (SongEntity) -> SongEntity = { it }) {
        val songEntity = block(mediaMetadata.toSongEntity())
        insert(songEntity)
        
        mediaMetadata.artists.forEachIndexed { index, artist ->
            artist.id?.let { artistId ->
                insert(ArtistEntity(id = artistId, name = artist.name))
                insert(SongArtistMap(songId = songEntity.id, artistId = artistId, position = index))
            }
        }
        
        mediaMetadata.album?.let { album ->
            insert(AlbumEntity(id = album.id, title = album.title, songCount = 0, duration = 0))
            insert(SongAlbumMap(songId = songEntity.id, albumId = album.id, index = 0))
        }
    }

    @Transaction
    open suspend fun insert(albumPage: AlbumPage) {
        val album = albumPage.album
        val albumEntity = AlbumEntity(
            id = album.id,
            playlistId = album.playlistId,
            title = album.title,
            year = album.year,
            thumbnailUrl = album.thumbnail,
            songCount = albumPage.songs.size,
            duration = albumPage.songs.sumOf { it.duration ?: 0 }
        )
        insert(albumEntity)

        album.artists?.forEachIndexed { index, artist ->
            artist.id?.let { artistId ->
                insert(ArtistEntity(id = artistId, name = artist.name))
                insert(AlbumArtistMap(albumId = albumEntity.id, artistId = artistId, order = index))
            }
        }
    }

    @Transaction
    open suspend fun update(playlist: PlaylistEntity, remote: PlaylistItem) {
        update(
            playlist.copy(
                name = remote.title,
                thumbnailUrl = remote.thumbnail,
                isEditable = remote.isEditable,
                remoteSongCount = remote.songCountText?.let {
                    Regex("""\d+""").find(it)?.value?.toIntOrNull()
                },
                playEndpointParams = remote.playEndpoint?.params,
                shuffleEndpointParams = remote.shuffleEndpoint?.params,
                radioEndpointParams = remote.radioEndpoint?.params
            )
        )
    }

    @Transaction
    open suspend fun addSongsToPlaylist(playlist: PlaylistEntity, songs: List<Pair<String, String?>>) {
        val startPosition = (playlistSongsBlocking(playlist.id).maxOfOrNull { it.map.position } ?: -1) + 1
        songs.forEachIndexed { index, (songId, setVideoId) ->
            insert(
                PlaylistSongMap(
                    playlistId = playlist.id,
                    songId = songId,
                    position = startPosition + index,
                    setVideoId = setVideoId
                )
            )
        }
    }
}











