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
        isDark = false,
        glassSurface = FrostedWhiteSurface,
        glassSurfaceElevated = FrostedWhiteElevated,
        glassBorder = GlassBorderLavender,
        glassBorderSubtle = GlassBorderSubtleLight,
        accentCyan = LilacAccent,
        accentBlue = VioletPrimary,
        accentViolet = VioletDeep,
        primaryGradient = Brush.linearGradient(listOf(LilacAccent, VioletPrimary, IndigoAccent)),
        glassBorderGradient = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.92f),
                LilacAccent.copy(alpha = 0.32f),
                Color(0x2894A3B8),
                VioletPrimary.copy(alpha = 0.25f)
            )
        ),
        cardBackgroundGradient = Brush.verticalGradient(
            listOf(
                Color(0xF5FFFFFF),
                Color(0xEBFAF8FF)
            )
        ),
        successColor = EmeraldSuccess,
        warningColor = Color(0xFFD97706),
        errorColor = CoralError
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = LilacAccent,
    onPrimary = Color.White,
    primaryContainer = MidnightElevated,
    onPrimaryContainer = LavenderSoft,
    secondary = VioletPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = IndigoAccent,
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
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = LavenderTint,
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = LilacAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    background = PearlWhiteBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightElevated,
    onSurfaceVariant = TextSecondaryLight,
    error = CoralError,
    onError = Color.White,
    outline = GlassBorderLavender,
    outlineVariant = Color(0xFFE2E8F0)
)

val LinkFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun LinkFlowTheme(
    themeMode: String = "LIGHT", // "LIGHT" (default), "DARK", "SYSTEM"
    accentPreset: String = "VIOLET_PULSE", // "VIOLET_PULSE" (default), "ELECTRIC_CYAN", "EMERALD_GLOW"
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "DARK" -> true
        "SYSTEM" -> isSystemInDarkTheme()
        else -> false
    }

    val (primaryAccent, secondaryAccent) = when (accentPreset) {
        "ELECTRIC_CYAN" -> Color(0xFF6366F1) to Color(0xFF8B5CF6)
        "EMERALD_GLOW" -> EmeraldSuccess to Color(0xFF14B8A6)
        else -> VioletPrimary to LilacAccent
    }

    val colorScheme = if (isDark) {
        DarkColorScheme.copy(
            primary = secondaryAccent,
            secondary = primaryAccent
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
            glassSurface = Color(0x521B1738),
            glassSurfaceElevated = Color(0x6E27224E),
            glassBorder = secondaryAccent.copy(alpha = 0.36f),
            glassBorderSubtle = Color(0x2E94A3B8),
            accentCyan = secondaryAccent,
            accentBlue = primaryAccent,
            accentViolet = LilacAccent,
            primaryGradient = Brush.linearGradient(listOf(secondaryAccent, primaryAccent, IndigoAccent)),
            glassBorderGradient = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.32f),
                    secondaryAccent.copy(alpha = 0.45f),
                    Color(0x2294A3B8),
                    primaryAccent.copy(alpha = 0.35f)
                )
            ),
            cardBackgroundGradient = Brush.verticalGradient(
                listOf(
                    Color(0x66221D42),
                    Color(0x4816132E)
                )
            ),
            successColor = EmeraldSuccess,
            warningColor = Color(0xFFF59E0B),
            errorColor = CoralError
        )
    } else {
        LinkFlowDesignTokens(
            isDark = false,
            glassSurface = Color(0xEBFFFFFF),
            glassSurfaceElevated = Color(0xF8FFFFFF),
            glassBorder = primaryAccent.copy(alpha = 0.24f),
            glassBorderSubtle = Color(0x2664748B),
            accentCyan = primaryAccent,
            accentBlue = primaryAccent,
            accentViolet = secondaryAccent,
            primaryGradient = Brush.linearGradient(
                listOf(
                    Color(0xFFA855F7),
                    primaryAccent,
                    Color(0xFF6366F1)
                )
            ),
            glassBorderGradient = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    primaryAccent.copy(alpha = 0.22f),
                    Color(0x1F94A3B8),
                    secondaryAccent.copy(alpha = 0.20f)
                )
            ),
            cardBackgroundGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFAFFFFFF),
                    Color(0xF0FAF8FF),
                    Color(0xE8F3EFFE)
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
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    LinkFlowTheme(themeMode = if (darkTheme) "DARK" else "LIGHT", content = content)
}
