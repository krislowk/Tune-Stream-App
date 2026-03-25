package com.vynce.music.ui.commponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vynce.music.ui.theme.VynceTheme

/**
 * A specialized bottom modal component designed to host a player interface.
 * It provides a "peek" (Mini Player) and an "expanded" (Full Player) state.
 * Uses Material 3's ModalBottomSheet for the expanded view.
 *
 * @param isExpanded Whether the full player is visible.
 * @param onExpandChange Callback to change the expanded state.
 * @param peekHeight The height of the mini player peek.
 * @param showSheet Whether to show the sheet at all (e.g., hide if no track is playing).
 * @param sheetContent The content of the sheet, receives the expansion progress (0f or 1f).
 * @param modifier Modifier for the container.
 * @param content The main background content (e.g., Navigation Graph).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomModal(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    peekHeight: Dp,
    showSheet: Boolean,
    sheetContent: @Composable (expansionProgress: Float) -> Unit,
    modifier: Modifier = Modifier,
    onProgress: (Float) -> Unit = {},
    onHeightChanged: (Float) -> Unit = {},
    containerColor: Color = VynceTheme.colors.background,
    content: @Composable (PaddingValues) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalWindowInfo.current.containerSize.height.dp.toPx() }
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Calculate progress from Modal's offset
    val modalProgress by remember(isExpanded) {
        derivedStateOf {
            if (!isExpanded) 0f else {
                val offset = try { sheetState.requireOffset() } catch (_: Exception) { screenHeightPx }
                (1f - (offset / screenHeightPx)).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(modalProgress) {
        onProgress(modalProgress)
    }

    Box(modifier = modifier.fillMaxSize().background(containerColor)) {
        // Main Content
        content(PaddingValues(bottom = if (showSheet && !isExpanded) peekHeight else 0.dp))

        // Mini Player Peek (Visible when not expanded)
        if (showSheet && !isExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(peekHeight + navBarPadding)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (dragAmount < -10) { // Significant drag up
                                onExpandChange(true)
                            }
                        }
                    }
                    .clickable { onExpandChange(true) }
                    .background(VynceTheme.colors.surface)
                    .onSizeChanged { onHeightChanged(it.height.toFloat()) }
            ) {
                sheetContent(0f)
            }
        }

        // Modal Full Player
        if (isExpanded) {
            ModalBottomSheet(
                onDismissRequest = { onExpandChange(false) },
                sheetState = sheetState,
                dragHandle = null,
                containerColor = VynceTheme.colors.surface,
                scrimColor = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                contentWindowInsets = { WindowInsets(0) }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    sheetContent(modalProgress)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun BottomModalCollapsedPreview() {
    PreviewContainer(isExpanded = false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun BottomModalExpandedPreview() {
    PreviewContainer(isExpanded = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewContainer(isExpanded: Boolean) {
    VynceTheme {
        var expanded by remember { mutableStateOf(isExpanded) }
        
        BottomModal(
            isExpanded = expanded,
            onExpandChange = { expanded = it },
            peekHeight = 72.dp,
            showSheet = true,
            sheetContent = { progress ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (progress > 0.5f) 800.dp else 72.dp)
                        .background(VynceTheme.colors.surface),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (progress < 0.5f) {
                        Text("Mini Player Preview", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Full Player Content", fontSize = 24.sp, fontWeight = FontWeight.Black, color = VynceTheme.colors.primary)
                            Spacer(Modifier.height(16.dp))
                            Text("Expanded State", color = VynceTheme.colors.textSecondary)
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VynceTheme.colors.background)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("App Content (NavGraph)", color = VynceTheme.colors.textSecondary)
            }
        }
    }
}
