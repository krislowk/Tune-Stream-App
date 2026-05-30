package com.vynce.music.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object ExploreRoute
@Serializable object SearchRoute
@Serializable object LibraryRoute
@Serializable object HistoryRoute
@Serializable object SettingsRoute
@Serializable data class ArtistRoute(val id: String)
@Serializable data class ArtistItemsRoute(val id: String, val params: String?, val title: String)
@Serializable data class PlaylistRoute(val id: String)
@Serializable data class AlbumRoute(val id: String)
@Serializable object PlayerRoute

sealed class Route<T : Any>(
    val label: String,
    val route: T,
    val icon: ImageVector
) {
    data object Home : Route<HomeRoute>("Home", HomeRoute, Icons.Outlined.Home)
    data object Explore : Route<ExploreRoute>("Explore", ExploreRoute, Icons.Outlined.Explore)
    data object Search : Route<SearchRoute>("Search", SearchRoute, Icons.Outlined.Search)
    data object Library : Route<LibraryRoute>("Library", LibraryRoute, Icons.Outlined.LibraryMusic)
    data object Settings : Route<SettingsRoute>("Settings", SettingsRoute, Icons.Outlined.Settings)

    companion object {
        val all = listOf(Home, Explore, Library, Settings)
    }
}












