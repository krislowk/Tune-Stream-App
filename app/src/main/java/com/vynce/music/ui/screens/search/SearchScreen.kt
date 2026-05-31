package com.vynce.music.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.vynce.music.ui.components.ListItem
import com.vynce.music.ui.components.MoreOptionsSheet
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.shareText
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.YouTube
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.YTItem

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onItemClick: (String, String?) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var active by remember { mutableStateOf(true) }
    
    var showMoreOptions by remember { mutableStateOf(false) }
    var selectedItemForOptions by remember { mutableStateOf<YTItem?>(null) }

    val colors = VynceTheme.colors

    val filters = listOf(
        "Songs" to YouTube.SearchFilter.FILTER_SONG,
        "Videos" to YouTube.SearchFilter.FILTER_VIDEO,
        "Albums" to YouTube.SearchFilter.FILTER_ALBUM,
        "Artists" to YouTube.SearchFilter.FILTER_ARTIST,
        "Featured Playlists" to YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST,
        "Community Playlists" to YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST
    )

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(colors.background)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    val searchBarColors = SearchBarDefaults.colors(
                        containerColor = colors.surface,
                    )
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = query,
                                onQueryChange = { newQuery -> viewModel.updateQuery(newQuery) },
                                onSearch = { searchQuery ->
                                    viewModel.search(searchQuery, selectedFilter)
                                    active = false
                                    focusManager.clearFocus()
                                },
                                expanded = active,
                                onExpandedChange = { isExpanded -> active = isExpanded },
                                enabled = true,
                                placeholder = { Text("Songs, artists, albums", color = colors.textSecondary) },
                                leadingIcon = {
                                    IconButton(onClick = { if (active && query.isEmpty()) onBackClick() else if (active) active = false else onBackClick() }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = colors.textPrimary
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateQuery("") }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = colors.textPrimary
                                            )
                                        }
                                    }
                                },
                                colors = searchBarColors.inputFieldColors,
                            )
                        },
                        expanded = active,
                        onExpandedChange = { isExpanded -> active = isExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (active) 0.dp else 16.dp),
                        colors = searchBarColors,
                        content = {
                            // Suggestions List
                            suggestions?.queries?.let { queries ->
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(queries) { suggestion ->
                                        SuggestionItem(
                                            suggestion = suggestion,
                                            onClick = {
                                                viewModel.updateQuery(suggestion)
                                                viewModel.search(suggestion, selectedFilter)
                                                active = false
                                                focusManager.clearFocus()
                                            }
                                        )
                                    }
                                }
                            }
                        },
                    )
                }

                if (!active && query.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filters) { (label, filter) ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = colors.onPrimary,
                                    containerColor = colors.surface,
                                    labelColor = colors.textSecondary
                                ),
                                border = null
                            )
                        }
                    }
                }
            }
        },
        containerColor = colors.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary
                )
            } else {
                val result = searchResult
                if (result != null && !active) {
                    // Show Results
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        itemsIndexed(
                            items = result.items,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            if (index >= result.items.size - 1) {
                                LaunchedEffect(result.items.size) {
                                    viewModel.loadMore()
                                }
                            }

                            ListItem(
                                item = item,
                                onClick = {
                                    when (item) {
                                        is SongItem -> playerViewModel.play(item.toMediaItem())
                                        is AlbumItem -> onItemClick("album", item.browseId)
                                        is ArtistItem -> onItemClick("artist", item.id)
                                        is PlaylistItem -> onItemClick("playlist", item.id)
                                        else -> {}
                                    }
                                },
                                onMoreClick = if (item is SongItem) {
                                    {
                                        selectedItemForOptions = item
                                        showMoreOptions = true
                                    }
                                } else null,
                                onSwipeRight = {
                                    if (item is SongItem) {
                                        playerViewModel.addToQueue(item.toMediaItem())
                                    }
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = colors.glassBorder.copy(alpha = 0.1f)
                            )
                        }

                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = colors.primary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                } else if (!active && query.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Search for your favorite music", color = colors.textSecondary)
                    }
                }
            }

            if (showMoreOptions && selectedItemForOptions is SongItem) {
                val item = selectedItemForOptions as SongItem
                val metadata = item.toMediaItem().mediaMetadata
                val albumId = metadata.extras?.getString("album_id")
                val artistId = metadata.extras?.getString("artist_id")

                MoreOptionsSheet(
                    title = item.title,
                    subtitle = item.artists.joinToString { it.name },
                    thumbnailUrl = item.thumbnail,
                    onDismiss = { showMoreOptions = false },
                    onAddToPlaylist = { 
                        Toast.makeText(context, "Added to playlist (Simulated)", Toast.LENGTH_SHORT).show()
                    },
                    onViewAlbum = albumId?.let { id -> { onItemClick("album", id) } },
                    onGoToArtist = artistId?.let { id -> { onItemClick("artist", id) } },
                    onShare = { 
                        shareText(context, item.shareLink)
                    }
                )
            }
        }
    }
}

@Composable
fun SuggestionItem(suggestion: String, onClick: () -> Unit) {
    val colors = VynceTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            suggestion,
            color = colors.textPrimary,
            style = VynceTheme.typography.body
        )
    }
}












