package com.vynce.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.DarkMode
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
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vynce.music.ui.commponents.Card
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

    var showLoginDialog by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

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
        AlertDialog(
            onDismissRequest = { showWebViewLogin = false },
            title = { Text("Login to YouTube Music") },
            text = {
                Box(modifier = Modifier.height(500.dp)) {
                    LoginWebView(
                        onCookieSelected = { cookie ->
                            viewModel.login(cookie)
                            showWebViewLogin = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWebViewLogin = false }) {
                    Text("Cancel")
                }
            }
        )
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
            style = VynceTheme.typography.title.copy(fontSize = 28.sp),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Account Section
        Card(modifier = Modifier.fillMaxWidth()) {
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
                state = viewModel.normalizeVolume
            )
            SettingsToggleItem(
                title = "Skip Silence",
                subtitle = "Automatically skip silent parts of songs",
                icon = Icons.Outlined.SkipNext,
                state = viewModel.skipSilence
            )
            SettingsToggleItem(
                title = "Crossfade",
                subtitle = "Smooth transition between songs",
                icon = Icons.Outlined.LibraryMusic,
                state = viewModel.crossfadeEnabled
            )
            SettingsSelectItem(
                title = "Playback Speed",
                subtitle = "${viewModel.playbackSpeed.collectAsState().value}x",
                icon = Icons.Outlined.Speed
            )
            SettingsSelectItem(
                title = "Audio Quality",
                subtitle = viewModel.audioQuality.collectAsState().value,
                icon = Icons.Outlined.HighQuality
            )
            SettingsSelectItem(
                title = "Audio Output",
                subtitle = viewModel.audioOutput.collectAsState().value,
                icon = Icons.Outlined.Speaker
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Downloads & Storage") {
            SettingsToggleItem(
                title = "Download over Wi-Fi only",
                subtitle = "Save mobile data",
                icon = Icons.Outlined.Wifi,
                state = viewModel.wifiOnlyDownloads
            )
            SettingsSelectItem(
                title = "Download Quality",
                subtitle = viewModel.downloadQuality.collectAsState().value,
                icon = Icons.Outlined.Download
            )
            SettingsSelectItem(
                title = "Buffer Size",
                subtitle = viewModel.bufferSize.collectAsState().value,
                icon = Icons.Outlined.Storage
            )
            SettingsActionItem(
                title = "Clear Cache",
                subtitle = "Used: 1.2 GB",
                icon = Icons.Outlined.DeleteSweep
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Appearance & Interface") {
            SettingsToggleItem(
                title = "Dynamic Colors",
                subtitle = "Theme matches your wallpaper",
                icon = Icons.Outlined.Palette,
                state = viewModel.dynamicColors
            )
            SettingsSelectItem(
                title = "Language",
                subtitle = viewModel.language.collectAsState().value,
                icon = Icons.Outlined.Language
            )
            SettingsActionItem(
                title = "App Theme",
                subtitle = "Dark Mode (Default)",
                icon = Icons.Outlined.DarkMode
            )
            SettingsActionItem(
                title = "Accent Color",
                subtitle = "Lime Green",
                icon = Icons.Outlined.Brush
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Privacy & Content") {
            SettingsToggleItem(
                title = "Restricted Mode",
                subtitle = "Hide potentially mature content",
                icon = Icons.Outlined.Lock,
                state = viewModel.restrictedMode
            )
            SettingsToggleItem(
                title = "Enable History",
                subtitle = "Personalize your recommendations",
                icon = Icons.Outlined.History,
                state = viewModel.enableHistory
            )
            SettingsToggleItem(
                title = "Show Lyrics on Lockscreen",
                icon = Icons.Outlined.Lyrics,
                state = viewModel.showLyricsOnLockscreen
            )
            SettingsSelectItem(
                title = "Content Region",
                subtitle = viewModel.contentRegion.collectAsState().value,
                icon = Icons.Outlined.Public
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Advanced") {
            SettingsToggleItem(
                title = "Use Login for Browse",
                subtitle = "Show personalized recommendations",
                icon = Icons.Outlined.AccountCircle,
                state = viewModel.useLoginForBrowse,
                onToggle = { viewModel.toggleUseLoginForBrowse(it) }
            )
            SettingsToggleItem(
                title = "External Player",
                subtitle = "Use a third-party app for playback",
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                state = viewModel.externalPlayerEnabled
            )
            SettingsToggleItem(
                title = "Enable Proxy",
                subtitle = "Route traffic through a custom server",
                icon = Icons.Outlined.VpnLock,
                state = viewModel.proxyEnabled
            )
            SettingsToggleItem(
                title = "Battery Optimization",
                subtitle = "Recommended to disable for background play",
                icon = Icons.Outlined.BatteryFull,
                state = viewModel.batteryOptimization
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
    Column {
        Text(
            text = title,
            style = VynceTheme.typography.label.copy(fontWeight = FontWeight.Bold),
            color = VynceTheme.colors.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        Card(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
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
                state.value = newValue
                onToggle(newValue)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = VynceTheme.colors.textSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = VynceTheme.typography.body)
            if (subtitle != null) {
                Text(text = subtitle, style = VynceTheme.typography.label)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                state.value = it
                onToggle(it)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = VynceTheme.colors.primary)
        )
    }
}

@Composable
fun SettingsSelectItem(title: String, subtitle: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle selection */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = VynceTheme.colors.textSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = VynceTheme.typography.body)
            Text(text = subtitle, style = VynceTheme.typography.label, color = VynceTheme.colors.primary)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VynceTheme.colors.textSecondary)
    }
}

@Composable
fun SettingsActionItem(title: String, subtitle: String? = null, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle action */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = VynceTheme.colors.textSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = VynceTheme.typography.body)
            if (subtitle != null) {
                Text(text = subtitle, style = VynceTheme.typography.label)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VynceTheme.colors.textSecondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun SettingsScreenPreview() {
    VynceTheme {
        SettingsScreen()
    }
}
