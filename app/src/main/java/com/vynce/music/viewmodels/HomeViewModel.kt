package com.vynce.music.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.db.entities.LocalItem
import com.vynce.music.extensions.toEnum
import com.vynce.music.models.SimilarRecommendation
import com.vynce.music.models.Song
import com.vynce.music.models.User
import com.vynce.music.provider.YoutubeProvider
import com.vynce.music.repository.SongRepository
import com.vynce.music.repository.UserRepository
import com.vynce.music.repository.constants.PreferenceConstants
import com.vynce.music.repository.constants.PreferenceConstants.HOME_PAGE_CACHE
import com.vynce.music.repository.constants.QuickPicks
import com.vynce.music.utils.dataStore
import com.vynce.vynceclient.models.BrowseEndpoint
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.YTItem
import com.vynce.vynceclient.pages.ExplorePage
import com.vynce.vynceclient.pages.HomePage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

fun SongItem.toSongModel() = Song(
    mediaId = id,
    title = title,
    artist = artists.joinToString { it.name },
    album = album?.name,
    duration = (duration ?: 0).toLong(),
    durationMs = (duration ?: 0) * 1000L,
    thumbnail = thumbnail,
    isYoutube = true
)

data class DailyDiscoverItem(
    val seed: Song,
    val recommendation: YTItem,
    val relatedEndpoint: BrowseEndpoint?
)

data class CommunityPlaylistItem(
    val playlist: PlaylistItem,
    val songs: List<SongItem>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    userRepository: UserRepository,
    songRepository: SongRepository,
    private val youtubeProvider: YoutubeProvider
) : ViewModel() {

    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val isRandomizing = MutableStateFlow(false)
    private val quickPicksEnum = context.dataStore.data.map {
        it[PreferenceConstants.QUICK_PICKS].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val dailyDiscover = MutableStateFlow<List<DailyDiscoverItem>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = userRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentlyPlayed: StateFlow<List<Song>> = songRepository.getRecentlyPlayed(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentParams: String? = null

    init {
        fetchHome(null)
        fetchExplore()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            fetchHomeSuspend(currentParams)
            isRefreshing.value = false
        }
    }

    fun onFilterSelected(filter: HomePage.Chip?) {
        if (filter == null || selectedChip.value == filter) {
            selectedChip.value = null
            previousHomePage.value?.let { 
                homePage.value = it
                _uiState.value = HomeUiState.Success(it)
                return
            }
            fetchHome(null)
        } else {
            if (selectedChip.value == null) {
                previousHomePage.value = homePage.value
            }
            selectedChip.value = filter
            fetchHome(filter.endpoint?.params)
        }
    }

    fun fetchHome(params: String? = null) {
        viewModelScope.launch {
            fetchHomeSuspend(params)
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success && currentState.data.continuation != null && !currentState.isLoadingMore) {
            viewModelScope.launch {
                _uiState.value = currentState.copy(isLoadingMore = true)
                youtubeProvider.getHome(continuation = currentState.data.continuation)
                    .catch { 
                        it.printStackTrace()
                        _uiState.value = currentState.copy(isLoadingMore = false)
                    }
                    .collect { page ->
                        val mergedSections = currentState.data.sections + page.sections
                        val newPage = page.copy(sections = mergedSections, chips = currentState.data.chips)
                        homePage.value = newPage
                        _uiState.value = HomeUiState.Success(newPage)
                        mapSections(newPage)
                    }
            }
        }
    }

    fun fetchExplore() {
        viewModelScope.launch {
            youtubeProvider.getExplore()
                .catch { it.printStackTrace() }
                .collect { explorePage.value = it }
        }
    }

    private suspend fun fetchHomeSuspend(params: String? = null) {
        currentParams = params
        val currentState = _uiState.value
        
        // Try to load from cache if we are not already showing success data
        if (currentState !is HomeUiState.Success && params == null) {
            viewModelScope.launch {
                context.dataStore.data.map { it[HOME_PAGE_CACHE] }.firstOrNull()?.let { cache ->
                    try {
                        val cachedPage = json.decodeFromString<HomePage>(cache)
                        if (_uiState.value !is HomeUiState.Success) {
                            _uiState.value = HomeUiState.Success(cachedPage)
                            mapSections(cachedPage)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        if (currentState is HomeUiState.Success && params == currentParams) {
            _uiState.value = currentState.copy(isRefreshing = true)
        } else if (_uiState.value !is HomeUiState.Success) {
            _uiState.value = HomeUiState.Loading
            isLoading.value = true
        }
        
        youtubeProvider.getHome(params)
            .catch { e ->
                e.printStackTrace()
                if (_uiState.value !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
                }
                isLoading.value = false
                isRefreshing.value = false
            }
            .collect { page: HomePage ->
                _uiState.value = HomeUiState.Success(page)
                homePage.value = page
                isLoading.value = false
                isRefreshing.value = false
                mapSections(page)
                
                // Save to cache only for the main home page (no params)
                if (params == null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val cache = json.encodeToString(HomePage.serializer(), page)
                            context.dataStore.edit { it[HOME_PAGE_CACHE] = cache }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    private fun mapSections(page: HomePage) {
        val communityList = mutableListOf<CommunityPlaylistItem>()
        val similarList = mutableListOf<SimilarRecommendation>()
        val dailyList = mutableListOf<DailyDiscoverItem>()
        
        page.sections.forEach { section ->
            val title = section.title?.lowercase() ?: ""
            when {
                title.contains("quick picks") || title.contains("trending") -> {
                    quickPicks.value = section.items.filterIsInstance<SongItem>().map { it.toSongModel() }
                }
                title.contains("forgotten favorites") -> {
                    forgottenFavorites.value = section.items.filterIsInstance<SongItem>().map { it.toSongModel() }
                }
                title.contains("community playlists") -> {
                    section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                        communityList.add(
                            CommunityPlaylistItem(
                                playlist = playlist,
                                songs = section.items.filterIsInstance<SongItem>()
                            )
                        )
                    }
                }
                title.contains("listen again") || title.contains("keep listening") -> {
                    // keepListening.value = ...
                }
            }
        }
        
        communityPlaylists.value = communityList.distinctBy { it.playlist.id }
        similarRecommendations.value = similarList
        dailyDiscover.value = dailyList
        
        accountPlaylists.value = page.sections
            .flatMap { s -> s.items }
            .filterIsInstance<PlaylistItem>()
    }
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val data: HomePage,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}














