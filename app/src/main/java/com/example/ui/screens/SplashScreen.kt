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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.math.cos
import kotlin.math.sin

private data class SplashOrb(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val alpha: Float,
    val speedFactor: Float
)

@Composable
fun AnimatedSplashScreen(
    brandName: String,
    reducedMotion: Boolean,
    onSplashFinished: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val logoScale = remember { Animatable(if (reducedMotion) 1f else 0.55f) }
    val logoAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val textAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )

    val orbs = remember {
        listOf(
            SplashOrb(0.18f, 0.16f, 22f, 0.25f, 1.0f),
            SplashOrb(0.82f, 0.22f, 34f, 0.30f, -0.8f),
            SplashOrb(0.14f, 0.42f, 28f, 0.22f, 1.2f),
            SplashOrb(0.86f, 0.64f, 24f, 0.28f, -1.1f),
            SplashOrb(0.24f, 0.80f, 38f, 0.26f, 0.9f),
            SplashOrb(0.76f, 0.84f, 18f, 0.20f, 1.4f),
            SplashOrb(0.50f, 0.12f, 14f, 0.18f, -1.3f)
        )
    }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            delay(650)
            onSplashFinished()
        } else {
            launch {
                logoAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
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
            delay(220)
            launch {
                textAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            }
            delay(1300)
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050811))
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h * 0.43f)

            val rayCount = 12
            for (i in 0 until rayCount) {
                val angleDeg = i * (360f / rayCount) + ringRotation * 0.25f
                rotate(degrees = angleDeg, pivot = center) {
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tokens.accentCyan.copy(alpha = 0.22f),
                                tokens.accentViolet.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            start = center,
                            end = Offset(center.x, center.y - w * 0.65f)
                        ),
                        start = center,
                        end = Offset(center.x, center.y - w * 0.65f),
                        strokeWidth = if (i % 2 == 0) 6f else 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentCyan.copy(alpha = 0.32f),
                        tokens.accentBlue.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.56f
                ),
                radius = w * 0.56f,
                center = center
            )

            val radAngle = Math.toRadians(ringRotation.toDouble())
            orbs.forEach { orb ->
                val ox = w * orb.xFraction + (cos(radAngle * orb.speedFactor) * 18f).toFloat()
                val oy = h * orb.yFraction + (sin(radAngle * orb.speedFactor) * 18f).toFloat()
                val orbCenter = Offset(ox, oy)
                val rPx = orb.radiusDp.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = orb.alpha * 0.9f),
                            tokens.accentCyan.copy(alpha = orb.alpha * 0.6f),
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
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(205.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Luminous rotating ring around the cobalt-blue liquid-glass orb icon
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF38BDF8),
                                Color(0xFFE0F2FE),
                                Color(0xFF2563EB),
                                Color(0xFF22D3EE),
                                Color(0xFF38BDF8)
                            )
                        ),
                        radius = r * 0.94f,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.img_linkflow_blue_cutout_1791516839274),
                    contentDescription = "$brandName icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(178.dp)
                        .shadow(24.dp, CircleShape, spotColor = tokens.accentCyan)
                        .clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = brandName,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = Color(0xFFE0F2FE),
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "LIQUID-GLASS MEDIA ENGINE",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.8.sp
                ),
                color = tokens.accentCyan.copy(alpha = 0.85f),
                modifier = Modifier.alpha(textAlpha.value)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
                .alpha(textAlpha.value),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..3) {
                val activeDot = i == 1 || i == 2
                Box(
                    modifier = Modifier
                        .size(if (activeDot) 10.dp else 6.dp)
                        .background(
                            color = if (activeDot) Color(0xFF22D3EE) else Color(0x8838BDF8),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
