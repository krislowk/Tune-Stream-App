package com.vynce.music.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.vynce.music.models.Song
import com.vynce.music.repository.constants.LibraryFilter
import com.vynce.music.ui.components.ListItem
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.models.Artist
import com.vynce.vynceclient.models.SongItem


@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val selectedTab by viewModel.selectedTab.collectAsState()

    val colors = VynceTheme.colors

    val tabs = listOf(
        LibraryFilter.LIKED_SONGS,
        LibraryFilter.PLAYLISTS,
        LibraryFilter.LOCAL_SONGS,
        LibraryFilter.LIKED_ALBUMS,
        LibraryFilter.BOOKMARKED_ARTISTS
    )
    val tabNames = listOf("Liked", "Playlists", "Songs", "Albums", "Artists")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(colors.background)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val searchBarColors = SearchBarDefaults.colors(
                        containerColor = colors.surface,
                    )
                    
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchQuery,
                                onQueryChange = viewModel::onSearchQueryChanged,
                                onSearch = { expanded = false },
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                placeholder = { Text("Search library", color = colors.textSecondary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = colors.textPrimary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
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
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.weight(1f),
                        colors = searchBarColors
                    ) {
                        // Optional: Results list inside SearchBar if we want it to overlay
                    }

                    Spacer(Modifier.width(8.dp))

                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = colors.textPrimary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Category Tabs (Outlined style)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.size) { index ->
                    val filter = tabs[index]
                    val selected = selectedTab == filter
                    OutlinedButton(
                        onClick = { viewModel.onTabSelected(filter) },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) colors.primary.copy(alpha = 0.1f) else Color.Transparent,
                            contentColor = if (selected) colors.primary else colors.textSecondary
                        ),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) colors.primary else colors.glassBorder
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tabNames[index],
                            style = VynceTheme.typography.label.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Main Library Content
                when (val state = uiState) {
                    is LibraryUiState.Loading -> item { LoadingState() }
                    is LibraryUiState.Empty -> item { EmptyState(tabNames[tabs.indexOf(selectedTab)]) }
                    is LibraryUiState.Error -> item { ErrorState(state.message) }
                    is LibraryUiState.SearchEmpty -> item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No results found", color = VynceTheme.colors.textSecondary)
                        }
                    }
                    is LibraryUiState.Success -> {
                        when (selectedTab) {
                            LibraryFilter.BOOKMARKED_ARTISTS -> {
                                item {
                                    ArtistGrid(
                                        songs = state.items,
                                        onArtistClick = onArtistClick
                                    )
                                }
                            }
                            LibraryFilter.PLAYLISTS, LibraryFilter.LIKED_ALBUMS -> {
                                item {
                                    AlbumGrid(
                                        songs = state.items.filterIsInstance<Song>(),
                                        onAlbumClick = onAlbumClick
                                    )
                                }
                            }
                            else -> {
                                // Default List for Liked and Songs
                                items(state.items.filterIsInstance<Song>()) { song ->
                                    val songItem = SongItem(
                                        id = song.mediaId,
                                        title = song.title,
                                        artists = listOf(Artist(name = song.artist, id = null)),
                                        thumbnail = song.thumbnail,
                                        explicit = false
                                    )
                                    ListItem(
                                        item = songItem,
                                        onClick = { playerViewModel.play(song.toMediaItem()) },
                                        onSwipeRight = { playerViewModel.addToQueue(song.toMediaItem()) },
                                        onSwipeLeft = { viewModel.toggleLike(song) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumGrid(
    songs: List<Song>,
    onAlbumClick: (String) -> Unit = {}
) {
    val albums = songs.distinctBy { it.album ?: it.title }
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val chunked = albums.chunked(2)
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { album ->
                    Column(modifier = Modifier.weight(1f).clickable { onAlbumClick(album.mediaId) }) {
                        AsyncImage(
                            model = album.thumbnail,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(VynceTheme.colors.surface),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = album.album ?: album.title,
                            style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = album.artist,
                            style = VynceTheme.typography.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ArtistGrid(
    songs: List<Any>,
    onArtistClick: (String) -> Unit = {}
) {
    val artists = songs.filterIsInstance<com.vynce.music.db.entities.Artist>().distinctBy { it.id }
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        artists.forEach { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist.id) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = artist.artist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(VynceTheme.colors.surface),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = artist.artist.name,
                    style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = VynceTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = VynceTheme.colors.primary)
    }
}

@Composable
fun EmptyState(tabName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$tabName is empty",
            style = VynceTheme.typography.title.copy(fontSize = 20.sp),
            color = VynceTheme.colors.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Items will appear here as you interact with the app.",
            style = VynceTheme.typography.body,
            color = VynceTheme.colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ErrorState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("Error: $message", color = Color.Red)
    }
}












