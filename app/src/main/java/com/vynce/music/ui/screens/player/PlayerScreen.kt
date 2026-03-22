package com.vynce.music.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.vynce.music.ui.commponents.MoreOptionsSheet
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    expansionProgress: () -> Float = { 1f },
    onClose: () -> Unit,
    onExpand: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()

    var showMoreOptions by remember { mutableStateOf(false) }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val colors = VynceTheme.colors

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    val animatedSliderPosition by animateFloatAsState(
        targetValue = if (isSeeking) sliderPosition else {
            if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration else 0f
        },
        animationSpec = tween(if (isSeeking) 0 else 250),
        label = "slider"
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            TabbedQueueContent(
                uiState = uiState,
                lyrics = lyrics,
                onPlayItem = { viewModel.playQueueItem(it) },
                onRemoveItem = { viewModel.removeFromQueue(it) },
                onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
                onToggleAutoplay = { viewModel.toggleAutoplay() }
            )
        },
        sheetPeekHeight = 64.dp,
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.textSecondary.copy(alpha = 0.3f))
            )
        },
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = colors.surface,
        containerColor = colors.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.2f),
                            colors.background
                        )
                    )
                )
        ) {
            // Full Player UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        alpha = expansionProgress()
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Close",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "NOW PLAYING",
                        style = VynceTheme.typography.label.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.textSecondary
                    )
                    IconButton(onClick = { showMoreOptions = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // Artwork
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(24.dp),
                            clip = false
                        )
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = uiState.currentTrack?.mediaMetadata?.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.currentTrack?.mediaMetadata?.title?.toString() ?: "Unknown",
                            style = VynceTheme.typography.title.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.textPrimary
                        )
                        Text(
                            text = uiState.currentTrack?.mediaMetadata?.artist?.toString() ?: "Unknown",
                            style = VynceTheme.typography.body.copy(fontSize = 18.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.textSecondary,
                            modifier = Modifier.clickable {
                                uiState.currentTrack?.mediaMetadata?.extras?.getString("artist_id")?.let { onNavigateToArtist(it) }
                            }
                        )
                    }

                    IconButton(onClick = { viewModel.toggleLike() }) {
                        Icon(
                            imageVector = if (uiState.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (uiState.isLiked) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = animatedSliderPosition,
                        onValueChange = {
                            sliderPosition = it
                            isSeeking = true
                        },
                        onValueChangeFinished = {
                            viewModel.seekTo((sliderPosition * uiState.duration).toLong())
                            isSeeking = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(uiState.currentPosition),
                            style = VynceTheme.typography.label,
                            color = colors.textSecondary
                        )
                        Text(
                            text = formatTime(uiState.duration),
                            style = VynceTheme.typography.label,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (uiState.shuffleEnabled) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        IconButton(onClick = { viewModel.skipPrevious() }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(36.dp),
                                tint = colors.textPrimary
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(8.dp, CircleShape)
                                .clickable { viewModel.togglePlayPause() },
                            shape = CircleShape,
                            color = colors.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(40.dp),
                                    tint = colors.onPrimary
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.skipNext() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(36.dp),
                                tint = colors.textPrimary
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.toggleRepeat() }) {
                        Icon(
                            imageVector = when (uiState.repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Mini Player UI (Overlay when collapsed)
            if (expansionProgress() < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp + bottomPadding)
                        .padding(bottom = bottomPadding)
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            alpha = 1f - expansionProgress()
                        }
                ) {
                    MiniPlayer(
                        viewModel = viewModel,
                        onClick = onExpand
                    )
                }
            }

            if (showMoreOptions && uiState.currentTrack != null) {
                val metadata = uiState.currentTrack!!.mediaMetadata
                val albumId = metadata.extras?.getString("album_id")
                val artistId = metadata.extras?.getString("artist_id")

                MoreOptionsSheet(
                    title = metadata.title?.toString() ?: "Unknown",
                    subtitle = metadata.artist?.toString() ?: "Unknown",
                    thumbnailUrl = metadata.artworkUri?.toString() ?: "",
                    onDismiss = { showMoreOptions = false },
                    onAddToPlaylist = { /* TODO */ },
                    onViewAlbum = albumId?.let { { onNavigateToAlbum(it) } },
                    onGoToArtist = artistId?.let { { onNavigateToArtist(it) } },
                    onShare = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun TabbedQueueContent(
    uiState: PlayerUiState,
    lyrics: String?,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onToggleAutoplay: () -> Unit
) {
    val colors = VynceTheme.colors
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("UP NEXT", "LYRICS", "RELATED")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = colors.primary
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = VynceTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedTab == index) colors.primary else colors.textSecondary
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when (selectedTab) {
                0 -> QueueTab(uiState, onPlayItem, onRemoveItem, onMoveItem, onToggleAutoplay)
                1 -> LyricsTab(lyrics)
                2 -> RelatedTab(uiState.relatedSongs)
            }
        }
    }
}

@Composable
fun QueueTab(
    uiState: PlayerUiState,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onToggleAutoplay: () -> Unit
) {
    val colors = VynceTheme.colors
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playing from queue",
                    style = VynceTheme.typography.label,
                    color = colors.textSecondary
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Autoplay",
                        style = VynceTheme.typography.label,
                        color = if (uiState.isAutoplayEnabled) colors.primary else colors.textSecondary
                    )
                    Switch(
                        checked = uiState.isAutoplayEnabled,
                        onCheckedChange = { onToggleAutoplay() },
                        modifier = Modifier.scale(0.7f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(uiState.queue, key = { _, item -> item.mediaId + item.hashCode() }) { index, item ->
            val isCurrent = uiState.currentTrack?.mediaId == item.mediaId

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlayItem(index) }
                    .padding(vertical = 4.dp),
                color = if (isCurrent) colors.primary.copy(alpha = 0.1f) else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AsyncImage(
                        model = item.mediaMetadata.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.mediaMetadata.title?.toString() ?: "Unknown",
                            style = VynceTheme.typography.body.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isCurrent) colors.primary else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.mediaMetadata.artist?.toString() ?: "Unknown",
                            style = VynceTheme.typography.label,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onRemoveItem(index) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsTab(lyrics: String?) {
    val colors = VynceTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = lyrics ?: "No lyrics available.",
                style = VynceTheme.typography.body.copy(
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun RelatedTab(related: List<MediaItem>) {
    val colors = VynceTheme.colors
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Spacer(Modifier.height(16.dp))
        }
        items(related) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Play related */ }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = item.mediaMetadata.artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.mediaMetadata.title?.toString() ?: "Unknown",
                        style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.mediaMetadata.artist?.toString() ?: "Unknown",
                        style = VynceTheme.typography.label,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
