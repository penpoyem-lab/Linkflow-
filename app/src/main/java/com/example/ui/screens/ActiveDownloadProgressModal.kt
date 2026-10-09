package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadJobState
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.springBounceClickable
import com.example.ui.theme.LocalLinkFlowTokens
import com.example.util.FormatUtils
import kotlin.math.cos
import kotlin.math.sin

private data class ConfettiParticle(
    val xFraction: Float,
    val yFraction: Float,
    val widthDp: Float,
    val heightDp: Float,
    val color: Color,
    val baseAngle: Float,
    val speedFactor: Float
)

@Composable
fun ActiveDownloadProgressModal(
    task: DownloadTaskEntity,
    reducedMotion: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRunInBackground: () -> Unit,
    onPlayMedia: () -> Unit,
    onShareMedia: () -> Unit,
    onDownloadAnother: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val stateEnum = runCatching { DownloadJobState.valueOf(task.state) }
        .getOrDefault(DownloadJobState.DOWNLOADING)

    val isCompleted = stateEnum == DownloadJobState.COMPLETED
    val isFailedOrCancelled = stateEnum == DownloadJobState.FAILED || stateEnum == DownloadJobState.CANCELLED
    val isPaused = stateEnum == DownloadJobState.PAUSED

    val animatedFraction by animateFloatAsState(
        targetValue = (task.progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "progress_fraction"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "modal_fx")
    val phaseAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_angle"
    )

    val confettiParticles = remember {
        listOf(
            ConfettiParticle(0.14f, 0.18f, 9f, 5f, Color(0xFFFBBF24), 25f, 1.1f),
            ConfettiParticle(0.84f, 0.16f, 10f, 6f, Color(0xFF38BDF8), -40f, 0.9f),
            ConfettiParticle(0.22f, 0.30f, 8f, 8f, Color(0xFFF472B6), 15f, 1.3f),
            ConfettiParticle(0.78f, 0.28f, 11f, 5f, Color(0xFFA855F7), 55f, 1.0f),
            ConfettiParticle(0.10f, 0.44f, 9f, 5f, Color(0xFF34D399), -20f, 1.2f),
            ConfettiParticle(0.88f, 0.46f, 10f, 6f, Color(0xFFFBBF24), 35f, 0.8f),
            ConfettiParticle(0.18f, 0.56f, 8f, 5f, Color(0xFF60A5FA), -60f, 1.4f),
            ConfettiParticle(0.82f, 0.58f, 9f, 6f, Color(0xFFF472B6), 45f, 1.1f),
            ConfettiParticle(0.30f, 0.22f, 7f, 7f, Color(0xFF22D3EE), 12f, 0.95f),
            ConfettiParticle(0.70f, 0.21f, 8f, 5f, Color(0xFFFDE047), -30f, 1.15f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2060A15))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("active_download_progress_modal")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rad = Math.toRadians(phaseAngle.toDouble())

            confettiParticles.forEach { p ->
                val px = w * p.xFraction + (cos(rad * p.speedFactor) * 14f).toFloat()
                val py = h * p.yFraction + (sin(rad * p.speedFactor) * 16f).toFloat()
                rotate(degrees = p.baseAngle + phaseAngle * 0.4f, pivot = Offset(px, py)) {
                    drawRect(
                        color = p.color.copy(alpha = if (isCompleted) 0.9f else 0.72f),
                        topLeft = Offset(px, py),
                        size = Size(p.widthDp.dp.toPx(), p.heightDp.dp.toPx())
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 18.dp)
            ) {
                val headerText = when {
                    isCompleted -> "Download Complete"
                    isFailedOrCancelled -> stateEnum.displayName
                    task.format == "MP3" -> "Downloading Audio"
                    else -> "Downloading Video"
                }

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                val sizeSubtitle = if (task.totalBytes > 0) {
                    "${task.fileName} • ${FormatUtils.formatBytes(task.downloadedBytes)} of ${FormatUtils.formatBytes(task.totalBytes)}"
                } else {
                    "${task.fileName} • ${FormatUtils.formatBytes(task.downloadedBytes)}"
                }
                Text(
                    text = sizeSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier.size(265.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 22.dp.toPx()
                    val diameter = size.minDimension - strokePx * 1.6f
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = diameter / 2f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isCompleted) tokens.successColor.copy(alpha = 0.28f)
                                else if (isFailedOrCancelled) tokens.errorColor.copy(alpha = 0.25f)
                                else tokens.accentCyan.copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.25f
                        ),
                        radius = radius * 1.25f,
                        center = center
                    )

                    drawCircle(
                        color = Color(0x4D38BDF8),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokePx)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = radius + strokePx * 0.48f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val sweepAngle = 360f * animatedFraction
                    val arcBrush = when {
                        isCompleted -> Brush.sweepGradient(
                            listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF22D3EE), Color(0xFF10B981))
                        )
                        isFailedOrCancelled -> Brush.sweepGradient(
                            listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFEF4444))
                        )
                        else -> Brush.sweepGradient(
                            listOf(
                                Color(0xFF22D3EE),
                                Color(0xFF38BDF8),
                                Color(0xFF818CF8),
                                Color(0xFF22D3EE)
                            )
                        )
                    }

                    drawArc(
                        brush = arcBrush,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokePx * 0.82f, cap = StrokeCap.Round)
                    )

                    val dropletAngles = listOf(35f, 140f, 225f, 310f)
                    dropletAngles.forEachIndexed { idx, baseDeg ->
                        val deg = baseDeg + phaseAngle * (if (idx % 2 == 0) 0.15f else -0.15f)
                        val rRad = Math.toRadians(deg.toDouble())
                        val dx = center.x + (radius + strokePx * 0.55f) * cos(rRad).toFloat()
                        val dy = center.y + (radius + strokePx * 0.55f) * sin(rRad).toFloat()
                        drawCircle(
                            color = Color(0xAA7DD3FC),
                            radius = (5 + idx * 1.5f).dp.toPx(),
                            center = Offset(dx, dy)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(tokens.successColor.copy(alpha = 0.22f))
                                .border(2.dp, tokens.successColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = tokens.successColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    } else if (isFailedOrCancelled) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Failed or Cancelled",
                            tint = tokens.errorColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = "${task.progressPercent}%",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.testTag("modal_progress_percentage")
                    )

                    val statusSubtitle = when (stateEnum) {
                        DownloadJobState.COMPLETED -> "Download complete."
                        DownloadJobState.PAUSED -> "Paused"
                        DownloadJobState.FAILED -> task.errorMessage ?: "Failed"
                        DownloadJobState.CANCELLED -> "Cancelled"
                        DownloadJobState.PROCESSING -> "Finalizing container…"
                        else -> "Downloading..."
                    }

                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF93C5FD),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (stateEnum == DownloadJobState.DOWNLOADING && task.speedBytesPerSec > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${FormatUtils.formatSpeed(task.speedBytesPerSec)} • ${FormatUtils.formatEta(task.etaSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // Frosted Liquid-Glass Media Card (using real format icon badge instead of demo image)
            LiquidGlassCard(
                cornerRadius = 26.dp,
                isHighlighted = true,
                onClick = if (isCompleted) onPlayMedia else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 74.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(tokens.accentBlue.copy(alpha = 0.4f), tokens.accentViolet.copy(alpha = 0.4f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!task.thumbnailUrl.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = task.thumbnailUrl,
                                contentDescription = task.title,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = if (task.format == "MP3") Icons.Default.AudioFile else Icons.Default.VideoFile,
                                contentDescription = task.title,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = task.fileName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${task.qualityLabel} • ${task.resolutionOrBitrate} • ${task.providerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isCompleted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCapsuleActionButton(
                            label = "Play File",
                            icon = Icons.Default.PlayArrow,
                            borderTint = tokens.successColor,
                            onClick = onPlayMedia,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("modal_play_button")
                        )
                        GlassCapsuleActionButton(
                            label = "Share",
                            icon = Icons.Default.Share,
                            borderTint = tokens.accentCyan,
                            onClick = onShareMedia,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("modal_share_button")
                        )
                    }
                    GlassCapsuleActionButton(
                        label = "Download Another",
                        icon = Icons.Default.DownloadDone,
                        borderTint = tokens.accentBlue,
                        onClick = onDownloadAnother,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_done_button")
                    )
                } else if (isFailedOrCancelled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCapsuleActionButton(
                            label = "Close",
                            icon = Icons.Default.Close,
                            borderTint = tokens.errorColor,
                            onClick = onRunInBackground,
                            modifier = Modifier.weight(1f)
                        )
                        GlassCapsuleActionButton(
                            label = "Retry Job",
                            icon = Icons.Default.Refresh,
                            borderTint = tokens.accentCyan,
                            onClick = onRetry,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCapsuleActionButton(
                            label = "Cancel",
                            icon = Icons.Default.Close,
                            borderTint = Color(0xFFEF4444),
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("modal_cancel_button")
                        )

                        GlassCapsuleActionButton(
                            label = "Background",
                            icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew,
                            borderTint = Color(0xFF38BDF8),
                            onClick = onRunInBackground,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("modal_background_button")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                if (isPaused) onResume() else onPause()
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("modal_pause_resume_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPaused) "Resume Transfer" else "Pause Transfer",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF93C5FD)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Waves,
                        contentDescription = null,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Runs in background • You can switch tabs and it will continue",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassCapsuleActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    borderTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        borderTint.copy(alpha = 0.18f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        borderTint.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.45f),
                        borderTint.copy(alpha = 0.65f)
                    )
                ),
                shape = RoundedCornerShape(30.dp)
            )
            .springBounceClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}
