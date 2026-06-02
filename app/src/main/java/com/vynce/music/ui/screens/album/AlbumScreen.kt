package com.vynce.music.ui.screens.album

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vynce.music.ui.components.ListItem
import com.vynce.music.ui.components.MoreOptionsSheet
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.shareText
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.Artist
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.pages.AlbumPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    id: String,
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    viewModel: AlbumViewModel = hiltViewModel()
) {
    val album by viewModel.album.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    var showMoreOptions by remember { mutableStateOf(false) }
    var selectedItemForOptions by remember { mutableStateOf<SongItem?>(null) }

    LaunchedEffect(id) {
        viewModel.fetchAlbum(id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VynceTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = VynceTheme.colors.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = VynceTheme.colors.primary
                )
            } else if (album != null) {
                AlbumContent(
                    album = album!!,
                    onPlayClick = {
                        val songs = album!!.songs
                        if (songs.isNotEmpty()) {
                            playerViewModel.play(songs.first().toMediaItem())
                            songs.drop(1).forEach { song ->
                                playerViewModel.addToQueue(song.toMediaItem())
                            }
                        }
                    },
                    onShuffleClick = {
                        val songs = album!!.songs.shuffled()
                        if (songs.isNotEmpty()) {
                            playerViewModel.play(songs.first().toMediaItem())
                            songs.drop(1).forEach { song ->
                                playerViewModel.addToQueue(song.toMediaItem())
                            }
                        }
                    },
                    onLikeClick = { id, like ->
                        viewModel.toggleLike(id, like)
                    },
                    onTrackClick = { song ->
                        playerViewModel.play(song.toMediaItem())
                    },
                    onMoreClick = { song ->
                        selectedItemForOptions = song
                        showMoreOptions = true
                    },
                    onSwipeRight = { song ->
                        playerViewModel.addToQueue(song.toMediaItem())
                    }
                )
            }

            if (showMoreOptions && selectedItemForOptions != null) {
                val item = selectedItemForOptions!!
                val metadata = item.toMediaItem().mediaMetadata
                val albumId = metadata.extras?.getString("album_id")
                val artistId = metadata.extras?.getString("artist_id")

                MoreOptionsSheet(
                    title = item.title,
                    subtitle = item.artists.joinToString { it.name },
                    thumbnailUrl = item.thumbnail,
                    onDismiss = { showMoreOptions = false },
                    onAddToPlaylist = { 
                        Toast.makeText(context, "Added to playlist (Simulated)", Toast.LENGTH_SHORT).show()
                    },
                    onViewAlbum = albumId?.let { id -> { onNavigateToAlbum(id) } },
                    onGoToArtist = artistId?.let { id -> { onNavigateToArtist(id) } },
                    onShare = { 
                        shareText(context, item.shareLink)
                    }
                )
            }
        }
    }
}

@Composable
fun AlbumContent(
    album: AlbumPage,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onTrackClick: (SongItem) -> Unit,
    onMoreClick: (SongItem) -> Unit,
    onLikeClick: (String, Boolean) -> Unit = { _, _ -> },
    onSwipeRight: (SongItem) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            AlbumHeader(
                album = album.album,
                onPlayClick = onPlayClick,
                onShuffleClick = onShuffleClick,
                onLikeClick = onLikeClick
            )
        }

        items(album.songs) { song ->
            ListItem(
                item = song,
                onClick = { onTrackClick(song) },
                onMoreClick = { onMoreClick(song) }
            )
        }
    }
}

@Composable
fun AlbumHeader(
    album: AlbumItem,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onLikeClick: (String, Boolean) -> Unit = { _, _ -> }
) {
    var isLiked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(VynceTheme.colors.surface)
        ) {
            AsyncImage(
                model = album.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.4f))
                        )
                    )
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = album.title,
            style = VynceTheme.typography.title.copy(fontSize = 28.sp, fontWeight = FontWeight.Black),
            color = VynceTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Album",
                style = VynceTheme.typography.label,
                color = VynceTheme.colors.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " • ",
                style = VynceTheme.typography.label,
                color = VynceTheme.colors.textSecondary
            )
            Text(
                text = album.artists?.joinToString { it.name } ?: "Unknown Artist",
                style = VynceTheme.typography.body.copy(fontSize = 14.sp),
                color = VynceTheme.colors.textSecondary
            )
            album.year?.let {
                 Text(
                    text = " • $it",
                    style = VynceTheme.typography.body.copy(fontSize = 14.sp),
                    color = VynceTheme.colors.textSecondary
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayClick,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VynceTheme.colors.primary,
                    contentColor = VynceTheme.colors.onPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play", style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold))
            }

            IconButton(
                onClick = onShuffleClick,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VynceTheme.colors.surface),
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = VynceTheme.colors.primary
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { 
                isLiked = !isLiked
                onLikeClick(album.browseId, isLiked)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLiked) VynceTheme.colors.surface else VynceTheme.colors.primary,
                contentColor = if (isLiked) VynceTheme.colors.textPrimary else VynceTheme.colors.onPrimary
            )
        ) {
            Text(if (isLiked) "Liked" else "Like Album", fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun AlbumHeaderPreview() {
    VynceTheme {
        AlbumHeader(
            album = AlbumItem(
                browseId = "1",
                playlistId = "1",
                title = "Starboy",
                artists = listOf(Artist(name = "The Weekend", id = null)),
                year = 2016,
                thumbnail = "https://example.com/thumb.jpg",
                explicit = true
            ),
            onPlayClick = {},
            onShuffleClick = {}
        )
    }
}















