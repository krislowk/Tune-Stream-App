package com.vynce.music.ui.commponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vynce.music.ui.theme.VynceTheme

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
    BottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(VynceTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = VynceTheme.typography.title.copy(fontSize = 18.sp),
                    color = VynceTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = VynceTheme.typography.body.copy(fontSize = 14.sp),
                    color = VynceTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        MediaItem(
            title = "Add to Playlist",
            subtitle = "Add this song to one of your playlists",
            leadingIcon = Icons.AutoMirrored.Filled.PlaylistAdd,
            onClick = {
                onAddToPlaylist()
                onDismiss()
            }
        )

        if (onViewAlbum != null) {
            MediaItem(
                title = "View Album",
                subtitle = "See all songs from this album",
                leadingIcon = Icons.Default.Album,
                onClick = {
                    onViewAlbum()
                    onDismiss()
                }
            )
        }

        if (onGoToArtist != null) {
            MediaItem(
                title = "Go to Artist",
                subtitle = "See more from this artist",
                leadingIcon = Icons.Default.Person,
                onClick = {
                    onGoToArtist()
                    onDismiss()
                }
            )
        }

        MediaItem(
            title = "Share",
            subtitle = "Share this song with others",
            leadingIcon = Icons.Default.Share,
            onClick = {
                onShare()
                onDismiss()
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismissRequest() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {},
            cornerRadius = 32.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(VynceTheme.colors.textSecondary.copy(alpha = 0.3f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                content()
            }
        }
    }
}