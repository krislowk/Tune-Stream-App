package com.vynce.music.ui.screens.player

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.vynce.music.lyrics.LyricsEntry
import com.vynce.music.lyrics.LyricsTranslationHelper
import com.vynce.music.lyrics.WordTimestamp
import com.vynce.music.repository.constants.LibraryFilter
import com.vynce.music.ui.components.AddToPlaylistSheet
import com.vynce.music.ui.components.BottomSheet
import com.vynce.music.ui.components.MoreOptionsSheet
import com.vynce.music.ui.components.rememberBottomSheetState
import com.vynce.music.ui.screens.library.LibraryViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.formatTime
import com.vynce.music.utils.shimmerEffect
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.PlaylistItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@androidx.annotation.OptIn(UnstableApi::class)
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
    val context = LocalContext.current

    var showMoreOptions by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var isKaraokeMode by remember { mutableStateOf(false) }
    
    val queueSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        expandedBound = 800.dp,
        collapsedBound = 80.dp,
        initialAnchor = com.vynce.music.ui.components.collapsedAnchor
    )

    val colors = VynceTheme.colors
    val progress = expansionProgress()

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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isKaraokeMode) {
            ImmersiveKaraokeScreen(
                viewModel = viewModel,
                onClose = { isKaraokeMode = false }
            )
        } else {
            // Player Background
            PlayerScreenBackground(
                artworkUri = uiState.currentTrack?.mediaMetadata?.artworkUri,
                alpha = fullPlayerAlpha
            )

            // Player Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fullPlayerAlpha }
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp),
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

                    Spacer(modifier = Modifier.height(24.dp))

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

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { 
                                translationY = (40f * (1f - progress))
                                alpha = fullPlayerAlpha
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.currentTrack?.mediaMetadata?.title?.toString() ?: "Unknown",
                                style = VynceTheme.typography.title.copy(fontSize = 26.sp, fontWeight = FontWeight.Black),
                                maxLines = 1,
                                color = colors.textPrimary,
                                modifier = Modifier.fillMaxWidth().basicMarquee()
                            )
                            val artist = uiState.currentTrack?.mediaMetadata?.artist?.toString() ?: "Unknown"
                            val source = uiState.currentTrack?.mediaMetadata?.extras?.getString("source")
                            Text(
                                text = if (source != null) "$artist • $source" else artist,
                                style = VynceTheme.typography.body.copy(fontSize = 18.sp, color = colors.textSecondary),
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        uiState.currentTrack?.mediaMetadata?.extras?.getString("artist_id")?.let { onNavigateToArtist(it) }
                                    }
                                    .basicMarquee()
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

                    Spacer(modifier = Modifier.height(24.dp))

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
                                inactiveTrackColor = colors.textPrimary.copy(alpha = 0.1f)
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

                        Spacer(modifier = Modifier.height(16.dp))

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
                                        .clickable { viewModel.togglePlayPause() },
                                    shape = CircleShape,
                                    color = colors.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (uiState.isBuffering && !uiState.isPlaying) {
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
                    Spacer(modifier = Modifier.height(32.dp))
                }

            // Mini Player
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

            // Queue/Lyrics BottomSheet
            BottomSheet(
                state = queueSheetState,
                isExpandable = progress > 0.5f,
                background = {
                    if (queueSheetState.progress > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = (queueSheetState.progress * 0.4f).coerceIn(0f, 0.4f)))
                                .clickable { queueSheetState.collapseSoft() }
                        )
                    }
                },
                collapsedContent = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        TabbedQueueContent(
                            uiState = uiState,
                            onPlayItem = { viewModel.playQueueItem(it) },
                            onPlayRelated = { viewModel.play(it) },
                            onPlayAlbum = { viewModel.playAlbum(it) },
                            onPlayArtist = { viewModel.playArtist(it) },
                            onPlayPlaylist = { viewModel.playPlaylist(it) },
                            onRemoveItem = { viewModel.removeFromQueue(it) },
                            onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
                            onToggleAutoplay = { viewModel.toggleAutoplay() },
                            onTranslateLyrics = { viewModel.translateLyrics() },
                            onGenerateLyrics = { viewModel.generateLyricsWithGemini() },
                            onEnterKaraoke = { isKaraokeMode = true },
                            currentPosition = uiState.currentPosition,
                            onSeek = { viewModel.seekTo(it) },
                            progress = 0f
                        )
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    TabbedQueueContent(
                        uiState = uiState,
                        onPlayItem = { viewModel.playQueueItem(it) },
                        onPlayRelated = { viewModel.play(it) },
                        onPlayAlbum = { viewModel.playAlbum(it) },
                        onPlayArtist = { viewModel.playArtist(it) },
                        onPlayPlaylist = { viewModel.playPlaylist(it) },
                        onRemoveItem = { viewModel.removeFromQueue(it) },
                        onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
                        onToggleAutoplay = { viewModel.toggleAutoplay() },
                        onTranslateLyrics = { viewModel.translateLyrics() },
                        onGenerateLyrics = { viewModel.generateLyricsWithGemini() },
                        onEnterKaraoke = { isKaraokeMode = true },
                        currentPosition = uiState.currentPosition,
                        onSeek = { viewModel.seekTo(it) },
                        progress = queueSheetState.progress
                    )
                }
            }

            if (showMoreOptions && uiState.currentTrack != null) {
                val metadata = uiState.currentTrack!!.mediaMetadata
                val albumId = metadata.extras?.getString("album_id")
                val artistId = metadata.extras?.getString("artist_id")
                val shareLink = metadata.extras?.getString("share_link")

                MoreOptionsSheet(
                    title = metadata.title?.toString() ?: "Unknown",
                    subtitle = metadata.artist?.toString() ?: "Unknown",
                    thumbnailUrl = metadata.artworkUri?.toString() ?: "",
                    onDismiss = { showMoreOptions = false },
                    onAddToPlaylist = {
                        showAddToPlaylist = true
                        showMoreOptions = false
                    },
                    onViewAlbum = albumId?.let { id -> { onNavigateToAlbum(id); onClose() } },
                    onGoToArtist = artistId?.let { id -> { onNavigateToArtist(id); onClose() } },
                    onShare = {
                        if (shareLink != null) {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareLink)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        } else {
                            Toast.makeText(context, "Share link not available", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            if (showAddToPlaylist && uiState.currentTrack != null) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                LaunchedEffect(Unit) {
                    libraryViewModel.onTabSelected(LibraryFilter.PLAYLISTS)
                }
                
                AddToPlaylistSheet(
                    onDismiss = { showAddToPlaylist = false },
                    onPlaylistSelected = { playlistId ->
                        viewModel.addToPlaylist(playlistId, uiState.currentTrack!!.mediaId)
                        Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                    },
                    libraryViewModel = libraryViewModel
                )
            }
        }
    }
}

@Composable
fun PlayerScreenBackground(
    artworkUri: android.net.Uri?,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val colors = VynceTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
    ) {
        // Blurred Artwork
        SubcomposeAsyncImage(
            model = artworkUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.5f
                    scaleY = 1.5f
                }
                .blur(radius = 100.dp),
            contentScale = ContentScale.Crop,
            loading = {
                Box(modifier = Modifier.fillMaxSize().shimmerEffect())
            },
            error = {
                Box(modifier = Modifier.fillMaxSize().background(colors.background))
            }
        )

        // Overlays for readability and depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            colors.background.copy(alpha = 0.8f),
                            colors.background
                        )
                    )
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedQueueContent(
    uiState: PlayerUiState,
    onPlayItem: (Int) -> Unit,
    onPlayRelated: (MediaItem) -> Unit,
    onPlayAlbum: (AlbumItem) -> Unit = {},
    onPlayArtist: (ArtistItem) -> Unit = {},
    onPlayPlaylist: (PlaylistItem) -> Unit = {},
    onRemoveItem: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onToggleAutoplay: () -> Unit,
    onTranslateLyrics: () -> Unit = {},
    onGenerateLyrics: () -> Unit = {},
    onEnterKaraoke: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    currentPosition: Long = 0L,
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
                        1 -> {
                            val translationStatus by LyricsTranslationHelper.status.collectAsState()
                            val hasActiveTranslations by LyricsTranslationHelper.hasActiveTranslations.collectAsState()
                            
                            LyricsTab(
                                lyrics = uiState.lyrics,
                                isLoading = uiState.isLyricsLoading,
                                currentPosition = uiState.currentPosition,
                                onTranslate = onTranslateLyrics,
                                onGenerate = onGenerateLyrics,
                                onEnterKaraoke = onEnterKaraoke,
                                translationStatus = translationStatus,
                                hasActiveTranslations = hasActiveTranslations
                            )
                        }
                        2 -> RelatedTab(
                            relatedSongs = uiState.relatedSongs,
                            relatedAlbums = uiState.relatedAlbums,
                            relatedArtists = uiState.relatedArtists,
                            relatedPlaylists = uiState.relatedPlaylists,
                            onPlaySong = onPlayRelated,
                            onPlayAlbum = onPlayAlbum,
                            onPlayArtist = onPlayArtist,
                            onPlayPlaylist = onPlayPlaylist
                        )
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
fun RelatedTab(
    relatedSongs: List<MediaItem>,
    relatedAlbums: List<AlbumItem>,
    relatedArtists: List<ArtistItem>,
    relatedPlaylists: List<PlaylistItem>,
    onPlaySong: (MediaItem) -> Unit,
    onPlayAlbum: (AlbumItem) -> Unit,
    onPlayArtist: (ArtistItem) -> Unit,
    onPlayPlaylist: (PlaylistItem) -> Unit
) {
    val colors = VynceTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (relatedSongs.isEmpty() && relatedAlbums.isEmpty() && relatedArtists.isEmpty() && relatedPlaylists.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No related content found",
                        style = VynceTheme.typography.body,
                        color = colors.textSecondary
                    )
                }
            }
        }

        if (relatedSongs.isNotEmpty()) {
            item {
                Text(
                    "Related Songs",
                    style = VynceTheme.typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            items(relatedSongs) { item ->
                RelatedSongItem(item, onClick = { onPlaySong(item) })
            }
        }

        if (relatedAlbums.isNotEmpty()) {
            item {
                Text(
                    "Albums",
                    style = VynceTheme.typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(relatedAlbums) { album ->
                        RelatedGridItem(
                            title = album.title,
                            subtitle = album.artists?.firstOrNull()?.name ?: "Unknown",
                            thumbnail = album.thumbnail,
                            onClick = { onPlayAlbum(album) }
                        )
                    }
                }
            }
        }

        if (relatedArtists.isNotEmpty()) {
            item {
                Text(
                    "Artists",
                    style = VynceTheme.typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(relatedArtists) { artist ->
                        RelatedArtistItem(artist, onClick = { onPlayArtist(artist) })
                    }
                }
            }
        }

        if (relatedPlaylists.isNotEmpty()) {
            item {
                Text(
                    "Playlists",
                    style = VynceTheme.typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(relatedPlaylists) { playlist ->
                        RelatedGridItem(
                            title = playlist.title,
                            subtitle = playlist.author?.name ?: "YouTube Music",
                            thumbnail = playlist.thumbnail,
                            onClick = { onPlayPlaylist(playlist) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(300.dp))
        }
    }
}

@Composable
fun RelatedSongItem(item: MediaItem, onClick: () -> Unit) {
    val colors = VynceTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.mediaMetadata.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface),
            contentScale = ContentScale.Crop
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

@Composable
fun RelatedGridItem(title: String, subtitle: String, thumbnail: String?, onClick: () -> Unit) {
    val colors = VynceTheme.colors
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = VynceTheme.typography.label.copy(fontSize = 11.sp),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RelatedArtistItem(artist: ArtistItem, onClick: () -> Unit) {
    val colors = VynceTheme.colors
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(colors.surface),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = artist.title,
            style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = colors.textPrimary,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsTab(
    lyrics: List<LyricsEntry>,
    isLoading: Boolean,
    currentPosition: Long,
    onTranslate: () -> Unit,
    onGenerate: () -> Unit = {},
    onEnterKaraoke: () -> Unit = {},
    translationStatus: LyricsTranslationHelper.TranslationStatus,
    hasActiveTranslations: Boolean
) {
    val listState = rememberLazyListState()
    val activeIndex = remember(lyrics, currentPosition) {
        val index = lyrics.indexOfLast { it.time <= currentPosition }
        if (index == -1) 0 else index
    }

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            listState.animateScrollToItem(activeIndex, scrollOffset = -300)
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VynceTheme.colors.primary)
        }
    } else if (lyrics.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No lyrics found",
                style = VynceTheme.typography.body,
                color = VynceTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                onClick = onGenerate,
                color = VynceTheme.colors.primary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Box(Modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Try Gemini AI",
                        style = VynceTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                        color = VynceTheme.colors.onPrimary
                    )
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEnterKaraoke) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Immersive Karaoke",
                        tint = VynceTheme.colors.primary
                    )
                }

                IconButton(onClick = onGenerate) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Generate with Gemini",
                        tint = VynceTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (translationStatus is LyricsTranslationHelper.TranslationStatus.Translating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = VynceTheme.colors.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onTranslate) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Translate",
                            tint = if (hasActiveTranslations) VynceTheme.colors.primary else VynceTheme.colors.textSecondary
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.15f to Color.Black,
                                0.85f to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                state = listState,
                contentPadding = PaddingValues(top = 100.dp, bottom = 350.dp)
            ) {
                itemsIndexed(lyrics) { index, entry ->
                    LyricsLine(
                        entry = entry,
                        isActive = index == activeIndex,
                        currentPosition = currentPosition
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsLine(
    entry: LyricsEntry,
    isActive: Boolean,
    currentPosition: Long,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = tween(400),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f,
        animationSpec = tween(400),
        label = "alpha"
    )

    val translatedText by entry.translatedTextFlow.collectAsState()
    val displayText = translatedText ?: entry.text
    val subText = if (translatedText != null) entry.text else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        if (!entry.words.isNullOrEmpty() && translatedText == null) {
            FlowRow(
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.Center
            ) {
                entry.words.forEach { word ->
                    KaraokeWord(
                        word = word,
                        currentPosition = currentPosition,
                        isActive = isActive
                    )
                }
            }
        } else {
            Text(
                text = displayText,
                style = VynceTheme.typography.body.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp
                ),
                color = if (isActive) Color.White else VynceTheme.colors.textPrimary
            )
        }

        if (subText != null) {
            Text(
                text = subText,
                style = VynceTheme.typography.body.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (isActive) Color.White.copy(alpha = 0.6f) else VynceTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun KaraokeWord(
    word: WordTimestamp,
    currentPosition: Long,
    isActive: Boolean
) {
    val progress = remember(currentPosition, word) {
        val currentTimeSec = currentPosition / 1000.0
        when {
            currentTimeSec < word.startTime -> 0f
            currentTimeSec > word.endTime -> 1f
            else -> ((currentTimeSec - word.startTime) / (word.endTime - word.startTime)).toFloat()
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(150),
        label = "wordProgress"
    )

    Box(modifier = Modifier.padding(end = if (word.hasTrailingSpace) 10.dp else 0.dp)) {
        // Base text (semi-transparent)
        Text(
            text = word.text,
            style = VynceTheme.typography.body.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp
            ),
            color = if (isActive) Color.White.copy(alpha = 0.2f) else VynceTheme.colors.textPrimary.copy(alpha = 0.3f)
        )

        // Filled text (bright) with clipping
        Text(
            text = word.text,
            style = VynceTheme.typography.body.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp
            ),
            color = if (isActive) Color.White else VynceTheme.colors.textPrimary,
            modifier = Modifier
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    val width = size.width * animatedProgress
                    drawRect(
                        color = Color.Black,
                        size = Size(width, size.height),
                        blendMode = BlendMode.DstIn
                    )
                    drawContent()
                }
        )
    }
}
