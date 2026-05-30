package com.vynce.music.ui.screens.settings

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vynce.music.repository.constants.PreferenceConstants
import com.vynce.music.ui.components.Card
import com.vynce.music.ui.theme.VynceTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val username by viewModel.username.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userThumbnail by viewModel.userThumbnail.collectAsState()
    val context = LocalContext.current

    var showLoginDialog by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

    var activeSelection by remember { mutableStateOf<SelectionType?>(null) }

    when (activeSelection) {
        SelectionType.PLAYBACK_SPEED -> SettingsSelectionDialog(
            title = "Playback Speed",
            options = listOf("0.5x" to 0.5f, "0.75x" to 0.75f, "1.0x" to 1.0f, "1.25x" to 1.25f, "1.5x" to 1.5f, "2.0x" to 2.0f),
            selectedOption = viewModel.playbackSpeed.collectAsState().value,
            onOptionSelected = { viewModel.setPlaybackSpeed(it); activeSelection = null },
            onDismiss = { activeSelection = null }
        )
        SelectionType.AUDIO_QUALITY -> SettingsSelectionDialog(
            title = "Audio Quality",
            options = listOf("Auto" to "Auto", "Low (48kbps)" to "Low (48kbps)", "Normal (128kbps)" to "Normal (128kbps)", "High (256kbps)" to "High (256kbps)"),
            selectedOption = viewModel.audioQuality.collectAsState().value,
            onOptionSelected = { viewModel.setAudioQuality(it); activeSelection = null },
            onDismiss = { activeSelection = null }
        )
        SelectionType.DOWNLOAD_QUALITY -> SettingsSelectionDialog(
            title = "Download Quality",
            options = listOf("Low (48kbps)" to "Low (48kbps)", "Normal (128kbps)" to "Normal (128kbps)", "High (256kbps)" to "High (256kbps)"),
            selectedOption = viewModel.downloadQuality.collectAsState().value,
            onOptionSelected = { viewModel.setDownloadQuality(it); activeSelection = null },
            onDismiss = { activeSelection = null }
        )
        SelectionType.LANGUAGE -> SettingsSelectionDialog(
            title = "Language",
            options = listOf("English" to "English", "Spanish" to "Spanish", "French" to "French", "German" to "German", "Hindi" to "Hindi"),
            selectedOption = viewModel.language.collectAsState().value,
            onOptionSelected = { viewModel.setLanguage(it); activeSelection = null },
            onDismiss = { activeSelection = null }
        )
        SelectionType.CONTENT_REGION -> SettingsSelectionDialog(
            title = "Content Region",
            options = listOf("United States" to "United States", "United Kingdom" to "United Kingdom", "India" to "India", "Global" to "Global"),
            selectedOption = viewModel.contentRegion.collectAsState().value,
            onOptionSelected = { viewModel.setContentRegion(it); activeSelection = null },
            onDismiss = { activeSelection = null }
        )
        null -> {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 100.dp)
    ) {
        Text(
            text = "Settings",
            style = VynceTheme.typography.title.copy(fontSize = 28.sp, fontWeight = FontWeight.Black),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Account Section
        Card(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            if (isLoggedIn) {
                UserHeader(
                    username = username ?: "User",
                    email = userEmail,
                    thumbnail = userThumbnail,
                    onLogout = { viewModel.logout() },
                    onRefresh = { viewModel.refreshAccountInfo() }
                )
            } else {
                LoginPrompt(
                    onLoginClick = { showWebViewLogin = true },
                    onManualCookieClick = { showLoginDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Groups
        SettingsSection(title = "Audio & Playback") {
            SettingsToggleItem(
                title = "Normalize Volume",
                subtitle = "Keep volume consistent across tracks",
                icon = Icons.Outlined.GraphicEq,
                state = viewModel.normalizeVolume,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.NORMALIZE_VOLUME, viewModel.normalizeVolume, it) }
            )
            SettingsToggleItem(
                title = "Skip Silence",
                subtitle = "Automatically skip silent parts of songs",
                icon = Icons.Outlined.SkipNext,
                state = viewModel.skipSilence,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.SKIP_SILENCE, viewModel.skipSilence, it) }
            )
            SettingsToggleItem(
                title = "Crossfade",
                subtitle = "Smooth transition between songs",
                icon = Icons.Outlined.LibraryMusic,
                state = viewModel.crossfadeEnabled,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.CROSSFADE_ENABLED, viewModel.crossfadeEnabled, it) }
            )
            SettingsSelectItem(
                title = "Playback Speed",
                subtitle = "${viewModel.playbackSpeed.collectAsState().value}x",
                icon = Icons.Outlined.Speed,
                onClick = { activeSelection = SelectionType.PLAYBACK_SPEED }
            )
            SettingsSelectItem(
                title = "Audio Quality",
                subtitle = viewModel.audioQuality.collectAsState().value,
                icon = Icons.Outlined.HighQuality,
                onClick = { activeSelection = SelectionType.AUDIO_QUALITY }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Downloads & Storage") {
            SettingsToggleItem(
                title = "Download over Wi-Fi only",
                subtitle = "Save mobile data",
                icon = Icons.Outlined.Wifi,
                state = viewModel.wifiOnlyDownloads,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.WIFI_ONLY_DOWNLOADS, viewModel.wifiOnlyDownloads, it) }
            )
            SettingsSelectItem(
                title = "Download Quality",
                subtitle = viewModel.downloadQuality.collectAsState().value,
                icon = Icons.Outlined.Download,
                onClick = { activeSelection = SelectionType.DOWNLOAD_QUALITY }
            )
            SettingsActionItem(
                title = "Clear Cache",
                subtitle = "Free up storage space",
                icon = Icons.Outlined.DeleteSweep,
                onClick = { 
                    viewModel.clearCache()
                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show() 
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Appearance & Interface") {
            SettingsToggleItem(
                title = "Dynamic Colors",
                subtitle = "Theme matches your wallpaper",
                icon = Icons.Outlined.Palette,
                state = viewModel.dynamicColors,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.DYNAMIC_COLORS, viewModel.dynamicColors, it) }
            )
            SettingsSelectItem(
                title = "Language",
                subtitle = viewModel.language.collectAsState().value,
                icon = Icons.Outlined.Language,
                onClick = { activeSelection = SelectionType.LANGUAGE }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Privacy & Content") {
            SettingsToggleItem(
                title = "Restricted Mode",
                subtitle = "Hide potentially mature content",
                icon = Icons.Outlined.Lock,
                state = viewModel.restrictedMode,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.RESTRICTED_MODE, viewModel.restrictedMode, it) }
            )
            SettingsToggleItem(
                title = "Enable History",
                subtitle = "Personalize your recommendations",
                icon = Icons.Outlined.History,
                state = viewModel.enableHistory,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.ENABLE_HISTORY, viewModel.enableHistory, it) }
            )
            SettingsToggleItem(
                title = "Show Lyrics on Lockscreen",
                icon = Icons.Outlined.Lyrics,
                state = viewModel.showLyricsOnLockscreen,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.SHOW_LYRICS_LOCKSCREEN, viewModel.showLyricsOnLockscreen, it) }
            )
            SettingsSelectItem(
                title = "Content Region",
                subtitle = viewModel.contentRegion.collectAsState().value,
                icon = Icons.Outlined.Public,
                onClick = { activeSelection = SelectionType.CONTENT_REGION }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Advanced") {
            SettingsToggleItem(
                title = "Use Login for Browse",
                subtitle = "Show personalized recommendations",
                icon = Icons.Outlined.AccountCircle,
                state = viewModel.useLoginForBrowse,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.USE_LOGIN_FOR_BROWSE, viewModel.useLoginForBrowse, it) }
            )
            SettingsToggleItem(
                title = "External Player",
                subtitle = "Use a third-party app for playback",
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                state = viewModel.externalPlayerEnabled,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.EXTERNAL_PLAYER_ENABLED, viewModel.externalPlayerEnabled, it) }
            )
            SettingsToggleItem(
                title = "Enable Proxy",
                icon = Icons.Outlined.VpnLock,
                state = viewModel.proxyEnabled,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.PROXY_ENABLED, viewModel.proxyEnabled, it) }
            )
            SettingsToggleItem(
                title = "Battery Optimization",
                subtitle = "Recommended to disable for background play",
                icon = Icons.Outlined.BatteryFull,
                state = viewModel.batteryOptimization,
                onToggle = { viewModel.toggleBoolean(PreferenceConstants.BATTERY_OPTIMIZATION, viewModel.batteryOptimization, it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "About") {
            SettingsActionItem(title = "Version", subtitle = "1.0.0-alpha", icon = Icons.Outlined.Info)
            SettingsActionItem(title = "Check for Updates", icon = Icons.Outlined.Update)
            SettingsActionItem(title = "Privacy Policy", icon = Icons.Outlined.Policy)
            SettingsActionItem(title = "Open Source Licenses", icon = Icons.Outlined.Description)
        }
    }

    if (showLoginDialog) {
        CookieLoginDialog(
            onDismiss = { showLoginDialog = false },
            onLogin = { cookie ->
                viewModel.login(cookie)
                showLoginDialog = false
            }
        )
    }

    if (showWebViewLogin) {
        Dialog(
            onDismissRequest = { showWebViewLogin = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = VynceTheme.colors.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showWebViewLogin = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = VynceTheme.colors.textPrimary)
                        }
                        Text(
                            "Login to YouTube Music",
                            style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                            color = VynceTheme.colors.textPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    LoginWebView(
                        onLoginSuccess = { cookie, visitorData ->

                            viewModel.login(cookie, visitorData)

                            showWebViewLogin = false
                        },

                        onLoginFailed = { error ->
                            Log.e("Login", error)
                        },

                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

enum class SelectionType {
    PLAYBACK_SPEED,
    AUDIO_QUALITY,
    DOWNLOAD_QUALITY,
    LANGUAGE,
    CONTENT_REGION
}

@Composable
fun UserHeader(username: String, email: String?, thumbnail: String?, onLogout: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(VynceTheme.colors.surface)
                .clickable { onRefresh() },
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = username, style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
            if (email != null) {
                Text(text = email, style = VynceTheme.typography.label)
            }
        }
        IconButton(onClick = onLogout) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = "Logout",
                tint = Color.Red.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LoginPrompt(
    onLoginClick: () -> Unit,
    onManualCookieClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = VynceTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Connect to YouTube Music",
            style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Sync your library and get better recommendations.",
            style = VynceTheme.typography.label,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = VynceTheme.colors.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login with WebView", color = VynceTheme.colors.onPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onManualCookieClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Advanced: Manual Cookie", color = VynceTheme.colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun CookieLoginDialog(onDismiss: () -> Unit, onLogin: (String) -> Unit) {
    var cookieText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Login with Cookie") },
        text = {
            Column {
                Text("Paste your YouTube Music cookie here. This is used to authenticate with InnerTube.")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = cookieText,
                    onValueChange = { cookieText = it },
                    placeholder = { Text("Cookie string...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (cookieText.isNotBlank()) onLogin(cookieText) }) {
                Text("Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = VynceTheme.typography.label.copy(fontWeight = FontWeight.Bold),
            color = VynceTheme.colors.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        Card(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    state: MutableStateFlow<Boolean>,
    onToggle: (Boolean) -> Unit = {}
) {
    val checked by state.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val newValue = !checked
                onToggle(newValue)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = VynceTheme.colors.textSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Medium))
            if (subtitle != null) {
                Text(text = subtitle, style = VynceTheme.typography.label)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = VynceTheme.colors.primary,
                checkedTrackColor = VynceTheme.colors.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun SettingsSelectItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = VynceTheme.colors.textSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Medium))
            Text(text = subtitle, style = VynceTheme.typography.label, color = VynceTheme.colors.primary)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VynceTheme.colors.textSecondary)
    }
}

@Composable
fun SettingsActionItem(title: String, subtitle: String? = null, icon: ImageVector, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = VynceTheme.colors.textSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = VynceTheme.typography.body.copy(fontWeight = FontWeight.Medium))
            if (subtitle != null) {
                Text(text = subtitle, style = VynceTheme.typography.label)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VynceTheme.colors.textSecondary)
    }
}












