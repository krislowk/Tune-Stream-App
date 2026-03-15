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
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.formatTime
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    onDismiss: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    val animatedSliderPosition by animateFloatAsState(
        targetValue = sliderPosition,
        animationSpec = tween(
            durationMillis = 250
        ),
        label = "sliderAnimation"
    )

    val colors = VynceTheme.colors

    /**
     * Sync slider with player position
     */
    LaunchedEffect(currentPosition, duration) {
        if (!isSeeking && duration > 0) {
            sliderPosition = currentPosition.toFloat() / duration.toFloat()
            delay(300)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.primary.copy(alpha = 0.15f),
                        colors.background
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /**
             * TOP BAR
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    "NOW PLAYING",
                    style = VynceTheme.typography.label.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textSecondary
                )

                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            /**
             * ARTWORK
             */
            Surface(
                modifier = Modifier
                    .fillMaxWidth(.9f)
                    .aspectRatio(1f)
                    .shadow(40.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = colors.surface
            ) {

                AsyncImage(
                    model = currentSong?.mediaMetadata?.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(24.dp))

            /**
             * TITLE + ARTIST
             */
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {

                Text(
                    text = currentSong?.mediaMetadata?.title.toString() ?: "",
                    style = VynceTheme.typography.title.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.textPrimary
                )

                Text(
                    text = currentSong?.mediaMetadata?.artist.toString() ?: "Unknown",
                    style = VynceTheme.typography.body.copy(fontSize = 18.sp),
                    color = colors.primary
                )
            }

            Spacer(Modifier.height(32.dp))

            /**
             * PROGRESS SLIDER
             */
            Slider(
                value = animatedSliderPosition,
                onValueChange = {
                    sliderPosition = it
                    isSeeking = true
                },
                onValueChangeFinished = {

                    val seekPosition =
                        (sliderPosition * duration).toLong()

                    viewModel.seekTo(seekPosition)

                    isSeeking = false
                },
                modifier = Modifier.fillMaxWidth()
            )

            /**
             * TIME LABELS
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    formatTime(currentPosition),
                    style = VynceTheme.typography.label,
                    color = colors.textSecondary
                )

                Text(
                    formatTime(duration),
                    style = VynceTheme.typography.label,
                    color = colors.textSecondary
                )
            }

            Spacer(Modifier.height(24.dp))

            /**
             * PLAYER CONTROLS
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    IconButton(onClick = {  }) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colors.textPrimary
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(88.dp)
                            .shadow(20.dp, CircleShape)
                            .clickable { viewModel.togglePlayPause() },
                        shape = CircleShape,
                        color = Color.White
                    ) {

                        Box(contentAlignment = Alignment.Center) {

                            Icon(
                                if (isPlaying)
                                    Icons.Default.Pause
                                else
                                    Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    IconButton(onClick = {  }) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colors.textPrimary
                        )
                    }
                }

                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
private fun PlayerPreview() {
    VynceTheme {
        PlayerScreen(
            viewModel = viewModel(),
            onDismiss = {})

    }
}