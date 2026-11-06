package com.taskalarm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

val Accent = Color(0xFFFF8A3D)
val Success = Color(0xFF2EC27E)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF2B1100),
    primaryContainer = Color(0xFF5C2A00),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFF8FB4FF),
    onSecondary = Color(0xFF00225C),
    background = Color(0xFF0F1729),
    onBackground = Color(0xFFE7EAF3),
    surface = Color(0xFF0F1729),
    onSurface = Color(0xFFE7EAF3),
    surfaceVariant = Color(0xFF1C2742),
    onSurfaceVariant = Color(0xFFB8C0D6),
    surfaceContainer = Color(0xFF182238),
    surfaceContainerHigh = Color(0xFF1F2A44),
    error = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFD9620F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF3A5BA9),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFF7F8FC),
    surfaceVariant = Color(0xFFE6E9F2),
    surfaceContainer = Color(0xFFEDEFF6),
    surfaceContainerHigh = Color(0xFFE6E9F2),
)

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

val ClockStyle = TextStyle(fontSize = 84.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

/** App theme. The whole UI is Hebrew, so the layout is always right-to-left. */
@Composable
fun TaskAlarmTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
    }
}
