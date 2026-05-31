package com.vynce.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.shimmerEffect
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.YTItem

@Composable
fun CarouselColumn(
    title: String,
    items: List<YTItem>,
    onItemClick: (YTItem) -> Unit = {},
    onItemMoreClick: (YTItem) -> Unit = {},
    onPlayAllClick: () -> Unit = {},
) {
    Column {
        SectionHeader(title, onPlayAllClick = onPlayAllClick)

        val chunkedItems = remember(items) { items.chunked(4) }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = chunkedItems,
                key = { it.firstOrNull()?.id ?: it.hashCode() }
            ) { columnItems ->
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
fun CarouselRow(
    title: String,
    items: List<YTItem>,
    onItemClick: (YTItem) -> Unit = {},
    onMoreClick: (YTItem) -> Unit = {},
    onPlayAllClick: () -> Unit = {}
) {
    Column() {
        SectionHeader(
            title = title,
            onPlayAllClick = onPlayAllClick
        )

        LazyRow(
            Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                val subtitle = remember(item) {
                    when (item) {
                        is SongItem -> item.artists.joinToString { it.name }
                        is AlbumItem -> item.artists?.joinToString { it.name } ?: ""
                        is PlaylistItem -> item.author?.name ?: ""
                        is ArtistItem -> "Artist"
                        else -> ""
                    }
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
fun ArtistCarousel(
    title: String,
    items: List<YTItem>,
    onItemClick: (YTItem) -> Unit = {}
) {
    Column {
        SectionHeader(title)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                ArtistItem(
                    name = item.title,
                    imageUrl = item.thumbnail,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
fun ArtistItem(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            loading = { Box(modifier = Modifier.fillMaxSize().shimmerEffect()) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = VynceTheme.typography.label.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = VynceTheme.colors.textPrimary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onMoreClick: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit) = {},
    onSwipeLeft: (() -> Unit) = {},
) {
    SwipeableItem(
        onSwipeRight = onSwipeRight,
        onSwipeLeft = onSwipeLeft
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(VynceTheme.colors.background)
                .clickable { onClick() }
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(VynceTheme.shapes.small)
                    .background(VynceTheme.colors.textPrimary.copy(0.05f)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = VynceTheme.typography.body.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = VynceTheme.colors.textPrimary,
                    maxLines = 1
                )
                val subtitle = remember(item) {
                    when (item) {
                        is SongItem -> item.artists.joinToString { it.name }
                        is AlbumItem -> item.artists?.joinToString { it.name } ?: ""
                        is PlaylistItem -> item.author?.name ?: ""
                        is ArtistItem -> "Artist"
                        else -> ""
                    }
                }

                Text(
                    text = subtitle,
                    style = VynceTheme.typography.label.copy(fontSize = 10.sp),
                    color = VynceTheme.colors.textSecondary,
                    maxLines = 1
                )
            }

            if (onMoreClick != null) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = VynceTheme.colors.textSecondary.copy(0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityCarousel(
    title: String,
    items: List<YTItem>,
    onItemClick: (YTItem) -> Unit = {},
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        SectionHeader(title)

        val chunkedItems = remember(items) { items.chunked(5) }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = chunkedItems,
                key = { it.firstOrNull()?.id ?: it.hashCode() }
            ) { columnItems ->
                CommunityItem(
                    items = columnItems,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

@Composable
fun CommunityItem(
    items: List<YTItem>,
    onItemClick: (YTItem) -> Unit
) {
    val colors = listOf(
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF00BCD4),
        Color(0xFF009688), Color(0xFF4CAF50), Color(0xFFFF9800)
    )
    val tintColor = remember(items.firstOrNull()?.id) {
        val hash = items.firstOrNull()?.id?.hashCode() ?: 0
        colors[(hash % colors.size).let { if (it < 0) it + colors.size else it }]
    }

    Column(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(tintColor.copy(alpha = 0.12f))
            .padding(16.dp)
    ) {
        // Main thumbnail for the group
        SubcomposeAsyncImage(
            model = items.firstOrNull()?.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop,
            loading = { Box(modifier = Modifier.fillMaxSize().shimmerEffect()) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        items.take(5).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = VynceTheme.typography.body.copy(
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = VynceTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                val artistName = when (item) {
                    is SongItem -> item.artists.firstOrNull()?.name
                    is AlbumItem -> item.artists?.firstOrNull()?.name
                    is PlaylistItem -> item.author?.name
                    else -> null
                }
                
                if (artistName != null) {
                    Text(
                        text = artistName,
                        style = VynceTheme.typography.label.copy(fontSize = 10.sp),
                        color = VynceTheme.colors.textSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
