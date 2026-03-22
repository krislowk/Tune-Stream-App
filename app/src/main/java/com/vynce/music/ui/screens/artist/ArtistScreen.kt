package com.vynce.music.ui.screens.artist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vynce.music.ui.commponents.ListItem
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.SongItem

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
                title = { Text("Artist", color = VynceTheme.colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (artist != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = artist!!.artist.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = artist!!.artist.title,
                                style = VynceTheme.typography.title,
                                color = VynceTheme.colors.textPrimary
                            )
                        }
                    }
                    artist!!.sections.forEach { section ->
                        item {
                            Text(
                                text = section.title,
                                style = VynceTheme.typography.title,
                                modifier = Modifier.padding(16.dp),
                                color = VynceTheme.colors.textPrimary
                            )
                        }
                        items(section.items) { item ->
                            ListItem(
                                item = item,
                                onClick = {
                                    when (item) {
                                        is SongItem -> playerViewModel.play(item.toMediaItem())
                                        is AlbumItem -> onAlbumClick(item.browseId)
                                        else -> {}
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
