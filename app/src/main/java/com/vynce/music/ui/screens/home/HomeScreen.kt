package com.vynce.music.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vynce.music.data.model.User
import com.vynce.music.ui.commponents.CarouselList
import com.vynce.music.ui.commponents.QuickPicksCarousel
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem

@Composable
fun HomeScreen(
    playerViewModel: PlayerViewModel,
    onItemClick: (String, String?) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val refreshing = viewModel.refreshing
    val user by viewModel.currentUser.collectAsState()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize().background(VynceTheme.colors.background)) {
            when (val state = uiState) {
                is HomeUiState.Loading -> HomeSkeleton()
                is HomeUiState.Error -> ErrorState(state.message)
                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        item {
                            HomeTopBar(
                                user = user,
                                onSettingsClick = onSettingsClick
                            )
                        }

                        if (state.data.filters.isNotEmpty()) {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    items(state.data.filters) { filter ->
                                        FilterChip(
                                            selected = filter.isSelected,
                                            onClick = { viewModel.onFilterSelected(filter) },
                                            label = { Text(filter.title) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = VynceTheme.colors.primary,
                                                selectedLabelColor = VynceTheme.colors.onPrimary,
                                                containerColor = VynceTheme.colors.surface,
                                                labelColor = VynceTheme.colors.textSecondary
                                            ),
                                            border = null,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = state.data.sections,
                            key = { it.title + it.items.size }
                        ) { section ->
                            val isQuickPick = section.title.contains("quick", ignoreCase = true) ||
                                    section.title.contains("trending", ignoreCase = true)

                            if (isQuickPick) {
                                QuickPicksCarousel(
                                    title = section.title,
                                    items = section.items,
                                    onItemClick = { item ->
                                        handleItemClick(item, onItemClick, playerViewModel, section.items)
                                    },
                                    onPlayAllClick = {
                                        val songs = section.items.filterIsInstance<SongItem>()
                                        if (songs.isNotEmpty()) {
                                            playerViewModel.playAll(songs.map { it.toMediaItem() })
                                        }
                                    }
                                )
                            } else {
                                CarouselList(
                                    title = section.title,
                                    items = section.items,
                                    onItemClick = { item ->
                                        handleItemClick(item, onItemClick, playerViewModel, section.items)
                                    },
                                    onPlayAllClick = {
                                        val songs = section.items.filterIsInstance<SongItem>()
                                        if (songs.isNotEmpty()) {
                                            playerViewModel.playAll(songs.map { it.toMediaItem() })
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleItemClick(
    item: Any,
    onItemClick: (String, String?) -> Unit,
    playerViewModel: PlayerViewModel,
    sectionItems: List<Any>
) {
    when (item) {
        is SongItem -> playerViewModel.play(item.toMediaItem())
        is AlbumItem -> onItemClick("album", item.browseId)
        is PlaylistItem -> onItemClick("playlist", item.id)
        is ArtistItem -> onItemClick("artist", item.id)
    }
}

@Composable
fun HomeTopBar(
    user: User?,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val greeting = getGreeting()
            Text(
                text = greeting,
                style = VynceTheme.typography.title.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                ),
                color = VynceTheme.colors.textPrimary
            )
            Text(
                text = user?.name?.let { "Welcome back, $it" } ?: "Discover new music",
                style = VynceTheme.typography.body.copy(
                    fontSize = 16.sp,
                    color = VynceTheme.colors.textSecondary
                )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VynceTheme.colors.surface)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = VynceTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))

            AsyncImage(
                model = user?.avatarUrl ?: "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&s=200",
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VynceTheme.colors.surface),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun getGreeting(): String {
    val calendar = java.util.Calendar.getInstance()
    return when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
}

@Composable
fun ErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun HomeSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VynceTheme.colors.surface.copy(alpha = 0.5f))
            )
        }
    }
}
