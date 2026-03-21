package com.vynce.music.ui.screens.player

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.formatTime

@Composable
fun PlayerScreen(
    onDismiss: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = VynceTheme.colors

    // Local seek state
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    val animatedSliderPosition by animateFloatAsState(
        targetValue = if (isSeeking) sliderPosition else {
            if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration else 0f
        },
        animationSpec = tween(if (isSeeking) 0 else 250),
        label = "slider"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(colors.primary.copy(0.15f), colors.background))
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /** TOP BAR **/
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = colors.textPrimary, modifier = Modifier.size(32.dp))
                }
                Text(
                    "NOW PLAYING",
                    style = VynceTheme.typography.label.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                    color = colors.textSecondary
                )
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreHoriz, null, tint = colors.textPrimary, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            /** ARTWORK **/
            Box(
                modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize().shadow(40.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surface
                ) {
                    AsyncImage(
                        model = uiState.currentMediaItem?.mediaMetadata?.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (uiState.isBuffering) {
                    WaveLoadingIndicator(modifier = Modifier.size(80.dp), color = Color.White)
                }
            }

            Spacer(Modifier.height(24.dp))

            /** INFO **/
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Unknown Title",
                    style = VynceTheme.typography.title.copy(fontSize = 28.sp, fontWeight = FontWeight.Black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.textPrimary
                )
                Text(
                    text = uiState.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist",
                    style = VynceTheme.typography.body.copy(fontSize = 18.sp),
                    color = colors.primary
                )
            }

            Spacer(Modifier.height(32.dp))

            /** SEEKBAR **/
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(uiState.currentPosition), style = VynceTheme.typography.label, color = colors.textSecondary)
                Text(formatTime(uiState.duration), style = VynceTheme.typography.label, color = colors.textSecondary)
            }

            Spacer(Modifier.height(24.dp))

            /** CONTROLS **/
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle, null,
                        tint = if (uiState.shuffleEnabled) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(onClick = { viewModel.skipPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(48.dp), tint = colors.textPrimary)
                    }

                    Surface(
                        modifier = Modifier.size(88.dp).shadow(20.dp, CircleShape).clip(CircleShape)
                            .clickable { viewModel.togglePlayPause() },
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.isBuffering) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = colors.background, strokeWidth = 3.dp)
                            }
                            Icon(
                                if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null, modifier = Modifier.size(48.dp), tint = colors.background
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.skipNext() }) {
                        Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(48.dp), tint = colors.textPrimary)
                    }
                }

                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    val icon = when (uiState.repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    Icon(icon, null, tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) colors.primary else colors.textSecondary, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun WaveLoadingIndicator(modifier: Modifier = Modifier, color: Color = Color.White) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val animScale by infiniteTransition.animateFloat(
        1f, 1.5f,
        infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "scale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().scale(animScale).background(color.copy(0.2f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.7f).scale(animScale * 0.8f).background(color.copy(0.4f), CircleShape))
        CircularProgressIndicator(color = color, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
    }
}
