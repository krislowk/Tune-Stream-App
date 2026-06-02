package com.vynce.music.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vynce.music.db.entities.Playlist
import com.vynce.music.ui.screens.library.LibraryUiState
import com.vynce.music.ui.screens.library.LibraryViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.shimmerEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    onDismiss: () -> Unit,
    onPlaylistSelected: (String) -> Unit,
    libraryViewModel: LibraryViewModel
) {
    val uiState by libraryViewModel.uiState.collectAsState()
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
            Text(
                text = "Add to Playlist",
                style = VynceTheme.typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 16.dp),
                thickness = 0.5.dp,
                color = colors.textSecondary.copy(alpha = 0.1f)
            )

            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                is LibraryUiState.Success -> {
                    val playlists = state.items.filterIsInstance<Playlist>()
                    if (playlists.isEmpty()) {
                        Text(
                            text = "No playlists found",
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(400.dp)) {
                            items(playlists) { playlist ->
                                PlaylistOptionItem(
                                    playlist = playlist,
                                    onClick = {
                                        onPlaylistSelected(playlist.id)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {
                    Text("Error loading playlists", color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun PlaylistOptionItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    val colors = VynceTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.thumbnails.firstOrNull(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .shimmerEffect(true),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = playlist.playlist.name,
                style = VynceTheme.typography.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.songCount} songs",
                style = VynceTheme.typography.label.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



