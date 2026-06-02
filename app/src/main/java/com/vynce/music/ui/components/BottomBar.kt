package com.vynce.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vynce.music.navigation.Route
import com.vynce.music.ui.theme.VynceTheme

@Composable
fun BottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Using the standardized VynceNavRail with Horizontal orientation
    NavRail(
        modifier = modifier.fillMaxWidth(),
        orientation = NavigationOrientation.Horizontal,
        containerColor = VynceTheme.colors.surface
    ) {
        Route.all.forEach { screen ->
            val selected =
                currentDestination?.hierarchy?.any { it.hasRoute(screen.route::class) } == true

            NavRailItem(
                selected = selected,
                icon = screen.icon,
                label = screen.label,
                orientation = NavigationOrientation.Horizontal,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun BottomBarPreview() {
    VynceTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.BottomCenter) {
            BottomBar(navController = rememberNavController())
        }
    }
}















