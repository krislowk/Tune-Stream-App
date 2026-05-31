package com.vynce.music.ui.screens.lyrics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vynce.music.db.entities.SyncedLyric
import com.vynce.music.service.dsp.DspEngine
import com.vynce.music.ui.theme.AmberSnapping
import com.vynce.music.ui.theme.NeonCyan
import com.vynce.music.ui.theme.VynceTheme
import kotlinx.coroutines.launch

@Composable
fun LyricsScreen(
    modifier: Modifier = Modifier,
    viewModel: LyricsViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Observe StateFlows
    val rawLyricsInput by viewModel.rawLyrics.collectAsState()
    val songTitle by viewModel.songTitle.collectAsState()
    val songArtist by viewModel.songArtist.collectAsState()
    val songDurationMs by viewModel.songDurationMs.collectAsState()
    val generatedLrc by viewModel.generatedLrc.collectAsState()
    val timeline by viewModel.parsedLrcTimeline.collectAsState()

    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackMs by viewModel.playbackMs.collectAsState()
    val currentLineIndex by viewModel.currentLineIndex.collectAsState()

    val dspOnsets by viewModel.dspOnsets.collectAsState()
    val staticWaveform by viewModel.staticWaveform.collectAsState()

    val recordingActive by viewModel.recordingActive.collectAsState()
    val realtimeAmplitude by viewModel.realtimeAmplitude.collectAsState()
    val micOnsetsDetected by viewModel.micOnsetsDetected.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()

    val useSyntheticTrack by viewModel.useSyntheticTrack.collectAsState()
    val tapSyncProgress by viewModel.tapSyncProgress.collectAsState()
    val activeTapIndex by viewModel.activeTapLineIndex.collectAsState()
    val snappingFlashText by viewModel.tapSnappingFlashes.collectAsState()

    val savedLyricsList by viewModel.savedLyrics.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Alignment Deck, 1 = Karaoke Playback, 2 = Save/Library
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    var editSongInfoDialog by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf(songTitle) }
    var tempArtist by remember { mutableStateOf(songArtist) }

    Column(
        modifier = modifier
            .background(VynceTheme.colors.background)
            .fillMaxSize()
    ) {
        // App Header
        AppHeader(
            songTitle = songTitle,
            songArtist = songArtist,
            useSynthetic = useSyntheticTrack,
            onEditClick = {
                tempTitle = songTitle
                tempArtist = songArtist
                editSongInfoDialog = true
            },
            onSourceToggle = {
                viewModel.setSourceMode(it)
            }
        )

        // Custom Navigation Tab Bar
        TabNavigationHeader(
            activeTab = activeTab,
            onTabSelected = { activeTab = it }
        )

        // Animated Screen Selector
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                0 -> AlignmentDeck(
                    viewModel = viewModel,
                    rawLyrics = rawLyricsInput,
                    useSynthetic = useSyntheticTrack,
                    staticWaveform = staticWaveform,
                    dspOnsets = dspOnsets,
                    isPlaying = isPlaying,
                    playbackMs = playbackMs,
                    songDurationMs = songDurationMs,
                    tapSyncProgress = tapSyncProgress,
                    activeTapIndex = activeTapIndex,
                    snappingFlashText = snappingFlashText,
                    recordingActive = recordingActive,
                    recordingDurationMs = recordingDurationMs,
                    micOnsetsCount = micOnsetsDetected.size,
                    realtimeAmplitude = realtimeAmplitude,
                    permissionGranted = permissionGranted,
                    onReqPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
                )

                1 -> LiveKaraokeView(
                    timeline = timeline,
                    currentLineIndex = currentLineIndex,
                    isPlaying = isPlaying,
                    playbackMs = playbackMs,
                    songDurationMs = songDurationMs,
                    realtimeAmplitude = realtimeAmplitude,
                    onTogglePlay = { viewModel.togglePlayback() },
                    onSeek = { viewModel.seekTo(it) }
                )

                2 -> LibraryDeck(
                    generatedLrc = generatedLrc,
                    savedLyrics = savedLyricsList,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(generatedLrc))
                        Toast.makeText(context, "LRC string copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onSave = {
                        viewModel.saveSyncedLyricsToLibrary()
                        Toast.makeText(context, "Saved document to local library!", Toast.LENGTH_SHORT).show()
                    },
                    onLoad = {
                        viewModel.loadLrcFromDocument(it)
                        activeTab = 1
                    },
                    onDelete = { viewModel.deleteSavedLyric(it) }
                )
            }
        }
    }

    // Dialog for Editing Metadata
    if (editSongInfoDialog) {
        AlertDialog(
            onDismissRequest = { editSongInfoDialog = false },
            title = { Text("Song Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_title_input")
                    )
                    OutlinedTextField(
                        value = tempArtist,
                        onValueChange = { tempArtist = it },
                        label = { Text("Artist") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_artist_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSongInfo(tempTitle, tempArtist)
                        editSongInfoDialog = false
                    },
                    modifier = Modifier.testTag("dialog_save_button")
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editSongInfoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AppHeader(
    songTitle: String,
    songArtist: String,
    useSynthetic: Boolean,
    onEditClick: () -> Unit,
    onSourceToggle: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "DSP ENGINE V2.4",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SyncLyric",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pro",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Modes Switcher Block with a bold border
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(3.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (useSynthetic) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onSourceToggle(true) }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .testTag("mode_synth_tab")
                    ) {
                        Text(
                            text = "Synth",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (useSynthetic) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!useSynthetic) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onSourceToggle(false) }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .testTag("mode_mic_tab")
                    ) {
                        Text(
                            text = "Vocal Mic",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!useSynthetic) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = songTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("update_metadata_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Song metadata",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "by $songArtist",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TabNavigationHeader(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = activeTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Tab(
            selected = activeTab == 0,
            onClick = { onTabSelected(0) },
            text = { Text("Stage Deck", fontWeight = FontWeight.Bold) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            modifier = Modifier.testTag("tab_alignment")
        )
        Tab(
            selected = activeTab == 1,
            onClick = { onTabSelected(1) },
            text = { Text("Synced UI", fontWeight = FontWeight.Bold) },
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
            modifier = Modifier.testTag("tab_visualization")
        )
        Tab(
            selected = activeTab == 2,
            onClick = { onTabSelected(2) },
            text = { Text("Library [LRC]", fontWeight = FontWeight.Bold) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            modifier = Modifier.testTag("tab_library")
        )
    }
}

@Composable
fun AlignmentDeck(
    viewModel: LyricsViewModel,
    rawLyrics: String,
    useSynthetic: Boolean,
    staticWaveform: List<Float>,
    dspOnsets: List<Long>,
    isPlaying: Boolean,
    playbackMs: Long,
    songDurationMs: Long,
    tapSyncProgress: List<Pair<String, Long>>,
    activeTapIndex: Int,
    snappingFlashText: String?,
    recordingActive: Boolean,
    recordingDurationMs: Long,
    micOnsetsCount: Int,
    realtimeAmplitude: Float,
    permissionGranted: Boolean,
    onReqPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (useSynthetic) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Synthetic Soundscape Player",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Plays fully multi-toned music generated via our digital synthesizer. Real-time ExoPlayer processes PCM signals to align timestamps.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Waveform and dynamic onsets indicator
                        Text(
                            text = "Synthesizer Offsets & Peak Matrix (${dspOnsets.size} peaks):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        StaticWaveformRenderer(
                            height = 50.dp,
                            waveform = staticWaveform,
                            onsets = dspOnsets,
                            playbackMs = playbackMs,
                            songDurationMs = songDurationMs
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Trigger playback
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Duration: ${songDurationMs / 1000}s | Time: ${playbackMs / 1000}.${(playbackMs % 1000) / 100}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = { viewModel.togglePlayback() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("alignment_play_toggle")
                            ) {
                                if (isPlaying) {
                                    CustomPauseIcon(tint = VynceTheme.colors.background, modifier = Modifier.size(width = 12.dp, height = 12.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlaying) "Pause Play" else "Play Soundscape")
                            }
                        }
                    }
                }
            } else {
                // Microphone Deck
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Voice Recorder Stage",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Capture vocal lines directly. Real-time energy filters detect syllables and onset boundaries which are instantly fed to ExoPlayer.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!permissionGranted) {
                            Button(
                                onClick = onReqPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant Mic Access Permission")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (recordingActive) {
                                            viewModel.stopMicrophoneRecording()
                                        } else if (permissionGranted) {
                                            if (ActivityCompat.checkSelfPermission(
                                                    this as Context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                // TODO: Consider calling
                                                //    ActivityCompat#requestPermissions
                                                // here to request the missing permissions, and then overriding
                                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                //                                          int[] grantResults)
                                                // to handle the case where the user grants the permission. See the documentation
                                                // for ActivityCompat#requestPermissions for more details.
                                            }
                                            viewModel.startMicrophoneRecording()
                                        } else {
                                            onReqPermission()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (recordingActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("mic_record_toggle")
                                ) {
                                    if (recordingActive) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color.White, RoundedCornerShape(2.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (recordingActive) "Stop & Finish" else "Record Voice")
                                }
                            }

                            if (recordingActive || recordingDurationMs > 0) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Duration: ${(recordingDurationMs / 1000)}s",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Captured Onsets: $micOnsetsCount",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Realtime micro amplitude bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(VynceTheme.colors.background, RoundedCornerShape(4.dp))
                                ) {
                                    val smoothWidth by animateFloatAsState(
                                        targetValue = realtimeAmplitude.coerceIn(0f, 1f),
                                        animationSpec = spring()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(smoothWidth)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Deck for Alignment Controllers
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Align Timing Profiles",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.generateHeuristicsLrc() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("align_heuristics_button")
                        ) {
                            Text("Fast Heuristic", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.runAutoDspAlignment() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(50.dp))
                                .testTag("align_dsp_button")
                        ) {
                            Text("Auto DSP Align", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        // Tapping Alignment Tool
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Snapping Tapping Assister",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap dynamically on playback to register timings. Taps will split-second snap to the closest audio waveform peak automatically.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (tapSyncProgress.isEmpty()) {
                        Button(
                            onClick = { viewModel.startTappingSession() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_tapping_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Spawn Tapping Engine")
                        }
                    } else {
                        // Tapping layout
                        Column {
                            // Target text preview with premium primaryContainer look
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                if (activeTapIndex < tapSyncProgress.size) {
                                    Column {
                                        Text(
                                            "PROMPT LINE (${activeTapIndex + 1}/${tapSyncProgress.size})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tapSyncProgress[activeTapIndex].first,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "All lines recorded! Taps compiled.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Large physical tapping button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.recordTapAtCurrentTime()
                                    }
                                    .padding(16.dp)
                                    .testTag("tap_trigger_button")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        "TAP FOR NEXT LINE",
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Snapping status notification flash
                            AnimatedVisibility(
                                visible = snappingFlashText != null,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color(0xFFFFB300).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFFFFB300),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = snappingFlashText ?: "",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.resetTappingSession() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reset Taps")
                                }

                                Button(
                                    onClick = { viewModel.compileTappedLrc() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Finish Sync")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Raw input lyrics edit area
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Input Raw Lyrics Text",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rawLyrics,
                        onValueChange = { viewModel.updateRawLyrics(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("lyrics_input_field"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StaticWaveformRenderer(
    height: androidx.compose.ui.unit.Dp,
    waveform: List<Float>,
    onsets: List<Long>,
    playbackMs: Long,
    songDurationMs: Long
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(VynceTheme.colors.background, RoundedCornerShape(8.dp))
            .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        if (waveform.isEmpty() || songDurationMs <= 0) {
            drawRect(
                color = Color.Gray.copy(alpha = 0.2f)
            )
            return@Canvas
        }

        val barCount = waveform.size
        val barSpacing = size.width / barCount
        val verticalScaleFactor = size.height * 0.9f

        for (i in 0 until barCount) {
            val amplitude = waveform[i]
            val barHeight = maxOf(3f, amplitude * verticalScaleFactor)
            val bx = i * barSpacing
            val by = size.height / 2f - barHeight / 2f

            val currentChunkMs = (i.toDouble() / barCount * songDurationMs).toLong()
            val isPassed = playbackMs >= currentChunkMs

            val color = if (isPassed) {
                NeonCyan // Deep purple branding color
            } else {
                Color(0xFF90A4AE).copy(alpha = 0.4f)
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(bx, by),
                size = Size(maxOf(2f, barSpacing - 2f), barHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        val dotRadius = 3.5f
        onsets.forEach { onsetMs ->
            val relativeX = (onsetMs.toFloat() / songDurationMs) * size.width
            if (relativeX in 0f..size.width) {
                drawCircle(
                    color = AmberSnapping, // Highly visible Amber dots for voice/sound peaks
                    radius = dotRadius,
                    center = Offset(relativeX, size.height - 8f)
                )
            }
        }

        val playheadX = (playbackMs.toFloat() / songDurationMs) * size.width
        if (playheadX in 0f..size.width) {
            drawLine(
                color = Color.Black, // High contrast black cursor for light mode `#fdf8ff` waveform background
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, size.height),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun LiveKaraokeView(
    timeline: List<Pair<Long, String>>,
    currentLineIndex: Int,
    isPlaying: Boolean,
    playbackMs: Long,
    songDurationMs: Long,
    realtimeAmplitude: Float,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && currentLineIndex < timeline.size) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(
                    index = maxOf(0, currentLineIndex - 2),
                    scrollOffset = 0
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(VynceTheme.colors.background)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (timeline.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                radius = size.minDimension / 2f
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Timeline Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Align some raw lyrics or import loaded LRC records.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(timeline) { idx, item ->
                        val isCurrent = idx == currentLineIndex
                        val lineEndMs = if (idx + 1 < timeline.size) {
                            timeline[idx + 1].first
                        } else {
                            if (songDurationMs > 0L) songDurationMs else item.first + 4000L
                        }

                        KaraokeLineText(
                            lineText = item.second,
                            lineStartMs = item.first,
                            lineEndMs = lineEndMs,
                            playbackMs = playbackMs,
                            isCurrent = isCurrent,
                            onSeek = { onSeek(item.first) }
                        )
                    }
                }
            }

            // Realtime responsive peak amplitude visual ripple
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 6f
                    val gap = 4f
                    val wavePeaks = 30
                    val center = size.width / 2f

                    for (i in 0 until wavePeaks) {
                        val scale = (1f - (i.toFloat() / wavePeaks)) * size.height * realtimeAmplitude * 1.5f
                        val offsetLeft = center - (i * (barWidth + gap))
                        val offsetRight = center + (i * (barWidth + gap))

                        drawRoundRect(
                            color = NeonCyan.copy(alpha = 0.6f - (i.toFloat() / wavePeaks) * 0.4f),
                            topLeft = Offset(offsetLeft, size.height / 2f - scale / 2f),
                            size = Size(barWidth, maxOf(4f, scale)),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

                        if (i > 0) {
                            drawRoundRect(
                                color = NeonCyan.copy(alpha = 0.6f - (i.toFloat() / wavePeaks) * 0.4f),
                                topLeft = Offset(offsetRight, size.height / 2f - scale / 2f),
                                size = Size(barWidth, maxOf(4f, scale)),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seeking controller line
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Slider(
                value = if (songDurationMs > 0) playbackMs.toFloat() / songDurationMs else 0f,
                onValueChange = { fraction ->
                    onSeek((fraction * songDurationMs).toLong())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("karaoke_scrubber")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DspEngine.formatLrcTimestamp(playbackMs),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                        .testTag("karaoke_play_toggle")
                ) {
                    if (isPlaying) {
                        CustomPauseIcon(tint = VynceTheme.colors.background, modifier = Modifier.size(width = 16.dp, height = 16.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Playback Action Toggle",
                            tint = VynceTheme.colors.background
                        )
                    }
                }

                Text(
                    text = DspEngine.formatLrcTimestamp(songDurationMs),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun LibraryDeck(
    generatedLrc: String,
    savedLyrics: List<SyncedLyric>,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onLoad: (SyncedLyric) -> Unit,
    onDelete: (SyncedLyric) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Output LRC Preview Block
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "LRC Target Output Compilation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "This exports conforming LRC timed layouts to feed lyric files on streaming platforms and players.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(VynceTheme.colors.background, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    if (generatedLrc.isEmpty()) {
                        Text(
                            "No synced lyrics formatted block generated.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = generatedLrc,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCopy,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lrc_copy_button"),
                        enabled = generatedLrc.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy String")
                    }

                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lrc_save_button"),
                        enabled = generatedLrc.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = VynceTheme.colors.background, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Library", color = VynceTheme.colors.background)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History local library List
        Text(
            text = "Saved Sync Library History:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (savedLyrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = NeonCyan.copy(alpha = 0.15f),
                                    radius = size.minDimension / 2f
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Sync database catalog is empty.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(savedLyrics) { idx, item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                                .testTag("saved_lyric_item_$idx")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "by ${item.artist} • ${item.durationSec}s",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { onLoad(item) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("saved_lyric_load_btn_$idx")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Load alignment item file into deck",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDelete(item) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove item database file",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DSP Sync Engine", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Welcome to the DSP Sync Studio, $name!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
fun KaraokeWord(
    word: String,
    startMs: Long,
    endMs: Long,
    playbackMs: Long
) {
    val wordProgress = if (playbackMs >= endMs) {
        1f
    } else if (playbackMs >= startMs) {
        ((playbackMs - startMs).toFloat() / (endMs - startMs)).coerceIn(0f, 1f)
    } else {
        0f
    }

    val isCurrentlyActive = playbackMs in startMs until endMs
    val scale by animateFloatAsState(
        targetValue = if (isCurrentlyActive) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "word_scale"
    )

    val baseColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
    val activeColor = if (isCurrentlyActive) AmberSnapping else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Base/Unsung grey text layer
        Text(
            text = word,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = baseColor,
                fontFamily = FontFamily.SansSerif
            ),
            maxLines = 1,
            softWrap = false
        )

        // Progressive sweep text layer
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = wordProgress)
                .clipToBounds()
        ) {
            Text(
                text = word,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = activeColor,
                    fontFamily = FontFamily.SansSerif,
                    shadow = if (isCurrentlyActive) {
                        Shadow(
                            color = activeColor.copy(alpha = 0.6f),
                            offset = Offset(0f, 1f),
                            blurRadius = 4f
                        )
                    } else null
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun KaraokeLineText(
    lineText: String,
    lineStartMs: Long,
    lineEndMs: Long,
    playbackMs: Long,
    isCurrent: Boolean,
    onSeek: () -> Unit
) {
    if (!isCurrent) {
        val color = if (playbackMs >= lineStartMs) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        }
        Text(
            text = lineText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable { onSeek() },
            textAlign = TextAlign.Center
        )
    } else {
        val words = remember(lineText) {
            lineText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        }

        val wordTimings = remember(words, lineStartMs, lineEndMs) {
            if (words.isEmpty()) emptyList()
            else {
                val totalDuration = maxOf(2000L, lineEndMs - lineStartMs)
                // Fit nicely within the lyrical span with 90% progressive active duration
                val activeLyricDuration = (totalDuration * 0.90f).toLong()

                val totalChars = words.sumOf { it.length }.toFloat()
                var currentStart = lineStartMs
                words.map { word ->
                    val proportion = if (totalChars > 0f) word.length / totalChars else 1f / words.size
                    val wordDuration = (proportion * activeLyricDuration).toLong()
                    val end = currentStart + wordDuration
                    val timing = WordTiming(word, currentStart, end)
                    currentStart = end
                    timing
                }
            }
        }

        // Animated layout container
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable { onSeek() },
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center
        ) {
            wordTimings.forEach { timing ->
                KaraokeWord(
                    word = timing.word,
                    startMs = timing.startMs,
                    endMs = timing.endMs,
                    playbackMs = playbackMs
                )
            }
        }
    }
}

/**
 * Custom modern vector drawings representing Pause indicators.
 */
@Composable
fun CustomPauseIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(tint, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(tint, RoundedCornerShape(1.dp))
        )
    }
}

data class WordTiming(val word: String, val startMs: Long, val endMs: Long)