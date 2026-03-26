package com.vynce.music.ui.commponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vynce.music.ui.screens.player.shimmerEffect
import com.vynce.music.ui.theme.VynceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsSheet(
    title: String,
    subtitle: String,
    thumbnailUrl: String,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onViewAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onShare: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = VynceTheme.colors
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(40.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textSecondary.copy(alpha = 0.3f))
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp + navBarPadding)
        ) {
            // Header: Track Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .shimmerEffect(true),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = VynceTheme.typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = VynceTheme.typography.body.copy(fontSize = 14.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = colors.textSecondary.copy(alpha = 0.1f)
            )

            // Options List
            OptionItem(
                title = "Add to Playlist",
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                onClick = {
                    onAddToPlaylist()
                    onDismiss()
                }
            )

            if (onViewAlbum != null) {
                OptionItem(
                    title = "View Album",
                    icon = Icons.Default.Album,
                    onClick = {
                        onViewAlbum()
                        onDismiss()
                    }
                )
            }

            if (onGoToArtist != null) {
                OptionItem(
                    title = "Go to Artist",
                    icon = Icons.Default.Person,
                    onClick = {
                        onGoToArtist()
                        onDismiss()
                    }
                )
            }

            OptionItem(
                title = "Share",
                icon = Icons.Default.Share,
                onClick = {
                    onShare()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun OptionItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = VynceTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = colors.primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = VynceTheme.typography.body.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            color = colors.textPrimary
        )
    }
}
