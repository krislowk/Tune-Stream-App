package com.vynce.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vynce.music.ui.screens.home.HomeScreen
import com.vynce.music.ui.screens.home.HomeViewModel
import com.vynce.music.ui.screens.player.MiniPlayer
import com.vynce.music.ui.screens.player.PlayerScreen
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VynceTheme {
                val context = LocalContext.current
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

// 1. Initialize with the actual current permission status
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasPermission = isGranted
                }

// 2. Only launch if we don't already have it
                LaunchedEffect(permission) {
                    if (!hasPermission) {
                        launcher.launch(permission)
                    }
                }

// 3. Structured UI for the different states
                Box(Modifier
                    .fillMaxSize()
                    .background(VynceTheme.colors.background)) {
                    when {
                        hasPermission -> {
                            var showPlayer by rememberSaveable { mutableStateOf(false) }

                            // Using a Box to keep VynceApp alive in the background
                            // prevents it from losing scroll state/ViewModel state

                            if (showPlayer) {
                                PlayerScreen(onDismiss = { showPlayer = false })
                            } else {
                                VynceApp(onShowPlayer = { showPlayer = true })
                            }
                        }
                        else -> {
                            // 4. Actionable Permission Screen
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Vynce needs access to your music files to play them.")
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { launcher.launch(permission) }) {
                                    Text("Grant Permission")
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
fun VynceApp(onShowPlayer: () -> Unit) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val homeViewModel: HomeViewModel = viewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination },
                )
            }
        },
        modifier = Modifier.background(Color.Black)
    ) {
        // 2. Persistent Scaffold for the MiniPlayer
        Scaffold(
            bottomBar = {
                MiniPlayer(
                    viewModel = playerViewModel,
                    onClick = onShowPlayer
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(
                        playerViewModel= playerViewModel,
                        onItemClick = { _, _ -> },
                        viewModel = homeViewModel
                    )
                    AppDestinations.SEARCH -> Text("SEARCH Screen")
                    AppDestinations.LIBRARY -> Text("LIBRARY Screen")
                    AppDestinations.SETTINGS -> Text("SETTINGS Screen")
                }
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    SEARCH("Search", Icons.Outlined.Search),
    LIBRARY("Library", Icons.Outlined.LibraryMusic),
    SETTINGS("Settings", Icons.Default.Settings)
}
