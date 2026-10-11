package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadJobState
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.components.LiquidGlassSecondaryButton
import com.example.ui.components.PlayStorePullToRefreshBox
import com.example.ui.components.PlayStoreScallopedLoader
import com.example.ui.components.StaggeredAnimatedEntrance
import com.example.ui.components.springBounceClickable
import com.example.ui.theme.LocalLinkFlowTokens
import com.example.util.FormatUtils

private enum class DownloadsFilterSegment(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETE("Complete"),
    FAILED("Failed")
}

@Composable
fun DownloadsQueueScreen(
    allDownloads: List<DownloadTaskEntity>,
    searchQuery: String,
    isRefreshing: Boolean,
    reducedMotion: Boolean = false,
    onSearchQueryChange: (String) -> Unit,
    onPullToRefresh: () -> Unit,
    onOpenLiveProgressModal: (String) -> Unit,
    onOpenJobDetails: (String) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onPlayFile: (DownloadTaskEntity) -> Unit,
    onShareFile: (DownloadTaskEntity) -> Unit,
    onClearCompleted: () -> Unit,
    onClearFailed: () -> Unit = {},
    onExploreHome: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    var selectedSegment by remember { mutableStateOf(DownloadsFilterSegment.ALL) }
    var deleteConfirmTask by remember { mutableStateOf<DownloadTaskEntity?>(null) }

    val searchFiltered = if (searchQuery.isBlank()) {
        allDownloads
    } else {
        val q = searchQuery.trim().lowercase()
        allDownloads.filter {
            it.title.lowercase().contains(q) ||
                it.fileName.lowercase().contains(q) ||
                it.qualityLabel.lowercase().contains(q) ||
                it.providerName.lowercase().contains(q)
        }
    }

    val activeStates = setOf(
        DownloadJobState.QUEUED.name,
        DownloadJobState.ANALYZING.name,
        DownloadJobState.PREPARING.name,
        DownloadJobState.DOWNLOADING.name,
        DownloadJobState.PROCESSING.name,
        DownloadJobState.PAUSED.name
    )

    val activeCount = allDownloads.count { it.state in activeStates }
    val completedCount = allDownloads.count { it.state == DownloadJobState.COMPLETED.name }
    val failedCount = allDownloads.count {
        it.state == DownloadJobState.FAILED.name || it.state == DownloadJobState.CANCELLED.name
    }

    val segmentFiltered = remember(searchFiltered, selectedSegment) {
        when (selectedSegment) {
            DownloadsFilterSegment.ALL -> searchFiltered
            DownloadsFilterSegment.ACTIVE -> searchFiltered.filter { it.state in activeStates }
            DownloadsFilterSegment.COMPLETE -> searchFiltered.filter { it.state == DownloadJobState.COMPLETED.name }
            DownloadsFilterSegment.FAILED -> searchFiltered.filter {
                it.state == DownloadJobState.FAILED.name || it.state == DownloadJobState.CANCELLED.name
            }
        }
    }

    PlayStorePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onPullToRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .testTag("downloads_lazy_column"),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 116.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header matching reference screenshot: Left circular back button, centered "Downloads" title, right circular notification bell button
            item {
                StaggeredAnimatedEntrance(index = 0, reducedMotion = reducedMotion) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = CircleShape,
                                        ambientColor = Color.Black.copy(alpha = 0.08f),
                                        spotColor = tokens.accentViolet.copy(alpha = 0.18f)
                                    )
                                    .clip(CircleShape)
                                    .background(if (tokens.isDark) tokens.glassSurfaceElevated else Color(0xFFF3EFFB))
                                    .border(
                                        width = 1.dp,
                                        color = if (tokens.isDark) tokens.glassBorderSubtle else Color.White,
                                        shape = CircleShape
                                    )
                                    .springBounceClickable { onExploreHome() }
                                    .testTag("downloads_back_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Back to Home",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Downloads",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (allDownloads.isNotEmpty()) {
                                    Text(
                                        text = "$activeCount active • $completedCount completed • $failedCount failed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = CircleShape,
                                        ambientColor = Color.Black.copy(alpha = 0.08f),
                                        spotColor = tokens.accentViolet.copy(alpha = 0.18f)
                                    )
                                    .clip(CircleShape)
                                    .background(if (tokens.isDark) tokens.glassSurfaceElevated else Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = if (tokens.isDark) tokens.glassBorderSubtle else Color.White,
                                        shape = CircleShape
                                    )
                                    .springBounceClickable {
                                        if (failedCount > 0) {
                                            onClearFailed()
                                        } else if (completedCount > 0) {
                                            onClearCompleted()
                                        } else {
                                            onPullToRefresh()
                                        }
                                    }
                                    .testTag("downloads_bell_action_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                                    contentDescription = "Refresh or clear finished jobs",
                                    tint = if (tokens.isDark) Color.White else Color(0xFF1E1B4B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (failedCount > 0 || completedCount > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (failedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(tokens.errorColor.copy(alpha = 0.14f))
                                            .springBounceClickable { onClearFailed() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("clear_failed_button")
                                    ) {
                                        Text(
                                            text = "Clear Failed ($failedCount)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = tokens.errorColor
                                        )
                                    }
                                }
                                if (completedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(tokens.accentBlue.copy(alpha = 0.12f))
                                            .springBounceClickable { onClearCompleted() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("clear_completed_button")
                                    ) {
                                        Text(
                                            text = "Clear Completed ($completedCount)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = tokens.accentBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Crisp White Capsule Segmented Filter (matching the reference screenshot: rounded white pill container + solid purple active pill)
            item {
                StaggeredAnimatedEntrance(index = 1, reducedMotion = reducedMotion) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(30.dp),
                                    ambientColor = Color(0xFF7C3AED).copy(alpha = 0.12f),
                                    spotColor = Color(0xFF7C3AED).copy(alpha = 0.18f)
                                )
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (tokens.isDark) tokens.glassSurfaceElevated else Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (tokens.isDark) tokens.glassBorderSubtle else Color.White,
                                    shape = RoundedCornerShape(30.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DownloadsFilterSegment.entries.forEach { segment ->
                                    val isSelected = selectedSegment == segment
                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.0f else 0.97f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "seg_scale_${segment.name}"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .scale(scale)
                                            .then(
                                                if (isSelected) {
                                                    Modifier
                                                        .shadow(
                                                            elevation = 6.dp,
                                                            shape = RoundedCornerShape(26.dp),
                                                            ambientColor = Color(0xFF7C3AED).copy(alpha = 0.30f),
                                                            spotColor = Color(0xFF7C3AED).copy(alpha = 0.35f)
                                                        )
                                                        .clip(RoundedCornerShape(26.dp))
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(
                                                                    Color(0xFF8B5CF6),
                                                                    Color(0xFF7C3AED)
                                                                )
                                                            )
                                                        )
                                                } else {
                                                    Modifier
                                                        .clip(RoundedCornerShape(26.dp))
                                                        .background(Color.Transparent)
                                                }
                                            )
                                            .springBounceClickable(pressedScale = 0.94f) {
                                                selectedSegment = segment
                                            }
                                            .padding(vertical = 11.dp)
                                            .testTag("downloads_filter_${segment.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = segment.label,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                fontSize = 13.5.sp
                                            ),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Show search bar when there are downloads or an active query
                        if (allDownloads.isNotEmpty() || searchQuery.isNotEmpty()) {
                            LiquidGlassCard(
                                cornerRadius = 22.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search queue",
                                        tint = tokens.accentBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search downloads by filename, format, or source…",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = onSearchQueryChange,
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(tokens.accentBlue),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("downloads_search_input")
                                        )
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { onSearchQueryChange("") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Empty State or Filtered Download Items (clean open pearl-white layout matching the right reference screenshot)
            if (segmentFiltered.isEmpty()) {
                item {
                    StaggeredAnimatedEntrance(index = 2, reducedMotion = reducedMotion) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 120.dp, start = 24.dp, end = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when {
                                    searchQuery.isNotBlank() -> "No matching downloads"
                                    selectedSegment != DownloadsFilterSegment.ALL -> "No ${selectedSegment.label.lowercase()} downloads"
                                    else -> "No downloads yet"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Paste a link on the Home tab to download video or audio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(segmentFiltered, key = { _, it -> it.jobId }) { idx, task ->
                    StaggeredAnimatedEntrance(index = idx + 2, reducedMotion = reducedMotion) {
                        DownloadQueueItemCard(
                            task = task,
                            reducedMotion = reducedMotion,
                            onOpenLiveModal = { onOpenLiveProgressModal(task.jobId) },
                            onOpenDetails = { onOpenJobDetails(task.jobId) },
                            onPause = { onPause(task.jobId) },
                            onResume = { onResume(task.jobId) },
                            onCancel = { onCancel(task.jobId) },
                            onRetry = { onRetry(task.jobId) },
                            onDeleteRequest = {
                                val isDone = task.state == DownloadJobState.COMPLETED.name
                                if (isDone) {
                                    deleteConfirmTask = task
                                } else {
                                    onDelete(task.jobId)
                                }
                            },
                            onPlay = { onPlayFile(task) },
                            onShare = { onShareFile(task) }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog before deleting completed files
    deleteConfirmTask?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirmTask = null },
            title = { Text("Delete Completed File?") },
            text = {
                Text("Remove \"${target.fileName}\" from your downloads and device storage?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target.jobId)
                        deleteConfirmTask = null
                    }
                ) {
                    Text("Delete", color = tokens.errorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTask = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DownloadQueueItemCard(
    task: DownloadTaskEntity,
    reducedMotion: Boolean,
    onOpenLiveModal: () -> Unit,
    onOpenDetails: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDeleteRequest: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val stateEnum = runCatching { DownloadJobState.valueOf(task.state) }
        .getOrDefault(DownloadJobState.DOWNLOADING)

    val isCompleted = stateEnum == DownloadJobState.COMPLETED
    val isFailedOrCancelled = stateEnum == DownloadJobState.FAILED || stateEnum == DownloadJobState.CANCELLED
    val isPaused = stateEnum == DownloadJobState.PAUSED
    val isActiveTransfer = !isCompleted && !isFailedOrCancelled && !isPaused

    val animatedProgress by animateFloatAsState(
        targetValue = (task.progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "queue_item_progress"
    )

    LiquidGlassCard(
        cornerRadius = 22.dp,
        isHighlighted = !isCompleted && !isFailedOrCancelled,
        onClick = {
            if (isCompleted) onOpenDetails() else onOpenLiveModal()
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_task_card_${task.jobId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    tokens.accentBlue.copy(alpha = 0.22f),
                                    tokens.accentViolet.copy(alpha = 0.22f)
                                )
                            )
                        )
                        .border(0.8.dp, tokens.glassBorderSubtle, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActiveTransfer) {
                        PlayStoreScallopedLoader(
                            size = 28.dp,
                            color = Color(0xFF38BDF8),
                            isSpinning = true,
                            reducedMotion = reducedMotion
                        )
                    } else if (!task.thumbnailUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = task.thumbnailUrl,
                            contentDescription = task.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (task.format == "MP3") Icons.Default.AudioFile else Icons.Default.VideoFile,
                            contentDescription = task.title,
                            tint = tokens.accentBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when {
                                        isCompleted -> tokens.successColor.copy(alpha = 0.14f)
                                        isFailedOrCancelled -> tokens.errorColor.copy(alpha = 0.14f)
                                        else -> tokens.accentBlue.copy(alpha = 0.14f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${task.format} • ${task.qualityLabel}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = when {
                                    isCompleted -> tokens.successColor
                                    isFailedOrCancelled -> tokens.errorColor
                                    else -> tokens.accentBlue
                                }
                            )
                        }
                        Text(
                            text = stateEnum.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = task.fileName.ifBlank { task.title },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val transferSummaryText = when {
                        isFailedOrCancelled -> "${task.providerName} • Tap Retry to re-resolve fresh stream"
                        task.totalBytes > 0L -> "${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.totalBytes)} • ${task.providerName}"
                        task.downloadedBytes > 0L -> "${FormatUtils.formatBytes(task.downloadedBytes)} streamed • ${task.providerName}"
                        else -> "${stateEnum.displayName} • ${task.providerName}"
                    }

                    Text(
                        text = transferSummaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isCompleted && !isFailedOrCancelled) {
                    Text(
                        text = "${task.progressPercent}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = tokens.accentBlue
                    )
                } else {
                    IconButton(
                        onClick = onOpenDetails,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Task details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isCompleted && !isFailedOrCancelled) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = tokens.accentBlue,
                    trackColor = tokens.accentBlue.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isCompleted -> "Complete • ${FormatUtils.formatBytes(task.totalBytes.coerceAtLeast(task.downloadedBytes))}"
                        isFailedOrCancelled -> task.errorMessage ?: "Interrupted"
                        else -> "${task.progressPercent}% • ${FormatUtils.formatSpeed(task.speedBytesPerSec)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCompleted) tokens.successColor else if (isFailedOrCancelled) tokens.errorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isCompleted) {
                        IconButton(onClick = onPlay, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play file",
                                tint = tokens.accentBlue
                            )
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share file",
                                tint = tokens.accentViolet
                            )
                        }
                        IconButton(onClick = onDeleteRequest, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete file",
                                tint = tokens.errorColor
                            )
                        }
                    } else if (isFailedOrCancelled) {
                        IconButton(onClick = onRetry, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry download",
                                tint = tokens.accentBlue
                            )
                        }
                        IconButton(onClick = onDeleteRequest, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove task",
                                tint = tokens.errorColor
                            )
                        }
                    } else {
                        IconButton(
                            onClick = if (isPaused) onResume else onPause,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = tokens.accentBlue
                            )
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel download",
                                tint = tokens.errorColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadJobDetailsSheet(
    task: DownloadTaskEntity,
    onClose: () -> Unit,
    onOpenLiveProgress: () -> Unit,
    onPlayFile: () -> Unit,
    onShareFile: () -> Unit,
    onDeleteFile: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (tokens.isDark) Color(0xE6070612) else Color(0xB31E1B2E))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
            .testTag("download_details_sheet"),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            cornerRadius = 28.dp,
            isHighlighted = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Download Job Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close details",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(color = tokens.glassBorderSubtle)

                DetailRow("Job ID", task.jobId)
                DetailRow("Title", task.title)
                DetailRow("File Name", task.fileName)
                DetailRow("State", task.state)
                DetailRow("Format & Quality", "${task.format} • ${task.qualityLabel}")
                DetailRow("Resolution / Bitrate", task.resolutionOrBitrate)
                DetailRow("Codec", task.codec)
                DetailRow("Source Provider", task.providerName)
                DetailRow("Source URL", task.sourceUrl)
                DetailRow(
                    "Transferred",
                    "${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.totalBytes)} (${task.progressPercent}%)"
                )
                DetailRow("Created", FormatUtils.formatTimestamp(task.createdAt))
                task.completedAt?.let {
                    DetailRow("Completed", FormatUtils.formatTimestamp(it))
                }
                task.localFilePath?.let {
                    DetailRow("Storage Path", it)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.state == DownloadJobState.COMPLETED.name) {
                        LiquidGlassPrimaryButton(
                            text = "Play / Open",
                            icon = Icons.Default.FolderOpen,
                            onClick = onPlayFile,
                            cornerRadius = 16.dp,
                            modifier = Modifier.weight(1f)
                        )
                        LiquidGlassSecondaryButton(
                            text = "Share",
                            icon = Icons.Default.Share,
                            onClick = onShareFile,
                            cornerRadius = 16.dp,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LiquidGlassPrimaryButton(
                            text = "Open Live Progress",
                            onClick = onOpenLiveProgress,
                            cornerRadius = 16.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    IconButton(onClick = onDeleteFile) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete job",
                            tint = tokens.errorColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(128.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
