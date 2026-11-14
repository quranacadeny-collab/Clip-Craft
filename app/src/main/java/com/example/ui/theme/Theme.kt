package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricCoral,
    onPrimary = Color.White,
    primaryContainer = ElectricCoralContainer,
    onPrimaryContainer = OnElectricCoralContainer,
    secondary = CreatorTeal,
    onSecondary = Color(0xFF052E2B),
    secondaryContainer = CreatorTealContainer,
    onSecondaryContainer = OnCreatorTealContainer,
    tertiary = StudioAmber,
    onTertiary = Color(0xFF281800),
    tertiaryContainer = StudioAmberContainer,
    onTertiaryContainer = OnStudioAmberContainer,
    background = StudioBgDark,
    onBackground = TextPrimaryDark,
    surface = StudioSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioSurfaceContainerDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = StudioSurfaceDark,
    surfaceContainer = StudioSurfaceContainerDark,
    surfaceContainerHigh = StudioSurfaceHighDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF232836)
)

private val StudioLightColorScheme = lightColorScheme(
    primary = ElectricCoralLight,
    onPrimary = Color.White,
    primaryContainer = ElectricCoralContainerLight,
    onPrimaryContainer = OnElectricCoralContainerLight,
    secondary = CreatorTealLight,
    onSecondary = Color.White,
    secondaryContainer = CreatorTealContainerLight,
    onSecondaryContainer = OnCreatorTealContainerLight,
    tertiary = StudioAmber,
    onTertiary = Color.White,
    background = StudioBgLight,
    onBackground = TextPrimaryLight,
    surface = StudioSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = StudioSurfaceContainerLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClipCraftTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}
