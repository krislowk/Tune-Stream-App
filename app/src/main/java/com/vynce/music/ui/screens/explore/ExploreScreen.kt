package com.vynce.music.ui.screens.explore

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vynce.music.ui.commponents.CarouselList
import com.vynce.music.ui.commponents.SectionHeader
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.pages.MoodAndGenres

@Composable
fun ExploreScreen(
    onSearchClick: () -> Unit,
    onItemClick: (String, String?) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val exploreData by viewModel.exploreData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val colors = VynceTheme.colors

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Explore",
                    style = VynceTheme.typography.title.copy(fontSize = 28.sp, fontWeight = FontWeight.Black),
                    color = colors.textPrimary
                )
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = colors.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary
                )
            } else {
                exploreData?.let { data ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        item {
                            CarouselList(
                                title = "New Releases",
                                items = data.newReleaseAlbums,
                                onItemClick = { item -> onItemClick("album", (item as AlbumItem).browseId) }
                            )
                        }

                        item {
                            SectionHeader(title = "Moods & Genres")
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                items(data.moodAndGenres) { mood ->
                                    MoodCard(mood = mood)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodCard(mood: MoodAndGenres.Item) {
    val colors = VynceTheme.colors
    val shapes = VynceTheme.shapes
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(90.dp)
            .clip(shapes.medium)
            .background(Color(mood.stripeColor).copy(alpha = 0.15f))
            .clickable { 
                Toast.makeText(context, "Browsing ${mood.title} (Simulated)", Toast.LENGTH_SHORT).show()
            }
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = mood.title,
            style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = colors.textPrimary
        )
    }
}
