package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class LinkFlowDesignTokens(
    val isDark: Boolean,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassBorder: Color,
    val glassBorderSubtle: Color,
    val accentCyan: Color,
    val accentBlue: Color,
    val accentViolet: Color,
    val primaryGradient: Brush,
    val glassBorderGradient: Brush,
    val cardBackgroundGradient: Brush,
    val successColor: Color,
    val warningColor: Color,
    val errorColor: Color
)

val LocalLinkFlowTokens = staticCompositionLocalOf {
    LinkFlowDesignTokens(
        isDark = true,
        glassSurface = GlassSurfaceDark,
        glassSurfaceElevated = GlassSurfaceHighlightDark,
        glassBorder = GlassBorderDark,
        glassBorderSubtle = GlassBorderSubtleDark,
        accentCyan = LuminousCyanBright,
        accentBlue = ElectricBlueBright,
        accentViolet = NeonVioletBright,
        primaryGradient = Brush.linearGradient(listOf(ElectricBlueBright, LuminousCyanBright)),
        glassBorderGradient = Brush.linearGradient(
            listOf(
                Color(0x9938BDF8),
                Color(0x2694A3B8),
                Color(0x66818CF8)
            )
        ),
        cardBackgroundGradient = Brush.verticalGradient(
            listOf(
                Color(0x401E294B),
                Color(0x220F172A)
            )
        ),
        successColor = EmeraldSuccess,
        warningColor = Color(0xFFF59E0B),
        errorColor = CoralError
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlueBright,
    onPrimary = Color.White,
    primaryContainer = MidnightElevated,
    onPrimaryContainer = LuminousCyanBright,
    secondary = LuminousCyanBright,
    onSecondary = MidnightObsidian,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = NeonVioletBright,
    onTertiary = Color.White,
    background = MidnightObsidian,
    onBackground = TextPrimaryDark,
    surface = MidnightDeep,
    onSurface = TextPrimaryDark,
    surfaceVariant = MidnightSurface,
    onSurfaceVariant = TextSecondaryDark,
    error = CoralError,
    onError = Color.White,
    outline = GlassBorderDark,
    outlineVariant = GlassBorderSubtleDark
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = LuminousCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = NeonViolet,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightElevated,
    onSurfaceVariant = TextSecondaryLight,
    error = CoralError,
    onError = Color.White,
    outline = GlassBorderLight,
    outlineVariant = Color(0xFFCBD5E1)
)

val LinkFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun LinkFlowTheme(
    themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    accentPreset: String = "ELECTRIC_CYAN", // "ELECTRIC_CYAN", "VIOLET_PULSE", "EMERALD_GLOW"
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemInDarkTheme()
        else -> true
    }

    val (primaryAccent, secondaryAccent) = when (accentPreset) {
        "VIOLET_PULSE" -> NeonVioletBright to ElectricBlueBright
        "EMERALD_GLOW" -> EmeraldSuccess to LuminousCyanBright
        else -> ElectricBlueBright to LuminousCyanBright
    }

    val colorScheme = if (isDark) {
        DarkColorScheme.copy(
            primary = primaryAccent,
            secondary = secondaryAccent
        )
    } else {
        LightColorScheme.copy(
            primary = primaryAccent,
            secondary = secondaryAccent
        )
    }

    val tokens = if (isDark) {
        LinkFlowDesignTokens(
            isDark = true,
            glassSurface = GlassSurfaceDark,
            glassSurfaceElevated = GlassSurfaceHighlightDark,
            glassBorder = primaryAccent.copy(alpha = 0.38f),
            glassBorderSubtle = GlassBorderSubtleDark,
            accentCyan = secondaryAccent,
            accentBlue = primaryAccent,
            accentViolet = NeonVioletBright,
            primaryGradient = Brush.linearGradient(listOf(primaryAccent, secondaryAccent)),
            glassBorderGradient = Brush.linearGradient(
                listOf(
                    secondaryAccent.copy(alpha = 0.65f),
                    Color(0x2694A3B8),
                    primaryAccent.copy(alpha = 0.45f)
                )
            ),
            cardBackgroundGradient = Brush.verticalGradient(
                listOf(
                    Color(0x4D1E294B),
                    Color(0x260B1021)
                )
            ),
            successColor = EmeraldSuccess,
            warningColor = Color(0xFFF59E0B),
            errorColor = CoralError
        )
    } else {
        LinkFlowDesignTokens(
            isDark = false,
            glassSurface = GlassSurfaceLight,
            glassSurfaceElevated = Color(0xE6FFFFFF),
            glassBorder = primaryAccent.copy(alpha = 0.35f),
            glassBorderSubtle = Color(0x3364748B),
            accentCyan = secondaryAccent,
            accentBlue = primaryAccent,
            accentViolet = NeonViolet,
            primaryGradient = Brush.linearGradient(listOf(primaryAccent, secondaryAccent)),
            glassBorderGradient = Brush.linearGradient(
                listOf(
                    primaryAccent.copy(alpha = 0.55f),
                    Color(0x3394A3B8),
                    secondaryAccent.copy(alpha = 0.45f)
                )
            ),
            cardBackgroundGradient = Brush.verticalGradient(
                listOf(
                    Color(0xE6FFFFFF),
                    Color(0xCCF1F5F9)
                )
            ),
            successColor = EmeraldSuccess,
            warningColor = Color(0xFFD97706),
            errorColor = CoralError
        )
    }

    CompositionLocalProvider(LocalLinkFlowTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = LinkFlowShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    LinkFlowTheme(themeMode = if (darkTheme) "DARK" else "LIGHT", content = content)
}
