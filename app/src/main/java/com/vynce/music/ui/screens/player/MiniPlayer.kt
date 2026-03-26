package com.vynce.music.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vynce.music.ui.theme.VynceTheme

@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel,
    onClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val song = uiState.currentTrack ?: return
    val colors = VynceTheme.colors

    val progress = if (uiState.duration > 0) {
        uiState.currentPosition.toFloat() / uiState.duration
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "mini_player_progress"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        color = VynceTheme.colors.background,
        tonalElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Progress Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(colors.primary.copy(alpha = 0.05f))
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.mediaMetadata.artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.mediaMetadata.title?.toString() ?: "Unknown Title",
                        style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                        style = VynceTheme.typography.label.copy(fontSize = 12.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(48.dp)
                ) {
                    if (uiState.isBuffering) {
                        CircularProgressIndicator(
                            color = colors.primary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            // Bottom Progress Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomStart)
                    .background(colors.glassBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(colors.primary)
                )
            }
        }
    }
}
