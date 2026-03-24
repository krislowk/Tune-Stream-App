package com.vynce.music.ui.screens.search

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vynce.music.ui.commponents.CarouselList
import com.vynce.music.ui.commponents.ListItem
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onItemClick: (String, String?) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val exploreData by viewModel.exploreData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VynceTheme.colors.background)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                TextField(
                    value = query,
                    onValueChange = { viewModel.updateQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = { Text("Search songs, artists, albums", color = VynceTheme.colors.textSecondary) },
                    leadingIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = VynceTheme.colors.textPrimary
                            )
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateQuery("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = VynceTheme.colors.textPrimary
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.search(query)
                        focusManager.clearFocus()
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VynceTheme.colors.surface,
                        unfocusedContainerColor = VynceTheme.colors.surface,
                        disabledContainerColor = VynceTheme.colors.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = VynceTheme.colors.primary,
                        focusedTextColor = VynceTheme.colors.textPrimary,
                        unfocusedTextColor = VynceTheme.colors.textPrimary
                    ),
                    singleLine = true
                )
            }
        },
        containerColor = VynceTheme.colors.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = VynceTheme.colors.primary
                )
            } else if (query.isEmpty()) {
                // Show Explore Content
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    exploreData?.let { data ->
                        item {
                            CarouselList(
                                title = "New Releases",
                                items = data.newReleaseAlbums,
                                onItemClick = { onItemClick("album", (it as AlbumItem).browseId) }
                            )
                        }

                        item {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Moods & Genres",
                                style = VynceTheme.typography.title.copy(fontSize = 22.sp),
                                color = VynceTheme.colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(data.moodAndGenres) { mood ->
                                    Box(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(mood.stripeColor).copy(alpha = 0.2f))
                                            .clickable { /* TODO: Navigate to mood endpoint */ }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        Text(
                                            text = mood.title,
                                            style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                                            color = VynceTheme.colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (searchResult != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResult!!.items) { item ->
                        ListItem(
                            item = item,
                            onClick = {
                                when (item) {
                                    is SongItem -> playerViewModel.play(item.toMediaItem())
                                    is AlbumItem -> onItemClick("album", item.browseId)
                                    is ArtistItem -> onItemClick("artist", item.id)
                                    is PlaylistItem -> onItemClick("playlist", item.id)
                                }
                            }
                        )
                    }
                }
            } else if (suggestions != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(suggestions!!.queries) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.search(suggestion)
                                    focusManager.clearFocus()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = VynceTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                suggestion,
                                color = VynceTheme.colors.textPrimary,
                                style = VynceTheme.typography.body
                            )
                        }
                    }
                }
            }
        }
    }
}
