package com.vynce.music.ui.screens.playlist

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
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    id: String,
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(id) {
        viewModel.fetchPlaylist(id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlist", color = VynceTheme.colors.textPrimary) },
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
            } else if (playlist != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = playlist!!.playlist.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = playlist!!.playlist.title,
                                style = VynceTheme.typography.title,
                                color = VynceTheme.colors.textPrimary
                            )
                            Text(
                                text = playlist!!.playlist.author?.name ?: "",
                                style = VynceTheme.typography.body,
                                color = VynceTheme.colors.textSecondary
                            )
                        }
                    }
                    items(playlist!!.songs) { song ->
                        ListItem(
                            item = song,
                            onClick = { playerViewModel.play(song.toMediaItem()) }
                        )
                    }
                }
            }
        }
    }
}
