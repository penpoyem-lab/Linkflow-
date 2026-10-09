package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.RecentAnalysisEntity
import com.example.data.model.MediaAnalysisResult
import com.example.data.model.MediaFormat
import com.example.data.model.ProviderStatusInfo
import com.example.data.model.ProviderSupportLevel
import com.example.data.service.UrlAnalysisOutcome
import com.example.ui.LinkFlowUiState
import com.example.ui.PrimaryTab
import com.example.ui.components.LinkFlowLogoEmblem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.PlayStorePullToRefreshBox
import com.example.ui.components.PlayStoreScallopedLoader
import com.example.ui.components.StaggeredAnimatedEntrance
import com.example.ui.components.springBounceClickable
import com.example.ui.theme.LocalLinkFlowTokens
import com.example.util.FormatUtils

@Composable
fun HomeScreen(
    uiState: LinkFlowUiState,
    brandName: String,
    reducedMotion: Boolean,
    recentAnalyses: List<RecentAnalysisEntity>,
    activeAndRecentTasks: List<DownloadTaskEntity>,
    providerAdapters: List<ProviderStatusInfo>,
    onUrlChange: (String) -> Unit,
    onClearUrl: () -> Unit,
    onAnalyzeUrl: (String?) -> Unit,
    onSelectQuickFormat: (MediaFormat) -> Unit,
    onOpenQualitySheet: () -> Unit,
    onOpenActiveModal: (String) -> Unit,
    onNavigateToTab: (PrimaryTab) -> Unit,
    onPullToRefresh: () -> Unit,
    onShowAbout: () -> Unit,
    onShowSupportedSources: () -> Unit,
    onShowPrivacyTerms: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    var overflowExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "home_live_dot")
    val statusDotScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (reducedMotion) 1f else 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_dot_scale"
    )

    PlayStorePullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onPullToRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .testTag("home_lazy_column"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 116.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Liquid-Glass Header Card with Staggered Entrance
            item {
                StaggeredAnimatedEntrance(index = 0, reducedMotion = reducedMotion) {
                    LiquidGlassCard(
                        cornerRadius = 26.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                LinkFlowLogoEmblem(size = 44.dp, animated = !reducedMotion)
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = brandName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = tokens.accentCyan
                                        )
                                        Box(
                                            modifier = Modifier
                                                .scale(statusDotScale)
                                                .size(7.dp)
                                                .background(tokens.successColor, CircleShape)
                                        )
                                    }
                                    Text(
                                        text = "Video Downloader",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onNavigateToTab(PrimaryTab.DOWNLOADS) },
                                    modifier = Modifier.testTag("header_activity_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = "Download Queue & Activity",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { overflowExpanded = true },
                                        modifier = Modifier.testTag("header_overflow_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = overflowExpanded,
                                        onDismissRequest = { overflowExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("About $brandName") },
                                            onClick = {
                                                overflowExpanded = false
                                                onShowAbout()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Supported Sources & Adapters") },
                                            onClick = {
                                                overflowExpanded = false
                                                onShowSupportedSources()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Privacy & Terms of Use") },
                                            onClick = {
                                                overflowExpanded = false
                                                onShowPrivacyTerms()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Appearance & Settings") },
                                            onClick = {
                                                overflowExpanded = false
                                                onNavigateToTab(PrimaryTab.SETTINGS)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Version 2.4.0 (Build 240)") },
                                            onClick = {
                                                overflowExpanded = false
                                                onShowToast("$brandName v2.4.0 • Liquid-Glass Engine Active")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Welcome Headline & Subtitle
            item {
                StaggeredAnimatedEntrance(index = 1, reducedMotion = reducedMotion) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Your media. Your way.",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Paste a supported link to explore available video and audio options.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Smart Link Input Pill Surface
            item {
                StaggeredAnimatedEntrance(index = 2, reducedMotion = reducedMotion) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LiquidGlassCard(
                            cornerRadius = 32.dp,
                            isHighlighted = uiState.urlInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Link input icon",
                                    tint = tokens.accentCyan,
                                    modifier = Modifier.size(24.dp)
                                )

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (uiState.urlInput.isEmpty()) {
                                        Text(
                                            text = "Paste your video or media link…",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = uiState.urlInput,
                                        onValueChange = onUrlChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(tokens.accentCyan),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                focusManager.clearFocus()
                                                onAnalyzeUrl(null)
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("url_input_field")
                                    )
                                }

                                AnimatedVisibility(
                                    visible = uiState.urlInput.isNotEmpty(),
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    IconButton(
                                        onClick = onClearUrl,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("clear_url_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear URL",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(22.dp),
                                            spotColor = tokens.accentBlue
                                        )
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                listOf(tokens.accentBlue, tokens.accentCyan)
                                            )
                                        )
                                        .springBounceClickable(pressedScale = 0.91f) {
                                            val clipText = clipboardManager.getText()?.text?.trim()
                                            if (!clipText.isNullOrBlank()) {
                                                onUrlChange(clipText)
                                                onAnalyzeUrl(clipText)
                                            } else {
                                                onShowToast("Clipboard is empty. Copy a media link first.")
                                            }
                                        }
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                        .testTag("paste_url_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Paste",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Primary Animated "Analyze Link" Action Bar when URL is entered
                        AnimatedVisibility(
                            visible = uiState.urlInput.isNotBlank() && !uiState.isAnalyzingUrl,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    onAnalyzeUrl(null)
                                },
                                enabled = !uiState.isAnalyzingUrl,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.accentBlue
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("analyze_link_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyze Link",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 4. Transparent-Background Scalloped Star Loading OR Error Banner OR Analyzed Preview Card
            if (uiState.isAnalyzingUrl) {
                item {
                    TransparentScallopedLoadingView(reducedMotion = reducedMotion)
                }
            }

            uiState.analysisError?.let { error ->
                item {
                    StaggeredAnimatedEntrance(index = 0, reducedMotion = reducedMotion) {
                        ErrorNoticeCard(
                            error = error,
                            onDismiss = onClearUrl
                        )
                    }
                }
            }

            uiState.currentAnalysis?.let { analysis ->
                item {
                    StaggeredAnimatedEntrance(index = 0, reducedMotion = reducedMotion) {
                        AnalyzedMediaPreviewCard(
                            analysis = analysis,
                            selectedFormat = uiState.quickFormatSelection,
                            onOpenQualityOptions = onOpenQualitySheet
                        )
                    }
                }
            }

            // 5. Quick Format Selector Pill Bar ("Format  [ MP4 ]  [ MP3 ]")
            item {
                StaggeredAnimatedEntrance(index = 3, reducedMotion = reducedMotion) {
                    FormatSelectorBar(
                        selectedFormat = uiState.quickFormatSelection,
                        onSelectFormat = onSelectQuickFormat
                    )
                }
            }

            // 6. Real Recent Activity (Downloads & Link Analyses) or Clean Empty State
            if (activeAndRecentTasks.isNotEmpty()) {
                item {
                    StaggeredAnimatedEntrance(index = 4, reducedMotion = reducedMotion) {
                        ActiveQueueMiniBanner(
                            tasks = activeAndRecentTasks.take(4),
                            onOpenModal = onOpenActiveModal,
                            onViewAllDownloads = { onNavigateToTab(PrimaryTab.DOWNLOADS) }
                        )
                    }
                }
            } else if (recentAnalyses.isEmpty() && uiState.currentAnalysis == null && !uiState.isAnalyzingUrl) {
                item {
                    StaggeredAnimatedEntrance(index = 4, reducedMotion = reducedMotion) {
                        EmptyActivityCard(
                            onPasteFromClipboard = {
                                val clipText = clipboardManager.getText()?.text?.trim()
                                if (!clipText.isNullOrBlank()) {
                                    onUrlChange(clipText)
                                    onAnalyzeUrl(clipText)
                                } else {
                                    onShowToast("Clipboard is empty. Copy a media link first.")
                                }
                            }
                        )
                    }
                }
            }

            // 7. Recent Link Inspections (if user has analyzed real links)
            if (recentAnalyses.isNotEmpty()) {
                item {
                    StaggeredAnimatedEntrance(index = 5, reducedMotion = reducedMotion) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Recent Link Inspections",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            recentAnalyses.take(5).forEachIndexed { idx, rec ->
                                StaggeredAnimatedEntrance(index = idx + 1, reducedMotion = reducedMotion) {
                                    LiquidGlassCard(
                                        cornerRadius = 18.dp,
                                        onClick = { onAnalyzeUrl(rec.originalUrl) },
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
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(tokens.accentBlue.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = tokens.accentCyan
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rec.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val sizeInfo = if (rec.estimatedSizeBytes > 0) {
                                                    " • ${FormatUtils.formatBytes(rec.estimatedSizeBytes)}"
                                                } else ""
                                                Text(
                                                    text = "${rec.providerName} • ${rec.highestQualityLabel}$sizeInfo",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Re-open options",
                                                tint = tokens.accentCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. Supported Sources & Adapter Verification Status
            item {
                StaggeredAnimatedEntrance(index = 6, reducedMotion = reducedMotion) {
                    SupportedSourcesSummaryCard(
                        adapters = providerAdapters,
                        onOpenFullDetails = onShowSupportedSources
                    )
                }
            }
        }
    }
}

/**
 * Pure transparent-background Google Play Store scalloped cookie-star loading view
 * matching the user's reference image.
 */
@Composable
private fun TransparentScallopedLoadingView(
    reducedMotion: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp)
            .testTag("transparent_scalloped_loader"),
        contentAlignment = Alignment.Center
    ) {
        PlayStoreScallopedLoader(
            size = 54.dp,
            color = Color(0xFFA8C7FA),
            isSpinning = true,
            reducedMotion = reducedMotion
        )
    }
}

@Composable
private fun EmptyActivityCard(
    onPasteFromClipboard: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    LiquidGlassCard(
        cornerRadius = 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_recent_activity_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(tokens.accentBlue.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = tokens.accentCyan,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "No Recent Activity Yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Paste a direct HTTPS media link (.mp4, .mp3, .webm, .wav) above to inspect real stream headers and start downloading.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onPasteFromClipboard,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Paste Link from Clipboard", color = Color.White)
            }
        }
    }
}

@Composable
private fun FormatSelectorBar(
    selectedFormat: MediaFormat,
    onSelectFormat: (MediaFormat) -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    LiquidGlassCard(
        cornerRadius = 26.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.padding(end = 4.dp)) {
                Text(
                    text = "FORMAT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .border(1.dp, tokens.glassBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MediaFormat.entries.forEach { format ->
                    val isSelected = selectedFormat == format
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.0f else 0.96f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "fmt_scale"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) tokens.accentBlue else Color.Transparent,
                        animationSpec = tween(260),
                        label = "fmt_bg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .springBounceClickable(pressedScale = 0.93f) {
                                onSelectFormat(format)
                            }
                            .padding(vertical = 10.dp)
                            .testTag("format_selector_${format.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (format == MediaFormat.MP4) Icons.Default.Movie else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = format.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyzedMediaPreviewCard(
    analysis: MediaAnalysisResult,
    selectedFormat: MediaFormat,
    onOpenQualityOptions: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    LiquidGlassCard(
        cornerRadius = 24.dp,
        isHighlighted = true,
        onClick = onOpenQualityOptions,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analyzed_media_preview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = tokens.successColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "STREAM VERIFIED • ${analysis.providerName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accentCyan
                    )
                }
                Text(
                    text = "${analysis.videoOptions.size} MP4 • ${analysis.audioOptions.size} MP3",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(tokens.accentBlue.copy(alpha = 0.35f), tokens.accentViolet.copy(alpha = 0.35f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (analysis.audioOptions.isNotEmpty() && analysis.videoOptions.isEmpty()) {
                            Icons.Default.AudioFile
                        } else {
                            Icons.Default.VideoFile
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = analysis.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${analysis.authorOrChannel} • ${analysis.durationFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onOpenQualityOptions,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_quality_selector_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Choose ${selectedFormat.name} Download Options",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ErrorNoticeCard(
    error: UrlAnalysisOutcome.Error,
    onDismiss: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    LiquidGlassCard(
        cornerRadius = 22.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analysis_error_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = tokens.errorColor
                )
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = error.errorCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.errorColor
                )
            }
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Recovery: ${error.recoverySuggestion}",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.accentCyan
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.glassSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Dismiss", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ActiveQueueMiniBanner(
    tasks: List<DownloadTaskEntity>,
    onOpenModal: (String) -> Unit,
    onViewAllDownloads: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    LiquidGlassCard(
        cornerRadius = 20.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Download Activity",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = tokens.accentCyan
                )
                Text(
                    text = "Open Queue >",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.accentBlue,
                    modifier = Modifier.clickable { onViewAllDownloads() }
                )
            }

            tasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.22f))
                        .springBounceClickable { onOpenModal(task.jobId) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.fileName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${task.state} • ${task.progressPercent}% • ${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${task.progressPercent}%",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.accentCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportedSourcesSummaryCard(
    adapters: List<ProviderStatusInfo>,
    onOpenFullDetails: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    LiquidGlassCard(
        cornerRadius = 22.dp,
        onClick = onOpenFullDetails,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = tokens.accentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Configured Source Adapters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Details >",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.accentBlue
                )
            }

            adapters.forEach { adapter ->
                val isFull = adapter.status == ProviderSupportLevel.VERIFIED_ACTIVE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = adapter.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isFull) tokens.successColor.copy(alpha = 0.16f) else tokens.warningColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isFull) "Active Stream" else "oEmbed Metadata",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isFull) tokens.successColor else tokens.warningColor
                        )
                    }
                }
            }
        }
    }
}
