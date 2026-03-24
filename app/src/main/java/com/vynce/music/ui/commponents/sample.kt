package com.vynce.music.ui.commponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vynce.vynceclient.models.MusicCarouselShelfRenderer
import com.vynce.vynceclient.models.MusicShelfRenderer
import com.vynce.vynceclient.pages.HomePage

/**
 * Material 3 Home Screen Content with SearchBar and Dynamic Renderer-based lists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    homePage: HomePage,
    onSearch: (String) -> Unit,
    onItemClick: (HomePage.Section, Any) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = { 
                onSearch(it)
                active = false
            },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text("Search songs, albums, artists") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (active) 0.dp else 16.dp),
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            // Search suggestions could go here
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(homePage.sections) { section ->
                SectionRenderer(
                    section = section,
                    onItemClick = { item -> onItemClick(section, item) }
                )
            }
        }
    }
}

@Composable
fun SectionRenderer(
    section: HomePage.Section,
    onItemClick: (Any) -> Unit
) {
    // Determine which carousel/list component to use based on section title or other metadata
    val isQuickPick = section.title.contains("Quick", ignoreCase = true) || 
                      section.title.contains("Trending", ignoreCase = true)

    if (isQuickPick) {
        QuickPicksCarousel(
            title = section.title,
            items = section.items,
            onItemClick = { onItemClick(it) }
        )
    } else {
        CarouselList(
            title = section.title,
            items = section.items,
            onItemClick = { onItemClick(it) }
        )
    }
}

/**
 * Model-to-UI mapping for specific Renderers if needed directly.
 */
@Composable
fun MusicCarouselShelf(
    renderer: MusicCarouselShelfRenderer,
    onItemClick: (Any) -> Unit
) {
    val section = HomePage.Section.fromMusicCarouselShelfRenderer(renderer)
    if (section != null) {
        SectionRenderer(section = section, onItemClick = onItemClick)
    }
}

@Composable
fun MusicShelf(
    renderer: MusicShelfRenderer,
    onItemClick: (Any) -> Unit
) {
    val section = HomePage.Section.fromMusicShelfRenderer(renderer)
    if (section != null) {
        SectionRenderer(section = section, onItemClick = onItemClick)
    }
}
