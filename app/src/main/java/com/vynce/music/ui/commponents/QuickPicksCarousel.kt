package com.vynce.music.ui.commponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.YTItem

@Composable
fun QuickPicksCarousel(
    title: String,
    items: List<YTItem>,
    onItemClick: (YTItem) -> Unit = {},
    onItemMoreClick: (YTItem) -> Unit = {},
    onPlayAllClick: () -> Unit = {}
) {
    Column {
        SectionHeader(title, onPlayAllClick = onPlayAllClick)
        
        val chunkedItems = items.chunked(4)
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chunkedItems) { columnItems ->
                Column(
                    modifier = Modifier.width(300.dp)
                ) {
                    columnItems.forEach { item ->
                        ListItem(
                            item = item,
                            onClick = { onItemClick(item) },
                            onMoreClick = { onItemMoreClick(item) },
                            modifier = Modifier.padding(horizontal = 0.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CarouselList(
    title: String,
    items: List<YTItem>,
    onItemClick: (YTItem) -> Unit = {},
    onMoreClick: (YTItem) -> Unit = {},
    onPlayAllClick: () -> Unit = {}
) {
    Column {
        SectionHeader(title, onPlayAllClick = onPlayAllClick)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                val subtitle = when (item) {
                    is SongItem -> item.artists.joinToString { it.name }
                    is AlbumItem -> item.artists?.joinToString { it.name } ?: ""
                    is PlaylistItem -> item.author?.name ?: ""
                    is ArtistItem -> "Artist"
                }
                MediaItemCard(
                    title = item.title,
                    subtitle = subtitle,
                    imageUrl = item.thumbnail,
                    onClick = { onItemClick(item) },
                    onLongClick = { onMoreClick(item) }
                )
            }
        }
    }
}
@Composable
fun MediaItemCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    imageUrl: String? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(VynceTheme.shapes.medium)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = imageVector ?: Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = VynceTheme.colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = VynceTheme.typography.body.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = VynceTheme.colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = subtitle,
                style = VynceTheme.typography.label.copy(fontSize = 12.sp),
                color = VynceTheme.colors.textSecondary,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    elevation: Dp = 6.dp,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = VynceTheme.colors.surface,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(cornerRadius))
            .padding(elevation)
    ) {
        content()
    }
}

@Composable
fun MediaItem(
    title: String,
    subtitle: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = VynceTheme.colors.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = VynceTheme.typography.body,
                color = VynceTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                style = VynceTheme.typography.label,
                color = VynceTheme.colors.textSecondary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0)
@Composable
private fun CardPreview() {

}