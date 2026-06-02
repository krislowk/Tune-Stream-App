package com.vynce.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vynce.music.ui.theme.VynceTheme

/**
 * BottomModal provides a unified container for the side navigation drawer 
 * and a persistent bottom sheet player using BottomSheetScaffold.
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
    drawerContent: @Composable () -> Unit = {},
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable (PaddingValues) -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = if (isExpanded) SheetValue.Expanded else SheetValue.PartiallyExpanded,
            skipHiddenState = false
        )
    )

    // Sync external isExpanded state to internal sheet state
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            scaffoldState.bottomSheetState.expand()
        } else if (showSheet) {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    // Sync internal sheet state back to external isExpanded state
    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        val expanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
        if (expanded != isExpanded) {
            onExpandChange(expanded)
        }
    }

    // Handle showSheet visibility
    LaunchedEffect(showSheet) {
        if (!showSheet) {
            scaffoldState.bottomSheetState.hide()
        } else if (!isExpanded) {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    // Report progress (0f partially expanded, 1f fully expanded)
    // Note: M3 StandardBottomSheetState doesn't expose offset easily for progress yet,
    // we use targetValue as a fallback for simple transitions.
    val progress = if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) 1f else 0f
    
    LaunchedEffect(progress) {
        onProgress(progress)
    }

    ModalNavigationDrawer(
        drawerContent = drawerContent,
        drawerState = drawerState,
        gesturesEnabled = scaffoldState.bottomSheetState.currentValue != SheetValue.Expanded
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    sheetContent(progress)
                }
            },
            sheetPeekHeight = if (showSheet) peekHeight else 0.dp,
            sheetContainerColor = VynceTheme.colors.surface,
            sheetTonalElevation = 8.dp,
            sheetShadowElevation = 16.dp,
            sheetDragHandle = null,
            sheetSwipeEnabled = true,
            containerColor = containerColor,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}















