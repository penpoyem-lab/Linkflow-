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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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

private data class ExpressiveMorphPose(
    val lobes: Float,
    val amp1: Float,
    val sharp2: Float,
    val sharp3: Float,
    val scaleX: Float,
    val scaleY: Float,
    val tiltDeg: Float,
    val baseRadiusFactor: Float
)

private val ExpressiveMorphKeyframes = listOf(
    // 0: Rounded Pentagon (5 soft corners, flat-ish edges)
    ExpressiveMorphPose(lobes = 5f, amp1 = 0.085f, sharp2 = -0.020f, sharp3 = 0.004f, scaleX = 1.00f, scaleY = 1.00f, tiltDeg = -18f, baseRadiusFactor = 0.80f),
    // 1: 9-Scallop Wavy Cookie
    ExpressiveMorphPose(lobes = 9f, amp1 = 0.068f, sharp2 = 0.006f, sharp3 = 0.0f, scaleX = 1.00f, scaleY = 1.00f, tiltDeg = 12f, baseRadiusFactor = 0.81f),
    // 2: 4-Leaf Clover / Soft Quatrefoil
    ExpressiveMorphPose(lobes = 4f, amp1 = 0.145f, sharp2 = -0.024f, sharp3 = 0.0f, scaleX = 1.00f, scaleY = 1.00f, tiltDeg = 45f, baseRadiusFactor = 0.77f),
    // 3: 12-Point Sharp Starburst Cookie
    ExpressiveMorphPose(lobes = 12f, amp1 = 0.125f, sharp2 = 0.036f, sharp3 = 0.010f, scaleX = 1.00f, scaleY = 1.00f, tiltDeg = 0f, baseRadiusFactor = 0.77f),
    // 4: 10-Scallop Soft Flower Cookie
    ExpressiveMorphPose(lobes = 10f, amp1 = 0.062f, sharp2 = 0.005f, sharp3 = 0.0f, scaleX = 1.00f, scaleY = 1.00f, tiltDeg = 18f, baseRadiusFactor = 0.82f),
    // 5: Vertical Pill / Pebble Oval
    ExpressiveMorphPose(lobes = 2f, amp1 = 0.045f, sharp2 = -0.012f, sharp3 = 0.0f, scaleX = 0.77f, scaleY = 1.12f, tiltDeg = -8f, baseRadiusFactor = 0.82f),
    // 6: 8-Point Scalloped Star Cookie
    ExpressiveMorphPose(lobes = 8f, amp1 = 0.105f, sharp2 = 0.018f, sharp3 = 0.0f, scaleX = 1.00f, scaleY = 1.00f, tiltDeg = 22.5f, baseRadiusFactor = 0.79f),
    // 7: Diagonal Tilted Egg / Elliptical Bean
    ExpressiveMorphPose(lobes = 2f, amp1 = 0.050f, sharp2 = 0.0f, sharp3 = 0.0f, scaleX = 1.15f, scaleY = 0.74f, tiltDeg = -36f, baseRadiusFactor = 0.82f),
    // 8: 5-Lobe Organic Amoeba / Splash Flower
    ExpressiveMorphPose(lobes = 5f, amp1 = 0.158f, sharp2 = 0.016f, sharp3 = -0.008f, scaleX = 1.02f, scaleY = 0.98f, tiltDeg = 28f, baseRadiusFactor = 0.76f)
)

private fun evaluateExpressivePoseRadius(
    pose: ExpressiveMorphPose,
    thetaRad: Double,
    maxR: Float
): Float {
    val tiltRad = Math.toRadians(pose.tiltDeg.toDouble())
    val relAngle = thetaRad - tiltRad

    // Elliptical aspect ratio compression/expansion along pose's tilted axes
    val cosT = cos(relAngle).toFloat()
    val sinT = sin(relAngle).toFloat()
    val invX = cosT / pose.scaleX
    val invY = sinT / pose.scaleY
    val ellipseMod = 1f / kotlin.math.sqrt((invX * invX + invY * invY).coerceAtLeast(0.0001f))

    // Harmonic radial lobes (fundamental + 2nd + 3rd harmonic for crisp star tips or flat polygon sides)
    val nTheta = pose.lobes * relAngle
    val wave = 1f +
        pose.amp1 * cos(nTheta).toFloat() +
        pose.sharp2 * cos(2.0 * nTheta).toFloat() +
        pose.sharp3 * cos(3.0 * nTheta).toFloat()

    return maxR * pose.baseRadiusFactor * ellipseMod * wave
}

/**
 * Material 3 Expressive Morphing Shape Loading Indicator on a completely transparent background
 * (matching the user's uploaded reference video).
 *
 * Visual & Motion Fidelity from the reference video:
 * - Solid filled white (`Color.White` by default) organic geometric shape on a 100% transparent background.
 * - Continuously morphs through the exact sequence of Material 3 Expressive shapes shown in the video:
 *   1. Rounded Pentagon (5 soft corners)
 *   2. 9-Scallop Wavy Cookie
 *   3. 4-Leaf Clover / Quatrefoil
 *   4. 12-Point Sharp Starburst Cookie
 *   5. 10-Scallop Soft Flower
 *   6. Vertical Pill / Pebble Oval
 *   7. 8-Point Scalloped Star
 *   8. Diagonal Tilted Elliptical Bean
 *   9. 5-Lobe Organic Splash Flower
 * - Uses snappy cubic-bezier spring-like morph transitions between each distinct shape while gently rotating.
 */
@Composable
fun PlayStoreScallopedLoader(
    size: Dp = 36.dp,
    color: Color = Color(0xFF38BDF8),
    trackColor: Color = Color.Transparent,
    pullFraction: Float = 1f,
    isSpinning: Boolean = true,
    reducedMotion: Boolean = false,
    strokeRatio: Float = 0.056f,
    waveAmplitudeRatio: Float = 0.038f,
    modifier: Modifier = Modifier
) {
    val poseCount = ExpressiveMorphKeyframes.size
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_shape_morph_loader")

    // Morphs through all 9 expressive shapes over 7200ms (~800ms per shape transition, matching the video cadence)
    val rawStageProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSpinning && !reducedMotion) poseCount.toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shape_morph_stage"
    )

    // Continuous smooth rotation + subtle spring step rotation during each morph
    val continuousRotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSpinning && !reducedMotion) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shape_continuous_rotation"
    )

    // When dragging down during pull-to-refresh (before release), morph progressively through shapes with pullFraction
    val effectiveStageFloat = if (isSpinning && !reducedMotion) {
        rawStageProgress
    } else if (isSpinning) {
        3f // 12-point star static fallback when reducedMotion is enabled
    } else {
        (pullFraction.coerceIn(0f, 1.15f) * 4.2f) % poseCount.toFloat()
    }

    val stageIndex = effectiveStageFloat.toInt().coerceIn(0, poseCount - 1)
    val nextStageIndex = (stageIndex + 1) % poseCount
    val localFraction = (effectiveStageFloat - stageIndex.toFloat()).coerceIn(0f, 1f)

    // Hold each distinct shape briefly (~25% of step) then morph briskly with FastOutSlowInEasing (~75% of step)
    // so every shape (Pentagon, Clover, 12-Star, Pill, Bean, Scallop) is unmistakably recognizable like in the video!
    val morphT = if (localFraction < 0.22f) {
        0f
    } else {
        FastOutSlowInEasing.transform(((localFraction - 0.22f) / 0.78f).coerceIn(0f, 1f))
    }

    // Subtle scale pulse during the peak of each shape morph
    val morphBounceScale = if (isSpinning && !reducedMotion) {
        1f + 0.06f * sin(morphT * PI.toFloat())
    } else if (isSpinning) {
        1f
    } else {
        pullFraction.coerceIn(0.30f, 1.05f)
    }

    val stepRotationBoost = (stageIndex * 40f) + (morphT * 40f)
    val totalRotationDeg = if (isSpinning) {
        (continuousRotationDeg * 0.45f + stepRotationBoost) % 360f
    } else {
        (pullFraction * 220f)
    }

    val currentPose = ExpressiveMorphKeyframes[stageIndex]
    val nextPose = ExpressiveMorphKeyframes[nextStageIndex]
    val morphPath = remember { Path() }

    Canvas(
        modifier = modifier
            .size(size)
            .background(Color.Transparent)
            .graphicsLayer {
                scaleX = morphBounceScale
                scaleY = morphBounceScale
            }
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val maxR = this.size.minDimension / 2f

        rotate(degrees = totalRotationDeg, pivot = Offset(cx, cy)) {
            val samplePoints = 180
            morphPath.reset()

            for (i in 0 until samplePoints) {
                val theta = (i.toDouble() / samplePoints.toDouble()) * 2.0 * PI
                val r1 = evaluateExpressivePoseRadius(currentPose, theta, maxR)
                val r2 = evaluateExpressivePoseRadius(nextPose, theta, maxR)
                val r = r1 + (r2 - r1) * morphT

                val x = cx + r * cos(theta).toFloat()
                val y = cy + r * sin(theta).toFloat()

                if (i == 0) {
                    morphPath.moveTo(x, y)
                } else {
                    morphPath.lineTo(x, y)
                }
            }
            morphPath.close()

            drawPath(
                path = morphPath,
                color = color
            )
        }
    }
}

/**
 * Full-screen transparent loading overlay where a single small blue Material 3 Expressive Morphing Shape Loader
 * drops down from the top of the screen with spring physics and settles in the exact middle of
 * the screen on a completely transparent background.
 */
@Composable
fun TopDropTransparentLoadingOverlay(
    visible: Boolean,
    reducedMotion: Boolean = false,
    loaderSize: Dp = 36.dp,
    loaderColor: Color = Color(0xFF38BDF8),
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { fullHeight -> -fullHeight / 2 - 160 }
        ) + androidx.compose.animation.fadeIn(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ) + androidx.compose.animation.scaleIn(
            initialScale = 0.65f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = androidx.compose.animation.slideOutVertically(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            targetOffsetY = { fullHeight -> -fullHeight / 2 - 160 }
        ) + androidx.compose.animation.fadeOut(
            animationSpec = tween(durationMillis = 180)
        ) + androidx.compose.animation.scaleOut(
            targetScale = 0.70f,
            animationSpec = tween(durationMillis = 180)
        ),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .testTag("top_drop_center_loading_overlay"),
            contentAlignment = Alignment.Center
        ) {
            PlayStoreScallopedLoader(
                size = loaderSize,
                color = loaderColor,
                isSpinning = true,
                reducedMotion = reducedMotion
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
            .background(
                if (tokens.isDark) {
                    Brush.verticalGradient(listOf(bgColor, Color(0xFF130F26)))
                } else {
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF9F7BFF),
                            0.12f to Color(0xFFD6C7FF),
                            0.24f to Color(0xFFF3F0FA),
                            0.45f to Color(0xFFF7F7FA),
                            1.0f to Color(0xFFF5F4F9)
                        )
                    )
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top violet/lavender atmospheric header bloom (matching the reference screenshot)
            val cTop = Offset(
                x = w * (0.48f + 0.03f * cos(drift)),
                y = h * 0.02f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (tokens.isDark) tokens.accentViolet.copy(alpha = 0.28f)
                        else Color(0xFF936EFE).copy(alpha = 0.42f),
                        Color.Transparent
                    ),
                    center = cTop,
                    radius = w * 0.88f * secondaryPulse
                ),
                radius = w * 0.88f * secondaryPulse,
                center = cTop
            )

            // Bottom soft lavender glow behind floating navigation capsule
            val c3 = Offset(
                x = w * (0.50f + 0.06f * sin(drift * 1.5f)),
                y = h * 0.94f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentViolet.copy(alpha = if (tokens.isDark) 0.22f else 0.14f),
                        Color.Transparent
                    ),
                    center = c3,
                    radius = w * 0.72f * secondaryPulse
                ),
                radius = w * 0.72f * secondaryPulse,
                center = c3
            )
        }
        content()
    }
}

/**
 * Custom composable container using background blur (`Modifier.blur`), multi-stop translucent
 * gradients, thin low-contrast borders, and soft ambient shadows for an iOS-inspired liquid-glass UI.
 */
@Composable
fun LiquidGlassContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 22.dp,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_container_press_scale"
    )

    val borderBrush = if (isHighlighted) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (tokens.isDark) 0.55f else 0.95f),
                tokens.accentViolet.copy(alpha = if (tokens.isDark) 0.58f else 0.42f),
                tokens.accentBlue.copy(alpha = if (tokens.isDark) 0.50f else 0.32f),
                Color.White.copy(alpha = if (tokens.isDark) 0.35f else 0.85f)
            )
        )
    } else {
        tokens.glassBorderGradient
    }

    val translucentGradientBrush = if (isHighlighted) {
        Brush.verticalGradient(
            listOf(
                tokens.glassSurfaceElevated,
                tokens.glassSurface,
                if (tokens.isDark) Color(0x45171332) else Color(0xF2F7F4FE)
            )
        )
    } else {
        tokens.cardBackgroundGradient
    }

    val baseModifier = modifier
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .shadow(
            elevation = if (isHighlighted) 14.dp else 8.dp,
            shape = shape,
            ambientColor = tokens.accentBlue.copy(alpha = if (tokens.isDark) 0.28f else 0.14f),
            spotColor = tokens.accentViolet.copy(alpha = if (tokens.isDark) 0.34f else 0.18f)
        )
        .clip(shape)
        .border(
            width = if (isHighlighted) 1.2.dp else 0.9.dp,
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
        // Frosted background blur & translucent gradient backdrop layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(
                    radius = blurRadius,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded
                )
                .background(brush = translucentGradientBrush)
        )

        // Subtle liquid-glass top specular highlight
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width

            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (tokens.isDark) 0.36f else 0.88f),
                        tokens.accentViolet.copy(alpha = if (tokens.isDark) 0.25f else 0.18f),
                        Color.White.copy(alpha = if (tokens.isDark) 0.32f else 0.82f),
                        Color.Transparent
                    )
                ),
                start = Offset(w * 0.08f, 1.dp.toPx()),
                end = Offset(w * 0.92f, 1.dp.toPx()),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
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
    LiquidGlassContainer(
        modifier = modifier,
        cornerRadius = cornerRadius,
        blurRadius = 22.dp,
        isHighlighted = isHighlighted,
        onClick = onClick,
        content = content
    )
}

/**
 * Reusable 3D Liquid-Glass Primary Action Button with a smooth lavender-to-violet gradient fill,
 * soft ambient shadow, and tactile spring press physics.
 */
@Composable
fun LiquidGlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    cornerRadius: Dp = 18.dp
) {
    val tokens = LocalLinkFlowTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    val gradientColors = when {
        !enabled -> if (tokens.isDark) {
            listOf(Color(0xFF334155), Color(0xFF1E293B))
        } else {
            listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))
        }
        isDestructive -> listOf(
            Color(0xFFEF4444),
            Color(0xFFDC2626)
        )
        else -> listOf(
            Color(0xFFA855F7),
            tokens.accentBlue,
            Color(0xFF6366F1)
        )
    }

    val borderColors = if (isDestructive) {
        listOf(
            Color.White.copy(alpha = 0.65f),
            Color(0xFFFCA5A5).copy(alpha = 0.70f),
            Color(0xFFEF4444).copy(alpha = 0.45f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.78f),
            tokens.accentViolet.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.30f)
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) 10.dp else 2.dp,
                shape = shape,
                ambientColor = if (isDestructive) tokens.errorColor.copy(alpha = 0.35f) else tokens.accentBlue.copy(alpha = 0.35f),
                spotColor = if (isDestructive) tokens.errorColor.copy(alpha = 0.45f) else tokens.accentViolet.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(Brush.linearGradient(gradientColors))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(borderColors),
                shape = shape
            )
            .springBounceClickable(enabled = enabled, pressedScale = 0.95f, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top-edge inner glass specular sheen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                color = Color.White
            )
        }
    }
}

/**
 * Translucent Frosted Liquid-Glass Secondary Pill Button with subtle lavender border
 * and clean readable typography.
 */
@Composable
fun LiquidGlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accentTint: Color = LocalLinkFlowTokens.current.accentBlue,
    cornerRadius: Dp = 18.dp
) {
    val tokens = LocalLinkFlowTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = 5.dp,
                shape = shape,
                ambientColor = accentTint.copy(alpha = 0.14f),
                spotColor = accentTint.copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (tokens.isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.92f),
                        accentTint.copy(alpha = if (tokens.isDark) 0.14f else 0.10f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (tokens.isDark) 0.45f else 0.90f),
                        accentTint.copy(alpha = 0.35f)
                    )
                ),
                shape = shape
            )
            .springBounceClickable(pressedScale = 0.95f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (tokens.isDark) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LinkFlowLogoEmblem(
    size: Dp = 44.dp,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLinkFlowTokens.current
    val infiniteTransition = rememberInfiniteTransition(label = "logo_ring")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale)
            .shadow(8.dp, CircleShape, spotColor = tokens.accentViolet.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFA855F7),
                        Color(0xFF7C3AED),
                        Color(0xFF6366F1)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.90f),
                        tokens.accentViolet.copy(alpha = 0.45f)
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
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Material 3 Expressive Pull-to-Refresh Box with the Morphing Shape
 * indicator that drops down from the top of the screen and settles in the middle of the screen
 * on a completely transparent background.
 */
@Composable
fun PlayStorePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var pullDistancePx by remember { mutableFloatStateOf(0f) }
    val triggerThresholdPx = 180f

    val animatedPull by animateFloatAsState(
        targetValue = if (isRefreshing) triggerThresholdPx else pullDistancePx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pull_offset"
    )

    BoxWithConstraints(
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

                        if (!isRefreshing && deltaY > 20f) {
                            draggingDown = true
                            pullDistancePx = (deltaY * 0.52f).coerceIn(0f, 320f)
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
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(800f)
        val centerTargetYPx = (containerHeightPx * 0.44f)

        // GPU-accelerated translation via graphicsLayer (zero layout pass during pull)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = animatedPull * 0.24f
                }
        ) {
            content()
        }

        // Material 3 Expressive Morphing Shape Indicator on a Pure Transparent Background
        // Drops smoothly from the top of the screen (-72px) down to the middle of the screen (centerTargetYPx)
        val visibilityFraction = (animatedPull / triggerThresholdPx).coerceIn(0f, 1.15f)
        val targetDropYPx = if (isRefreshing) {
            centerTargetYPx
        } else {
            (-72f) + (visibilityFraction.coerceIn(0f, 1f) * (centerTargetYPx + 72f))
        }

        val animatedDropY by animateFloatAsState(
            targetValue = targetDropYPx,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "top_to_center_drop_y"
        )

        if (visibilityFraction > 0.03f || isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color.Transparent)
                    .graphicsLayer {
                        translationY = animatedDropY
                        alpha = if (isRefreshing) 1f else (visibilityFraction * 1.35f).coerceIn(0f, 1f)
                    }
                    .testTag("pull_to_refresh_wavy_loader"),
                contentAlignment = Alignment.Center
            ) {
                PlayStoreScallopedLoader(
                    size = 36.dp,
                    color = Color(0xFF38BDF8),
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
 * Powerful Floating Liquid-Glass Bottom Navigation Bar (`FloatingBottomNavigationBar`)
 *
 * Features:
 * - Translucent, rounded capsule design (`RoundedCornerShape(44.dp)`) with system-aware
 *   `WindowInsets.navigationBars` padding and floating ambient elevation.
 * - Multi-layer liquid-glass refraction: frosted lilac/lavender glass fill, animated ambient
 *   micro-droplet refraction canvas, top-edge specular rim highlight, and glossy dual-layer border.
 * - Spring-physics animated 3D lavender active-tab pill indicator with inner lens sheen,
 *   caustic bottom glow, and tactile press/stretch squircle physics.
 * - System-aware haptics, accessibility semantics (`Role.Tab`, 48.dp+ touch targets), and
 *   live pulsing download count badge.
 */
@Composable
fun FloatingBottomNavigationBar(
    currentTab: PrimaryTab,
    activeDownloadCount: Int,
    onSelectTab: (PrimaryTab) -> Unit,
    reducedMotion: Boolean = false,
    hapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLinkFlowTokens.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val tabs = PrimaryTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)

    val infiniteTransition = rememberInfiniteTransition(label = "floating_liquid_nav_fx")
    val badgePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reducedMotion || activeDownloadCount == 0) 1f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_pulse"
    )

    val glassShimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glass_shimmer_phase"
    )

    val droplets = remember {
        listOf(
            NavDropletParticle(0.14f, 0.28f, 18f, 1.0f, 0.16f),
            NavDropletParticle(0.38f, 0.72f, 22f, -0.8f, 0.14f),
            NavDropletParticle(0.66f, 0.25f, 16f, 1.2f, 0.15f),
            NavDropletParticle(0.86f, 0.68f, 20f, -1.0f, 0.14f)
        )
    }

    val outerCapsuleShape = RoundedCornerShape(44.dp)
    val activePillShape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("floating_bottom_navigation_bar"),
        contentAlignment = Alignment.Center
    ) {
        // Outer Ambient Lavender Halo behind the floating capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = outerCapsuleShape,
                    ambientColor = Color(0xFF7C3AED).copy(alpha = if (tokens.isDark) 0.46f else 0.30f),
                    spotColor = Color(0xFF8B5CF6).copy(alpha = if (tokens.isDark) 0.54f else 0.38f)
                )
                .clip(outerCapsuleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (tokens.isDark) {
                            listOf(
                                Color(0xE8251F47),
                                Color(0xF0191434),
                                Color(0xF5120E26)
                            )
                        } else {
                            listOf(
                                Color(0xF2E3DBFC),
                                Color(0xF0D4C8F8),
                                Color(0xECC5B6F2)
                            )
                        }
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.verticalGradient(
                        colors = if (tokens.isDark) {
                            listOf(
                                Color.White.copy(alpha = 0.48f),
                                Color(0xFFA78BFA).copy(alpha = 0.28f),
                                Color(0xFF7C3AED).copy(alpha = 0.42f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.98f),
                                Color.White.copy(alpha = 0.72f),
                                Color(0xFF8B5CF6).copy(alpha = 0.36f)
                            )
                        }
                    ),
                    shape = outerCapsuleShape
                )
        ) {
            // Subtle liquid-glass refraction & specular rim overlay inside the capsule
            Canvas(modifier = Modifier.fillMaxSize()) {
                val widthPx = size.width
                val heightPx = size.height

                // Floating ambient lavender-white glass refraction orbs
                droplets.forEachIndexed { index, droplet ->
                    val driftX = if (reducedMotion) 0f else cos(glassShimmerPhase * droplet.speed + index) * 14.dp.toPx()
                    val driftY = if (reducedMotion) 0f else sin(glassShimmerPhase * droplet.speed + index) * 4.dp.toPx()
                    val center = Offset(
                        x = (widthPx * droplet.baseXFraction) + driftX,
                        y = (heightPx * droplet.baseYFraction) + driftY
                    )
                    val radius = droplet.radiusDp.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (tokens.isDark) droplet.alpha * 0.65f else droplet.alpha * 1.45f),
                                Color(0xFFA78BFA).copy(alpha = droplet.alpha * 0.45f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 2.1f
                        ),
                        radius = radius * 2.1f,
                        center = center
                    )
                }

                // Top-edge liquid glass specular refraction highlight line
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (tokens.isDark) 0.24f else 0.72f),
                            Color.White.copy(alpha = 0.0f)
                        ),
                        startY = 0f,
                        endY = heightPx * 0.38f
                    ),
                    topLeft = Offset(8.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(widthPx - 16.dp.toPx(), heightPx * 0.36f),
                    cornerRadius = CornerRadius(40.dp.toPx(), 40.dp.toPx())
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                val tabSlotWidth = maxWidth / tabs.size

                val indicatorOffsetX by animateDpAsState(
                    targetValue = tabSlotWidth * selectedIndex,
                    animationSpec = if (reducedMotion) {
                        tween(durationMillis = 160)
                    } else {
                        spring(
                            dampingRatio = 0.74f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    },
                    label = "lavender_active_tab_offset"
                )

                // Stretch/squish morph effect during tab travel
                val normalizedProgress = if (tabSlotWidth.value > 0f) {
                    (indicatorOffsetX.value / tabSlotWidth.value) - selectedIndex.toFloat()
                } else {
                    0f
                }
                val travelStretchX = (1f + (kotlin.math.abs(normalizedProgress) * 0.08f)).coerceIn(1f, 1.08f)
                val travelSquishY = (1f - (kotlin.math.abs(normalizedProgress) * 0.04f)).coerceIn(0.95f, 1f)

                // Lavender Active-Tab Liquid Glass Pill Indicator
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffsetX)
                        .width(tabSlotWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                        .graphicsLayer {
                            scaleX = if (reducedMotion) 1f else travelStretchX
                            scaleY = if (reducedMotion) 1f else travelSquishY
                        }
                        .shadow(
                            elevation = 10.dp,
                            shape = activePillShape,
                            ambientColor = Color(0xFF7C3AED).copy(alpha = 0.28f),
                            spotColor = Color(0xFF7C3AED).copy(alpha = 0.34f)
                        )
                        .clip(activePillShape)
                        .background(
                            brush = if (tokens.isDark) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF8B5CF6).copy(alpha = 0.56f),
                                        Color(0xFF6D28D9).copy(alpha = 0.44f),
                                        Color(0xFF4C1D95).copy(alpha = 0.50f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFF3EEFF),
                                        Color(0xFFD9CCFC),
                                        Color(0xFFC1AEF9)
                                    )
                                )
                            }
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = if (tokens.isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        Color(0xFFC4B5FD).copy(alpha = 0.35f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.99f),
                                        Color.White.copy(alpha = 0.68f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.30f)
                                    )
                                }
                            ),
                            shape = activePillShape
                        )
                ) {
                    // Inner 3D convex glass lens highlight on the active lavender pill
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (tokens.isDark) 0.26f else 0.76f),
                                    Color.White.copy(alpha = 0.0f)
                                ),
                                startY = 0f,
                                endY = size.height * 0.48f
                            ),
                            topLeft = Offset(4.dp.toPx(), 1.5.dp.toPx()),
                            size = Size(size.width - 8.dp.toPx(), size.height * 0.45f),
                            cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                        )
                    }
                }

                // Tab Destination Items Row
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
                                isPressed -> 0.91f
                                isSelected -> 1.04f
                                else -> 1.0f
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "nav_item_scale_${tab.route}"
                        )

                        val iconOffsetY by animateDpAsState(
                            targetValue = if (isSelected) (-1).dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "nav_icon_offset_${tab.route}"
                        )

                        val activeColor = if (tokens.isDark) Color.White else Color(0xFF7C3AED)
                        val inactiveColor = if (tokens.isDark) Color(0xFFCBD5E1) else Color(0xFF111827)

                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) activeColor else inactiveColor,
                            animationSpec = tween(220),
                            label = "nav_icon_tint_${tab.route}"
                        )

                        val labelColor by animateColorAsState(
                            targetValue = if (isSelected) activeColor else inactiveColor,
                            animationSpec = tween(220),
                            label = "nav_label_color_${tab.route}"
                        )

                        val icon = when (tab) {
                            PrimaryTab.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                            PrimaryTab.DOWNLOADS -> if (isSelected) Icons.Filled.Download else Icons.Outlined.Download
                            PrimaryTab.LIBRARY -> if (isSelected) Icons.Filled.Folder else Icons.Outlined.Folder
                            PrimaryTab.SETTINGS -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
                        }

                        val displayLabel = when (tab) {
                            PrimaryTab.DOWNLOADS -> "Download"
                            PrimaryTab.LIBRARY -> "Library"
                            else -> tab.label
                        }

                        Column(
                            modifier = Modifier
                                .width(tabSlotWidth)
                                .fillMaxHeight()
                                .scale(itemScale)
                                .clip(activePillShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        if (hapticEnabled && currentTab != tab) {
                                            haptic.performHapticFeedback(
                                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                                            )
                                        }
                                        onSelectTab(tab)
                                    }
                                )
                                .testTag("nav_tab_${tab.route}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .offset(y = iconOffsetY),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = displayLabel,
                                    tint = iconTint,
                                    modifier = Modifier.size(23.dp)
                                )

                                if (tab == PrimaryTab.DOWNLOADS && activeDownloadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 5.dp, y = (-3).dp)
                                            .scale(badgePulse)
                                            .size(16.dp)
                                            .background(Color(0xFF7C3AED), CircleShape)
                                            .border(1.2.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = activeDownloadCount.coerceAtMost(99).toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = displayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                ),
                                color = labelColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backwards-compatible alias delegating to [FloatingBottomNavigationBar].
 */
@Composable
fun LiquidGlassBottomNavigation(
    currentTab: PrimaryTab,
    activeDownloadCount: Int,
    onSelectTab: (PrimaryTab) -> Unit,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    FloatingBottomNavigationBar(
        currentTab = currentTab,
        activeDownloadCount = activeDownloadCount,
        onSelectTab = onSelectTab,
        reducedMotion = reducedMotion,
        modifier = modifier
    )
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
