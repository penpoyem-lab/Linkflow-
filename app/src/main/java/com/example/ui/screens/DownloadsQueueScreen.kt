package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onExploreHome: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current

    val filtered = if (searchQuery.isBlank()) {
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

    val inProgressList = filtered.filter {
        it.state in listOf(
            DownloadJobState.ANALYZING.name,
            DownloadJobState.PREPARING.name,
            DownloadJobState.DOWNLOADING.name,
            DownloadJobState.PROCESSING.name,
            DownloadJobState.PAUSED.name
        )
    }
    val queuedList = filtered.filter { it.state == DownloadJobState.QUEUED.name }
    val completedList = filtered.filter { it.state == DownloadJobState.COMPLETED.name }
    val failedOrCancelledList = filtered.filter {
        it.state == DownloadJobState.FAILED.name || it.state == DownloadJobState.CANCELLED.name
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
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 116.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StaggeredAnimatedEntrance(index = 0, reducedMotion = reducedMotion) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Downloads & Queue",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${inProgressList.size} active • ${completedList.size} completed • ${failedOrCancelledList.size} failed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (completedList.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(tokens.glassSurfaceElevated)
                                    .springBounceClickable { onClearCompleted() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("clear_completed_button")
                            ) {
                                Text(
                                    text = "Clear Done",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.accentCyan
                                )
                            }
                        }
                    }
                }
            }

            item {
                StaggeredAnimatedEntrance(index = 1, reducedMotion = reducedMotion) {
                    LiquidGlassCard(
                        cornerRadius = 22.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search queue",
                                tint = tokens.accentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search downloads by name, format, or host…",
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
                                    cursorBrush = SolidColor(tokens.accentCyan),
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

            if (filtered.isEmpty()) {
                item {
                    StaggeredAnimatedEntrance(index = 2, reducedMotion = reducedMotion) {
                        LiquidGlassCard(
                            cornerRadius = 26.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(tokens.accentBlue.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = tokens.accentCyan,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No Matching Downloads" else "Your Download Queue is Empty",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        "Try clearing your search filter to see all tasks."
                                    } else {
                                        "Paste a supported media link on the Home screen to start downloading."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LiquidGlassPrimaryButton(
                                    text = "Go to Home Screen",
                                    icon = Icons.Default.Download,
                                    onClick = onExploreHome,
                                    cornerRadius = 18.dp
                                )
                            }
                        }
                    }
                }
            }

            if (inProgressList.isNotEmpty()) {
                item {
                    SectionLabel("IN PROGRESS (${inProgressList.size})")
                }
                itemsIndexed(inProgressList, key = { _, it -> it.jobId }) { idx, task ->
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
                            onDelete = { onDelete(task.jobId) },
                            onPlay = { onPlayFile(task) },
                            onShare = { onShareFile(task) }
                        )
                    }
                }
            }

            if (queuedList.isNotEmpty()) {
                item {
                    SectionLabel("QUEUED (${queuedList.size})")
                }
                itemsIndexed(queuedList, key = { _, it -> it.jobId }) { idx, task ->
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
                            onDelete = { onDelete(task.jobId) },
                            onPlay = { onPlayFile(task) },
                            onShare = { onShareFile(task) }
                        )
                    }
                }
            }

            if (completedList.isNotEmpty()) {
                item {
                    SectionLabel("COMPLETED (${completedList.size})")
                }
                itemsIndexed(completedList, key = { _, it -> it.jobId }) { idx, task ->
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
                            onDelete = { onDelete(task.jobId) },
                            onPlay = { onPlayFile(task) },
                            onShare = { onShareFile(task) }
                        )
                    }
                }
            }

            if (failedOrCancelledList.isNotEmpty()) {
                item {
                    SectionLabel("FAILED / CANCELLED (${failedOrCancelledList.size})")
                }
                itemsIndexed(failedOrCancelledList, key = { _, it -> it.jobId }) { idx, task ->
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
                            onDelete = { onDelete(task.jobId) },
                            onPlay = { onPlayFile(task) },
                            onShare = { onShareFile(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Bold
        ),
        color = LocalLinkFlowTokens.current.accentCyan,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
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
    onDelete: () -> Unit,
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(tokens.accentBlue.copy(alpha = 0.35f), tokens.accentViolet.copy(alpha = 0.35f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActiveTransfer) {
                        PlayStoreScallopedLoader(
                            size = 34.dp,
                            color = Color(0xFFEBD0C7),
                            trackColor = Color(0xFF5D423B),
                            isSpinning = true,
                            reducedMotion = reducedMotion
                        )
                    } else {
                        Icon(
                            imageVector = if (task.format == "MP3") Icons.Default.AudioFile else Icons.Default.VideoFile,
                            contentDescription = task.title,
                            tint = Color.White,
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
                                        isCompleted -> tokens.successColor.copy(alpha = 0.2f)
                                        isFailedOrCancelled -> tokens.errorColor.copy(alpha = 0.2f)
                                        else -> tokens.accentBlue.copy(alpha = 0.25f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${task.format} • ${task.qualityLabel}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = when {
                                    isCompleted -> tokens.successColor
                                    isFailedOrCancelled -> tokens.errorColor
                                    else -> tokens.accentCyan
                                }
                            )
                        }
                        Text(
                            text = stateEnum.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.totalBytes)} • ${task.providerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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

            if (!isCompleted && !isFailedOrCancelled) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = tokens.accentCyan,
                    trackColor = Color.White.copy(alpha = 0.12f),
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
                        isCompleted -> "Saved: ${task.fileName}"
                        isFailedOrCancelled -> task.errorMessage ?: "Interrupted"
                        else -> "${task.progressPercent}% • ${FormatUtils.formatSpeed(task.speedBytesPerSec)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                tint = tokens.accentCyan
                            )
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share file",
                                tint = tokens.accentBlue
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
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
                                tint = tokens.accentCyan
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
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
                                tint = tokens.accentCyan
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
            .background(Color(0xE6050811))
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
                        color = Color.White
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close details",
                            tint = Color.White
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                DetailRow("Job ID", task.jobId)
                DetailRow("Title", task.title)
                DetailRow("File Name", task.fileName)
                DetailRow("State Machine", task.state)
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
                            text = "Open Live Progress Ring",
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
            color = Color(0xFF94A3B8),
            modifier = Modifier.width(128.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
