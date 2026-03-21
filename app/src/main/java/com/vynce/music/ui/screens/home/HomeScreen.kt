package com.vynce.music.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val refreshing = viewModel.refreshing


    PullToRefreshBox(
        refreshing,
        onRefresh = { viewModel.refresh() },
        indicator = { if (refreshing) {
            Box(contentAlignment = Alignment.TopCenter) {
                CircularProgressIndicator()
            }
        }
        }
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
                            HomeHeader()
                        }

                        val quickPicksSection = state.data.sections.find {
                            it.title.contains(
                                "Quick",
                                ignoreCase = true
                            )
                        }
                        val otherSections = state.data.sections.filter { it != quickPicksSection }

                        if (quickPicksSection != null) {
                            item {
                                QuickPicksCarousel(
                                    title = quickPicksSection.title,
                                    items = quickPicksSection.items,
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
                                            quickPicksSection.items.filterIsInstance<SongItem>()
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

                        items(otherSections) { section ->
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
fun HomeHeader() {
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Rise and shine ☀️"
        in 12..16 -> "Good Afternoon ☕"
        in 17..20 -> "Cozy Evening 🌙"
        in 21..23 -> "Quiet Night ✨"
        else -> "Night Owl? 🦉"
    }

    val subtitle = when (hour) {
        in 5..11 -> "Start your day with some energy!"
        in 12..16 -> "Need a midday break?"
        in 17..20 -> "Wind down with your favorites."
        in 21..23 -> "Time for some relaxing beats."
        else -> "Keep the vibe going."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
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
}
