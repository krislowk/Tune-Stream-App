package com.vynce.music.ui.screens.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vynce.music.ui.commponents.CarouselList
import com.vynce.music.ui.commponents.QuickPicksCarousel
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.shimmerEffect
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshing = viewModel.refreshing

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize().background(VynceTheme.colors.background)) {
            when (val state = uiState) {
                is HomeUiState.Loading -> HomeSkeleton()
                is HomeUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        item {
                            HomeTopBar()
                        }

                        if (state.data.filters.isNotEmpty()) {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    items(
                                        items = state.data.filters,
                                        key = { it.title }
                                    ) { filter ->
                                        FilterChip(
                                            selected = filter.isSelected,
                                            onClick = { viewModel.onFilterSelected(filter) },
                                            label = { Text(filter.title) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = VynceTheme.colors.onPrimary,
                                                selectedLabelColor = VynceTheme.colors.onPrimary,
                                                containerColor = VynceTheme.colors.surface,
                                                labelColor = VynceTheme.colors.textSecondary
                                            ),
                                            border = if (filter.isSelected )BorderStroke(1.dp,VynceTheme.colors.primary) else null,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = state.data.sections,
                            key = { it.title }
                        ) { section ->
                            val isQuickPick = section.title.contains("quick", ignoreCase = true) ||
                                    section.title.contains("trending", ignoreCase = true)

                            if (isQuickPick) {
                                QuickPicksCarousel(
                                    title = section.title,
                                    items = section.items,
                                    onItemClick = { item ->
                                        handleItemClick(item, onItemClick, playerViewModel)
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
                                        handleItemClick(item, onItemClick, playerViewModel)
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
    playerViewModel: PlayerViewModel
) {
    when (item) {
        is SongItem -> playerViewModel.play(item.toMediaItem())
        is AlbumItem -> onItemClick("album", item.browseId)
        is PlaylistItem -> onItemClick("playlist", item.id)
        is ArtistItem -> onItemClick("artist", item.id)
    }
}

@Composable
fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val greeting =  remember { getGreeting() }
            Text(
                text = greeting,
                style = VynceTheme.typography.title.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                ),
                color = VynceTheme.colors.textPrimary
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
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = VynceTheme.colors.textSecondary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Oops! Something went wrong",
            style = VynceTheme.typography.title,
            color = VynceTheme.colors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val userFriendlyMessage = when {
            message.contains("timeout", ignoreCase = true) -> 
                "The connection timed out. Please check your internet and try again."
            message.contains("socket", ignoreCase = true) || message.contains("connection", ignoreCase = true) ->
                "We're having trouble reaching the server. Please check your network."
            else -> message
        }

        Text(
            text = userFriendlyMessage,
            style = VynceTheme.typography.body,
            color = VynceTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = VynceTheme.colors.primary,
                contentColor = VynceTheme.colors.onPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Bar Skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Box(modifier = Modifier.size(180.dp, 32.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.size(140.dp, 16.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).shimmerEffect())
        }

        // Filters Skeleton
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(5) {
                Box(modifier = Modifier.size(80.dp, 32.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp),
            userScrollEnabled = false
        ) {
            // Quick Picks Skeleton (Grid-like columns)
            item {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).size(120.dp, 24.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(3) {
                            Column(modifier = Modifier.width(300.dp)) {
                                repeat(4) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Box(modifier = Modifier.size(150.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                            Spacer(Modifier.height(6.dp))
                                            Box(modifier = Modifier.size(100.dp, 10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Standard Carousels Skeleton
            items(3) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).size(120.dp, 24.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(4) {
                            Column {
                                Box(modifier = Modifier.size(160.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
                                Spacer(Modifier.height(12.dp))
                                Box(modifier = Modifier.size(120.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                Spacer(Modifier.height(6.dp))
                                Box(modifier = Modifier.size(80.dp, 12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            }
                        }
                    }
                }
            }
        }
    }
}
