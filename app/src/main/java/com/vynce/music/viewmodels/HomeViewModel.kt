package com.vynce.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val songRepository: SongRepository,
    private val youtubeProvider: YoutubeProvider
) : ViewModel() {

    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val isRandomizing = MutableStateFlow(false)
    private val quickPicksEnum = context.dataStore.data.map {
        it[PreferenceConstants.QUICK_PICKS].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

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

    var refreshing by mutableStateOf(false)
        private set
    private var currentParams: String? = null

    fun refresh() {
        viewModelScope.launch {
            refreshing = true
            fetchHomeSuspend(currentParams)
            refreshing = false
        }
    }

    init {
        fetchHome(null)
    }

    fun onFilterSelected(filter: HomePage.Chip) {
        fetchHome(filter.endpoint?.params)
    }

    fun fetchHome(params: String? = null) {
        viewModelScope.launch {
            fetchHomeSuspend(params)
        }
    }

    private suspend fun fetchHomeSuspend(params: String? = null) {
        currentParams = params
        if (_uiState.value !is HomeUiState.Success) {
            _uiState.value = HomeUiState.Loading
        }
        
        youtubeProvider.getHome(params)
            .catch { e ->
                e.printStackTrace()
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
            .collect { page: HomePage ->
                _uiState.value = HomeUiState.Success(page)
                homePage.value = page
                
                // Map sections to specific flows
                page.sections.forEach { section ->
                    when (section.title?.lowercase()) {
                        "quick picks" -> {
                            quickPicks.value = section.items.filterIsInstance<SongItem>().map { it.toSongModel() }
                        }
                        "forgotten favorites" -> {
                            forgottenFavorites.value = section.items.filterIsInstance<SongItem>().map { it.toSongModel() }
                        }
                    }
                }
                
                accountPlaylists.value = page.sections
                    .flatMap { s -> s.items }
                    .filterIsInstance<PlaylistItem>()
            }
    }
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val data: HomePage) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}











