package com.vynce.music.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.vynce.music.ui.screens.album.AlbumScreen
import com.vynce.music.ui.screens.artist.ArtistScreen
import com.vynce.music.ui.screens.home.HomeScreen
import com.vynce.music.ui.screens.library.LibraryScreen
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.screens.playlist.PlaylistScreen
import com.vynce.music.ui.screens.search.SearchScreen
import com.vynce.music.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    modifier: Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                playerViewModel = playerViewModel,
                onItemClick = { type, id ->
                    when (type) {
                        "album" -> id?.let { navController.navigate(AlbumRoute(it)) }
                        "playlist" -> id?.let { navController.navigate(PlaylistRoute(it)) }
                        "artist" -> id?.let { navController.navigate(ArtistRoute(it)) }
                    }
                }
            )
        }
        composable<SearchRoute> {
            SearchScreen(
                playerViewModel = playerViewModel,
                onBackClick = { navController.popBackStack() },
                onItemClick = { type, id ->
                    when (type) {
                        "album" -> id?.let { navController.navigate(AlbumRoute(it)) }
                        "playlist" -> id?.let { navController.navigate(PlaylistRoute(it)) }
                        "artist" -> id?.let { navController.navigate(ArtistRoute(it)) }
                    }
                }
            )
        }
        composable<LibraryRoute> {
            LibraryScreen()
        }
        composable<SettingsRoute> {
            SettingsScreen()
        }
        composable<AlbumRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AlbumRoute>()
            AlbumScreen(
                id = route.id,
                playerViewModel = playerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<PlaylistRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PlaylistRoute>()
            PlaylistScreen(
                id = route.id,
                playerViewModel = playerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<ArtistRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ArtistRoute>()
            ArtistScreen(
                id = route.id,
                playerViewModel = playerViewModel,
                onBackClick = { navController.popBackStack() },
                onAlbumClick = { navController.navigate(AlbumRoute(it)) }
            )
        }
    }
}
