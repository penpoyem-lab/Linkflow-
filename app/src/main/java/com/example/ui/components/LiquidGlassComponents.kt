package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PrimaryTab
import com.example.ui.theme.LocalLinkFlowTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Staggered spring entrance animation for list items, cards, and sections.
 * Respects reducedMotion accessibility preference.
 */
@Composable
fun StaggeredAnimatedEntrance(
    index: Int = 0,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val offsetY = remember { Animatable(if (reducedMotion) 0f else 36f) }
    val scale = remember { Animatable(if (reducedMotion) 1f else 0.94f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            alpha.snapTo(1f)
            offsetY.snapTo(0f)
            scale.snapTo(1f)
        } else {
            val staggerDelay = (index.coerceIn(0, 12) * 52L)
            if (staggerDelay > 0) delay(staggerDelay)
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                )
            }
            launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = offsetY.value
            this.scaleX = scale.value
            this.scaleY = scale.value
        }
    ) {
        content()
    }
}

/**
 * Spring-physics tactile bounce modifier for buttons, chips, and interactive pills.
 */
fun Modifier.springBounceClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce_click_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
}

/**
 * Google Play Store / Material 3 Expressive Morphing Scalloped Cookie Star Loader
 * on a completely transparent background (matching the user's uploaded reference image).
 *
 * Features:
 * - Soft sky-blue (#A8C7FA) 10-lobe scalloped cookie / wavy star shape
 * - Smooth organic lobe breathing/morphing between a rounded scalloped badge and a deeper wavy star
 * - Continuous silky rotation + spring scale pop on a pure transparent background
 */
@Composable
fun PlayStoreScallopedLoader(
    size: Dp = 44.dp,
    color: Color = Color(0xFFA8C7FA),
    pullFraction: Float = 1f,
    isSpinning: Boolean = true,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scallop_loader")
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSpinning && !reducedMotion) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scallop_rotation"
    )

    val morphPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSpinning && !reducedMotion) (2f * PI.toFloat()) else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scallop_morph"
    )

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = if (isSpinning && !reducedMotion) 1.07f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scallop_breath"
    )

    val effectiveRotation = if (isSpinning) {
        rotationDeg
    } else {
        pullFraction * 240f
    }

    val effectiveScale = if (isSpinning) {
        breathScale
    } else {
        pullFraction.coerceIn(0.25f, 1.05f)
    }

    val path = remember { Path() }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = effectiveScale
                scaleY = effectiveScale
            }
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val baseRadius = this.size.minDimension * 0.42f

        // 10-lobe scalloped cookie / wavy star matching the reference image
        val lobes = 10
        val waveAmplitude = baseRadius * (0.105f + 0.035f * sin(morphPhase))
        val steps = 140

        path.reset()
        for (i in 0..steps) {
            val theta = (i.toFloat() / steps.toFloat()) * (2.0 * PI)
            // Secondary harmonic gives the smooth rounded cookie-scallop crests
            val r = baseRadius +
                waveAmplitude * cos(lobes * theta).toFloat() +
                (waveAmplitude * 0.18f) * sin((lobes / 2) * theta + morphPhase).toFloat()
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        rotate(degrees = effectiveRotation, pivot = Offset(cx, cy)) {
            drawPath(
                path = path,
                color = color
            )
        }
    }
}

@Composable
fun AmbientMidnightBackground(
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val bgColor = MaterialTheme.colorScheme.background
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_bg")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    val secondaryPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = if (reducedMotion) 1f else 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondary_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left electric blue nebula
            val c1 = Offset(
                x = w * (0.20f + 0.08f * cos(drift)),
                y = h * (0.12f + 0.05f * sin(drift))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentBlue.copy(alpha = if (tokens.isDark) 0.21f else 0.11f),
                        Color.Transparent
                    ),
                    center = c1,
                    radius = w * 0.74f * secondaryPulse
                ),
                radius = w * 0.74f * secondaryPulse,
                center = c1
            )

            // Middle-right violet/cyan aurora
            val c2 = Offset(
                x = w * (0.85f - 0.07f * sin(drift)),
                y = h * (0.45f + 0.06f * cos(drift))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentViolet.copy(alpha = if (tokens.isDark) 0.16f else 0.08f),
                        Color.Transparent
                    ),
                    center = c2,
                    radius = w * 0.68f
                ),
                radius = w * 0.68f,
                center = c2
            )

            // Bottom cyan liquid caustic glow behind navigation bar
            val c3 = Offset(
                x = w * (0.50f + 0.09f * sin(drift * 1.5f)),
                y = h * 0.92f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentCyan.copy(alpha = if (tokens.isDark) 0.18f else 0.10f),
                        Color.Transparent
                    ),
                    center = c3,
                    radius = w * 0.78f * secondaryPulse
                ),
                radius = w * 0.78f * secondaryPulse,
                center = c3
            )
        }
        content()
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_press_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "card_refraction")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isHighlighted) 1000f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "card_shimmer"
    )

    val borderBrush = if (isHighlighted) {
        Brush.linearGradient(
            colors = listOf(
                tokens.accentCyan,
                tokens.accentBlue,
                tokens.accentViolet,
                tokens.accentCyan
            ),
            start = Offset(shimmerPhase, 0f),
            end = Offset(shimmerPhase + 520f, 520f)
        )
    } else {
        tokens.glassBorderGradient
    }

    val baseModifier = modifier
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .shadow(
            elevation = if (isHighlighted) 18.dp else 8.dp,
            shape = shape,
            ambientColor = tokens.accentBlue.copy(alpha = 0.28f),
            spotColor = tokens.accentCyan.copy(alpha = 0.34f)
        )
        .clip(shape)
        .background(
            brush = if (isHighlighted) {
                Brush.verticalGradient(
                    listOf(
                        tokens.glassSurfaceElevated,
                        tokens.glassSurface
                    )
                )
            } else {
                tokens.cardBackgroundGradient
            }
        )
        .border(
            width = if (isHighlighted) 1.5.dp else 1.dp,
            brush = borderBrush,
            shape = shape
        )

    val clickableModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
    } else {
        baseModifier
    }

    Box(
        modifier = clickableModifier
    ) {
        // Subtle top-edge specular refraction highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isHighlighted) 0.42f else 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

@Composable
fun LinkFlowLogoEmblem(
    size: Dp = 40.dp,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLinkFlowTokens.current
    val infiniteTransition = rememberInfiniteTransition(label = "logo_ring")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rot"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale)
            .shadow(10.dp, CircleShape, spotColor = tokens.accentCyan)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        tokens.accentCyan,
                        Color.White.copy(alpha = 0.85f),
                        tokens.accentBlue,
                        tokens.accentViolet,
                        tokens.accentCyan
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_linkflow_blue_cutout_1791516839274),
            contentDescription = "LinkFlow emblem",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .rotate(haloRotation * 0.02f)
        )
    }
}

/**
 * Google Play Store-style Pull-to-Refresh Box with the Scalloped Cookie Star
 * indicator rendered on a completely transparent background.
 */
@Composable
fun PlayStorePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var pullDistancePx by remember { mutableFloatStateOf(0f) }
    val triggerThresholdPx = 185f

    val animatedPull by animateFloatAsState(
        targetValue = if (isRefreshing) 145f else pullDistancePx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pull_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isRefreshing) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val startY = down.position.y
                    var draggingDown = false

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        val deltaY = change.position.y - startY

                        if (!isRefreshing && deltaY > 22f) {
                            draggingDown = true
                            pullDistancePx = (deltaY * 0.50f).coerceIn(0f, 280f)
                        } else if (deltaY < 0f && draggingDown) {
                            pullDistancePx = 0f
                            draggingDown = false
                        }
                    } while (event.changes.any { it.pressed })

                    if (draggingDown) {
                        if (pullDistancePx >= triggerThresholdPx && !isRefreshing) {
                            onRefresh()
                        }
                        pullDistancePx = 0f
                    }
                }
            }
    ) {
        // GPU-accelerated translation via graphicsLayer (zero layout pass during pull)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = animatedPull * 0.26f
                }
        ) {
            content()
        }

        // Google Play Store-style Scalloped Cookie Star on a Pure Transparent Background
        val visibilityFraction = (animatedPull / triggerThresholdPx).coerceIn(0f, 1.15f)
        if (visibilityFraction > 0.04f || isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = (animatedPull * 0.48f) + 22f
                        alpha = if (isRefreshing) 1f else (visibilityFraction * 1.25f).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                PlayStoreScallopedLoader(
                    size = 46.dp,
                    color = Color(0xFFA8C7FA),
                    pullFraction = visibilityFraction,
                    isSpinning = isRefreshing
                )
            }
        }
    }
}

private data class NavDropletParticle(
    val baseXFraction: Float,
    val baseYFraction: Float,
    val radiusDp: Float,
    val speed: Float,
    val alpha: Float
)

/**
 * Ultra-polished Liquid-Glass Navigation Bar with:
 * - Multi-layered translucent glass dock & animated prismatic refraction rim
 * - Spring-physics sliding liquid-glass droplet pill that glides smoothly between tabs
 * - Specular top lens reflection & floating micro-droplet particles inside the glass
 * - Tactile icon bounce, levitation offset, and pulsing active badge
 */
@Composable
fun LiquidGlassBottomNavigation(
    currentTab: PrimaryTab,
    activeDownloadCount: Int,
    onSelectTab: (PrimaryTab) -> Unit,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLinkFlowTokens.current
    val tabs = PrimaryTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_nav_fx")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nav_wave_phase"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = if (reducedMotion) 300f else 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nav_shimmer"
    )

    val badgePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reducedMotion || activeDownloadCount == 0) 1f else 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_pulse"
    )

    val droplets = remember {
        listOf(
            NavDropletParticle(0.12f, 0.30f, 3.5f, 1.1f, 0.28f),
            NavDropletParticle(0.28f, 0.72f, 2.5f, -0.9f, 0.22f),
            NavDropletParticle(0.48f, 0.24f, 4.0f, 1.3f, 0.30f),
            NavDropletParticle(0.68f, 0.68f, 3.0f, -1.2f, 0.25f),
            NavDropletParticle(0.86f, 0.35f, 3.5f, 0.8f, 0.26f)
        )
    }

    val outerShape = RoundedCornerShape(38.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ambient neon aura beneath the floating glass dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .shadow(
                    elevation = 26.dp,
                    shape = outerShape,
                    ambientColor = tokens.accentBlue.copy(alpha = 0.55f),
                    spotColor = tokens.accentCyan.copy(alpha = 0.65f)
                )
                .clip(outerShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (tokens.isDark) {
                            listOf(
                                Color(0xD815203E),
                                Color(0xEB090E1E)
                            )
                        } else {
                            listOf(
                                Color(0xF0FFFFFF),
                                Color(0xE0E6F0FF)
                            )
                        }
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            tokens.accentCyan.copy(alpha = 0.80f),
                            tokens.accentViolet.copy(alpha = 0.65f),
                            tokens.accentBlue.copy(alpha = 0.80f),
                            Color.White.copy(alpha = 0.55f)
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 480f, 220f)
                    ),
                    shape = outerShape
                )
        ) {
            // Internal Liquid-Glass Caustics, Specular Highlights & Floating Micro-Droplets
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Soft liquid wave sheen across the bottom of the dock
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            tokens.accentBlue.copy(alpha = 0.10f),
                            tokens.accentCyan.copy(alpha = 0.18f),
                            tokens.accentViolet.copy(alpha = 0.12f),
                            tokens.accentBlue.copy(alpha = 0.10f)
                        )
                    ),
                    topLeft = Offset(0f, h * 0.45f),
                    size = Size(w, h * 0.55f),
                    cornerRadius = CornerRadius(38.dp.toPx(), 38.dp.toPx())
                )

                // Top-edge curved glass specular reflection bar
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.65f),
                            tokens.accentCyan.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.08f, 1.5.dp.toPx()),
                    end = Offset(w * 0.92f, 1.5.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Floating liquid micro-droplets inside the glass bar
                droplets.forEach { d ->
                    val dx = w * d.baseXFraction + (cos(wavePhase * d.speed) * 10f)
                    val dy = h * d.baseYFraction + (sin(wavePhase * d.speed) * 5f)
                    val rPx = d.radiusDp.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = d.alpha),
                                tokens.accentCyan.copy(alpha = d.alpha * 0.5f),
                                Color.Transparent
                            ),
                            center = Offset(dx, dy),
                            radius = rPx * 1.8f
                        ),
                        radius = rPx * 1.8f,
                        center = Offset(dx, dy)
                    )
                }
            }

            // Sliding Liquid-Glass Droplet Indicator + Interactive Tab Items
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                val tabSlotWidth = maxWidth / tabs.size

                // Smooth spring-physics horizontal position for the liquid droplet capsule
                val indicatorOffsetX by animateDpAsState(
                    targetValue = tabSlotWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "liquid_pill_offset_x"
                )

                // Sliding 3D Liquid-Glass Capsule Indicator behind active tab
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffsetX)
                        .width(tabSlotWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(30.dp),
                            ambientColor = tokens.accentBlue,
                            spotColor = tokens.accentCyan
                        )
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    tokens.accentBlue.copy(alpha = 0.85f),
                                    tokens.accentCyan.copy(alpha = 0.72f),
                                    tokens.accentViolet.copy(alpha = 0.78f)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    tokens.accentCyan.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.25f)
                                )
                            ),
                            shape = RoundedCornerShape(30.dp)
                        )
                ) {
                    // Inner top glass lens specular sheen on the sliding droplet
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .height(10.dp)
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.42f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Foreground Row of Tab Icons + Animated Labels
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentTab == tab
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val itemScale by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 0.90f
                                isSelected -> 1.07f
                                else -> 1.0f
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "nav_item_scale_${tab.route}"
                        )

                        val iconOffsetY by animateDpAsState(
                            targetValue = if (isSelected) (-2).dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "nav_icon_offset_${tab.route}"
                        )

                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            },
                            animationSpec = tween(250),
                            label = "nav_icon_tint_${tab.route}"
                        )

                        val labelColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                            },
                            animationSpec = tween(250),
                            label = "nav_label_color_${tab.route}"
                        )

                        val icon = when (tab) {
                            PrimaryTab.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                            PrimaryTab.DOWNLOADS -> if (isSelected) Icons.Filled.Download else Icons.Outlined.Download
                            PrimaryTab.LIBRARY -> if (isSelected) Icons.Filled.History else Icons.Outlined.History
                            PrimaryTab.SETTINGS -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
                        }

                        val displayLabel = if (tab == PrimaryTab.LIBRARY) "Library" else tab.label

                        Column(
                            modifier = Modifier
                                .width(tabSlotWidth)
                                .fillMaxHeight()
                                .scale(itemScale)
                                .clip(RoundedCornerShape(30.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = { onSelectTab(tab) }
                                )
                                .testTag("nav_tab_${tab.route}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(y = iconOffsetY)
                                    .size(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = displayLabel,
                                    tint = iconTint,
                                    modifier = Modifier.size(if (isSelected) 23.dp else 21.dp)
                                )

                                if (tab == PrimaryTab.DOWNLOADS && activeDownloadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 5.dp, y = (-3).dp)
                                            .scale(badgePulse)
                                            .size(17.dp)
                                            .shadow(4.dp, CircleShape, spotColor = tokens.accentCyan)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    listOf(tokens.accentCyan, Color(0xFF38BDF8))
                                                ),
                                                shape = CircleShape
                                            )
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = activeDownloadCount.toString(),
                                            color = Color(0xFF050811),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(1.dp))

                            Text(
                                text = displayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = if (isSelected) 11.5.sp else 10.5.sp,
                                    letterSpacing = if (isSelected) 0.3.sp else 0.sp
                                ),
                                color = labelColor
                            )

                            val dotSize by animateDpAsState(
                                targetValue = if (isSelected) 4.dp else 0.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "nav_dot_${tab.route}"
                            )
                            if (dotSize > 0.dp) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(dotSize)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedAudioWaveform(
    isPlaying: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLinkFlowTokens.current
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying && !reducedMotion) 6.28318f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val barWeights = remember {
        listOf(
            0.35f, 0.55f, 0.85f, 0.45f, 0.95f, 0.65f, 0.40f, 0.80f,
            1.0f, 0.60f, 0.75f, 0.50f, 0.90f, 0.42f, 0.78f, 0.62f,
            0.88f, 0.48f, 0.70f, 0.38f, 0.82f, 0.54f, 0.68f, 0.40f
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        val count = barWeights.size
        val spacing = size.width / (count * 1.6f)
        val barWidth = spacing * 0.75f
        val maxBarHeight = size.height * 0.9f

        barWeights.forEachIndexed { index, baseWeight ->
            val dynamicMod = if (isPlaying && !reducedMotion) {
                0.50f + 0.50f * ((sin(phase + index * 0.55f) + 1f) / 2f)
            } else {
                0.65f
            }
            val bh = (maxBarHeight * baseWeight * dynamicMod).coerceAtLeast(6f)
            val x = index * (barWidth + spacing * 0.65f)
            val y = (size.height - bh) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        tokens.accentCyan,
                        tokens.accentViolet
                    )
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, bh),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
