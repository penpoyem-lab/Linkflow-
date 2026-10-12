package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalLinkFlowTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class SplashOrb(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val alpha: Float,
    val speedFactor: Float,
    val colorIndex: Int
)

private data class PlatformNodeSpec(
    val angleDeg: Float,
    val label: String,
    val color: Color
)

@Composable
fun AnimatedSplashScreen(
    brandName: String,
    reducedMotion: Boolean,
    onSplashFinished: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val logoScale = remember { Animatable(if (reducedMotion) 1f else 0.34f) }
    val logoAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val textAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val shockwaveProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val secondShockwaveProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val badgeSlideProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val shimmerSweepProgress = remember { Animatable(if (reducedMotion) 1f else -0.4f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = if (reducedMotion) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rot"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = if (reducedMotion) 0.90f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val orbs = remember {
        listOf(
            SplashOrb(0.15f, 0.14f, 28f, 0.34f, 1.0f, 0),
            SplashOrb(0.85f, 0.18f, 38f, 0.36f, -0.85f, 1),
            SplashOrb(0.11f, 0.43f, 32f, 0.28f, 1.25f, 2),
            SplashOrb(0.89f, 0.60f, 30f, 0.32f, -1.15f, 3),
            SplashOrb(0.20f, 0.78f, 42f, 0.30f, 0.95f, 1),
            SplashOrb(0.80f, 0.82f, 24f, 0.28f, 1.45f, 2),
            SplashOrb(0.50f, 0.10f, 20f, 0.26f, -1.30f, 0)
        )
    }

    val platformNodes = remember {
        listOf(
            PlatformNodeSpec(0f, "YouTube", Color(0xFFEF4444)),
            PlatformNodeSpec(45f, "Instagram", Color(0xFFE1306C)),
            PlatformNodeSpec(90f, "Facebook", Color(0xFF1877F2)),
            PlatformNodeSpec(135f, "X", Color(0xFF38BDF8)),
            PlatformNodeSpec(180f, "Reddit", Color(0xFFFF4500)),
            PlatformNodeSpec(225f, "Spotify", Color(0xFF1DB954)),
            PlatformNodeSpec(270f, "Apollo", Color(0xFF06B6D4)),
            PlatformNodeSpec(315f, "AniVortex", Color(0xFF8B5CF6))
        )
    }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            delay(650)
            onSplashFinished()
        } else {
            launch {
                logoAlpha.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
            }
            launch {
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                shockwaveProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1150, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(240)
                secondShockwaveProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(200)
                shimmerSweepProgress.animateTo(
                    targetValue = 1.4f,
                    animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing)
                )
            }
            delay(150)
            launch {
                textAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            }
            delay(140)
            launch {
                badgeSlideProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            delay(1550)
            onSplashFinished()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (tokens.isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF070510),
                            Color(0xFF110D26),
                            Color(0xFF1B133A)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFCF9FF),
                            Color(0xFFF2EAFF),
                            Color(0xFFE5D6FF)
                        )
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSplashFinished
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Multi-layer Liquid-Glass Prism, Dual Shockwave Flash & Bokeh Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h * 0.37f)

            // 1. Rotating aurora prism rays
            val rayCount = 20
            for (i in 0 until rayCount) {
                val angleDeg = i * (360f / rayCount) + ringRotation * 0.38f
                rotate(degrees = angleDeg, pivot = center) {
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.28f),
                                Color(0xFF06B6D4).copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            start = center,
                            end = Offset(center.x, center.y - w * 0.72f)
                        ),
                        start = center,
                        end = Offset(center.x, center.y - w * 0.72f),
                        strokeWidth = if (i % 2 == 0) 7.5f else 3.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Primary expanding flash shockwave ring
            val waveR1 = w * (0.18f + 0.58f * shockwaveProgress.value)
            val waveAlpha1 = (1f - shockwaveProgress.value).coerceIn(0f, 1f) * 0.62f
            if (waveAlpha1 > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFA855F7).copy(alpha = waveAlpha1),
                            Color(0xFF38BDF8).copy(alpha = waveAlpha1 * 0.75f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = waveR1
                    ),
                    radius = waveR1,
                    center = center
                )
            }

            // 2B. Secondary harmonic shockwave ring
            val waveR2 = w * (0.16f + 0.54f * secondShockwaveProgress.value)
            val waveAlpha2 = (1f - secondShockwaveProgress.value).coerceIn(0f, 1f) * 0.48f
            if (waveAlpha2 > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF1DB954).copy(alpha = waveAlpha2 * 0.75f),
                            Color(0xFF06B6D4).copy(alpha = waveAlpha2),
                            Color.Transparent
                        ),
                        center = center,
                        radius = waveR2
                    ),
                    radius = waveR2,
                    center = center
                )
            }

            // 3. Pulsing core radial glow behind app icon
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentViolet.copy(alpha = 0.34f * pulseGlow),
                        tokens.accentBlue.copy(alpha = 0.18f * pulseGlow),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.60f * pulseGlow
                ),
                radius = w * 0.60f * pulseGlow,
                center = center
            )

            // 4. Floating glassmorphic bokeh orbs
            val radAngle = Math.toRadians(ringRotation.toDouble())
            orbs.forEach { orb ->
                val ox = w * orb.xFraction + (cos(radAngle * orb.speedFactor) * 24f).toFloat()
                val oy = h * orb.yFraction + (sin(radAngle * orb.speedFactor) * 24f).toFloat()
                val orbCenter = Offset(ox, oy)
                val rPx = orb.radiusDp.dp.toPx()
                val primaryOrbColor = when (orb.colorIndex) {
                    1 -> Color(0xFF1DB954)
                    2 -> Color(0xFF06B6D4)
                    3 -> Color(0xFFE1306C)
                    else -> Color(0xFF8B5CF6)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryOrbColor.copy(alpha = orb.alpha * 0.75f),
                            tokens.accentViolet.copy(alpha = orb.alpha * 0.40f),
                            Color.Transparent
                        ),
                        center = orbCenter,
                        radius = rPx
                    ),
                    radius = rPx,
                    center = orbCenter
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Dual-Ring Liquid-Glass Orbital App Icon Emblem with 8 Platform Constellation Nodes
            Box(
                modifier = Modifier
                    .size(226.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .testTag("splash_app_icon_emblem"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)

                    // Outer counter-rotating dashed prism arc
                    rotate(degrees = counterRotation, pivot = centerOffset) {
                        for (seg in 0 until 8) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF8B5CF6),
                                        Color(0xFF1DB954),
                                        Color(0xFF06B6D4),
                                        Color(0xFFE1306C),
                                        Color(0xFFFF4500),
                                        Color(0xFF8B5CF6)
                                    )
                                ),
                                startAngle = seg * 45f + 6f,
                                sweepAngle = 28f,
                                useCenter = false,
                                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Inner forward-rotating sweep prism ring + 8 orbiting platform constellation nodes
                    rotate(degrees = ringRotation, pivot = centerOffset) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFA855F7),
                                    Color(0xFF38BDF8),
                                    Color(0xFF1DB954),
                                    Color(0xFFE1306C),
                                    Color(0xFFA855F7)
                                )
                            ),
                            radius = r * 0.87f,
                            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        platformNodes.forEach { node ->
                            val rad = Math.toRadians(node.angleDeg.toDouble())
                            val nx = centerOffset.x + (r * 0.87f * cos(rad)).toFloat()
                            val ny = centerOffset.y + (r * 0.87f * sin(rad)).toFloat()
                            drawCircle(
                                color = node.color.copy(alpha = 0.38f),
                                radius = 9.5.dp.toPx(),
                                center = Offset(nx, ny)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 6.2.dp.toPx(),
                                center = Offset(nx, ny)
                            )
                            drawCircle(
                                color = node.color,
                                radius = 4.4.dp.toPx(),
                                center = Offset(nx, ny)
                            )
                        }
                    }
                }

                // App Launcher Icon with Luminous Halo & Diagonal Shimmer Sweep
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseGlow * 0.96f)
                        .shadow(28.dp, CircleShape, spotColor = tokens.accentViolet)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color(0xFF38BDF8).copy(alpha = 0.85f),
                                    Color(0xFFA855F7).copy(alpha = 0.90f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_linkflow_blue_cutout_1791516839274),
                        contentDescription = "$brandName app icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Diagonal flash shimmer across app icon
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sweepX = size.width * shimmerSweepProgress.value
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.45f),
                                    Color.Transparent
                                ),
                                start = Offset(sweepX - 60f, 0f),
                                end = Offset(sweepX + 60f, size.height)
                            ),
                            start = Offset(sweepX - 80f, -20f),
                            end = Offset(sweepX + 80f, size.height + 20f),
                            strokeWidth = 54.dp.toPx()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = brandName,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your media, your way • Ultra-Fast Turbo Engine",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Row 1: Video & Social Platforms Badge Pill (YouTube • Instagram • Facebook • X • Reddit)
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        val s = 0.86f + 0.14f * badgeSlideProgress.value
                        scaleX = s
                        scaleY = s
                        alpha = badgeSlideProgress.value
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (tokens.isDark) Color.White.copy(alpha = 0.08f)
                        else Color.White.copy(alpha = 0.82f)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF8B5CF6).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashPlatformDot(Color(0xFFEF4444), "YouTube")
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                SplashPlatformDot(Color(0xFFE1306C), "Insta")
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                SplashPlatformDot(Color(0xFF1877F2), "Facebook")
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                SplashPlatformDot(Color(0xFF38BDF8), "X")
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                SplashPlatformDot(Color(0xFFFF4500), "Reddit")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Music & Anime Platforms Badge Pill (Spotify • Apollo Music • AniVortex)
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        val s = 0.86f + 0.14f * badgeSlideProgress.value
                        scaleX = s
                        scaleY = s
                        alpha = badgeSlideProgress.value
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (tokens.isDark) Color.White.copy(alpha = 0.08f)
                        else Color.White.copy(alpha = 0.82f)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF06B6D4).copy(alpha = 0.38f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashPlatformDot(Color(0xFF1DB954), "Spotify")
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                SplashPlatformDot(Color(0xFF06B6D4), "Apollo Music")
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                SplashPlatformDot(Color(0xFF8B5CF6), "AniVortex")
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated Liquid Acoustic / Video Equalizer Wave Bars
            Canvas(
                modifier = Modifier
                    .width(164.dp)
                    .height(26.dp)
                    .alpha(textAlpha.value)
            ) {
                val barCount = 17
                val spacing = 4.5.dp.toPx()
                val barWidth = (size.width - spacing * (barCount - 1)) / barCount
                for (i in 0 until barCount) {
                    val norm = sin(wavePhase + i * 0.52f)
                    val factor = (0.26f + 0.74f * ((norm + 1f) / 2f)).coerceIn(0.20f, 1f)
                    val bh = size.height * factor
                    val x = i * (barWidth + spacing)
                    val y = (size.height - bh) / 2f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6),
                                Color(0xFF06B6D4),
                                Color(0xFF1DB954)
                            )
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, bh),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 38.dp)
                .background(Color.Transparent)
                .alpha(textAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            com.example.ui.components.PlayStoreScallopedLoader(
                size = 34.dp,
                color = Color(0xFF8B5CF6),
                isSpinning = true,
                reducedMotion = reducedMotion
            )
        }
    }
}

@Composable
private fun SplashPlatformDot(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
