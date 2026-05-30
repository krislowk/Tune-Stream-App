/**
 * Metrolist Project (C) 2026
 * OuterTune Project Copyright (C) 2025
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vynce.music.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.vynce.music.db.MusicDatabase
import com.vynce.music.db.entities.ArtistEntity
import com.vynce.music.db.entities.PlaylistEntity
import com.vynce.music.db.entities.PlaylistSongMap
import com.vynce.music.db.entities.PodcastEntity
import com.vynce.music.db.entities.SetVideoIdEntity
import com.vynce.music.db.entities.SongEntity
import com.vynce.music.extensions.isInternetConnected
import com.vynce.music.extensions.isSyncEnabled
import com.vynce.music.models.toMediaMetadata
import com.vynce.music.repository.constants.PreferenceConstants
import com.vynce.vynceclient.YouTube
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.utils.completed
import com.vynce.vynceclient.utils.parseCookieString
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay as delayMs

private const val TAG = "SyncUtils"

sealed class SyncOperation {
    data object FullSync : SyncOperation()
    data object LikedSongs : SyncOperation()
    data object LibrarySongs : SyncOperation()
    data object UploadedSongs : SyncOperation()
    data object LikedAlbums : SyncOperation()
    data object UploadedAlbums : SyncOperation()
    data object ArtistsSubscriptions : SyncOperation()
    data object PodcastSubscriptions : SyncOperation()
    data object EpisodesForLater : SyncOperation()
    data object SavedPlaylists : SyncOperation()
    data object AutoSyncPlaylists : SyncOperation()
    data class SinglePlaylist(val browseId: String, val playlistId: String) : SyncOperation()
    data class LikeSong(val song: SongEntity) : SyncOperation()
    data class SubscribeChannel(val channelId: String, val subscribe: Boolean) : SyncOperation()
    data class SavePodcast(val podcastId: String, val save: Boolean) : SyncOperation()
    data class SaveEpisode(val episodeId: String, val save: Boolean, val setVideoId: String?) : SyncOperation()
    data object CleanupDuplicates : SyncOperation()
    data object ClearAllSynced : SyncOperation()
    data object ClearPodcastData : SyncOperation()
}

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Error(val message: String) : SyncStatus()
    data object Completed : SyncStatus()
}

data class SyncState(
    val overallStatus: SyncStatus = SyncStatus.Idle,
    val likedSongs: SyncStatus = SyncStatus.Idle,
    val librarySongs: SyncStatus = SyncStatus.Idle,
    val uploadedSongs: SyncStatus = SyncStatus.Idle,
    val likedAlbums: SyncStatus = SyncStatus.Idle,
    val uploadedAlbums: SyncStatus = SyncStatus.Idle,
    val artists: SyncStatus = SyncStatus.Idle,
    val playlists: SyncStatus = SyncStatus.Idle,
    val currentOperation: String = ""
)

@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val databaseDao = database.databaseDao
    
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            Log.e(TAG, "Sync coroutine exception", throwable)
        }
    }

    private val syncJob = SupervisorJob()
    private val syncScope = CoroutineScope(Dispatchers.IO + syncJob + exceptionHandler)

    private val syncChannel = Channel<SyncOperation>(Channel.BUFFERED)
    private var processingJob: Job? = null

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    private val playlistsBeingModified = ConcurrentHashMap<String, AtomicInteger>()
    private val pendingYouTubeAdds = ConcurrentHashMap<String, MutableSet<String>>()
    private val pendingRemovals = ConcurrentHashMap<String, MutableSet<Triple<String, String, String>>>()

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val DB_OPERATION_DELAY_MS = 50L
    }
    
    private fun markPlaylistModifying(playlistId: String) {
        playlistsBeingModified.getOrPut(playlistId) { AtomicInteger(0) }.incrementAndGet()
    }

    private fun unmarkPlaylistModifying(playlistId: String) {
        playlistsBeingModified[playlistId]?.let { counter ->
            if (counter.decrementAndGet() <= 0) playlistsBeingModified.remove(playlistId)
        }
    }

    private fun isPlaylistBeingModified(playlistId: String): Boolean =
        (playlistsBeingModified[playlistId]?.get() ?: 0) > 0 ||
                pendingRemovals.any { (_, set) -> set.any { it.third == playlistId } }
                
    init {
        // LastFM functionality currently disabled until migration is complete
        /*context.dataStore.data
            .map { it[LastFMUseSendLikes] ?: false }
            .distinctUntilChanged()
            .collectLatest(syncScope) {
                lastfmSendLikes = it
            }*/

        startProcessingQueue()
    }

    private fun startProcessingQueue() {
        processingJob = syncScope.launch {
            for (operation in syncChannel) {
                try {
                    processOperation(operation)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing sync operation: $operation", e)
                }
            }
        }
    }

    private suspend fun processOperation(operation: SyncOperation) {
        when (operation) {
            is SyncOperation.FullSync -> executeFullSync()
            is SyncOperation.LikedSongs -> executeSyncLikedSongs()
            is SyncOperation.LibrarySongs -> executeSyncLibrarySongs()
            is SyncOperation.UploadedSongs -> executeSyncUploadedSongs()
            is SyncOperation.LikedAlbums -> executeSyncLikedAlbums()
            is SyncOperation.UploadedAlbums -> executeSyncUploadedAlbums()
            is SyncOperation.ArtistsSubscriptions -> executeSyncArtistsSubscriptions()
            is SyncOperation.PodcastSubscriptions -> executeSyncPodcastSubscriptions()
            is SyncOperation.EpisodesForLater -> executeSyncEpisodesForLater()
            is SyncOperation.SavedPlaylists -> executeSyncSavedPlaylists()
            is SyncOperation.AutoSyncPlaylists -> executeSyncAutoSyncPlaylists()
            is SyncOperation.SinglePlaylist -> executeSyncPlaylist(operation.browseId, operation.playlistId)
            is SyncOperation.LikeSong -> executeLikeSong(operation.song)
            is SyncOperation.SubscribeChannel -> executeSubscribeChannel(operation.channelId, operation.subscribe)
            is SyncOperation.SavePodcast -> executeSavePodcast(operation.podcastId, operation.save)
            is SyncOperation.SaveEpisode -> executeSaveEpisode(operation.episodeId, operation.save, operation.setVideoId)
            is SyncOperation.CleanupDuplicates -> executeCleanupDuplicatePlaylists()
            is SyncOperation.ClearAllSynced -> executeClearAllSyncedContent()
            is SyncOperation.ClearPodcastData -> executeClearPodcastData()
        }
    }

    private suspend fun isLoggedIn(): Boolean {
        return try {
            val cookie = context.dataStore.data
                .map { it[PreferenceConstants.INNER_TUBE_COOKIE] }
                .first()
            cookie?.let { "SAPISID" in parseCookieString(it) } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login status", e)
            false
        }
    }

    private suspend fun <T> withRetry(
        maxRetries: Int = MAX_RETRIES,
        initialdelay: Long = INITIAL_RETRY_DELAY_MS,
        block: suspend () -> T
    ): Result<T> {
        var currentdelay = initialdelay
        repeat(maxRetries) { attempt ->
            try {
                return Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1}/$maxRetries failed", e)
                if (attempt == maxRetries - 1) {
                    return Result.failure(e)
                }
                delayMs(currentdelay)
                currentdelay *= 2
            }
        }
        return Result.failure(Exception("Max retries exceeded"))
    }

    private fun updateState(update: SyncState.() -> SyncState) {
        _syncState.value = _syncState.value.update()
    }

    // Public API methods

    fun performFullSync() {
        syncScope.launch { syncChannel.send(SyncOperation.FullSync) }
    }

    suspend fun performFullSyncSuspend() {
        if (!isLoggedIn()) return
        executeFullSync()
    }

    fun tryAutoSync() {
        syncScope.launch {
            if (!isLoggedIn()) return@launch
            if (!context.isSyncEnabled() || !context.isInternetConnected()) return@launch

            val lastSync = context.dataStore.get(PreferenceConstants.LAST_FULL_SYNC, 0L)
            val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            if (lastSync > 0 && (currentTime - lastSync) < PreferenceConstants.SYNC_COOLDOWN) return@launch

            syncChannel.send(SyncOperation.FullSync)
            context.dataStore.edit { it[PreferenceConstants.LAST_FULL_SYNC] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) }
        }
    }

    fun runAllSyncs() = performFullSync()

    fun likeSong(s: SongEntity) {
        syncScope.launch { syncChannel.send(SyncOperation.LikeSong(s)) }
    }

    fun subscribeChannel(channelId: String, subscribe: Boolean) {
        syncScope.launch { syncChannel.send(SyncOperation.SubscribeChannel(channelId, subscribe)) }
    }

    fun savePodcast(podcastId: String, save: Boolean) {
        syncScope.launch { syncChannel.send(SyncOperation.SavePodcast(podcastId, save)) }
    }

    fun saveEpisode(episodeId: String, save: Boolean, setVideoId: String? = null) {
        syncScope.launch { syncChannel.send(SyncOperation.SaveEpisode(episodeId, save, setVideoId)) }
    }

    fun syncLikedSongs() {
        syncScope.launch { syncChannel.send(SyncOperation.LikedSongs) }
    }

    fun syncLibrarySongs() {
        syncScope.launch { syncChannel.send(SyncOperation.LibrarySongs) }
    }

    fun syncUploadedSongs() {
        syncScope.launch { syncChannel.send(SyncOperation.UploadedSongs) }
    }

    fun syncLikedAlbums() {
        syncScope.launch { syncChannel.send(SyncOperation.LikedAlbums) }
    }

    fun syncUploadedAlbums() {
        syncScope.launch { syncChannel.send(SyncOperation.UploadedAlbums) }
    }

    fun syncArtistsSubscriptions() {
        syncScope.launch { syncChannel.send(SyncOperation.ArtistsSubscriptions) }
    }

    fun syncSavedPlaylists() {
        syncScope.launch { syncChannel.send(SyncOperation.SavedPlaylists) }
    }

    fun syncAutoSyncPlaylists() {
        syncScope.launch { syncChannel.send(SyncOperation.AutoSyncPlaylists) }
    }

    fun syncAllAlbums() {
        syncScope.launch {
            syncChannel.send(SyncOperation.LikedAlbums)
            syncChannel.send(SyncOperation.UploadedAlbums)
        }
    }

    fun syncAllArtists() {
        syncScope.launch { syncChannel.send(SyncOperation.ArtistsSubscriptions) }
    }

    fun syncPodcastSubscriptions() {
        syncScope.launch { syncChannel.send(SyncOperation.PodcastSubscriptions) }
    }

    fun syncEpisodesForLater() {
        syncScope.launch { syncChannel.send(SyncOperation.EpisodesForLater) }
    }

    fun cleanupDuplicatePlaylists() {
        syncScope.launch { syncChannel.send(SyncOperation.CleanupDuplicates) }
    }

    fun clearAllSyncedContent() {
        syncScope.launch { syncChannel.send(SyncOperation.ClearAllSynced) }
    }

    fun clearPodcastData() {
        syncScope.launch { syncChannel.send(SyncOperation.ClearPodcastData) }
    }

    // Suspend versions

    suspend fun syncLikedSongsSuspend() = executeSyncLikedSongs()
    suspend fun syncLibrarySongsSuspend() = executeSyncLibrarySongs()
    suspend fun syncUploadedSongsSuspend() = executeSyncUploadedSongs()
    suspend fun syncLikedAlbumsSuspend() = executeSyncLikedAlbums()
    suspend fun syncUploadedAlbumsSuspend() = executeSyncUploadedAlbums()
    suspend fun syncArtistsSubscriptionsSuspend() = executeSyncArtistsSubscriptions()
    suspend fun syncPodcastSubscriptionsSuspend() = executeSyncPodcastSubscriptions()
    suspend fun syncEpisodesForLaterSuspend() = executeSyncEpisodesForLater()
    suspend fun syncSavedPlaylistsSuspend() = executeSyncSavedPlaylists()
    suspend fun syncAutoSyncPlaylistsSuspend() = executeSyncAutoSyncPlaylists()
    suspend fun cleanupDuplicatePlaylistsSuspend() = executeCleanupDuplicatePlaylists()
    suspend fun clearAllSyncedContentSuspend() = executeClearAllSyncedContent()

    suspend fun clearAllLibraryData() = withContext(Dispatchers.IO) {
        try {
            executeClearPodcastData()
            databaseDao.clearHistory()
            databaseDao.clearSearchHistory()

            val allTables = getAllUserTables()
            val skipTables = setOf("android_metadata", "room_master_table", "sqlite_sequence", "search_history", "history")
            val mappingTables = listOf("playlist_song_map", "song_album_map", "song_artist_map", "album_artist_map", "related_song_map")

            for (table in mappingTables) {
                if (table in allTables) safeDeleteTable(table)
            }
            for (table in allTables) {
                if (table !in skipTables && table !in mappingTables && table != "song") safeDeleteTable(table)
            }
            if ("song" in allTables) safeRawQuery("DELETE FROM song WHERE dateDownload IS NULL")

        } catch (e: Exception) {
            Log.e(TAG, "Error clearing library data", e)
            throw e
        }
    }

    private fun getAllUserTables(): List<String> {
        val tables = mutableListOf<String>()
        try {
            database.openHelper.writableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
            ).use { cursor ->
                while (cursor.moveToNext()) tables.add(cursor.getString(0))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting table list", e)
        }
        return tables
    }

    private fun safeDeleteTable(tableName: String) {
        try {
            database.openHelper.writableDatabase.execSQL("DELETE FROM $tableName")
        } catch (e: Exception) {
            Log.w(TAG, "Table $tableName error: ${e.message}")
        }
    }

    private fun safeRawQuery(query: String) {
        try {
            database.openHelper.writableDatabase.execSQL(query)
        } catch (e: Exception) {
            Log.w(TAG, "Query failed: $query - ${e.message}")
        }
    }

    // Private execution methods

    private suspend fun executeFullSync() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(overallStatus = SyncStatus.Syncing, currentOperation = "Starting full sync") }
        try {
            executeSyncLikedSongs()
            delayMs(DB_OPERATION_DELAY_MS)
            executeSyncLibrarySongs()
            delayMs(DB_OPERATION_DELAY_MS)
            executeSyncUploadedSongs()
            delayMs(DB_OPERATION_DELAY_MS)
            executeSyncLikedAlbums()
            delayMs(DB_OPERATION_DELAY_MS)
            executeSyncUploadedAlbums()
            delayMs(DB_OPERATION_DELAY_MS)
            executeSyncArtistsSubscriptions()
            delayMs(DB_OPERATION_DELAY_MS)
            executeSyncPodcastSubscriptions()
            delayMs(DB_OPERATION_DELAY_MS)
            executeSyncEpisodesForLater()
            delayMs(DB_OPERATION_DELAY_MS)
            executeSyncSavedPlaylists()
            delayMs(DB_OPERATION_DELAY_MS)
            executeSyncAutoSyncPlaylists()
            updateState { copy(overallStatus = SyncStatus.Completed, currentOperation = "") }
        } catch (e: Exception) {
            Log.e(TAG, "Error during full sync", e)
            updateState { copy(overallStatus = SyncStatus.Error(e.message ?: "Unknown error"), currentOperation = "") }
        }
    }

    private suspend fun executeLikeSong(s: SongEntity) = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        withRetry { YouTube.likeVideo(s.id, s.liked) }
    }

    private suspend fun executeSubscribeChannel(channelId: String, subscribe: Boolean) = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        withRetry { YouTube.subscribeChannel(channelId, subscribe) }
    }

    private suspend fun executeSavePodcast(podcastId: String, save: Boolean) = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        withRetry { YouTube.savePodcast(podcastId, save) }
    }

    private suspend fun executeSaveEpisode(episodeId: String, save: Boolean, setVideoId: String?) = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        if (save) {
            withRetry { YouTube.addEpisodeToSavedEpisodes(episodeId) }
        } else if (setVideoId != null) {
            withRetry { YouTube.removeEpisodeFromSavedEpisodes(episodeId, setVideoId) }
        }
    }

    private suspend fun executeSyncLikedSongs() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(likedSongs = SyncStatus.Syncing, currentOperation = "Syncing liked songs") }
        withRetry { YouTube.playlist("LM").completed() }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteIds = page.songs.map { it.id }.toSet()
                    val localSongs = databaseDao.likedSongsByNameAsc().first()
                    localSongs.filterNot { it.id in remoteIds }.forEach {
                        databaseDao.update(it.song.localToggleLike())
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    val now = LocalDateTime.now()
                    page.songs.forEachIndexed { index, song ->
                        val dbSong = databaseDao.getSongById(song.id).firstOrNull()
                        val timestamp = now.minusSeconds(index.toLong())
                        databaseDao.runInTransaction {
                            if (dbSong == null) {
                                databaseDao.insert(song.toMediaMetadata()) {
                                    it.copy(liked = true, likedDate = timestamp, isVideo = song.isVideoSong)
                                }
                            } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp) {
                                databaseDao.update(dbSong.song.copy(liked = true, likedDate = timestamp, isVideo = song.isVideoSong))
                            }
                        }
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    updateState { copy(likedSongs = SyncStatus.Completed) }
                } catch (e: Exception) {
                    updateState { copy(likedSongs = SyncStatus.Error(e.message ?: "Error")) }
                }
            }
        }
    }

    private suspend fun executeSyncLibrarySongs() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(librarySongs = SyncStatus.Syncing, currentOperation = "Syncing library songs") }
        withRetry { YouTube.library("FEmusic_liked_videos").completed() }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                    val remoteIds = remoteSongs.map { it.id }.toSet()
                    val localSongs = databaseDao.songsByNameAsc().first()
                    localSongs.filterNot { it.id in remoteIds }.forEach {
                        databaseDao.update(it.song.toggleLibrary())
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    remoteSongs.forEach { song ->
                        val dbSong = databaseDao.getSongById(song.id).firstOrNull()
                        databaseDao.runInTransaction {
                            if (dbSong == null) {
                                databaseDao.insert(song.toMediaMetadata()) { it.toggleLibrary() }
                            } else if (dbSong.song.inLibrary == null) {
                                databaseDao.update(dbSong.song.toggleLibrary())
                            }
                        }
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    updateState { copy(librarySongs = SyncStatus.Completed) }
                } catch (e: Exception) {
                    updateState { copy(librarySongs = SyncStatus.Error(e.message ?: "Error")) }
                }
            }
        }
    }

    private suspend fun executeSyncUploadedSongs() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(uploadedSongs = SyncStatus.Syncing, currentOperation = "Syncing uploaded songs") }
        withRetry { YouTube.library("FEmusic_library_privately_owned_tracks", tabIndex = 1).completed() }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                    val remoteIds = remoteSongs.map { it.id }.toSet()
                    val localSongs = databaseDao.uploadedSongsByNameAsc().first()
                    localSongs.filterNot { it.id in remoteIds }.forEach {
                        databaseDao.update(it.song.toggleUploaded())
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    remoteSongs.forEach { song ->
                        val dbSong = databaseDao.getSongById(song.id).firstOrNull()
                        databaseDao.runInTransaction {
                            if (dbSong == null) {
                                databaseDao.insert(song.toMediaMetadata()) { it.toggleUploaded() }
                            } else if (!dbSong.song.isUploaded) {
                                databaseDao.update(dbSong.song.copy(isUploaded = true, uploadEntityId = song.uploadEntityId))
                            }
                        }
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    updateState { copy(uploadedSongs = SyncStatus.Completed) }
                } catch (e: Exception) {
                    updateState { copy(uploadedSongs = SyncStatus.Error(e.message ?: "Error")) }
                }
            }
        }
    }

    private suspend fun executeSyncLikedAlbums() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(likedAlbums = SyncStatus.Syncing, currentOperation = "Syncing liked albums") }
        withRetry { YouTube.library("FEmusic_liked_albums").completed() }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                    val remoteIds = remoteAlbums.map { it.id }.toSet()
                    val localAlbums = databaseDao.albumsLikedByNameAsc().first()
                    localAlbums.filterNot { it.id in remoteIds }.forEach {
                        databaseDao.update(it.album.localToggleLike())
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    remoteAlbums.forEach { album ->
                        val dbAlbum = databaseDao.getAlbumById(album.id).firstOrNull()
                        YouTube.album(album.browseId).onSuccess { albumPage ->
                            if (dbAlbum == null) {
                                databaseDao.insert(albumPage)
                                databaseDao.getAlbumById(album.id).firstOrNull()?.let {
                                    databaseDao.update(it.album.localToggleLike())
                                }
                            } else if (dbAlbum.album.bookmarkedAt == null) {
                                databaseDao.update(dbAlbum.album.localToggleLike())
                            }
                        }
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    updateState { copy(likedAlbums = SyncStatus.Completed) }
                } catch (e: Exception) {
                    updateState { copy(likedAlbums = SyncStatus.Error(e.message ?: "Error")) }
                }
            }
        }
    }

    private suspend fun executeSyncUploadedAlbums() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(uploadedAlbums = SyncStatus.Syncing, currentOperation = "Syncing uploaded albums") }
        withRetry { YouTube.library("FEmusic_library_privately_owned_releases").completed() }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                    val remoteIds = remoteAlbums.map { it.id }.toSet()
                    val localAlbums = databaseDao.albumsUploadedByNameAsc().first()
                    localAlbums.filterNot { it.id in remoteIds }.forEach {
                        databaseDao.update(it.album.toggleUploaded())
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    remoteAlbums.forEach { album ->
                        val dbAlbum = databaseDao.getAlbumById(album.id).firstOrNull()
                        YouTube.album(album.browseId).onSuccess { albumPage ->
                            if (dbAlbum == null) {
                                databaseDao.insert(albumPage)
                                databaseDao.getAlbumById(album.id).firstOrNull()?.let {
                                    databaseDao.update(it.album.toggleUploaded())
                                }
                            } else if (!dbAlbum.album.isUploaded) {
                                databaseDao.update(dbAlbum.album.toggleUploaded())
                            }
                        }
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    updateState { copy(uploadedAlbums = SyncStatus.Completed) }
                } catch (e: Exception) {
                    updateState { copy(uploadedAlbums = SyncStatus.Error(e.message ?: "Error")) }
                }
            }
        }
    }

    private suspend fun executeSyncArtistsSubscriptions() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(artists = SyncStatus.Syncing, currentOperation = "Syncing artist subscriptions") }
        withRetry { YouTube.library("FEmusic_library_corpus_artists").completed() }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteArtists = page.items.filterIsInstance<ArtistItem>()
                    val remoteIds = remoteArtists.map { it.id }.toSet()
                    val localArtists = databaseDao.artistsBookmarkedByNameAsc().first()
                    localArtists.filterNot { it.id in remoteIds }.forEach {
                        databaseDao.update(it.artist.localToggleLike())
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    remoteArtists.forEach { artist ->
                        val dbArtist = databaseDao.getArtistById(artist.id).firstOrNull()
                        databaseDao.runInTransaction {
                            if (dbArtist == null) {
                                databaseDao.upsert(ArtistEntity(id = artist.id, name = artist.title, thumbnailUrl = artist.thumbnail, bookmarkedAt = LocalDateTime.now()))
                            } else if (dbArtist.artist.bookmarkedAt == null) {
                                databaseDao.update(dbArtist.artist.copy(bookmarkedAt = LocalDateTime.now()))
                            }
                        }
                        delayMs(DB_OPERATION_DELAY_MS)
                    }
                    updateState { copy(artists = SyncStatus.Completed) }
                } catch (e: Exception) {
                    updateState { copy(artists = SyncStatus.Error(e.message ?: "Error")) }
                }
            }
        }
    }

    private suspend fun executeSyncPodcastSubscriptions() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(currentOperation = "Syncing podcast subscriptions") }
        withRetry { YouTube.savedPodcastShows() }.onSuccess { result ->
            result.onSuccess { remotePodcasts ->
                remotePodcasts.forEach { podcast ->
                    val dbPodcast = databaseDao.getPodcastById(podcast.id).firstOrNull()
                    databaseDao.runInTransaction {
                        if (dbPodcast == null) {
                            databaseDao.upsert(PodcastEntity(id = podcast.id, title = podcast.title, author = podcast.author?.name, thumbnailUrl = podcast.thumbnail, bookmarkedAt = LocalDateTime.now()))
                        } else if (dbPodcast.bookmarkedAt != null) {
                            databaseDao.update(dbPodcast.copy(title = podcast.title, author = podcast.author?.name, thumbnailUrl = podcast.thumbnail))
                        }
                    }
                    delayMs(DB_OPERATION_DELAY_MS)
                }
            }
        }
    }

    private suspend fun executeSyncEpisodesForLater() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(currentOperation = "Syncing episodes for later") }
        withRetry { YouTube.episodesForLater() }.onSuccess { result ->
            result.onSuccess { page ->
                val remoteIds = page.songs.map { it.id }.toSet()
                val localEpisodes = databaseDao.podcastEpisodesByCreateDateAsc().first().filter { it.song.inLibrary != null }
                page.songs.forEach { episode ->
                    val dbSong = databaseDao.getSongById(episode.id).firstOrNull()
                    databaseDao.runInTransaction {
                        if (dbSong == null) {
                            databaseDao.upsert(episode.toMediaMetadata().toSongEntity().copy(inLibrary = LocalDateTime.now(), isEpisode = true))
                        } else if (!dbSong.song.isEpisode || dbSong.song.inLibrary == null) {
                            databaseDao.update(dbSong.song.copy(isEpisode = true, inLibrary = LocalDateTime.now()))
                        }
                        episode.setVideoId?.let { svid -> databaseDao.upsert(SetVideoIdEntity(videoId = episode.id, setVideoId = svid)) }
                    }
                    delayMs(DB_OPERATION_DELAY_MS)
                }
                localEpisodes.filterNot { it.id in remoteIds }.forEach {
                    databaseDao.update(it.song.copy(inLibrary = null))
                }
            }
        }
    }

    private suspend fun executeSyncSavedPlaylists() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        updateState { copy(playlists = SyncStatus.Syncing, currentOperation = "Syncing saved playlists") }
        withRetry { YouTube.library("FEmusic_liked_playlists").completed() }.onSuccess { result ->
            result.onSuccess { page ->
                val remotePlaylists = page.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "LM" || it.id == "SE" }.reversed()
                val remoteIds = remotePlaylists.map { it.id }.toSet()
                val localPlaylists = databaseDao.playlistsByNameAsc().first()
                localPlaylists.filterNot { it.playlist.browseId in remoteIds || it.playlist.browseId == null }.forEach {
                    databaseDao.update(it.playlist.localToggleLike())
                    delayMs(DB_OPERATION_DELAY_MS)
                }
                for (playlist in remotePlaylists) {
                    var entity = localPlaylists.find { it.playlist.browseId == playlist.id }?.playlist
                    if (entity == null) {
                        entity = PlaylistEntity(name = playlist.title, browseId = playlist.id, thumbnailUrl = playlist.thumbnail, isEditable = playlist.isEditable, bookmarkedAt = LocalDateTime.now())
                        databaseDao.upsert(entity)
                    } else {
                        databaseDao.update(entity, playlist)
                    }
                    if (!isPlaylistBeingModified(entity.id)) executeSyncPlaylist(playlist.id, entity.id)
                    delayMs(DB_OPERATION_DELAY_MS)
                }
                updateState { copy(playlists = SyncStatus.Completed) }
            }
        }
    }

    private suspend fun executeSyncAutoSyncPlaylists() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) return@withContext
        val autoSyncPlaylists = databaseDao.playlistsByNameAsc().first().filter { it.playlist.isAutoSync && it.playlist.browseId != null }
        autoSyncPlaylists.forEach {
            if (!isPlaylistBeingModified(it.playlist.id)) {
                executeSyncPlaylist(it.playlist.browseId!!, it.playlist.id)
                delayMs(DB_OPERATION_DELAY_MS)
            }
        }
    }

    private suspend fun executeSyncPlaylist(browseId: String, playlistId: String) = withContext(Dispatchers.IO) {
        withRetry { YouTube.playlist(browseId).completed() }.onSuccess { result ->
            result.onSuccess { page ->
                val songs = page.songs
                if (songs.isEmpty()) return@onSuccess
                val remoteIds = songs.map { it.id }
                val localSongs = databaseDao.playlistSongs(playlistId).first()
                if (remoteIds == localSongs.map { it.song.id }) return@onSuccess
                val downloadedIds = localSongs.filter { it.song.isDownloaded }.map { it.song.id }.toSet()
                databaseDao.runInTransaction {
                    databaseDao.clearPlaylist(playlistId)
                    songs.forEach { if (databaseDao.getSongByIdBlocking(it.id) == null) databaseDao.upsert(it.toMediaMetadata().toSongEntity()) }
                    downloadedIds.filterNot { it in remoteIds }.forEach { id ->
                        databaseDao.getSongByIdBlocking(id)?.let {
                            val maxPos = databaseDao.playlistSongsBlocking(playlistId).maxOfOrNull { it.map.position } ?: -1
                            databaseDao.upsert(PlaylistSongMap(playlistId = playlistId, songId = id, position = maxPos + 1))
                        }
                    }
                    databaseDao.playlistBlocking(playlistId)?.let { databaseDao.addSongsToPlaylist(it, songs.map { s -> s.id to s.setVideoId }) }
                }
            }
        }
    }

    private suspend fun executeCleanupDuplicatePlaylists() = withContext(Dispatchers.IO) {
        val allPlaylists = databaseDao.playlistsByNameAsc().first()
        allPlaylists.filter { it.playlist.browseId != null }.groupBy { it.playlist.browseId }.forEach { (_, list) ->
            if (list.size > 1) {
                val keep = list.maxByOrNull { it.songCount } ?: list.first()
                list.filter { it.id != keep.id }.forEach {
                    databaseDao.clearPlaylist(it.id)
                    databaseDao.delete(it.playlist)
                }
            }
        }
    }

    private suspend fun executeClearAllSyncedContent() = withContext(Dispatchers.IO) {
        updateState { copy(overallStatus = SyncStatus.Syncing, currentOperation = "Clearing synced content") }
        try {
            databaseDao.runInTransaction {
                databaseDao.likedSongsByNameAsc().first().forEach { databaseDao.update(it.song.copy(liked = false, likedDate = null)) }
                databaseDao.songsByNameAsc().first().forEach { if (it.song.inLibrary != null) databaseDao.update(it.song.copy(inLibrary = null)) }
                databaseDao.albumsLikedByNameAsc().first().forEach { databaseDao.update(it.album.copy(bookmarkedAt = null)) }
                databaseDao.artistsBookmarkedByNameAsc().first().forEach { databaseDao.update(it.artist.copy(bookmarkedAt = null)) }
                databaseDao.playlistsByNameAsc().first().forEach { if (it.playlist.browseId != null) { databaseDao.clearPlaylist(it.id); databaseDao.delete(it.playlist) } }
            }
            context.dataStore.edit { it[PreferenceConstants.LAST_FULL_SYNC] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) }
            updateState { copy(overallStatus = SyncStatus.Completed) }
        } catch (e: Exception) {
            updateState { copy(overallStatus = SyncStatus.Error(e.message ?: "Error")) }
        }
    }

    private suspend fun executeClearPodcastData() = withContext(Dispatchers.IO) {
        updateState { copy(overallStatus = SyncStatus.Syncing, currentOperation = "Clearing podcast data") }
        try {
            databaseDao.runInTransaction {
                databaseDao.subscribedPodcasts().first().forEach { databaseDao.update(it.copy(bookmarkedAt = null)) }
                databaseDao.podcastEpisodesByCreateDateAsc().first().filter { it.song.inLibrary != null }.forEach { databaseDao.update(it.song.copy(inLibrary = null)) }
            }
            updateState { copy(overallStatus = SyncStatus.Completed) }
        } catch (e: Exception) {
            updateState { copy(overallStatus = SyncStatus.Error(e.message ?: "Error")) }
        }
    }

    suspend fun removeFromPlaylistAndAwaitSync(browseId: String, songId: String, setVideoId: String, playlistId: String) {
        if (pendingYouTubeAdds[browseId]?.contains(songId) == true) {
            pendingRemovals.getOrPut(browseId) { ConcurrentHashMap.newKeySet() }.add(Triple(songId, setVideoId, playlistId))
            return
        }
        markPlaylistModifying(playlistId)
        try {
            withContext(Dispatchers.IO) {
                YouTube.removeFromPlaylist(browseId, songId, setVideoId)
                repeat(10) { delayMs(3000L); if (YouTube.playlist(browseId).completed().getOrNull()?.songs?.none { it.id == songId } == true) return@withContext }
            }
        } finally { unmarkPlaylistModifying(playlistId) }
    }

    fun registerPendingAdd(browseId: String, songId: String) {
        pendingYouTubeAdds.getOrPut(browseId) { ConcurrentHashMap.newKeySet() }.add(songId)
    }

    fun unregisterPendingAdd(browseId: String, songId: String) {
        syncScope.launch {
            var deferred: Triple<String, String, String>? = null
            withContext(Dispatchers.IO) {
                repeat(10) { delayMs(3000L); if (YouTube.playlist(browseId).completed().getOrNull()?.songs?.any { it.id == songId } == true) return@repeat }
                pendingYouTubeAdds[browseId]?.remove(songId)
                deferred = pendingRemovals[browseId]?.find { it.first == songId }?.also { pendingRemovals[browseId]?.remove(it) }
            }
            deferred?.let { removeFromPlaylistAndAwaitSync(browseId, it.first, it.second, it.third) }
        }
    }

    fun scheduleRemoveFromPlaylist(browseId: String, songId: String, playlistId: String, getSetVideoId: suspend () -> String?) {
        markPlaylistModifying(playlistId)
        syncScope.launch {
            try {
                val setVideoId = getSetVideoId() ?: YouTube.playlist(browseId).completed().getOrNull()?.songs?.find { it.id == songId }?.setVideoId
                if (setVideoId != null) removeFromPlaylistAndAwaitSync(browseId, songId, setVideoId, playlistId)
            } finally { unmarkPlaylistModifying(playlistId) }
        }
    }

    fun cancelAllSyncs() {
        processingJob?.cancel()
        startProcessingQueue()
        updateState { SyncState() }
    }
}











