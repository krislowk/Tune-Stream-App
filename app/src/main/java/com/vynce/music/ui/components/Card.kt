package com.vynce.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.shimmerEffect

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
    Column(
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
                    .aspectRatio(1f)
                    .fillMaxSize()
                    .clip(VynceTheme.shapes.medium)
                    .background(Color.Black)
                    .shimmerEffect(),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
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