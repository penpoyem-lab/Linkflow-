package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadJobState
import com.example.data.model.MediaFormat
import com.example.ui.LibrarySortOption
import com.example.ui.components.AnimatedAudioWaveform
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.components.LiquidGlassSecondaryButton
import com.example.ui.components.PlayStorePullToRefreshBox
import com.example.ui.components.StaggeredAnimatedEntrance
import com.example.ui.components.springBounceClickable
import com.example.ui.theme.LocalLinkFlowTokens
import com.example.util.FormatUtils
import kotlinx.coroutines.delay

@Composable
fun LibraryScreen(
    allDownloads: List<DownloadTaskEntity>,
    searchQuery: String,
    formatFilter: MediaFormat?,
    sortOption: LibrarySortOption,
    isGridMode: Boolean,
    isRefreshing: Boolean,
    reducedMotion: Boolean = false,
    onSearchChange: (String) -> Unit,
    onFormatFilterChange: (MediaFormat?) -> Unit,
    onSortChange: (LibrarySortOption) -> Unit,
    onToggleGridMode: () -> Unit,
    onPullToRefresh: () -> Unit,
    onPlayMedia: (DownloadTaskEntity) -> Unit,
    onShareMedia: (DownloadTaskEntity) -> Unit,
    onRenameMedia: (String, String) -> Unit,
    onDeleteMedia: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onExploreStreams: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<DownloadTaskEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteConfirmTarget by remember { mutableStateOf<DownloadTaskEntity?>(null) }

    val completedFiles = allDownloads.filter { it.state == DownloadJobState.COMPLETED.name }

    val filteredAndSorted = completedFiles
        .filter { item ->
            val matchesFormat = when (formatFilter) {
            null -> true
            MediaFormat.MP4, MediaFormat.WEBM -> item.format.equals("MP4", ignoreCase = true) || item.format.equals("WEBM", ignoreCase = true)
            MediaFormat.MP3, MediaFormat.M4A, MediaFormat.WAV -> item.format.equals("MP3", ignoreCase = true) || item.format.equals("M4A", ignoreCase = true) || item.format.equals("WAV", ignoreCase = true)
        }
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery.trim(), ignoreCase = true) ||
                item.fileName.contains(searchQuery.trim(), ignoreCase = true) ||
                item.qualityLabel.contains(searchQuery.trim(), ignoreCase = true)
            matchesFormat && matchesQuery
        }
        .let { list ->
            when (sortOption) {
                LibrarySortOption.DATE_DESC -> list.sortedByDescending { it.completedAt ?: it.createdAt }
                LibrarySortOption.NAME_ASC -> list.sortedBy { it.fileName.lowercase() }
                LibrarySortOption.SIZE_DESC -> list.sortedByDescending { it.totalBytes }
                LibrarySortOption.TYPE -> list.sortedBy { it.format }
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
                .testTag("library_lazy_column"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 116.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                text = "Media Library",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${completedFiles.size} saved files • ${FormatUtils.formatBytes(completedFiles.sumOf { it.totalBytes })}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onToggleGridMode,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tokens.glassSurfaceElevated)
                                    .size(38.dp)
                                    .testTag("library_view_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isGridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle grid or list view",
                                    tint = tokens.accentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { sortMenuExpanded = true },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tokens.glassSurfaceElevated)
                                        .size(38.dp)
                                        .testTag("library_sort_button")
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort library",
                                        tint = tokens.accentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    LibrarySortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                sortMenuExpanded = false
                                                onSortChange(option)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                StaggeredAnimatedEntrance(index = 1, reducedMotion = reducedMotion) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    contentDescription = null,
                                    tint = tokens.accentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search saved MP4 or MP3 files…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(tokens.accentCyan),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("library_search_input")
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchChange("") },
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChipPill(
                                label = "All Files",
                                selected = formatFilter == null,
                                onClick = { onFormatFilterChange(null) }
                            )
                            FilterChipPill(
                                label = "MP4 Video",
                                selected = formatFilter == MediaFormat.MP4,
                                onClick = { onFormatFilterChange(MediaFormat.MP4) }
                            )
                            FilterChipPill(
                                label = "MP3 Audio",
                                selected = formatFilter == MediaFormat.MP3,
                                onClick = { onFormatFilterChange(MediaFormat.MP3) }
                            )
                        }
                    }
                }
            }

            if (filteredAndSorted.isEmpty()) {
                item {
                    StaggeredAnimatedEntrance(index = 2, reducedMotion = reducedMotion) {
                        LiquidGlassCard(
                            cornerRadius = 26.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_empty_library_art_1791571260082),
                                    contentDescription = "Empty media library illustration",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                )
                                Text(
                                    text = if (completedFiles.isEmpty()) "No Downloaded Media Yet" else "No Matching Library Files",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (completedFiles.isEmpty()) {
                                        "Your completed MP4 videos and MP3 audio tracks will appear here automatically once downloaded."
                                    } else {
                                        "Try clearing your search or switching format filters."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LiquidGlassPrimaryButton(
                                    text = "Go to Home Screen",
                                    icon = Icons.Default.VideoLibrary,
                                    onClick = onExploreStreams,
                                    cornerRadius = 18.dp
                                )
                            }
                        }
                    }
                }
            } else if (isGridMode) {
                val rows = filteredAndSorted.chunked(2)
                itemsIndexed(rows) { rowIdx, rowItems ->
                    StaggeredAnimatedEntrance(index = rowIdx + 2, reducedMotion = reducedMotion) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { task ->
                                LibraryGridItemCard(
                                    task = task,
                                    onPlay = { onPlayMedia(task) },
                                    onShare = { onShareMedia(task) },
                                    onRename = {
                                        renameTarget = task
                                        renameText = task.fileName
                                    },
                                    onDelete = { deleteConfirmTarget = task },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(filteredAndSorted, key = { _, it -> it.jobId }) { idx, task ->
                    StaggeredAnimatedEntrance(index = idx + 2, reducedMotion = reducedMotion) {
                        LibraryListItemCard(
                            task = task,
                            onPlay = { onPlayMedia(task) },
                            onShare = { onShareMedia(task) },
                            onRename = {
                                renameTarget = task
                                renameText = task.fileName
                            },
                            onDelete = { deleteConfirmTarget = task },
                            onDetails = { onOpenDetails(task.jobId) }
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Media File") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("File Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameMedia(target.jobId, renameText)
                        renameTarget = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteConfirmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            title = { Text("Delete File?") },
            text = {
                Text("Remove \"${target.fileName}\" from your library and local storage?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMedia(target.jobId)
                        deleteConfirmTarget = null
                    }
                ) {
                    Text("Delete", color = LocalLinkFlowTokens.current.errorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chip_scale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) {
                    Brush.linearGradient(
                        listOf(
                            tokens.accentBlue.copy(alpha = 0.90f),
                            tokens.accentCyan.copy(alpha = 0.78f),
                            tokens.accentViolet.copy(alpha = 0.80f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (tokens.isDark) 0.12f else 0.75f),
                            tokens.glassSurfaceElevated
                        )
                    )
                }
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.80f), tokens.accentCyan.copy(alpha = 0.45f))
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = tokens.glassBorderSubtle,
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            )
            .springBounceClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LibraryGridItemCard(
    task: DownloadTaskEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLinkFlowTokens.current

    LiquidGlassCard(
        cornerRadius = 20.dp,
        onClick = onPlay,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.45f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(tokens.accentBlue.copy(alpha = 0.35f), tokens.accentViolet.copy(alpha = 0.35f))
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
                        modifier = Modifier.size(36.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(tokens.accentBlue, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.format,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.fileName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${FormatUtils.formatBytes(task.totalBytes)} • ${task.qualityLabel}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = tokens.accentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = tokens.errorColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryListItemCard(
    task: DownloadTaskEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current

    LiquidGlassCard(
        cornerRadius = 20.dp,
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(tokens.accentBlue.copy(alpha = 0.35f), tokens.accentViolet.copy(alpha = 0.35f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (task.format == "MP3") Icons.Default.AudioFile else Icons.Default.VideoFile,
                    contentDescription = task.title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.format} • ${task.qualityLabel} • ${FormatUtils.formatBytes(task.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = tokens.accentCyan, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDetails, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Info, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = tokens.errorColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun MediaPlaybackModal(
    task: DownloadTaskEntity,
    reducedMotion: Boolean,
    onClose: () -> Unit,
    onShare: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val isAudioFormat = remember(task.format, task.fileName) {
        val ext = task.fileName.substringAfterLast('.', "").lowercase()
        task.format.equals("MP3", ignoreCase = true) ||
            task.format.equals("M4A", ignoreCase = true) ||
            task.format.equals("WAV", ignoreCase = true) ||
            ext == "mp3" || ext == "m4a" || ext == "wav" || ext == "ogg"
    }

    val localFile = remember(task.localFilePath) {
        task.localFilePath?.let { java.io.File(it) }?.takeIf { it.exists() && it.length() > 0L }
    }

    val exoPlayer = remember(localFile) {
        if (localFile != null) {
            try {
                androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                    val mediaItem = androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(localFile))
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
            } catch (e: Exception) {
                playbackError = "Media player notice: ${e.localizedMessage ?: "Unable to initialize player"}"
                null
            }
        } else {
            null
        }
    }

    androidx.compose.runtime.DisposableEffect(exoPlayer) {
        if (exoPlayer == null) return@DisposableEffect onDispose {}
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    isPlaying = false
                    progress = 1f
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = "Hardware codec notice (${error.errorCodeName}) • Tap 'Open in System Player' below"
                isPlaying = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            runCatching {
                exoPlayer.removeListener(listener)
                exoPlayer.stop()
                exoPlayer.release()
            }
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        while (isPlaying && exoPlayer != null) {
            val dur = runCatching { exoPlayer.duration }.getOrDefault(0L)
            val pos = runCatching { exoPlayer.currentPosition }.getOrDefault(0L)
            if (dur > 0L) {
                progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            }
            delay(200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2050811))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
            .testTag("media_playback_modal"),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            cornerRadius = 30.dp,
            isHighlighted = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isAudioFormat) Icons.Default.AudioFile else Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = tokens.accentCyan
                        )
                        Text(
                            text = if (isAudioFormat) "LinkFlow Audio Player" else "LinkFlow Media Player",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close player", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(tokens.accentBlue.copy(alpha = 0.35f), tokens.accentViolet.copy(alpha = 0.35f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isAudioFormat && exoPlayer != null && playbackError == null) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply {
                                    useController = true
                                    player = exoPlayer
                                }
                            },
                            update = { playerView ->
                                if (playerView.player !== exoPlayer) {
                                    playerView.player = exoPlayer
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        if (!task.thumbnailUrl.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = task.thumbnailUrl,
                                contentDescription = task.title,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = 0.45f }
                            )
                        }
                        IconButton(
                            onClick = {
                                if (exoPlayer != null && playbackError == null) {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                            exoPlayer.seekTo(0L)
                                        }
                                        exoPlayer.play()
                                    }
                                } else {
                                    FormatUtils.openDownloadedFileExternally(context, task) { err ->
                                        playbackError = err
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(tokens.accentBlue.copy(alpha = 0.85f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "${task.fileName} • ${task.qualityLabel} • ${FormatUtils.formatBytes(task.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    playbackError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.errorColor
                        )
                    }
                }

                AnimatedAudioWaveform(
                    isPlaying = isPlaying,
                    reducedMotion = reducedMotion
                )

                LinearProgressIndicator(
                    progress = { progress },
                    color = tokens.accentCyan,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiquidGlassPrimaryButton(
                        text = if (playbackError != null) "Open in System Player" else if (isPlaying) "Pause" else "Play",
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = {
                            if (exoPlayer != null && playbackError == null) {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                        exoPlayer.seekTo(0L)
                                    }
                                    exoPlayer.play()
                                }
                            } else {
                                FormatUtils.openDownloadedFileExternally(context, task) { err ->
                                    playbackError = err
                                }
                            }
                        },
                        cornerRadius = 18.dp,
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassSecondaryButton(
                        text = "Share File",
                        icon = Icons.Default.Share,
                        onClick = onShare,
                        cornerRadius = 18.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
