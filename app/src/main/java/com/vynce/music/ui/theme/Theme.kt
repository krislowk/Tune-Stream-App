package com.vynce.music.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Custom Vynce Theme Properties
data class VynceColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val accent: Color,
)

data class VynceTypography(
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val branding: TextStyle
)

data class VynceShapes(
    val small: RoundedCornerShape,
    val medium: RoundedCornerShape,
    val large: RoundedCornerShape
)

val LocalvynceColors = staticCompositionLocalOf<VynceColors> { error("No vynceColors provided") }
val LocalvynceTypography = staticCompositionLocalOf<VynceTypography> { error("No vynceTypography provided") }
val LocalVynceShapes = staticCompositionLocalOf<VynceShapes> { error("No VynceShapes provided") }

private val DarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    secondary = SurfaceMedium,
    tertiary = CyanAccent,
    background = DeepSpace,
    surface = SurfaceDark,
    onPrimary = DeepSpace,
    onSecondary = TextPrimary,
    onTertiary = DeepSpace,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricPurple,
    secondary = Color(0xFFF0F0F0),
    tertiary = CyanAccent,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun VynceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val vynceColors = VynceColors(
        primary = ElectricPurple,
        accent = CyanAccent,
        background = if (darkTheme) DeepSpace else Color.White,
        surface = if (darkTheme) SurfaceDark else Color(0xFFF5F5F7),
        onPrimary = if (darkTheme) DeepSpace else Color.White,
        textPrimary = if (darkTheme) TextPrimary else Color.Black,
        textSecondary = if (darkTheme) TextSecondary else Color.Gray,
        glassSurface = GlassSurface,
        glassBorder = GlassBorder
    )

    val vynceTypography = VynceTypography(
        title = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = vynceColors.textPrimary
        ),
        body = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = vynceColors.textPrimary
        ),
        label = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = vynceColors.textSecondary
        ),
        branding = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = vynceColors.primary
        )
    )

    val vynceShapes = VynceShapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp)
    )

    CompositionLocalProvider(
        LocalvynceColors provides vynceColors,
        LocalvynceTypography provides vynceTypography,
        LocalVynceShapes provides vynceShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(
                titleLarge = vynceTypography.title,
                bodyMedium = vynceTypography.body,
                labelSmall = vynceTypography.label
            ),
            shapes = Shapes(
                small = vynceShapes.small,
                medium = vynceShapes.medium,
                large = vynceShapes.large
            ),
            content = content
        )
    }
}

object VynceTheme {
    val colors: VynceColors
        @Composable
        @ReadOnlyComposable
        get() = LocalvynceColors.current

    val typography: VynceTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalvynceTypography.current

    val shapes: VynceShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalVynceShapes.current
}
