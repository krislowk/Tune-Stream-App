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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
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
import coil.compose.SubcomposeAsyncImage
import com.vynce.music.ui.commponents.BottomModal
import com.vynce.music.ui.commponents.MoreOptionsSheet
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.formatTime
import com.vynce.music.utils.shimmerEffect
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    expansionProgress: () -> Float = { 1f },
    onClose: () -> Unit,
    onExpand: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()

    var showMoreOptions by remember { mutableStateOf(false) }
    var isQueueExpanded by remember { mutableStateOf(false) }

    val colors = VynceTheme.colors
    val progress = expansionProgress()

    // Smooth transition between Mini and Full UI
    val miniPlayerAlpha = (1f - (progress * 10f)).coerceIn(0f, 1f)
    val fullPlayerAlpha = (progress * 1.5f).coerceIn(0f, 1f)
    val artworkScale = (0.7f + (progress * 0.3f)).coerceIn(0.7f, 1f)
    val artworkCornerRadius = 12.dp + (24.dp * progress)

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    val animatedSliderPosition by animateFloatAsState(
        targetValue = if (isSeeking) sliderPosition else {
            if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration else 0f
        },
        animationSpec = tween(if (isSeeking) 0 else 250),
        label = "slider"
    )

    val animatedtranslation by animateFloatAsState(
        targetValue = (-20f * progress),
        animationSpec = tween(250),
        label = "translation"
    )

    BottomModal(
        isExpanded = isQueueExpanded,
        onExpandChange = { isQueueExpanded = it },
        peekHeight = 80.dp,
        showSheet = progress > 0.5f,
        sheetContent = { queueProgress ->
            TabbedQueueContent(
                uiState = uiState,
                lyrics = lyrics,
                onPlayItem = { viewModel.playQueueItem(it) },
                onRemoveItem = { viewModel.removeFromQueue(it) },
                onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
                onToggleAutoplay = { viewModel.toggleAutoplay() },
                progress = queueProgress
            )
        },

    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Full Player UI Background & Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fullPlayerAlpha }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.15f),
                                colors.background
                            )
                        )
                    )
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
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

                    Spacer(modifier = Modifier.weight(0.4f))

                    // Artwork (Scaled and Morphing)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .graphicsLayer {
                                scaleX = artworkScale
                                scaleY = artworkScale
                            }
                            .shadow(
                                elevation = (32.dp * progress),
                                shape = RoundedCornerShape(artworkCornerRadius),
                                clip = false
                            )
                            .clip(RoundedCornerShape(artworkCornerRadius))
                            .background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = uiState.currentTrack?.mediaMetadata?.artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.4f))

                    // Info (Slide up and fade in)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { 
                                translationY = (40f * (1f - progress))
                                alpha = fullPlayerAlpha
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.currentTrack?.mediaMetadata?.title?.toString() ?: "Unknown",
                                style = VynceTheme.typography.title.copy(fontSize = 26.sp, fontWeight = FontWeight.Black),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.textPrimary
                            )
                            Text(
                                text = uiState.currentTrack?.mediaMetadata?.artist?.toString() ?: "Unknown",
                                style = VynceTheme.typography.body.copy(fontSize = 18.sp, color = colors.textSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    uiState.currentTrack?.mediaMetadata?.extras?.getString("artist_id")?.let { onNavigateToArtist(it) }
                                }
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleLike() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                        ) {
                            Icon(
                                imageVector = if (uiState.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (uiState.isLiked) colors.primary else colors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Progress & Controls (Fade in)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = fullPlayerAlpha }
                    ) {
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.glassBorder
                            )
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

                        Spacer(modifier = Modifier.height(24.dp))

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
                                        modifier = Modifier.size(40.dp),
                                        tint = colors.textPrimary
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .shadow(12.dp, CircleShape)
                                        .clickable(enabled = !uiState.isBuffering) { viewModel.togglePlayPause() },
                                    shape = CircleShape,
                                    color = colors.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (uiState.isBuffering) {
                                            CircularProgressIndicator(
                                                color = colors.onPrimary,
                                                modifier = Modifier.size(36.dp),
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                                modifier = Modifier.size(48.dp),
                                                tint = colors.onPrimary
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { viewModel.skipNext() }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next",
                                        modifier = Modifier.size(40.dp),
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
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Mini Player UI (Fades out as we expand)
            if (progress < 0.8f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = miniPlayerAlpha
                            translationY = animatedtranslation
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
                    onViewAlbum = albumId?.let { id -> { onNavigateToAlbum(id); onClose() } },
                    onGoToArtist = artistId?.let { id -> { onNavigateToArtist(id); onClose() } },
                    onShare = { /* TODO */ }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedQueueContent(
    uiState: PlayerUiState,
    lyrics: String?,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onToggleAutoplay: () -> Unit,
    progress: Float = 1f
) {
    val colors = VynceTheme.colors
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("UP NEXT", "LYRICS", "RELATED")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (progress > 0.1f) Modifier.fillMaxHeight(0.85f) else Modifier.height(80.dp)),
        color = colors.background,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        if (progress < 0.1f) {
            // Peek view: Just "UP NEXT" and maybe the next song's title
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "UP NEXT",
                    style = VynceTheme.typography.label.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.primary
                )
                
                val nextTrack = uiState.queue.getOrNull(uiState.currentIndex + 1)
                
                if (nextTrack != null) {
                    Text(
                        text = nextTrack.mediaMetadata.title?.toString() ?: "Unknown",
                        style = VynceTheme.typography.body.copy(fontSize = 14.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp).weight(1f),
                        textAlign = TextAlign.End
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Expand",
                    tint = colors.textSecondary,
                    modifier = Modifier.padding(start = 16.dp).size(20.dp)
                )
            }
        } else {
            Column {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(40.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.textSecondary.copy(alpha = 0.2f))
                        .align(Alignment.CenterHorizontally)
                )

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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    when (selectedTab) {
                        0 -> QueueTab(uiState, onPlayItem, onRemoveItem, onMoveItem, onToggleAutoplay)
                        1 -> LyricsTab(lyrics)
                        2 -> RelatedTab(uiState.relatedSongs)
                    }
                }
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
    val lazyListState = rememberLazyListState()
    val state = rememberReorderableLazyListState(lazyListState, onMove = { from, to ->
        onMoveItem(from.index - 1, to.index - 1) // -1 for the header item
    })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
            ReorderableItem(state, key = item.mediaId + item.hashCode()) { isDragging ->
                val isCurrent = uiState.currentTrack?.mediaId == item.mediaId

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPlayItem(index) }
                        .graphicsLayer {
                            scaleX = if (isDragging) 1.05f else 1f
                            scaleY = if (isDragging) 1.05f else 1f
                            alpha = if (isDragging) 0.8f else 1f
                            shadowElevation = if (isDragging) 8f else 0f
                        },
                    color = if (isCurrent) colors.primary.copy(alpha = 0.1f) else Color.Transparent,
                    tonalElevation = if (isDragging) 4.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Reorder",
                            tint = colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(32.dp)
                                .padding(6.dp)
                                .draggableHandle()
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        SubcomposeAsyncImage(
                            model = item.mediaMetadata.artworkUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.mediaMetadata.title?.toString() ?: "Unknown",
                                style = VynceTheme.typography.body.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = if (isCurrent) colors.primary else colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.mediaMetadata.artist?.toString() ?: "Unknown",
                                style = VynceTheme.typography.label.copy(fontSize = 12.sp),
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
                SubcomposeAsyncImage(
                    model = item.mediaMetadata.artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.mediaMetadata.title?.toString() ?: "Unknown",
                        style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.mediaMetadata.artist?.toString() ?: "Unknown",
                        style = VynceTheme.typography.label.copy(fontSize = 12.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
