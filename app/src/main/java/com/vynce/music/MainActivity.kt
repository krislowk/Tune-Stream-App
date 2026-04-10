package com.vynce.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.vynce.music.data.repository.PreferenceRepository
import com.vynce.music.navigation.NavGraph
import com.vynce.music.ui.commponents.BottomBar
import com.vynce.music.ui.commponents.BottomModal
import com.vynce.music.ui.commponents.TopBar
import com.vynce.music.ui.screens.player.PlayerScreen
import com.vynce.music.ui.screens.player.PlayerViewModel
import com.vynce.music.ui.theme.VynceTheme
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.util.DefaultDownloaderImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

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
                    if (!isReady) {
                        NewPipe.init(DefaultDownloaderImpl.initDefault())
                        isReady = true
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VynceApp() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val uiState by playerViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var isPlayerExpanded by remember { mutableStateOf(false) }

    // Double back to exit logic
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val isAtRoot = navController.previousBackStackEntry == null

    BackHandler(enabled = isAtRoot && !isPlayerExpanded) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            (context as? ComponentActivity)?.finish()
        } else {
            backPressedTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    // Collapse player on back press if expanded
    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }

    val peekHeight = 72.dp

    val animatedProgress by animateDpAsState(
        if (isPlayerExpanded) 0.dp else 72.dp,
        animationSpec = tween(350),
        label = "sliding"
    )

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                modifier = Modifier.graphicsLayer {
                    alpha = if (isPlayerExpanded) 0f else 1f
                }
            )
        },
        bottomBar = {
            BottomBar(
                navController = navController,
                modifier = Modifier.graphicsLayer {
                    alpha = if (isPlayerExpanded) 0f else 1f
                }
            )
        },
        containerColor = VynceTheme.colors.background
    ) { navPadding ->
        BottomModal(
            isExpanded = isPlayerExpanded,
            onExpandChange = { isPlayerExpanded = it },
            peekHeight = peekHeight,
            showSheet = uiState.currentTrack != null,
            sheetContent = { progress ->
                PlayerScreen(
                    viewModel = playerViewModel,
                    expansionProgress = { progress },
                    onClose = { isPlayerExpanded = false },
                    onExpand = { isPlayerExpanded = true },
                    onNavigateToArtist = { id ->
                        navController.navigate(com.vynce.music.navigation.ArtistRoute(id))
                    },
                    onNavigateToAlbum = { id ->
                        navController.navigate(com.vynce.music.navigation.AlbumRoute(id))
                    }
                )
            },
            modifier = Modifier.padding(
                bottom = animatedProgress
            )
        ) {
            NavGraph(
                navController = navController,
                playerViewModel = playerViewModel,
                modifier = Modifier.padding(top = navPadding.calculateTopPadding() - 30.dp)
            )
        }
    }
}
