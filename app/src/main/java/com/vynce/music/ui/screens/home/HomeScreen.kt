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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.compose.SubcomposeAsyncImage
import com.vynce.music.ui.components.ArtistCarousel
import com.vynce.music.ui.components.CarouselColumn
import com.vynce.music.ui.components.CarouselRow
import com.vynce.music.ui.components.CommunityCarousel
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.shimmerEffect
import com.vynce.music.utils.toMediaItem
import com.vynce.music.viewmodels.HomeUiState
import com.vynce.music.viewmodels.HomeViewModel
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.pages.HomePage

@UnstableApi
@Composable
fun HomeScreen(
    playerViewModel: PlayerViewModel,
    onItemClick: (String, String?) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        selectedChip = selectedChip,
        currentUser = currentUser,
        onRefresh = { viewModel.refresh() },
        onFilterSelected = { viewModel.onFilterSelected(it) },
        onItemClick = { item -> handleItemClick(item, onItemClick, playerViewModel) },
        onPlayAllClick = { songs -> playerViewModel.playAll(songs.map { it.toMediaItem() }) },
        onAddToQueue = { playerViewModel.addToQueue(it.toMediaItem()) },
        onLoadMore = { viewModel.loadMore() }
    )
}

@UnstableApi
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    isRefreshing: Boolean,
    selectedChip: HomePage.Chip?,
    currentUser: com.vynce.music.models.User?,
    onRefresh: () -> Unit,
    onFilterSelected: (HomePage.Chip?) -> Unit,
    onItemClick: (com.vynce.vynceclient.models.YTItem) -> Unit,
    onPlayAllClick: (List<SongItem>) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onLoadMore: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().background(VynceTheme.colors.background)) {
            when (uiState) {
                is HomeUiState.Loading -> HomeSkeleton()
                is HomeUiState.Error -> ErrorState(
                    message = uiState.message,
                    onRetry = onRefresh
                )
                is HomeUiState.Success -> {
                    val sections = remember(uiState.data.sections) {
                        uiState.data.sections.sortedByDescending { section ->
                            val title = section.title?.lowercase() ?: ""
                            if (title.contains("quick picks")) 1 else 0
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val greeting = remember { getGreeting() }
                                    Text(
                                        text = greeting,
                                        style = VynceTheme.typography.title.copy(
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-1).sp
                                        ),
                                        color = VynceTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Discover your rhythm today",
                                        style = VynceTheme.typography.label,
                                        color = VynceTheme.colors.textSecondary
                                    )
                                }
                                Box(modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(VynceTheme.colors.glassSurface)
                                    .padding(2.dp),
                                    contentAlignment = Alignment.Center) {
                                    if (currentUser?.avatarUrl != null) {
                                        SubcomposeAsyncImage(
                                            model = currentUser.avatarUrl,
                                            contentDescription = "Account",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                            loading = {
                                                Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                                            },
                                            error = {
                                                Icon(imageVector = Icons.Default.AccountCircle, "Account", tint = VynceTheme.colors.textPrimary)
                                            }
                                        )
                                    } else {
                                        Icon(imageVector = Icons.Default.AccountCircle, "Account", tint = VynceTheme.colors.textPrimary)
                                    }
                                }
                            }
                        }

                        if (uiState.data.filters.isNotEmpty()) {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedChip == null,
                                            onClick = { onFilterSelected(null) },
                                            label = { Text("All") },
                                            shape = RoundedCornerShape(48.dp),
                                            border = BorderStroke(
                                                if (selectedChip == null) 0.dp else 1.dp,
                                                VynceTheme.colors.glassBorder
                                            ),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = VynceTheme.colors.primary,
                                                selectedLabelColor = VynceTheme.colors.onPrimary,
                                                containerColor = VynceTheme.colors.surface,
                                                labelColor = VynceTheme.colors.textSecondary
                                            )
                                        )
                                    }
                                    items(uiState.data.filters, key = { it.title }) { filter ->
                                        FilterChip(
                                            selected = filter.title == selectedChip?.title,
                                            onClick = { onFilterSelected(filter) },
                                            label = { Text(filter.title) },
                                            shape = RoundedCornerShape(48.dp),
                                            border = BorderStroke(
                                                if (filter.title == selectedChip?.title) 0.dp else 1.dp,
                                                VynceTheme.colors.glassBorder
                                            ),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = VynceTheme.colors.primary,
                                                selectedLabelColor = VynceTheme.colors.onPrimary,
                                                containerColor = VynceTheme.colors.surface,
                                                labelColor = VynceTheme.colors.textSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(
                            items = sections,
                            key = { _, section -> section.title ?: section.hashCode() }
                        ) { index, section ->
                            if (index >= sections.size - 1) {
                                LaunchedEffect(sections.size) {
                                    onLoadMore()
                                }
                            }

                            val title = section.title ?: return@itemsIndexed
                            val titleLower = title.lowercase()
                            val isQuickPick = titleLower.contains("quick") ||
                                    titleLower.contains("trending")
                            val isCommunity = titleLower.contains("community")
                            val isArtist = titleLower.contains("artist") || 
                                    titleLower.contains("music channels")

                            if (isQuickPick) {
                                CarouselColumn(
                                    title = title,
                                    items = section.items,
                                    onItemClick = onItemClick,
                                    onPlayAllClick = {
                                        val songs = section.items.filterIsInstance<SongItem>()
                                        if (songs.isNotEmpty()) {
                                            onPlayAllClick(songs)
                                        }
                                    }
                                )
                            } else if (isArtist) {
                                ArtistCarousel(
                                    title = title,
                                    items = section.items,
                                    onItemClick = onItemClick
                                )
                            } else if (isCommunity) {
                                CommunityCarousel(
                                    title = title,
                                    items = section.items,
                                    onItemClick = onItemClick
                                )
                            } else {
                                CarouselRow(
                                    title = title,
                                    items = section.items,
                                    onItemClick = onItemClick,
                                    onPlayAllClick = {
                                        val songs = section.items.filterIsInstance<SongItem>()
                                        if (songs.isNotEmpty()) {
                                            onPlayAllClick(songs)
                                        }
                                    }
                                )
                            }
                        }

                        if (uiState.isLoadingMore) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    repeat(3) {
                                        Box(
                                            modifier = Modifier
                                                .size(160.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .shimmerEffect()
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
}

@UnstableApi
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

@UnstableApi
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun HomePreview() {
    val mockArtist = com.vynce.vynceclient.models.Artist("Artist Name", "A1")
    val mockSong = SongItem(
        id = "1",
        title = "Song Title",
        artists = listOf(mockArtist),
        thumbnail = "https://picsum.photos/200",
        explicit = false
    )
    val mockAlbum = AlbumItem(
        browseId = "B1",
        playlistId = "P1",
        title = "Album Title",
        artists = listOf(mockArtist),
        thumbnail = "https://picsum.photos/200"
    )

    val mockArtistItem = ArtistItem(
        id = "A1",
        title = "Artist Name",
        thumbnail = "https://picsum.photos/200",
        shuffleEndpoint = null,
        radioEndpoint = null
    )

    VynceTheme {
        HomeScreenContent(
            uiState = HomeUiState.Success(
                data = HomePage(
                    chips = listOf(
                        HomePage.Chip("Energize", null, null),
                        HomePage.Chip("Focus", null, null),
                        HomePage.Chip("Relax", null, null)
                    ),
                    sections = listOf(
                        HomePage.Section(
                            title = "Quick Picks",
                            label = "Start a radio from a song",
                            thumbnail = null,
                            endpoint = null,
                            items = List(12) { mockSong.copy(id = "song_$it", title = "Song $it") }
                        ),
                        HomePage.Section(
                            title = "Community Playlists",
                            label = null,
                            thumbnail = null,
                            endpoint = null,
                            items = List(10) { mockSong.copy(id = "comm_$it", title = "Community Song $it") }
                        ),
                        HomePage.Section(
                            title = "Artists You Love",
                            label = null,
                            thumbnail = null,
                            endpoint = null,
                            items = List(6) { mockArtistItem.copy(id = "artist_$it", title = "Artist $it") }
                        ),
                        HomePage.Section(
                            title = "Forgotten Favorites",
                            label = null,
                            thumbnail = null,
                            endpoint = null,
                            items = List(6) { mockSong.copy(id = "fav_$it", title = "Favorite $it") }
                        ),
                        HomePage.Section(
                            title = "New Releases",
                            label = null,
                            thumbnail = null,
                            endpoint = null,
                            items = List(6) { mockAlbum.copy(browseId = "album_$it", id = "album_$it", title = "Album $it") }
                        )
                    )
                )
            ),
            isRefreshing = false,
            selectedChip = null,
            currentUser = com.vynce.music.models.User(
                name = "Vynce User",
                email = "user@vynce.app",
                avatarUrl = "https://picsum.photos/200"
            ),
            onRefresh = {},
            onFilterSelected = {},
            onItemClick = {},
            onPlayAllClick = {},
            onAddToQueue = {},
            onLoadMore = {}
        )
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












