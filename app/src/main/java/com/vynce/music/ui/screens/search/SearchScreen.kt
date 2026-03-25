package com.vynce.music.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vynce.music.ui.commponents.CarouselList
import com.vynce.music.ui.commponents.ListItem
import com.vynce.music.ui.commponents.MoreOptionsSheet
import com.vynce.music.ui.commponents.SectionHeader
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.YTItem
import com.vynce.vynceclient.pages.MoodAndGenres

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
    val exploreData by viewModel.exploreData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current
    var active by remember { mutableStateOf(false) }
    
    var showMoreOptions by remember { mutableStateOf(false) }
    var selectedItemForOptions by remember { mutableStateOf<YTItem?>(null) }

    val colors = VynceTheme.colors

    Scaffold(
        topBar = {
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
                                viewModel.search(searchQuery)
                                active = false
                                focusManager.clearFocus()
                            },
                            expanded = active,
                            onExpandedChange = { isExpanded -> active = isExpanded },
                            enabled = true,
                            placeholder = { Text("Songs, artists, albums", color = colors.textSecondary) },
                            leadingIcon = {
                                IconButton(onClick = { if (active) active = false else onBackClick() }) {
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
                                            viewModel.search(suggestion)
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
                        items(result.items) { item ->
                            ListItem(
                                item = item,
                                onClick = {
                                    when (item) {
                                        is SongItem -> playerViewModel.play(item.toMediaItem())
                                        is AlbumItem -> onItemClick("album", item.browseId)
                                        is ArtistItem -> onItemClick("artist", item.id)
                                        is PlaylistItem -> onItemClick("playlist", item.id)
                                    }
                                },
                                onMoreClick = if (item is SongItem) {
                                    {
                                        selectedItemForOptions = item
                                        showMoreOptions = true
                                    }
                                } else null
                            )
                        }
                    }
                } else if (!active) {
                    // Show Explore Content
                    SearchExploreContent(
                        exploreData = exploreData,
                        onItemClick = onItemClick
                    )
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
                    onAddToPlaylist = { /* TODO */ },
                    onViewAlbum = albumId?.let { { onItemClick("album", it) } },
                    onGoToArtist = artistId?.let { { onItemClick("artist", it) } },
                    onShare = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun SearchExploreContent(
    exploreData: com.vynce.vynceclient.pages.ExplorePage?,
    onItemClick: (String, String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        exploreData?.let { data ->
            item {
                CarouselList(
                    title = "New Releases",
                    items = data.newReleaseAlbums,
                    onItemClick = { item -> onItemClick("album", (item as AlbumItem).browseId) }
                )
            }

            item {
                SectionHeader(title = "Moods & Genres")
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(data.moodAndGenres) { mood ->
                        MoodCard(mood = mood)
                    }
                }
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

@Composable
fun MoodCard(mood: MoodAndGenres.Item) {
    val colors = VynceTheme.colors
    val shapes = VynceTheme.shapes
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(90.dp)
            .clip(shapes.medium)
            .background(Color(mood.stripeColor).copy(alpha = 0.15f))
            .clickable { /* TODO */ }
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = mood.title,
            style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = colors.textPrimary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MoodCardPreview() {
    VynceTheme {
        MoodCard(
            mood = MoodAndGenres.Item(
                title = "Chill",
                stripeColor = 0xFF2196F3,
                endpoint = com.vynce.vynceclient.models.BrowseEndpoint(browseId = "1")
            )
        )
    }
}
