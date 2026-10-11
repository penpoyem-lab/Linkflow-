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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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

@Composable
fun AnimatedSplashScreen(
    brandName: String,
    reducedMotion: Boolean,
    onSplashFinished: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val logoScale = remember { Animatable(if (reducedMotion) 1f else 0.42f) }
    val logoAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val textAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val shockwaveProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val badgeSlideProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = if (reducedMotion) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rot"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = if (reducedMotion) 0.88f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val orbs = remember {
        listOf(
            SplashOrb(0.16f, 0.15f, 26f, 0.30f, 1.0f, 0),
            SplashOrb(0.84f, 0.20f, 36f, 0.32f, -0.85f, 1),
            SplashOrb(0.12f, 0.44f, 30f, 0.25f, 1.25f, 2),
            SplashOrb(0.88f, 0.62f, 28f, 0.30f, -1.15f, 0),
            SplashOrb(0.22f, 0.78f, 40f, 0.28f, 0.95f, 1),
            SplashOrb(0.78f, 0.82f, 22f, 0.24f, 1.45f, 2),
            SplashOrb(0.50f, 0.11f, 18f, 0.22f, -1.30f, 0)
        )
    }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            delay(650)
            onSplashFinished()
        } else {
            launch {
                logoAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
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
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
                )
            }
            delay(180)
            launch {
                textAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
            }
            delay(160)
            launch {
                badgeSlideProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            delay(1320)
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (tokens.isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF090714),
                            Color(0xFF130F2A),
                            Color(0xFF1B1438)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFCF9FF),
                            Color(0xFFF3ECFF),
                            Color(0xFFE8DDFF)
                        )
                    )
                }
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Multi-layer Liquid-Glass Prism & Shockwave Flash Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h * 0.40f)

            // 1. Rotating aurora prism rays
            val rayCount = 16
            for (i in 0 until rayCount) {
                val angleDeg = i * (360f / rayCount) + ringRotation * 0.35f
                rotate(degrees = angleDeg, pivot = center) {
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.24f),
                                Color(0xFF38BDF8).copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            start = center,
                            end = Offset(center.x, center.y - w * 0.68f)
                        ),
                        start = center,
                        end = Offset(center.x, center.y - w * 0.68f),
                        strokeWidth = if (i % 2 == 0) 7f else 3.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Expanding flash shockwave ring
            val waveR = w * (0.20f + 0.52f * shockwaveProgress.value)
            val waveAlpha = (1f - shockwaveProgress.value).coerceIn(0f, 1f) * 0.55f
            if (waveAlpha > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFA855F7).copy(alpha = waveAlpha),
                            Color(0xFF38BDF8).copy(alpha = waveAlpha * 0.7f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = waveR
                    ),
                    radius = waveR,
                    center = center
                )
            }

            // 3. Pulsing core radial glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentViolet.copy(alpha = 0.30f * pulseGlow),
                        tokens.accentBlue.copy(alpha = 0.15f * pulseGlow),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.58f * pulseGlow
                ),
                radius = w * 0.58f * pulseGlow,
                center = center
            )

            // 4. Floating glassmorphic bokeh orbs (Violet, Spotify Emerald, Sky Blue)
            val radAngle = Math.toRadians(ringRotation.toDouble())
            orbs.forEach { orb ->
                val ox = w * orb.xFraction + (cos(radAngle * orb.speedFactor) * 22f).toFloat()
                val oy = h * orb.yFraction + (sin(radAngle * orb.speedFactor) * 22f).toFloat()
                val orbCenter = Offset(ox, oy)
                val rPx = orb.radiusDp.dp.toPx()
                val primaryOrbColor = when (orb.colorIndex) {
                    1 -> Color(0xFF1DB954)
                    2 -> Color(0xFF38BDF8)
                    else -> Color(0xFF8B5CF6)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryOrbColor.copy(alpha = orb.alpha * 0.72f),
                            tokens.accentViolet.copy(alpha = orb.alpha * 0.38f),
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
            modifier = Modifier.padding(bottom = 36.dp)
        ) {
            // Dual-Ring Liquid-Glass Orbital Logo Emblem
            Box(
                modifier = Modifier
                    .size(208.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)

                    // Outer counter-rotating dashed orbital arc
                    rotate(degrees = counterRotation, pivot = centerOffset) {
                        for (seg in 0 until 6) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF8B5CF6),
                                        Color(0xFF1DB954),
                                        Color(0xFF38BDF8),
                                        Color(0xFFEC4899),
                                        Color(0xFF8B5CF6)
                                    )
                                ),
                                startAngle = seg * 60f + 8f,
                                sweepAngle = 36f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Inner forward-rotating sweep prism ring + orbiting satellite nodes
                    rotate(degrees = ringRotation, pivot = centerOffset) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFA855F7),
                                    Color(0xFFC084FC),
                                    Color(0xFF7C3AED),
                                    Color(0xFF38BDF8),
                                    Color(0xFFA855F7)
                                )
                            ),
                            radius = r * 0.88f,
                            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 3 Orbiting luminous nodes representing Video, Music & Anime streams
                        val nodeAngles = listOf(0f, 120f, 240f)
                        val nodeColors = listOf(Color(0xFFEF4444), Color(0xFF1DB954), Color(0xFF8B5CF6))
                        nodeAngles.forEachIndexed { idx, deg ->
                            val rad = Math.toRadians(deg.toDouble())
                            val nx = centerOffset.x + (r * 0.88f * cos(rad)).toFloat()
                            val ny = centerOffset.y + (r * 0.88f * sin(rad)).toFloat()
                            drawCircle(
                                color = Color.White,
                                radius = 6.5.dp.toPx(),
                                center = Offset(nx, ny)
                            )
                            drawCircle(
                                color = nodeColors[idx],
                                radius = 4.5.dp.toPx(),
                                center = Offset(nx, ny)
                            )
                        }
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.img_linkflow_blue_cutout_1791516839274),
                    contentDescription = "$brandName icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(156.dp)
                        .scale(pulseGlow * 0.96f)
                        .shadow(24.dp, CircleShape, spotColor = tokens.accentViolet)
                        .clip(CircleShape)
                        .border(2.5.dp, Color.White.copy(alpha = 0.88f), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = brandName,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your media, your way.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Multi-Platform Engine Pill (YouTube • Spotify • AniVortex • MP3/MP4)
            Row(
                modifier = Modifier
                    .scale(0.85f + 0.15f * badgeSlideProgress.value)
                    .alpha(badgeSlideProgress.value)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (tokens.isDark) Color.White.copy(alpha = 0.08f)
                        else Color.White.copy(alpha = 0.78f)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF8B5CF6).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
                Text(
                    text = "YouTube",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1DB954))
                )
                Text(
                    text = "Spotify",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6))
                )
                Text(
                    text = "AniVortex",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated Liquid Acoustic / Video Equalizer Wave Bars
            Canvas(
                modifier = Modifier
                    .width(148.dp)
                    .height(26.dp)
                    .alpha(textAlpha.value)
            ) {
                val barCount = 15
                val spacing = 5.dp.toPx()
                val barWidth = (size.width - spacing * (barCount - 1)) / barCount
                for (i in 0 until barCount) {
                    val norm = sin(wavePhase + i * 0.55f)
                    val factor = (0.28f + 0.72f * ((norm + 1f) / 2f)).coerceIn(0.22f, 1f)
                    val bh = size.height * factor
                    val x = i * (barWidth + spacing)
                    val y = (size.height - bh) / 2f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6),
                                Color(0xFF38BDF8),
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
                .padding(bottom = 42.dp)
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

