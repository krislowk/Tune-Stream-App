package com.vynce.music.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vynce.music.lyrics.LyricsEntry
import com.vynce.music.lyrics.WordTimestamp
import com.vynce.music.ui.theme.VynceTheme
import kotlin.math.sin

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ImmersiveKaraokeScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = VynceTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Cinematic Background
        CinematicBackground(
            artworkUri = uiState.currentTrack?.mediaMetadata?.artworkUri?.toString(),
            isPlaying = uiState.isPlaying
        )

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding()
        ) {
            // 2. Top Header (Minimal)
            TopHeader(
                title = uiState.currentTrack?.mediaMetadata?.title?.toString() ?: "Unknown",
                artist = uiState.currentTrack?.mediaMetadata?.artist?.toString() ?: "Unknown",
                onClose = onClose
            )

            // 3. Hero Lyrics Area
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LyricsHeroSection(
                    lyrics = uiState.lyrics,
                    currentPosition = uiState.currentPosition,
                    isPlaying = uiState.isPlaying
                )
            }

            // 4. Floating Control Bar (Glassmorphism)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassyControlBar(
                    isPlaying = uiState.isPlaying,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.skipNext() },
                    onSkipPrevious = { viewModel.skipPrevious() },
                    progress = if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration else 0f,
                    onSeek = { viewModel.seekTo((it * uiState.duration).toLong()) }
                )
            }
        }

        // 5. Smart Features Overlay
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            SmartFeatureOverlay()
        }
    }
}

@Composable
fun CinematicBackground(artworkUri: String?, isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred Album Art Base
        AsyncImage(
            model = artworkUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.2f)
                .blur(80.dp),
            contentScale = ContentScale.Crop
        )

        // Reactive Glow Layer
        val primaryColor = VynceTheme.colors.primary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Floating Particles
            repeat(15) { index ->
                val x = (canvasWidth * 0.5f) + (canvasWidth * 0.4f) * sin(phase + index * 1.5f)
                val y = (canvasHeight * 0.5f) + (canvasHeight * 0.4f) * sin(phase * 0.7f + index)
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(x, y),
                        radius = 300f
                    ),
                    radius = 300f,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }

        // Dark Gradient Overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
    }
}

@Composable
fun TopHeader(title: String, artist: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = VynceTheme.typography.title.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White
            )
            Text(
                text = artist.uppercase(),
                style = VynceTheme.typography.label.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                ),
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LyricsHeroSection(
    lyrics: List<LyricsEntry>,
    currentPosition: Long,
    isPlaying: Boolean
) {
    val listState = rememberLazyListState()
    
    val activeIndex = remember(lyrics, currentPosition) {
        val index = lyrics.indexOfLast { it.time <= currentPosition }
        if (index == -1) 0 else index
    }

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            listState.animateScrollToItem(activeIndex, scrollOffset = -200)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                // Cinematic Fade Edges
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to Color.Black,
                        0.7f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        contentPadding = PaddingValues(vertical = 300.dp),
        userScrollEnabled = false
    ) {
        items(lyrics.size) { index ->
            val entry = lyrics[index]
            val isActive = index == activeIndex
            val isPast = index < activeIndex
            
            HeroLyricLine(
                entry = entry,
                isActive = isActive,
                isPast = isPast,
                currentPosition = currentPosition
            )
        }
    }
}

@Composable
fun HeroLyricLine(
    entry: LyricsEntry,
    isActive: Boolean,
    isPast: Boolean,
    currentPosition: Long
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            isPast -> 0.3f
            else -> 0.15f
        },
        animationSpec = tween(600),
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 24.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!entry.words.isNullOrEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                entry.words.forEach { word ->
                    HeroWord(
                        word = word,
                        currentPosition = currentPosition,
                        isActiveLine = isActive
                    )
                }
            }
        } else {
            Text(
                text = entry.text,
                style = VynceTheme.typography.title.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp
                ),
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun HeroWord(
    word: WordTimestamp,
    currentPosition: Long,
    isActiveLine: Boolean
) {
    val currentTimeSec = currentPosition / 1000.0
    val isCurrent = currentTimeSec >= word.startTime && currentTimeSec <= word.endTime
    val isPast = currentTimeSec > word.endTime

    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.2f else 1f,
        animationSpec = tween(200),
        label = "wordScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isCurrent) 0.8f else 0f,
        animationSpec = tween(200),
        label = "glow"
    )

    Box(
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Neon Background for current word
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .blur(12.dp)
                    .graphicsLayer { alpha = glowAlpha }
            ) {
                Text(
                    text = word.text,
                    style = VynceTheme.typography.title.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = VynceTheme.colors.primary
                    )
                )
            }
        }

        Text(
            text = word.text,
            style = VynceTheme.typography.title.copy(
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                brush = if (isCurrent) {
                    Brush.linearGradient(
                        colors = listOf(VynceTheme.colors.primary, VynceTheme.colors.accent)
                    )
                } else null
            ),
            color = when {
                isCurrent -> Color.Unspecified
                isPast -> Color.White.copy(alpha = 0.6f)
                else -> Color.White.copy(alpha = 0.3f)
            },
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

@Composable
fun GlassyControlBar(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    progress: Float,
    onSeek: (Float) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(50.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50.dp)),
        color = Color.Black.copy(alpha = 0.4f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // Slider (Minimal Neon Line)
            Slider(
                value = progress,
                onValueChange = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = VynceTheme.colors.primary,
                    activeTrackColor = VynceTheme.colors.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrevious) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(VynceTheme.colors.primary, VynceTheme.colors.accent)
                            )
                        )
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onSkipNext) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun SmartFeatureOverlay() {
    var showFeatures by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = showFeatures,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureToggle(Icons.Default.Mic, "Karaoke Mode", true)
                FeatureToggle(Icons.Default.Speed, "Practice (0.8x)", false)
                FeatureToggle(Icons.Default.AutoFixHigh, "AI Sync", true)
            }
        }

        IconButton(
            onClick = { showFeatures = !showFeatures },
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                imageVector = if (showFeatures) Icons.Default.KeyboardArrowDown else Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun FeatureToggle(icon: ImageVector, label: String, isActive: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) VynceTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = if (isActive) VynceTheme.colors.primary else Color.White, modifier = Modifier.size(18.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        maxItemsInEachRow = maxItemsInEachRow,
        content = content
    )
}

private fun expandVertically() = androidx.compose.animation.expandVertically()
private fun shrinkVertically() = androidx.compose.animation.shrinkVertically()
