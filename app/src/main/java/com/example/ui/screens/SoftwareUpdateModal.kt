package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.OtaInstallPhase
import com.example.data.service.OtaUpdateUiState
import com.example.ui.components.LinkFlowLogoEmblem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.springBounceClickable
import com.example.ui.theme.LocalLinkFlowTokens
import com.example.util.FormatUtils

@Composable
fun SoftwareUpdateModal(
    otaState: OtaUpdateUiState,
    brandName: String,
    reducedMotion: Boolean,
    onInstallNow: () -> Unit,
    onCancelDownload: () -> Unit,
    onGrantInstallPermission: () -> Unit,
    onDismissLater: () -> Unit
) {
    val release = otaState.releaseInfo ?: return
    val tokens = LocalLinkFlowTokens.current
    val scrollState = rememberScrollState()

    var entered by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(Unit) {
        entered = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ota_modal_scale"
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "ota_modal_alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ota_ambient_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (reducedMotion) 0.25f else 0.62f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ota_glow_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEB040711))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("software_update_modal"),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            cornerRadius = 32.dp,
            isHighlighted = true,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Header Block: "Software Update" + "Version X.Y is ready to install"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Software Update",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.testTag("software_update_title")
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(tokens.accentBlue.copy(alpha = 0.22f))
                                .border(1.dp, tokens.accentCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = release.tagName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = tokens.accentCyan
                            )
                        }
                    }

                    Text(
                        text = "Version ${release.cleanVersionName} is ready to install",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.testTag("software_update_subtitle")
                    )
                }

                // 2. "What's new" Section + Markdown Summary Paragraph
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "What's new",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color.White
                    )

                    Text(
                        text = release.summaryText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 21.sp,
                            fontSize = 14.sp
                        ),
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.testTag("software_update_summary")
                    )
                }

                // 3. Branded Liquid-Glass Feature Artwork Banner (Inspired by reference screenshot, customized for LinkFlow)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0A122C),
                                    Color(0xFF132352),
                                    Color(0xFF091534)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    tokens.accentCyan.copy(alpha = 0.55f),
                                    tokens.accentViolet.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.12f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = glowPulse * 0.45f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.5f),
                                radius = size.minDimension * 0.85f
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LinkFlowLogoEmblem(size = 66.dp, animated = !reducedMotion)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = brandName,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                ),
                                color = Color.White
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = tokens.accentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Liquid-Glass OTA Release • ${FormatUtils.formatBytes(release.apkAsset.sizeBytes)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFBAE6FD)
                                )
                            }
                        }
                    }
                }

                // 4. "Top Features" Heading & Clean Bulleted Changelog
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Top Features",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color.White
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        release.topFeatures.forEachIndexed { idx, feature ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("software_update_feature_$idx"),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(tokens.accentCyan)
                                )
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp
                                    ),
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 5. Security & Package Verification Metadata Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = tokens.successColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Installed: v${otaState.installedVersionName} → New: v${release.cleanVersionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Text(
                        text = release.apkAsset.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accentCyan
                    )
                }

                // 6. Live Download / Verification / Error Status Section
                AnimatedVisibility(
                    visible = otaState.phase == OtaInstallPhase.DOWNLOADING ||
                        otaState.phase == OtaInstallPhase.VERIFYING ||
                        otaState.phase == OtaInstallPhase.REQUIRES_UNKNOWN_SOURCES_PERMISSION ||
                        otaState.phase == OtaInstallPhase.ERROR
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(
                                1.dp,
                                if (otaState.phase == OtaInstallPhase.ERROR) tokens.errorColor.copy(alpha = 0.5f)
                                else tokens.accentCyan.copy(alpha = 0.4f),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (otaState.phase) {
                            OtaInstallPhase.DOWNLOADING, OtaInstallPhase.VERIFYING -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        com.example.ui.components.PlayStoreScallopedLoader(
                                            size = 24.dp,
                                            color = Color(0xFFEBD0C7),
                                            trackColor = Color(0xFF5D423B),
                                            isSpinning = true,
                                            reducedMotion = reducedMotion
                                        )
                                        Text(
                                            text = otaState.statusMessage ?: "Downloading update…",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "${otaState.progressPercent}%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = tokens.accentCyan
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { (otaState.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = tokens.accentCyan,
                                    trackColor = Color.White.copy(alpha = 0.12f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${FormatUtils.formatBytes(otaState.downloadedBytes)} / ${FormatUtils.formatBytes(otaState.totalBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "SHA-256 & Signature Guard Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.successColor
                                    )
                                }
                            }

                            OtaInstallPhase.REQUIRES_UNKNOWN_SOURCES_PERMISSION -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = tokens.warningColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Android requires permission to install updates from this app. Tap 'Allow & Install' below to open system settings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }

                            OtaInstallPhase.ERROR -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = tokens.errorColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = otaState.errorMessage ?: "Update failed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 7. Prominent "Install Now" Action Button + Dismissive "Later" Text Button at Base
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryButtonLabel = when (otaState.phase) {
                        OtaInstallPhase.DOWNLOADING -> "Cancel Download"
                        OtaInstallPhase.VERIFYING -> "Verifying Package…"
                        OtaInstallPhase.REQUIRES_UNKNOWN_SOURCES_PERMISSION -> "Allow & Install Now"
                        OtaInstallPhase.ERROR -> "Retry Install Now"
                        else -> "Install Now"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (otaState.phase == OtaInstallPhase.DOWNLOADING) {
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF2563EB),
                                            Color(0xFF3B82F6),
                                            Color(0xFF06B6D4)
                                        )
                                    )
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .springBounceClickable(
                                enabled = otaState.phase != OtaInstallPhase.VERIFYING,
                                onClick = {
                                    when (otaState.phase) {
                                        OtaInstallPhase.DOWNLOADING -> onCancelDownload()
                                        OtaInstallPhase.REQUIRES_UNKNOWN_SOURCES_PERMISSION -> onGrantInstallPermission()
                                        else -> onInstallNow()
                                    }
                                }
                            )
                            .testTag("software_update_install_now_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = primaryButtonLabel,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    if (release.isMandatory) {
                        Text(
                            text = "This is a required security & compatibility update.",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.warningColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        TextButton(
                            onClick = onDismissLater,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("software_update_later_button")
                        ) {
                            Text(
                                text = "Later",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}
