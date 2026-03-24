package com.vynce.music.ui.screens.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val refreshing = viewModel.refreshing


    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {

                is HomeUiState.Loading -> {
                    HomeSkeleton()
                }

                is HomeUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            val user by viewModel.currentUser.collectAsState()
                            HomeHeader(user = user)
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
                                                selectedLabelColor = VynceTheme.colors.onPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }



                        val quickPicksKeywords = listOf(
                            "quick",
                            "Long listens",
                            "Trending songs for you",
                            "Trending in Shorts"
                        )

                        items(
                            items = state.data.sections,
                            key = { section -> section.title }
                        ) { section ->

                            val isQuickPick = quickPicksKeywords.any { keyword ->
                                section.title.contains(keyword, ignoreCase = true)
                            }

                            if (isQuickPick) {
                                QuickPicksCarousel(
                                        title = section.title,
                                        items = section.items,
                                        onItemClick = { item ->
                                            when (item) {
                                                is SongItem -> {
                                                    playerViewModel.play(item.toMediaItem())
                                                }

                                                is AlbumItem -> onItemClick("album", item.browseId)
                                                is PlaylistItem -> onItemClick("playlist", item.id)
                                                is ArtistItem -> onItemClick("artist", item.id)
                                            }
                                        },
                                        onPlayAllClick = {
                                            val songs =
                                                section.items.filterIsInstance<SongItem>()
                                            if (songs.isNotEmpty()) {
                                                playerViewModel.play(songs.first().toMediaItem())
                                                songs.drop(1).forEach { song ->
                                                    playerViewModel.addToQueue(song.toMediaItem())
                                                }
                                            }
                                        }
                                    )
                            } else {
                                CarouselList(
                                    title = section.title,
                                    items = section.items,
                                    onItemClick = { item ->
                                        when (item) {
                                            is SongItem -> {
                                                playerViewModel.play(item.toMediaItem())
                                            }

                                            is AlbumItem -> onItemClick("album", item.browseId)
                                            is PlaylistItem -> onItemClick("playlist", item.id)
                                            is ArtistItem -> onItemClick("artist", item.id)
                                        }
                                    },
                                    onPlayAllClick = {
                                        val songs = section.items.filterIsInstance<SongItem>()
                                        if (songs.isNotEmpty()) {
                                            playerViewModel.play(songs.first().toMediaItem())
                                            songs.drop(1).forEach { song ->
                                                playerViewModel.addToQueue(song.toMediaItem())
                                            }
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

@Composable
fun HomeSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header Skeleton
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VynceTheme.colors.surface.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VynceTheme.colors.surface.copy(alpha = 0.3f))
            )
        }

        repeat(3) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(120.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(VynceTheme.colors.surface.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) {
                        Column(modifier = Modifier.width(150.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(VynceTheme.colors.surface.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VynceTheme.colors.surface.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(user: User?) {
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Rise and shine ☀️"
        in 12..16 -> "Good Afternoon ☕"
        in 17..20 -> "Cozy Evening 🌙"
        in 21..23 -> "Quiet Night ✨"
        else -> "Night Owl? 🦉"
    }

    val subtitle = user?.name?.let { "Ready for some music, $it?" } ?: when (hour) {
        in 5..11 -> "Start your day with some energy!"
        in 12..16 -> "Need a midday break?"
        in 17..20 -> "Wind down with your favorites."
        in 21..23 -> "Time for some relaxing beats."
        else -> "Keep the vibe going."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = VynceTheme.typography.title.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                ),
                color = VynceTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                style = VynceTheme.typography.body.copy(
                    fontSize = 16.sp,
                    color = VynceTheme.colors.textSecondary
                )
            )
        }

        AsyncImage(
            model = user?.avatarUrl ?: "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&s=200",
            contentDescription = "Profile",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(VynceTheme.colors.surface)
                .clickable { /* Navigate to profile settings */ }
        )
    }
}
