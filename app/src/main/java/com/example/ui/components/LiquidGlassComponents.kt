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
    size: Dp = 52.dp,
    color: Color = Color.White,
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
 * Full-screen transparent loading overlay where the Material 3 Expressive Morphing Shape Loader
 * drops down from the top of the screen with spring physics and settles in the exact middle of
 * the screen on a completely transparent background.
 */
@Composable
fun TopDropTransparentLoadingOverlay(
    visible: Boolean,
    reducedMotion: Boolean = false,
    loaderSize: Dp = 62.dp,
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
                color = Color.White,
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
                        tokens.accentCyan.copy(alpha = if (tokens.isDark) 0.22f else 0.12f),
                        Color.Transparent
                    ),
                    center = c3,
                    radius = w * 0.78f * secondaryPulse
                ),
                radius = w * 0.78f * secondaryPulse,
                center = c3
            )

            // Subtle floating glass lens bokeh orbs in the ambient background
            val orbOffsets = listOf(
                Triple(0.16f, 0.24f, 44f),
                Triple(0.84f, 0.18f, 32f),
                Triple(0.76f, 0.68f, 52f),
                Triple(0.22f, 0.74f, 38f)
            )
            orbOffsets.forEachIndexed { index, (bx, by, rDp) ->
                val phaseShift = drift + index * 1.4f
                val ox = w * bx + cos(phaseShift) * 18f
                val oy = h * by + sin(phaseShift) * 14f
                val rPx = rDp.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (tokens.isDark) 0.06f else 0.14f),
                            tokens.accentCyan.copy(alpha = if (tokens.isDark) 0.04f else 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(ox, oy),
                        radius = rPx * 1.6f
                    ),
                    radius = rPx * 1.6f,
                    center = Offset(ox, oy)
                )
            }
        }
        content()
    }
}

/**
 * Custom composable container using background blur (`Modifier.blur`), multi-stop translucent
 * gradients, specular rim lighting, and caustic refraction to achieve a 'liquid-glass' UI aesthetic.
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
        targetValue = if (isPressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_container_press_scale"
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
                Color.White.copy(alpha = 0.78f),
                tokens.accentCyan.copy(alpha = 0.90f),
                tokens.accentBlue.copy(alpha = 0.80f),
                tokens.accentViolet.copy(alpha = 0.78f),
                Color.White.copy(alpha = 0.65f)
            ),
            start = Offset(shimmerPhase, 0f),
            end = Offset(shimmerPhase + 540f, 540f)
        )
    } else {
        tokens.glassBorderGradient
    }

    val translucentGradientBrush = if (isHighlighted) {
        Brush.verticalGradient(
            listOf(
                tokens.glassSurfaceElevated,
                tokens.glassSurface,
                if (tokens.isDark) Color(0x360C142C) else Color(0xCCF1F5F9)
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
            elevation = if (isHighlighted) 20.dp else 10.dp,
            shape = shape,
            ambientColor = tokens.accentBlue.copy(alpha = if (isHighlighted) 0.42f else 0.26f),
            spotColor = tokens.accentCyan.copy(alpha = if (isHighlighted) 0.50f else 0.32f)
        )
        .clip(shape)
        .border(
            width = if (isHighlighted) 1.5.dp else 1.1.dp,
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

        // Multi-layered Liquid-Glass Specular Crown & Diagonal Caustic Sheen
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // 1. Soft upper-left liquid lens refraction glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isHighlighted) 0.14f else 0.08f),
                        tokens.accentCyan.copy(alpha = if (isHighlighted) 0.08f else 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.18f, 0f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.18f, 0f)
            )

            // 2. Bottom-right ambient violet/blue depth reflection
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentViolet.copy(alpha = if (isHighlighted) 0.12f else 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.85f, h)
            )

            // 3. Top-edge curved glass specular highlight bar
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isHighlighted) 0.68f else 0.38f),
                        tokens.accentCyan.copy(alpha = if (isHighlighted) 0.52f else 0.28f),
                        Color.White.copy(alpha = if (isHighlighted) 0.62f else 0.35f),
                        Color.Transparent
                    )
                ),
                start = Offset(w * 0.07f, 1.2.dp.toPx()),
                end = Offset(w * 0.93f, 1.2.dp.toPx()),
                strokeWidth = 1.6.dp.toPx(),
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
 * Reusable 3D Liquid-Glass Primary Action Button with specular top lens reflection,
 * luminous gradient fill, and tactile spring press physics.
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
        !enabled -> listOf(Color(0xFF334155), Color(0xFF1E293B))
        isDestructive -> listOf(
            Color(0xFFEF4444).copy(alpha = 0.85f),
            Color(0xFFDC2626).copy(alpha = 0.75f)
        )
        else -> listOf(
            tokens.accentBlue.copy(alpha = 0.92f),
            tokens.accentCyan.copy(alpha = 0.82f),
            tokens.accentViolet.copy(alpha = 0.85f)
        )
    }

    val borderColors = if (isDestructive) {
        listOf(
            Color.White.copy(alpha = 0.65f),
            Color(0xFFFCA5A5).copy(alpha = 0.80f),
            Color(0xFFEF4444).copy(alpha = 0.55f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.82f),
            tokens.accentCyan.copy(alpha = 0.75f),
            Color.White.copy(alpha = 0.35f)
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) 14.dp else 2.dp,
                shape = shape,
                ambientColor = if (isDestructive) tokens.errorColor else tokens.accentBlue,
                spotColor = if (isDestructive) tokens.errorColor else tokens.accentCyan
            )
            .clip(shape)
            .background(Brush.linearGradient(gradientColors))
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(borderColors),
                shape = shape
            )
            .springBounceClickable(enabled = enabled, pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top-edge inner glass lens specular sheen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.38f),
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
 * Translucent Frosted Liquid-Glass Secondary Pill Button with prismatic rim border
 * and top specular highlight.
 */
@Composable
fun LiquidGlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accentTint: Color = LocalLinkFlowTokens.current.accentCyan,
    cornerRadius: Dp = 18.dp
) {
    val tokens = LocalLinkFlowTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = accentTint.copy(alpha = 0.22f),
                spotColor = accentTint.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (tokens.isDark) 0.14f else 0.78f),
                        accentTint.copy(alpha = if (tokens.isDark) 0.14f else 0.18f),
                        tokens.glassSurfaceElevated
                    )
                )
            )
            .border(
                width = 1.1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        accentTint.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.22f)
                    )
                ),
                shape = shape
            )
            .springBounceClickable(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    size = 56.dp,
                    color = Color.White,
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
