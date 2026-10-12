package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.RecentAnalysisEntity
import com.example.data.model.DownloadJobState
import com.example.data.model.MediaAnalysisResult
import com.example.data.model.MediaFormat
import com.example.data.model.ProviderStatusInfo
import com.example.data.service.UrlAnalysisOutcome
import com.example.ui.LinkFlowUiState
import com.example.ui.PrimaryTab
import com.example.ui.components.LinkFlowLogoEmblem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.components.LiquidGlassSecondaryButton
import com.example.ui.components.PlayStorePullToRefreshBox
import com.example.ui.components.StaggeredAnimatedEntrance
import com.example.ui.components.TopDropTransparentLoadingOverlay
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
    onStartQuickDownload: () -> Unit = onOpenQualitySheet,
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

    val trimmedUrl = uiState.urlInput.trim()
    val isUrlValidSyntax = remember(trimmedUrl) {
        trimmedUrl.startsWith("https://", ignoreCase = true) ||
            trimmedUrl.startsWith("http://", ignoreCase = true) ||
            (trimmedUrl.contains(".") && !trimmedUrl.contains(" ") && trimmedUrl.length >= 6)
    }

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
                start = 22.dp,
                end = 22.dp,
                top = 18.dp,
                bottom = 122.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header matching reference screenshot: Circular avatar/logo on left, title & subtitle in middle, white circular crown button on right
            item {
                StaggeredAnimatedEntrance(index = 0, reducedMotion = reducedMotion) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            LinkFlowLogoEmblem(size = 50.dp, animated = !reducedMotion)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = brandName,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 21.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Your media, your way.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = CircleShape,
                                        ambientColor = Color.Black.copy(alpha = 0.08f),
                                        spotColor = tokens.accentViolet.copy(alpha = 0.22f)
                                    )
                                    .clip(CircleShape)
                                    .background(if (tokens.isDark) tokens.glassSurfaceElevated else Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = if (tokens.isDark) tokens.glassBorderSubtle else Color.White,
                                        shape = CircleShape
                                    )
                                    .springBounceClickable { overflowExpanded = true }
                                    .testTag("header_overflow_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Custom Crown Vector Icon matching top-right badge in screenshot
                                CrownBadgeIcon(
                                    tint = if (tokens.isDark) Color.White else Color(0xFF111827),
                                    modifier = Modifier.size(20.dp)
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
                                    text = { Text("Version 3.0.0 (Build 300)") },
                                    onClick = {
                                        overflowExpanded = false
                                        onShowToast("$brandName v3.0 • Liquid-Glass Engine Active")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Main URL Input Row + Violet Circular Download Button (matching the reference screenshot layout)
            item {
                StaggeredAnimatedEntrance(index = 1, reducedMotion = reducedMotion) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_hero_banner_card"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Frosted lavender pill input field on the left with thin dark-purple border
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(
                                        if (tokens.isDark) tokens.glassSurfaceElevated
                                        else Color(0x66FFFFFF)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            trimmedUrl.isEmpty() -> if (tokens.isDark) tokens.glassBorderSubtle else Color(0xFF9F8FEF)
                                            isUrlValidSyntax -> tokens.accentBlue
                                            else -> tokens.errorColor
                                        },
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Link input icon",
                                    tint = if (tokens.isDark) Color.White else Color(0xFF111827),
                                    modifier = Modifier.size(20.dp)
                                )

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (uiState.urlInput.isEmpty()) {
                                        Text(
                                            text = "Paste your link here or auto detect",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = uiState.urlInput,
                                        onValueChange = onUrlChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(tokens.accentBlue),
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

                                if (uiState.urlInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = onClearUrl,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .testTag("clear_url_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear URL",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Prominent Circular Violet Gradient Download Action Button on the right (matching screenshot)
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = CircleShape,
                                        ambientColor = Color(0xFF7C3AED).copy(alpha = 0.35f),
                                        spotColor = Color(0xFF7C3AED).copy(alpha = 0.45f)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF8B5CF6),
                                                Color(0xFF7C3AED)
                                            )
                                        )
                                    )
                                    .springBounceClickable(
                                        enabled = !uiState.isAnalyzingUrl,
                                        pressedScale = 0.92f
                                    ) {
                                        focusManager.clearFocus()
                                        if (uiState.urlInput.isBlank()) {
                                            val clipText = clipboardManager.getText()?.text?.trim()
                                            if (!clipText.isNullOrBlank()) {
                                                onUrlChange(clipText)
                                                onAnalyzeUrl(clipText)
                                            } else {
                                                onShowToast("Clipboard is empty. Copy a media link first.")
                                            }
                                        } else if (uiState.currentAnalysis != null &&
                                            uiState.currentAnalysis.originalUrl == trimmedUrl &&
                                            uiState.currentAnalysis.isAuthorizedStream
                                        ) {
                                            onStartQuickDownload()
                                        } else {
                                            onAnalyzeUrl(null)
                                        }
                                    }
                                    .testTag("analyze_link_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download or Analyze Link",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Subtle helper / automatic validation caption directly below input bar (matching screenshot)
                        AnimatedVisibility(
                            visible = trimmedUrl.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isUrlValidSyntax) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isUrlValidSyntax) tokens.successColor else tokens.errorColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isUrlValidSyntax) {
                                        "Valid media link detected • Tap the purple download button"
                                    } else {
                                        "Enter a valid HTTPS media link (e.g., https://...)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUrlValidSyntax) tokens.successColor else tokens.errorColor
                                )
                            }
                        }

                        if (trimmedUrl.isEmpty()) {
                            Text(
                                text = "No watermark, fast & lossless HD media streams",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }
            }

            // 3. Supported Platforms Section with Centered Line Divider, Horizontally Swipeable Platform Row (including YouTube, Spotify & AniVortex) & "View all >"
            item {
                StaggeredAnimatedEntrance(index = 2, reducedMotion = reducedMotion) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 0.8.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                            Text(
                                text = "Supported Platforms • Swipe Side",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 0.8.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        }

                        // Horizontally swipeable row of platform icons: TikTok, Instagram, Facebook, X, YouTube, Spotify, AniVortex
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("supported_platforms_swipe_row"),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            item(key = "TIKTOK") {
                                SocialPlatformCircle(
                                    label = "TikTok",
                                    platformType = "TIKTOK",
                                    onClick = onShowSupportedSources
                                )
                            }
                            item(key = "INSTAGRAM") {
                                SocialPlatformCircle(
                                    label = "Instagram",
                                    platformType = "INSTAGRAM",
                                    onClick = onShowSupportedSources
                                )
                            }
                            item(key = "FACEBOOK") {
                                SocialPlatformCircle(
                                    label = "Facebook",
                                    platformType = "FACEBOOK",
                                    onClick = onShowSupportedSources
                                )
                            }
                            item(key = "X") {
                                SocialPlatformCircle(
                                    label = "X",
                                    platformType = "X",
                                    onClick = onShowSupportedSources
                                )
                            }
                            item(key = "YOUTUBE") {
                                SocialPlatformCircle(
                                    label = "YouTube",
                                    platformType = "YOUTUBE",
                                    onClick = {
                                        if (uiState.urlInput.isBlank()) {
                                            onShowToast("YouTube MP4 (1080p/720p) & MP3 320kbps ready — paste any YouTube link")
                                        } else {
                                            onShowSupportedSources()
                                        }
                                    }
                                )
                            }
                            item(key = "SPOTIFY") {
                                SocialPlatformCircle(
                                    label = "Spotify",
                                    platformType = "SPOTIFY",
                                    onClick = {
                                        if (uiState.urlInput.isBlank()) {
                                            onShowToast("Spotify Full-Track MP3 (320kbps) & MP4 ready — paste any open.spotify.com link")
                                        } else {
                                            onShowSupportedSources()
                                        }
                                    }
                                )
                            }
                            item(key = "APOLLO_MUSIC") {
                                SocialPlatformCircle(
                                    label = "Apollo Music",
                                    platformType = "APOLLO_MUSIC",
                                    onClick = {
                                        if (uiState.urlInput.isBlank()) {
                                            onUrlChange("https://apollostation.app/")
                                            onShowToast("Apollo Music (apollostation.app) Hi-Res MP3/FLAC & MP4 ready — paste any track link")
                                        } else {
                                            onShowSupportedSources()
                                        }
                                    }
                                )
                            }
                            item(key = "ANIVORTEX") {
                                SocialPlatformCircle(
                                    label = "AniVortex",
                                    platformType = "ANIVORTEX",
                                    onClick = {
                                        if (uiState.urlInput.isBlank()) {
                                            onUrlChange("https://anivortex.in/")
                                            onShowToast("AniVortex (anivortex.in) MP4 & MP3 ready — paste your anime episode or OST link")
                                        } else {
                                            onShowSupportedSources()
                                        }
                                    }
                                )
                            }
                        }

                        // Subtle swipe hint + Right-aligned "View all >" link (matching the reference screenshot)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Swipe left for Spotify, Apollo Music & AniVortex",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onShowSupportedSources() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "View all",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "View all supported sources",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Clean Format Selection Bar (shown when URL is entered or analyzed, keeping initial Home screen clean like the reference screenshot)
            if (trimmedUrl.isNotEmpty() || uiState.currentAnalysis != null) {
                item {
                    StaggeredAnimatedEntrance(index = 3, reducedMotion = reducedMotion) {
                        val currentAnalysis = uiState.currentAnalysis
                        val supportsVideo = currentAnalysis == null ||
                            currentAnalysis.videoOptions.isNotEmpty() ||
                            !currentAnalysis.isAuthorizedStream
                        val supportsAudio = currentAnalysis == null ||
                            currentAnalysis.audioOptions.isNotEmpty() ||
                            currentAnalysis.videoOptions.isNotEmpty()

                        FormatSelectorBar(
                            selectedFormat = uiState.quickFormatSelection,
                            supportsVideo = supportsVideo,
                            supportsAudio = supportsAudio,
                            onSelectFormat = onSelectQuickFormat
                        )
                    }
                }
            }

            // 5. Error Notice or Verified Stream Preview Card (with Quality choices when metadata confirms availability)
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
                            onQuickDownload = onStartQuickDownload,
                            onOpenQualityOptions = onOpenQualitySheet
                        )
                    }
                }
            }

            // 6. Recent Activity Section (Compact clean cards with filename, format, size, status, real progress indicator & link to queue)
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
            }

            // 7. Recent Link Inspections (only shown when actively typing/inspecting a link so the initial Home screen stays open and airy like Photo 2)
            if (trimmedUrl.isNotEmpty() && recentAnalyses.isNotEmpty()) {
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
                            recentAnalyses.take(3).forEachIndexed { idx, rec ->
                                StaggeredAnimatedEntrance(index = idx + 1, reducedMotion = reducedMotion) {
                                    LiquidGlassCard(
                                        cornerRadius = 20.dp,
                                        onClick = { onAnalyzeUrl(rec.originalUrl) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(tokens.accentBlue.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = tokens.accentBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rec.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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
                                                tint = tokens.accentBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Single Centered Transparent Loading Overlay (Small & Blue) that drops down from the top of the screen
        TopDropTransparentLoadingOverlay(
            visible = uiState.isAnalyzingUrl && !uiState.isRefreshing,
            reducedMotion = reducedMotion,
            loaderSize = 36.dp,
            loaderColor = Color(0xFF38BDF8)
        )
    }
}

@Composable
private fun CrownBadgeIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.14f, h * 0.78f)
            lineTo(w * 0.86f, h * 0.78f)
            lineTo(w * 0.92f, h * 0.30f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.32f, h * 0.50f)
            lineTo(w * 0.08f, h * 0.30f)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

@Composable
private fun SocialPlatformCircle(
    label: String,
    platformType: String,
    isActiveHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .springBounceClickable(pressedScale = 0.92f, onClick = onClick)
            .testTag("platform_circle_${platformType.lowercase()}")
    ) {
        val backgroundModifier = when (platformType) {
            "INSTAGRAM" -> Modifier.background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFEDA75),
                        Color(0xFFFA7E1E),
                        Color(0xFFD62976),
                        Color(0xFF962FBF),
                        Color(0xFF4F5BD5)
                    )
                )
            )
            "FACEBOOK" -> Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1877F2), Color(0xFF0C5DC7))
                )
            )
            "YOUTUBE" -> Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE60023), Color(0xFFBD081C))
                )
            )
            "SPOTIFY" -> Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1ED760), Color(0xFF1DB954), Color(0xFF128C3F))
                )
            )
            "APOLLO_MUSIC" -> Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1))
                )
            )
            "ANIVORTEX" -> Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9), Color(0xFF4F46E5))
                )
            )
            else -> Modifier.background(Color(0xFF0A0A0D))
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(
                    elevation = if (isActiveHighlight) 10.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.14f),
                    spotColor = Color(0xFF7C3AED).copy(alpha = if (isActiveHighlight) 0.42f else 0.16f)
                )
                .clip(CircleShape)
                .then(backgroundModifier)
                .then(
                    if (isActiveHighlight) {
                        Modifier.border(2.dp, Color.White.copy(alpha = 0.92f), CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(26.dp)) {
                val w = size.width
                val h = size.height
                when (platformType) {
                    "TIKTOK" -> {
                        val stroke = 3.2.dp.toPx()
                        drawLine(
                            color = Color.White,
                            start = Offset(w * 0.58f, h * 0.15f),
                            end = Offset(w * 0.58f, h * 0.70f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White,
                            radius = w * 0.20f,
                            center = Offset(w * 0.38f, h * 0.72f),
                            style = Stroke(width = stroke)
                        )
                        val flagPath = Path().apply {
                            moveTo(w * 0.58f, h * 0.15f)
                            quadraticTo(w * 0.75f, h * 0.34f, w * 0.88f, h * 0.36f)
                        }
                        drawPath(
                            path = flagPath,
                            color = Color.White,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    "INSTAGRAM" -> {
                        val stroke = 2.4.dp.toPx()
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(w * 0.10f, h * 0.10f),
                            size = Size(w * 0.80f, h * 0.80f),
                            cornerRadius = CornerRadius(w * 0.24f, w * 0.24f),
                            style = Stroke(width = stroke)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = w * 0.19f,
                            center = Offset(w * 0.50f, h * 0.50f),
                            style = Stroke(width = stroke)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = w * 0.045f,
                            center = Offset(w * 0.72f, h * 0.28f)
                        )
                    }
                    "FACEBOOK" -> {
                        val stroke = 3.6.dp.toPx()
                        val fPath = Path().apply {
                            moveTo(w * 0.55f, h * 0.90f)
                            lineTo(w * 0.55f, h * 0.32f)
                            quadraticTo(w * 0.55f, h * 0.15f, w * 0.74f, h * 0.15f)
                            moveTo(w * 0.36f, h * 0.48f)
                            lineTo(w * 0.72f, h * 0.48f)
                        }
                        drawPath(
                            path = fPath,
                            color = Color.White,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    "X" -> {
                        val stroke = 2.8.dp.toPx()
                        drawLine(
                            color = Color.White,
                            start = Offset(w * 0.22f, h * 0.22f),
                            end = Offset(w * 0.78f, h * 0.78f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(w * 0.78f, h * 0.22f),
                            end = Offset(w * 0.22f, h * 0.78f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                    "YOUTUBE" -> {
                        val playPath = Path().apply {
                            moveTo(w * 0.34f, h * 0.24f)
                            lineTo(w * 0.76f, h * 0.50f)
                            lineTo(w * 0.34f, h * 0.76f)
                            close()
                        }
                        drawPath(path = playPath, color = Color.White)
                    }
                    "SPOTIFY" -> {
                        // Clean 3-wave acoustic arcs for Spotify
                        val topArc = Path().apply {
                            moveTo(w * 0.16f, h * 0.34f)
                            quadraticTo(w * 0.50f, h * 0.20f, w * 0.84f, h * 0.38f)
                        }
                        val midArc = Path().apply {
                            moveTo(w * 0.22f, h * 0.53f)
                            quadraticTo(w * 0.50f, h * 0.42f, w * 0.78f, h * 0.57f)
                        }
                        val botArc = Path().apply {
                            moveTo(w * 0.28f, h * 0.72f)
                            quadraticTo(w * 0.50f, h * 0.63f, w * 0.72f, h * 0.75f)
                        }
                        drawPath(
                            path = topArc,
                            color = Color.White,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = midArc,
                            color = Color.White,
                            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = botArc,
                            color = Color.White,
                            style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    "APOLLO_MUSIC" -> {
                        // Stylized Orbital Ring + Musical A/Note Emblem for Apollo Music Station (apollostation.app)
                        val stroke = 2.2.dp.toPx()
                        drawArc(
                            color = Color.White.copy(alpha = 0.90f),
                            startAngle = 150f,
                            sweepAngle = 240f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        // Note stem + head inside orbital ring
                        drawLine(
                            color = Color.White,
                            start = Offset(w * 0.58f, h * 0.24f),
                            end = Offset(w * 0.58f, h * 0.66f),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(w * 0.58f, h * 0.24f),
                            end = Offset(w * 0.76f, h * 0.32f),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White,
                            radius = w * 0.13f,
                            center = Offset(w * 0.46f, h * 0.66f)
                        )
                    }
                    "ANIVORTEX" -> {
                        // Stylized Vortex Swirl + Play/Anime emblem for AniVortex (anivortex.in)
                        val stroke = 2.2.dp.toPx()
                        drawArc(
                            color = Color.White.copy(alpha = 0.92f),
                            startAngle = -35f,
                            sweepAngle = 250f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        val aPlayPath = Path().apply {
                            moveTo(w * 0.50f, h * 0.22f)
                            lineTo(w * 0.74f, h * 0.72f)
                            lineTo(w * 0.58f, h * 0.72f)
                            lineTo(w * 0.50f, h * 0.53f)
                            lineTo(w * 0.42f, h * 0.72f)
                            lineTo(w * 0.26f, h * 0.72f)
                            close()
                        }
                        drawPath(path = aPlayPath, color = Color.White)
                    }
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = if (isActiveHighlight) FontWeight.Bold else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FormatSelectorBar(
    selectedFormat: MediaFormat,
    supportsVideo: Boolean,
    supportsAudio: Boolean,
    onSelectFormat: (MediaFormat) -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val availableFormats = buildList {
        if (supportsVideo) add(MediaFormat.MP4)
        if (supportsAudio) add(MediaFormat.MP3)
        if (isEmpty()) {
            add(MediaFormat.MP4)
            add(MediaFormat.MP3)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = tokens.accentBlue.copy(alpha = 0.10f),
                spotColor = tokens.accentViolet.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(if (tokens.isDark) tokens.glassSurfaceElevated else Color.White)
            .border(
                width = 1.dp,
                color = if (tokens.isDark) tokens.glassBorderSubtle else Color(0xFFE9E2FF),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        availableFormats.forEach { format ->
            val isSelected = selectedFormat == format ||
                (format == MediaFormat.MP4 && selectedFormat.isVideo) ||
                (format == MediaFormat.MP3 && !selectedFormat.isVideo)
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.0f else 0.97f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "fmt_scale"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(22.dp),
                                    ambientColor = Color(0xFF7C3AED).copy(alpha = 0.25f),
                                    spotColor = Color(0xFF7C3AED).copy(alpha = 0.30f)
                                )
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF936EFE),
                                            Color(0xFF7C3AED)
                                        )
                                    )
                                )
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.Transparent)
                        }
                    )
                    .springBounceClickable(pressedScale = 0.94f) {
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
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (format == MediaFormat.MP4) "MP4 Video" else "MP3 Audio",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzedMediaPreviewCard(
    analysis: MediaAnalysisResult,
    selectedFormat: MediaFormat,
    onQuickDownload: () -> Unit,
    onOpenQualityOptions: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val hasDownloadableOptions = analysis.isAuthorizedStream &&
        (analysis.videoOptions.isNotEmpty() || analysis.audioOptions.isNotEmpty())
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
                        imageVector = if (hasDownloadableOptions) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (hasDownloadableOptions) tokens.successColor else tokens.accentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (hasDownloadableOptions) {
                            "STREAM VERIFIED • ${analysis.providerName.uppercase()}"
                        } else {
                            "OFFICIAL METADATA PREVIEW • ${analysis.providerName.uppercase()}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.accentBlue
                    )
                }
                Text(
                    text = if (hasDownloadableOptions) {
                        "${analysis.videoOptions.size} Video • ${analysis.audioOptions.size} Audio"
                    } else {
                        "Provider Restricted"
                    },
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
                                listOf(tokens.accentBlue.copy(alpha = 0.25f), tokens.accentViolet.copy(alpha = 0.25f))
                            )
                        )
                        .border(1.dp, tokens.glassBorderSubtle, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!analysis.thumbnailUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = analysis.thumbnailUrl,
                            contentDescription = analysis.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (!selectedFormat.isVideo || (analysis.audioOptions.isNotEmpty() && analysis.videoOptions.isEmpty())) {
                                Icons.Default.AudioFile
                            } else {
                                Icons.Default.VideoFile
                            },
                            contentDescription = null,
                            tint = tokens.accentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
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

            if (!analysis.authorizationLimitationNotice.isNullOrBlank()) {
                Text(
                    text = analysis.authorizationLimitationNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("authorization_limitation_notice")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasDownloadableOptions) {
                    val quickButtonLabel = when (selectedFormat) {
                        MediaFormat.MP3 -> "Download MP3 320kbps"
                        MediaFormat.FLAC -> "Download FLAC Lossless"
                        MediaFormat.M4A -> "Download M4A Studio"
                        MediaFormat.WAV -> "Download WAV Master"
                        else -> "Download ${selectedFormat.name} HD"
                    }
                    LiquidGlassPrimaryButton(
                        text = quickButtonLabel,
                        icon = Icons.Default.Download,
                        onClick = onQuickDownload,
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_download_now_button")
                    )

                    LiquidGlassSecondaryButton(
                        text = "Qualities",
                        onClick = onOpenQualityOptions,
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .testTag("open_quality_selector_button")
                    )
                } else {
                    val launchTarget = analysis.externalLaunchUrl ?: analysis.originalUrl
                    LiquidGlassPrimaryButton(
                        text = "Open in ${analysis.providerName}",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            runCatching { uriHandler.openUri(launchTarget) }
                        },
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_in_external_provider_button")
                    )

                    LiquidGlassSecondaryButton(
                        text = "Details",
                        onClick = onOpenQualityOptions,
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .testTag("open_quality_selector_button")
                    )
                }
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
                color = tokens.accentBlue
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "View Queue >",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.accentBlue,
                modifier = Modifier.clickable { onViewAllDownloads() }
            )
        }

        tasks.forEach { task ->
            val stateEnum = runCatching { DownloadJobState.valueOf(task.state) }
                .getOrDefault(DownloadJobState.DOWNLOADING)
            val isCompleted = stateEnum == DownloadJobState.COMPLETED
            val isFailed = stateEnum == DownloadJobState.FAILED || stateEnum == DownloadJobState.CANCELLED

            LiquidGlassCard(
                cornerRadius = 20.dp,
                onClick = { onOpenModal(task.jobId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            tokens.accentBlue.copy(alpha = 0.22f),
                                            tokens.accentViolet.copy(alpha = 0.22f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!task.thumbnailUrl.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = task.thumbnailUrl,
                                    contentDescription = task.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (task.format == "MP3") Icons.Default.AudioFile else Icons.Default.VideoFile,
                                    contentDescription = null,
                                    tint = tokens.accentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.fileName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sizeLabel = if (task.totalBytes > 0L) {
                                "${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.totalBytes)}"
                            } else if (task.downloadedBytes > 0L) {
                                FormatUtils.formatBytes(task.downloadedBytes)
                            } else {
                                task.qualityLabel
                            }
                            Text(
                                text = "${task.format} • $sizeLabel • ${stateEnum.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isCompleted -> tokens.successColor.copy(alpha = 0.14f)
                                        isFailed -> tokens.errorColor.copy(alpha = 0.14f)
                                        else -> tokens.accentBlue.copy(alpha = 0.14f)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isCompleted) "Done" else "${task.progressPercent}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    isCompleted -> tokens.successColor
                                    isFailed -> tokens.errorColor
                                    else -> tokens.accentBlue
                                }
                            )
                        }
                    }

                    if (!isCompleted && !isFailed) {
                        LinearProgressIndicator(
                            progress = { (task.progressPercent / 100f).coerceIn(0f, 1f) },
                            color = tokens.accentBlue,
                            trackColor = tokens.accentBlue.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}
