package com.example.theekhai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * TheekHai civic design system — earthy tones matching the web app.
 *
 * The palette draws from the web's CSS custom properties:
 *   --green: #123e39,  --ink: #233c36,  --muted: #64736a,
 *   --line: #dddeda,   --paper: #fcfbf6, --amber: #9a5b18
 */
object TheekHaiColors {
    // Core
    val Green = Color(0xFF123E39)
    val GreenLight = Color(0xFF24564B)
    val GreenSurface = Color(0xFFE8EDE2)
    val Ink = Color(0xFF233C36)
    val Muted = Color(0xFF64736A)
    val Line = Color(0xFFDDDEDA)
    val Paper = Color(0xFFFCFBF6)
    val Amber = Color(0xFF9A5B18)
    val Background = Color(0xFFF5F3EC)

    // Sidebar
    val SidebarBg = Color(0xFFECEEE5)
    val SidebarBorder = Color(0xFFD9DED2)
    val NavActive = Green
    val NavHover = Color(0xFFE2E7DB)
    val NavText = Color(0xFF5B6B61)

    // Status colors
    val StatusPending = Color(0xFFF0E5CB)
    val StatusPendingText = Color(0xFF815D24)
    val StatusClosed = Color(0xFFDBE9DD)
    val StatusClosedText = Color(0xFF2F5F3E)
    val StatusDefault = Color(0xFFE5ECDF)
    val StatusDefaultText = Color(0xFF3F6150)

    // Priority
    val PriorityCritical = Color(0xFFEF4444)
    val PriorityHigh = Color(0xFFF97316)
    val PriorityMedium = Color(0xFFF59E0B)
    val PriorityLow = Color(0xFF10B981)

    // Accent
    val Cyan = Color(0xFF06B6D4)
    val CyanLight = Color(0xFF38BDF8)
    val Indigo = Color(0xFF4F46E5)
    val Emerald = Color(0xFF059669)
    val EmeraldLight = Color(0xFF34D399)
    val Rose = Color(0xFFF87171)

    // Dark mode overrides
    val DarkBg = Color(0xFF151C19)
    val DarkSurface = Color(0xFF202A26)
    val DarkCard = Color(0xFF1B2420)
    val DarkBorder = Color(0xFF3A4A43)
    val DarkMuted = Color(0xFFA9B8AD)
    val DarkText = Color(0xFFE4ECE6)
}

@Immutable
data class TheekHaiExtendedColors(
    val sidebarBg: Color,
    val sidebarBorder: Color,
    val navActive: Color,
    val navText: Color,
    val amber: Color,
    val line: Color,
    val muted: Color,
    val paper: Color,
    val ink: Color,
    val green: Color,
    val greenLight: Color,
    val greenSurface: Color,
    val cardBg: Color,
)

val LocalTheekHaiColors = staticCompositionLocalOf {
    TheekHaiExtendedColors(
        sidebarBg = TheekHaiColors.SidebarBg,
        sidebarBorder = TheekHaiColors.SidebarBorder,
        navActive = TheekHaiColors.NavActive,
        navText = TheekHaiColors.NavText,
        amber = TheekHaiColors.Amber,
        line = TheekHaiColors.Line,
        muted = TheekHaiColors.Muted,
        paper = TheekHaiColors.Paper,
        ink = TheekHaiColors.Ink,
        green = TheekHaiColors.Green,
        greenLight = TheekHaiColors.GreenLight,
        greenSurface = TheekHaiColors.GreenSurface,
        cardBg = TheekHaiColors.Paper,
    )
}

private val LightColorScheme = lightColorScheme(
    primary = TheekHaiColors.Green,
    onPrimary = Color(0xFFFFFDF5),
    primaryContainer = TheekHaiColors.GreenSurface,
    onPrimaryContainer = TheekHaiColors.Ink,
    secondary = TheekHaiColors.Amber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF0D6),
    onSecondaryContainer = TheekHaiColors.Amber,
    tertiary = TheekHaiColors.Indigo,
    onTertiary = Color.White,
    background = TheekHaiColors.Background,
    onBackground = TheekHaiColors.Ink,
    surface = TheekHaiColors.Paper,
    onSurface = TheekHaiColors.Ink,
    surfaceVariant = Color(0xFFECEEE5),
    onSurfaceVariant = TheekHaiColors.Muted,
    outline = TheekHaiColors.Line,
    outlineVariant = Color(0xFFCED6C8),
    error = Color(0xFF8A3D24),
    onError = Color.White,
    errorContainer = Color(0xFFF9EDE1),
    onErrorContainer = Color(0xFF8A3D24),
)

private val DarkColorScheme = darkColorScheme(
    primary = TheekHaiColors.CyanLight,
    onPrimary = TheekHaiColors.DarkBg,
    primaryContainer = TheekHaiColors.DarkSurface,
    onPrimaryContainer = TheekHaiColors.DarkText,
    secondary = TheekHaiColors.Amber,
    onSecondary = Color.White,
    background = TheekHaiColors.DarkBg,
    onBackground = TheekHaiColors.DarkText,
    surface = TheekHaiColors.DarkSurface,
    onSurface = TheekHaiColors.DarkText,
    surfaceVariant = TheekHaiColors.DarkCard,
    onSurfaceVariant = TheekHaiColors.DarkMuted,
    outline = TheekHaiColors.DarkBorder,
    outlineVariant = Color(0xFF475569),
    error = Color(0xFFF87171),
    onError = Color.White,
)

@Composable
fun TheekHaiTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    val extendedColors = if (useDarkTheme) {
        TheekHaiExtendedColors(
            sidebarBg = TheekHaiColors.DarkCard,
            sidebarBorder = TheekHaiColors.DarkBorder,
            navActive = Color(0xFF72C3A5),
            navText = TheekHaiColors.DarkMuted,
            amber = Color(0xFFE9B66D),
            line = TheekHaiColors.DarkBorder,
            muted = TheekHaiColors.DarkMuted,
            paper = TheekHaiColors.DarkSurface,
            ink = TheekHaiColors.DarkText,
            green = Color(0xFF72C3A5),
            greenLight = Color(0xFF23614E),
            greenSurface = Color(0xFF26392D),
            cardBg = TheekHaiColors.DarkSurface,
        )
    } else {
        TheekHaiExtendedColors(
            sidebarBg = TheekHaiColors.SidebarBg,
            sidebarBorder = TheekHaiColors.SidebarBorder,
            navActive = TheekHaiColors.NavActive,
            navText = TheekHaiColors.NavText,
            amber = TheekHaiColors.Amber,
            line = TheekHaiColors.Line,
            muted = TheekHaiColors.Muted,
            paper = TheekHaiColors.Paper,
            ink = TheekHaiColors.Ink,
            green = TheekHaiColors.Green,
            greenLight = TheekHaiColors.GreenLight,
            greenSurface = TheekHaiColors.GreenSurface,
            cardBg = TheekHaiColors.Paper,
        )
    }

    CompositionLocalProvider(LocalTheekHaiColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TheekHaiTypography,
            content = content
        )
    }
}

val TheekHaiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.5.sp,
    ),
)
