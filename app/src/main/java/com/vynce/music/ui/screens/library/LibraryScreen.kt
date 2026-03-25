package com.vynce.music.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vynce.music.data.model.Song
import com.vynce.music.ui.commponents.ListItem
import com.vynce.music.ui.commponents.SectionHeader
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.models.Artist
import com.vynce.vynceclient.models.SongItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val colors = VynceTheme.colors
    val shapes = VynceTheme.shapes
    val typography = VynceTheme.typography

    val tabs = listOf("Songs", "Liked", "History")

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
                        dividerColor = Color.Transparent
                    )
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchQuery,
                                onQueryChange = viewModel::onSearchQueryChanged,
                                onSearch = { expanded = false },
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                placeholder = { Text("Search your library", color = colors.textSecondary) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textPrimary) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.textPrimary)
                                        }
                                    }
                                },
                                colors = searchBarColors.inputFieldColors,
                            )
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (expanded) 0.dp else 16.dp),
                        colors = searchBarColors
                    ) {
                        // Optional: Results list inside SearchBar if we want it to overlay
                    }
                }

                if (!expanded) {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = colors.background,
                        contentColor = colors.primary,
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(selectedTab),
                                color = colors.primary
                            )
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { viewModel.onTabSelected(index) },
                                text = {
                                    Text(
                                        text = title,
                                        style = typography.label.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedTab == index) colors.primary else colors.textSecondary
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val showBackToTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 5 }
            }
            
            AnimatedVisibility(
                visible = showBackToTop,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    shape = shapes.medium
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Scroll to top")
                }
            }
        },
        containerColor = colors.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> LoadingState()
                is LibraryUiState.Empty -> EmptyState(tabs[selectedTab])
                is LibraryUiState.SearchEmpty -> SearchEmptyState()
                is LibraryUiState.Error -> ErrorState(state.message)
                is LibraryUiState.Success -> {
                    LibraryList(
                        songs = state.songs,
                        listState = listState,
                        onSongClick = { song ->
                            playerViewModel.play(song.toMediaItem())
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryList(
    songs: List<Song>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            val songItem = SongItem(
                id = song.id.toString(),
                title = song.title,
                artists = listOf(Artist(name = song.artist, id = null)),
                thumbnail = song.thumbnail,
                explicit = false
            )
            ListItem(
                item = songItem,
                onClick = { onSongClick(song) }
            )
        }
    }
}

@Composable
fun LoadingState() {
    val colors = VynceTheme.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colors.primary)
    }
}

@Composable
fun EmptyState(tabName: String) {
    val colors = VynceTheme.colors
    val typography = VynceTheme.typography
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$tabName is empty",
            style = typography.title.copy(fontSize = 20.sp),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Items will appear here as you interact with the app.",
            style = typography.body,
            color = colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SearchEmptyState() {
    val colors = VynceTheme.colors
    val typography = VynceTheme.typography
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No songs match your search.",
            style = typography.body,
            color = colors.textSecondary
        )
    }
}

@Composable
fun ErrorState(message: String) {
    val colors = VynceTheme.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Using red color directly as VynceColors doesn't have error yet
        Text("Error: $message", color = Color.Red)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun LibraryEmptyPreview() {
    VynceTheme {
        EmptyState("Songs")
    }
}
