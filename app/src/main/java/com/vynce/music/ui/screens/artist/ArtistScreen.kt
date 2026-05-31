package com.vynce.music.ui.screens.artist

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.vynce.music.ui.components.ListItem
import com.vynce.music.ui.components.MediaItemCard
import com.vynce.music.ui.components.SectionHeader
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.pages.ArtistPage

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    id: String,
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val artist by viewModel.artist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(id) {
        viewModel.fetchArtist(id)
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
            } else if (artist != null) {
                val context = LocalContext.current
                ArtistContent(
                    artist = artist!!,
                    onPlayClick = {
                        artist!!.artist.playEndpoint?.let {
                            playerViewModel.playQueue(it)
                        } ?: artist!!.sections.firstOrNull { it.title == "Songs" }?.items?.filterIsInstance<SongItem>()?.let { songs ->
                            if (songs.isNotEmpty()) {
                                playerViewModel.playAll(songs.map { it.toMediaItem() })
                            }
                        }
                    },
                    onShuffleClick = {
                        artist!!.artist.shuffleEndpoint?.let {
                            playerViewModel.playQueue(it)
                        } ?: run {
                            Toast.makeText(context, "No songs to shuffle", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onItemClick = { item ->
                        when (item) {
                            is SongItem -> playerViewModel.play(item.toMediaItem())
                            is AlbumItem -> onAlbumClick(item.browseId)
                            else -> {}
                        }
                    },
                    onSubscribeClick = { channelId, subscribe ->
                        viewModel.toggleSubscription(channelId, subscribe)
                    },
                    onSwipeRight = { item ->
                        if (item is SongItem) {
                            playerViewModel.addToQueue(item.toMediaItem())
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ArtistContent(
    artist: ArtistPage,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onItemClick: (Any) -> Unit,
    onSubscribeClick: (String, Boolean) -> Unit = { _, _ -> },
    onSwipeRight: (Any) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            ArtistHeader(
                artist = artist,
                onPlayClick = onPlayClick,
                onShuffleClick = onShuffleClick,
                onSubscribeClick = onSubscribeClick
            )
        }

        artist.sections.forEach { section ->
            item {
                SectionHeader(title = section.title)
            }
            
            if (section.items.firstOrNull() is AlbumItem) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(section.items.filterIsInstance<AlbumItem>()) { album ->
                            MediaItemCard(
                                title = album.title,
                                subtitle = album.year?.toString() ?: "",
                                imageUrl = album.thumbnail,
                                onClick = { onItemClick(album) }
                            )
                        }
                    }
                }
            } else {
                items(section.items) { item ->
                    ListItem(
                        item = item,
                        onClick = { onItemClick(item) },
                        onSwipeRight = { onSwipeRight(item) }
                    )
                }
            }
        }
        
        if (!artist.description.isNullOrBlank()) {
            item {
                SectionHeader(title = "About")
                Text(
                    text = artist.description!!,
                    style = VynceTheme.typography.body.copy(fontSize = 14.sp),
                    color = VynceTheme.colors.textSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ArtistHeader(
    artist: ArtistPage,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onSubscribeClick: (String, Boolean) -> Unit = { _, _ -> }
) {
    var isSubscribed by remember(artist) { mutableStateOf(artist.isSubscribed) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.artist.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = artist.artist.title,
            style = VynceTheme.typography.title.copy(fontSize = 32.sp, fontWeight = FontWeight.Black),
            color = VynceTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        
        artist.subscriberCountText?.let {
            Text(
                text = it,
                style = VynceTheme.typography.label,
                color = VynceTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Artist",
            style = VynceTheme.typography.label,
            color = VynceTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

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
                isSubscribed = !isSubscribed
                onSubscribeClick(artist.artist.id, isSubscribed)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSubscribed) VynceTheme.colors.surface else VynceTheme.colors.primary,
                contentColor = if (isSubscribed) VynceTheme.colors.textPrimary else VynceTheme.colors.onPrimary
            )
        ) {
            Text(if (isSubscribed) "Subscribed" else "Subscribe", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ArtistHeaderPreview() {
    VynceTheme {
        ArtistHeader(
            artist = ArtistPage(
                artist = ArtistItem(
                    id = "1",
                    title = "The Weeknd",
                    thumbnail = "https://example.com/thumb.jpg",
                    shuffleEndpoint = null,
                    radioEndpoint = null
                ),
                sections = emptyList(),
                description = "Bio here",
                subscriberCountText = "100M subscribers",
                isSubscribed = false
            ),
            onPlayClick = {},
            onShuffleClick = {}
        )
    }
}












