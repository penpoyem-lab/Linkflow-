package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaAnalysisResult
import com.example.data.model.MediaFormat
import com.example.data.model.QualityOption
import com.example.ui.components.AnimatedAudioWaveform
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.theme.LocalLinkFlowTokens
import com.example.util.FormatUtils

@Composable
fun QualitySelectorSheet(
    analysis: MediaAnalysisResult,
    selectedOption: QualityOption?,
    isAudioPreviewPlaying: Boolean,
    reducedMotion: Boolean,
    onSelectOption: (QualityOption) -> Unit,
    onToggleAudioPreview: () -> Unit,
    onStartDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val activeSelection = selectedOption
        ?: analysis.videoOptions.find { it.badge == "Recommended" }
        ?: analysis.videoOptions.firstOrNull()
        ?: analysis.audioOptions.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6050811))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("quality_selector_sheet")
    ) {
        LiquidGlassCard(
            cornerRadius = 36.dp,
            isHighlighted = true,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Handle Pill & Close Button matching Image 3
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, start = 20.dp, end = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(68.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .testTag("close_quality_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close quality selector",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Header Title, Thumbnail Preview & Subtitle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!analysis.thumbnailUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(width = 76.dp, height = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                        ) {
                            coil.compose.AsyncImage(
                                model = analysis.thumbnailUrl,
                                contentDescription = analysis.title,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Video Quality",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${analysis.providerName} • ${analysis.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Scrollable list of VIDEO and AUDIO ONLY (MP3) options matching Image 3
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (analysis.videoOptions.isNotEmpty()) {
                        item {
                            Text(
                                text = "VIDEO",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.6.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }

                        items(analysis.videoOptions, key = { it.id }) { option ->
                            QualityOptionRow(
                                option = option,
                                isSelected = activeSelection?.id == option.id,
                                onSelect = { onSelectOption(option) }
                            )
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }

                    if (analysis.audioOptions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AUDIO ONLY (MP3)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.6.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }

                        // Interactive Audio Waveform Preview Strip when MP3 is inspected
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x331E294B))
                                    .border(
                                        1.dp,
                                        tokens.accentViolet.copy(alpha = 0.35f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onToggleAudioPreview() }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("audio_waveform_preview_toggle"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAudioPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Preview audio waveform",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isAudioPreviewPlaying) {
                                            "Master Audio Stream Preview Active"
                                        } else {
                                            "Tap to Preview Audio Spectrum"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    AnimatedAudioWaveform(
                                        isPlaying = isAudioPreviewPlaying,
                                        reducedMotion = reducedMotion,
                                        modifier = Modifier.height(26.dp)
                                    )
                                }
                            }
                        }

                        items(analysis.audioOptions, key = { it.id }) { option ->
                            QualityOptionRow(
                                option = option,
                                isSelected = activeSelection?.id == option.id,
                                onSelect = { onSelectOption(option) }
                            )
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }

                // Bottom CTA "Download Selected" button & footnote matching Image 3
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x4D0B1021))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiquidGlassPrimaryButton(
                        text = "Download Selected",
                        icon = Icons.Default.ExpandMore,
                        onClick = onStartDownload,
                        cornerRadius = 20.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("download_selected_button")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Download will use cellular data • Manage in Settings",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityOptionRow(
    option: QualityOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "row_scale"
    )
    val rowBg by animateColorAsState(
        targetValue = if (isSelected) Color(0x4D1E3A8A) else Color.Transparent,
        label = "row_bg"
    )

    val is4K = option.badge == "4K" || option.badge == "2K"
    val isHD = option.badge == "HD" || option.badge == "1080p HD"
    val isRecommended = option.badge == "Recommended"
    val isAudio = option.format == MediaFormat.MP3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(rowBg)
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("quality_option_${option.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left circular icon badge matching Image 3
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = when {
                        isSelected -> Brush.linearGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                        )
                        isAudio -> Brush.linearGradient(
                            listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                        )
                        is4K -> Brush.linearGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFF7C3AED))
                        )
                        else -> Brush.linearGradient(
                            listOf(Color(0xFF334155), Color(0xFF1E293B))
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isSelected -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                isAudio -> {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                is4K -> {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(5.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "4K",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFF1E293B)
                        )
                    }
                }
                isHD -> {
                    Icon(
                        imageVector = Icons.Default.Hd,
                        contentDescription = null,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Center Title + Badge + SubLabel matching Image 3
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color.White
                )

                if (option.badge != null) {
                    val badgeBg = when (option.badge) {
                        "4K" -> Color(0xFFD97706)
                        "HD" -> Color(0xFF2563EB)
                        "Recommended" -> Color(0xFF3B82F6)
                        else -> Color(0xFF475569)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = option.badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            val sizeText = FormatUtils.formatBytes(option.estimatedSizeBytes)
            Text(
                text = "$sizeText • ${option.subLabel} • ${option.codec}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right circular check or download icon matching Image 3
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.08f)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.16f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Download,
                contentDescription = if (isSelected) "Selected" else "Select quality",
                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
