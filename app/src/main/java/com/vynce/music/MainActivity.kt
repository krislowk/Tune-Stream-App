package com.vynce.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.vynce.music.navigation.NavGraph
import com.vynce.music.repository.PreferenceRepository
import com.vynce.music.ui.components.BottomBar
import com.vynce.music.ui.components.BottomSheet
import com.vynce.music.ui.components.collapsedAnchor
import com.vynce.music.ui.components.dismissedAnchor
import com.vynce.music.ui.components.rememberBottomSheetState
import com.vynce.music.ui.screens.player.MiniPlayer
import com.vynce.music.ui.screens.player.PlayerScreen
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.vynce.music.utils.SyncUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    @Inject
    lateinit var syncUtils: SyncUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        var isReady by mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !isReady }

        setContent {
            VynceTheme {
                val context = LocalContext.current
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

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

                LaunchedEffect(Unit) {
                    preferenceRepository.ensureInitialized()
                    syncUtils.tryAutoSync()
                    isReady = true
                }

                LaunchedEffect(permission) {
                    if (!hasPermission) {
                        launcher.launch(permission)
                    }
                }

                Box(Modifier
                    .fillMaxSize()
                    .background(VynceTheme.colors.background)) {
                    
                    if (hasPermission) {
                        VynceApp()
                    } else {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = VynceTheme.colors.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Vynce needs access to your music files to play them.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = VynceTheme.colors.textPrimary
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { launcher.launch(permission) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VynceTheme.colors.primary,
                                    contentColor = VynceTheme.colors.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VynceApp() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val uiState by playerViewModel.uiState.collectAsState()

    val peekHeight = 72.dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullMaxHeight = maxHeight
        var bottomBarHeight by remember { mutableStateOf(0.dp) }

        val sheetState = rememberBottomSheetState(
            dismissedBound = bottomBarHeight,
            expandedBound = fullMaxHeight,
            collapsedBound = bottomBarHeight + peekHeight,
            initialAnchor = if (uiState.currentTrack != null) collapsedAnchor else dismissedAnchor
        )

        LaunchedEffect(uiState.currentTrack) {
            if (uiState.currentTrack != null && sheetState.isDismissed) {
                sheetState.collapseSoft()
            } else if (uiState.currentTrack == null) {
                sheetState.dismiss()
            }
        }

        BackHandler(enabled = sheetState.isExpanded) {
            sheetState.collapseSoft()
        }

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = sheetState.progress < 0.1f,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    BottomBar(navController = navController)
                }
            },
            containerColor = VynceTheme.colors.background
        ) { navPadding ->
            LaunchedEffect(navPadding.calculateBottomPadding()) {
                bottomBarHeight = navPadding.calculateBottomPadding()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val animatedBottomPadding by animateDpAsState(
                    targetValue = if (sheetState.isExpanded) 0.dp else peekHeight + navPadding.calculateBottomPadding(),
                    animationSpec = tween(350),
                    label = "bottomPadding"
                )

                NavGraph(
                    navController = navController,
                    playerViewModel = playerViewModel,
                    modifier = Modifier.padding(
                        top = (navPadding.calculateTopPadding() - 30.dp).coerceAtLeast(0.dp),

                    )
                )

                BottomSheet(
                    state = sheetState,
                    onDismiss = { playerViewModel.stopPlayer() },
                    background = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = (sheetState.progress * 0.6f).coerceIn(0f, 0.6f)))
                                .clickable(enabled = sheetState.isExpanded) { sheetState.collapseSoft() }
                        )
                    },
                    collapsedContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(peekHeight)
                                .background(VynceTheme.colors.surface)
                        ) {
                            MiniPlayer(
                                viewModel = playerViewModel,
                                onClick = { sheetState.expandSoft() }
                            )
                        }
                    }
                ) {
                    PlayerScreen(
                        viewModel = playerViewModel,
                        expansionProgress = { sheetState.progress },
                        onClose = { sheetState.collapseSoft() },
                        onExpand = { sheetState.expandSoft() },
                        onNavigateToArtist = { id ->
                            navController.navigate(com.vynce.music.navigation.ArtistRoute(id))
                        },
                        onNavigateToAlbum = { id ->
                            navController.navigate(com.vynce.music.navigation.AlbumRoute(id))
                        }
                    )
                }
            }
        }
    }
}




