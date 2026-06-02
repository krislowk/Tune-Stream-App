package com.vynce.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vynce.music.ui.theme.VynceTheme

enum class NavigationOrientation {
    Horizontal,
    Vertical
}

@Composable
fun NavRail(
    modifier: Modifier = Modifier,
    orientation: NavigationOrientation = NavigationOrientation.Vertical,
    containerColor: Color = VynceTheme.colors.background,
    content: @Composable () -> Unit
) {
    val railModifier = if (orientation == NavigationOrientation.Vertical) {
        modifier.fillMaxHeight().width(80.dp)
    } else {
        modifier.fillMaxWidth().wrapContentHeight()
    }

    Card(
        modifier = railModifier,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors(containerColor)
    ) {
        if (orientation == NavigationOrientation.Vertical) {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                content()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                content()
            }
        }
    }
}

@Composable
fun NavRailItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    orientation: NavigationOrientation = NavigationOrientation.Vertical
) {
    val colors = VynceTheme.colors
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.textSecondary.copy(alpha = 0.6f),
        label = "NavRailContentColor"
    )
    
    val indicatorSize by animateDpAsState(
        targetValue = if (selected) 24.dp else 0.dp,
        label = "IndicatorSize"
    )

    val itemModifier = modifier
        .clip(RoundedCornerShape(16.dp))
        .clickable { onClick() }

    if (orientation == NavigationOrientation.Vertical) {
        Row(
            modifier = itemModifier.width(80.dp).height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical Indicator Line
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(indicatorSize)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(colors.primary)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NavigationIndicatorWrapper(selected, icon, label, contentColor, badgeCount)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    color = contentColor,
                    style = VynceTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }
        }
    } else {
        Column(
            modifier = itemModifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal Indicator Line
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(indicatorSize)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(colors.primary)
            )

            Spacer(modifier.height(8.dp))

            Column(
                modifier = Modifier.padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NavigationIndicatorWrapper(selected, icon, label, contentColor, badgeCount)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    color = contentColor,
                    style = VynceTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun NavigationIndicatorWrapper(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    contentColor: Color,
    badgeCount: Int
) {
    Box(contentAlignment = Alignment.Center) {

        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 8.dp, y = (-4).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(VynceTheme.colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                        color = VynceTheme.colors.background,
                        style = VynceTheme.typography.label.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TabItem(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VynceTheme.colors
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.textPrimary else colors.textSecondary.copy(alpha = 0.6f),
        label = "TabContentColor"
    )

    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 24.dp else 0.dp,
        label = "TabIndicatorWidth"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = contentColor,
            style = VynceTheme.typography.body.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(indicatorWidth)
                .clip(CircleShape)
                .background(colors.primary)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun NavigationPreview() {
    VynceTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            Text("Vertical NavRail with Indicator Line", style = VynceTheme.typography.title, color = VynceTheme.colors.primary)
            
            Row(modifier = Modifier.height(250.dp)) {
                NavRail(orientation = NavigationOrientation.Vertical) {
                    NavRailItem(selected = true, icon = Icons.Default.Home, label = "Home", onClick = {})
                    NavRailItem(selected = false, icon = Icons.Default.Search, label = "Search", onClick = {})
                    NavRailItem(selected = false, icon = Icons.Default.LibraryMusic, label = "Library", onClick = {}, badgeCount = 3)
                }
            }
            
            Text("Horizontal NavRail / Bottom Bar", style = VynceTheme.typography.title, color = VynceTheme.colors.primary)
            
            NavRail(orientation = NavigationOrientation.Horizontal) {
                NavRailItem(
                    selected = true, 
                    icon = Icons.Default.Home, 
                    label = "Home", 
                    onClick = {}, 
                    orientation = NavigationOrientation.Horizontal
                )
                NavRailItem(
                    selected = false, 
                    icon = Icons.Default.LibraryMusic, 
                    label = "Library", 
                    onClick = {}, 
                    orientation = NavigationOrientation.Horizontal,
                    badgeCount = 5
                )
            }

            Text("Segmented Control Tabs", style = VynceTheme.typography.title, color = VynceTheme.colors.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TabItem(selected = true, text = "Artists", onClick = {}, modifier = Modifier.weight(1f))
                    TabItem(selected = false, text = "Albums", onClick = {}, modifier = Modifier.weight(1f))
                    TabItem(selected = false, text = "Playlists", onClick = {}, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}















