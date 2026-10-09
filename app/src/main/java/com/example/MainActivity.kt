package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DownloadJobState
import com.example.ui.LinkFlowViewModel
import com.example.ui.PrimaryTab
import com.example.ui.components.AmbientMidnightBackground
import com.example.ui.components.LiquidGlassBottomNavigation
import com.example.ui.screens.AboutLinkFlowDialog
import com.example.ui.screens.ActiveDownloadProgressModal
import com.example.ui.screens.AnimatedSplashScreen
import com.example.ui.screens.DownloadJobDetailsSheet
import com.example.ui.screens.DownloadsQueueScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MediaPlaybackModal
import com.example.ui.screens.PrivacyAndTermsSheet
import com.example.ui.screens.QualitySelectorSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SoftwareUpdateModal
import com.example.ui.screens.SupportedSourcesSheet
import com.example.ui.theme.LinkFlowTheme
import com.example.ui.theme.LocalLinkFlowTokens
import com.example.util.FormatUtils

class MainActivity : ComponentActivity() {
    private val incomingSharedTextState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractSharedTextFromIntent(intent)?.let { shared ->
            incomingSharedTextState.value = shared
        }
        setContent {
            LinkFlowApp(
                incomingSharedUrl = incomingSharedTextState.value,
                onConsumeSharedUrl = { incomingSharedTextState.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedTextFromIntent(intent)?.let { shared ->
            incomingSharedTextState.value = shared
        }
    }

    private fun extractSharedTextFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: androidx.core.content.IntentCompat.getParcelableExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        android.net.Uri::class.java
                    )?.toString()
            }
            Intent.ACTION_VIEW -> {
                intent.dataString
            }
            else -> null
        }?.takeIf { it.isNotBlank() }
    }
}

@Composable
fun LinkFlowApp(
    incomingSharedUrl: String? = null,
    onConsumeSharedUrl: () -> Unit = {},
    viewModel: LinkFlowViewModel = viewModel(
        factory = LinkFlowViewModel.Factory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val otaUpdateState by viewModel.otaUpdateState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val recentAnalyses by viewModel.recentAnalyses.collectAsStateWithLifecycle()

    LaunchedEffect(incomingSharedUrl) {
        if (!incomingSharedUrl.isNullOrBlank()) {
            viewModel.handleIncomingSharedContent(incomingSharedUrl)
            onConsumeSharedUrl()
        }
    }

    val activeDownloadCount = allDownloads.count {
        it.state in listOf(
            DownloadJobState.QUEUED.name,
            DownloadJobState.ANALYZING.name,
            DownloadJobState.PREPARING.name,
            DownloadJobState.DOWNLOADING.name,
            DownloadJobState.PROCESSING.name
        )
    }

    // Handle system Back navigation cleanly across modals and tabs
    BackHandler(
        enabled = otaUpdateState.showModal ||
            uiState.playbackPreviewTask != null ||
            uiState.inspectJobDetailsId != null ||
            uiState.activeModalJobId != null ||
            uiState.showQualitySheet ||
            uiState.showSupportedSourcesSheet ||
            uiState.showPrivacyTermsSheet ||
            uiState.currentTab != PrimaryTab.HOME
    ) {
        when {
            otaUpdateState.showModal -> viewModel.dismissOtaUpdateLater()
            uiState.playbackPreviewTask != null -> viewModel.closePlaybackPreview()
            uiState.inspectJobDetailsId != null -> viewModel.closeJobDetails()
            uiState.activeModalJobId != null -> viewModel.dismissActiveProgressModal()
            uiState.showQualitySheet -> viewModel.closeQualitySelector()
            uiState.showSupportedSourcesSheet -> viewModel.setShowSupportedSourcesSheet(false)
            uiState.showPrivacyTermsSheet -> viewModel.setShowPrivacyTermsSheet(false)
            uiState.currentTab != PrimaryTab.HOME -> viewModel.selectTab(PrimaryTab.HOME)
        }
    }

    LinkFlowTheme(
        themeMode = preferences.themeMode,
        accentPreset = preferences.accentPreset
    ) {
        val tokens = LocalLinkFlowTokens.current

        Crossfade(
            targetState = uiState.showSplash,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            label = "splash_to_main"
        ) { isSplash ->
            if (isSplash) {
                AnimatedSplashScreen(
                    brandName = preferences.appBrandName,
                    reducedMotion = preferences.reducedMotion,
                    onSplashFinished = { viewModel.completeSplash() }
                )
            } else {
                AmbientMidnightBackground(reducedMotion = preferences.reducedMotion) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            LiquidGlassBottomNavigation(
                                currentTab = uiState.currentTab,
                                activeDownloadCount = activeDownloadCount,
                                onSelectTab = { viewModel.selectTab(it) },
                                reducedMotion = preferences.reducedMotion
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Directional Spring-Physics Animated Tab Transitions
                            AnimatedContent(
                                targetState = uiState.currentTab,
                                transitionSpec = {
                                    if (preferences.reducedMotion) {
                                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                                    } else {
                                        val forward = targetState.ordinal >= initialState.ordinal
                                        val slideIn = slideInHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            initialOffsetX = { fullWidth ->
                                                if (forward) fullWidth / 4 else -fullWidth / 4
                                            }
                                        ) + fadeIn(tween(280)) + scaleIn(
                                            initialScale = 0.96f,
                                            animationSpec = tween(280)
                                        )

                                        val slideOut = slideOutHorizontally(
                                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                                            targetOffsetX = { fullWidth ->
                                                if (forward) -fullWidth / 5 else fullWidth / 5
                                            }
                                        ) + fadeOut(tween(220)) + scaleOut(
                                            targetScale = 0.96f,
                                            animationSpec = tween(220)
                                        )

                                        (slideIn togetherWith slideOut).using(
                                            SizeTransform(clip = false)
                                        )
                                    }
                                },
                                label = "main_tab_animated_content"
                            ) { targetTab ->
                                when (targetTab) {
                                    PrimaryTab.HOME -> {
                                        HomeScreen(
                                            uiState = uiState,
                                            brandName = preferences.appBrandName,
                                            reducedMotion = preferences.reducedMotion,
                                            recentAnalyses = recentAnalyses,
                                            activeAndRecentTasks = allDownloads,
                                            providerAdapters = viewModel.providerAdapters,
                                            onUrlChange = viewModel::updateUrlInput,
                                            onClearUrl = viewModel::clearUrlInput,
                                            onAnalyzeUrl = { urlOverride ->
                                                viewModel.analyzeCurrentUrl(urlOverride)
                                            },
                                            onSelectQuickFormat = viewModel::selectQuickFormat,
                                            onOpenQualitySheet = viewModel::openQualitySelector,
                                            onStartQuickDownload = viewModel::startSelectedDownload,
                                            onOpenActiveModal = viewModel::openActiveProgressModal,
                                            onNavigateToTab = viewModel::selectTab,
                                            onPullToRefresh = viewModel::triggerPullToRefresh,
                                            onShowAbout = { viewModel.setShowAboutDialog(true) },
                                            onShowSupportedSources = { viewModel.setShowSupportedSourcesSheet(true) },
                                            onShowPrivacyTerms = { viewModel.setShowPrivacyTermsSheet(true) },
                                            onShowToast = viewModel::showToast
                                        )
                                    }

                                    PrimaryTab.DOWNLOADS -> {
                                        DownloadsQueueScreen(
                                            allDownloads = allDownloads,
                                            searchQuery = uiState.downloadsSearchQuery,
                                            isRefreshing = uiState.isRefreshing,
                                            reducedMotion = preferences.reducedMotion,
                                            onSearchQueryChange = viewModel::updateDownloadsSearch,
                                            onPullToRefresh = viewModel::triggerPullToRefresh,
                                            onOpenLiveProgressModal = viewModel::openActiveProgressModal,
                                            onOpenJobDetails = viewModel::openJobDetails,
                                            onPause = viewModel::pauseDownload,
                                            onResume = viewModel::resumeDownload,
                                            onCancel = viewModel::cancelDownload,
                                            onRetry = viewModel::retryDownload,
                                            onDelete = viewModel::deleteDownloadItem,
                                            onPlayFile = viewModel::openPlaybackPreview,
                                            onShareFile = { task ->
                                                FormatUtils.shareDownloadedFile(
                                                    context = context,
                                                    task = task,
                                                    onError = viewModel::showToast
                                                )
                                            },
                                            onClearCompleted = viewModel::clearCompletedDownloads,
                                            onExploreHome = { viewModel.selectTab(PrimaryTab.HOME) }
                                        )
                                    }

                                    PrimaryTab.LIBRARY -> {
                                        LibraryScreen(
                                            allDownloads = allDownloads,
                                            searchQuery = uiState.librarySearchQuery,
                                            formatFilter = uiState.libraryFormatFilter,
                                            sortOption = uiState.librarySortOption,
                                            isGridMode = uiState.libraryGridMode,
                                            isRefreshing = uiState.isRefreshing,
                                            reducedMotion = preferences.reducedMotion,
                                            onSearchChange = viewModel::updateLibrarySearch,
                                            onFormatFilterChange = viewModel::updateLibraryFormatFilter,
                                            onSortChange = viewModel::updateLibrarySort,
                                            onToggleGridMode = viewModel::toggleLibraryGridMode,
                                            onPullToRefresh = viewModel::triggerPullToRefresh,
                                            onPlayMedia = viewModel::openPlaybackPreview,
                                            onShareMedia = { task ->
                                                FormatUtils.shareDownloadedFile(
                                                    context = context,
                                                    task = task,
                                                    onError = viewModel::showToast
                                                )
                                            },
                                            onRenameMedia = viewModel::renameLibraryFile,
                                            onDeleteMedia = viewModel::deleteDownloadItem,
                                            onOpenDetails = viewModel::openJobDetails,
                                            onExploreStreams = { viewModel.selectTab(PrimaryTab.HOME) }
                                        )
                                    }

                                    PrimaryTab.SETTINGS -> {
                                        SettingsScreen(
                                            preferences = preferences,
                                            storageMetrics = uiState.storageMetrics,
                                            otaState = otaUpdateState,
                                            onThemeModeChange = viewModel::setThemeMode,
                                            onAccentPresetChange = viewModel::setAccentPreset,
                                            onBrandNameChange = viewModel::setAppBrandName,
                                            onPreferredFormatChange = viewModel::selectQuickFormat,
                                            onPreferredVideoQualityChange = viewModel::setPreferredVideoQuality,
                                            onPreferredAudioBitrateChange = viewModel::setPreferredAudioBitrate,
                                            onReducedMotionChange = viewModel::setReducedMotion,
                                            onHapticFeedbackChange = viewModel::setHapticFeedback,
                                            onNotificationsChange = viewModel::setNotificationsEnabled,
                                            onWifiOnlyChange = viewModel::setWifiOnlyDownloads,
                                            onClearHistory = viewModel::clearAllHistoryAndAnalyses,
                                            isRefreshing = uiState.isRefreshing,
                                            onPullToRefresh = viewModel::triggerPullToRefresh,
                                            onCheckForUpdates = { viewModel.checkForAppUpdates(isManualUserTrigger = true) },
                                            onPreviewUpdateDialog = viewModel::triggerPreviewOtaUpdateDialog,
                                            onGithubRepoChange = viewModel::setGithubRepoSlug,
                                            onShowAbout = { viewModel.setShowAboutDialog(true) },
                                            onShowSupportedSources = { viewModel.setShowSupportedSourcesSheet(true) },
                                            onShowPrivacyTerms = { viewModel.setShowPrivacyTermsSheet(true) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Overlay 1: Quality Selector Sheet
                    AnimatedVisibility(
                        visible = uiState.showQualitySheet && uiState.currentAnalysis != null,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn() + scaleIn(initialScale = 0.94f),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut() + scaleOut(targetScale = 0.94f)
                    ) {
                        uiState.currentAnalysis?.let { analysis ->
                            QualitySelectorSheet(
                                analysis = analysis,
                                selectedOption = uiState.selectedQualityOption,
                                isAudioPreviewPlaying = uiState.isAudioPreviewPlaying,
                                reducedMotion = preferences.reducedMotion,
                                onSelectOption = viewModel::selectQualityOption,
                                onToggleAudioPreview = viewModel::toggleAudioPreview,
                                onStartDownload = viewModel::startSelectedDownload,
                                onDismiss = viewModel::closeQualitySelector
                            )
                        }
                    }

                    // Overlay 2: Full-Screen Active Download Progress Modal
                    val activeModalTask = uiState.activeModalJobId?.let { id ->
                        allDownloads.find { it.jobId == id }
                    }
                    AnimatedVisibility(
                        visible = activeModalTask != null,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { it }
                        ) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        activeModalTask?.let { task ->
                            ActiveDownloadProgressModal(
                                task = task,
                                reducedMotion = preferences.reducedMotion,
                                onPause = { viewModel.pauseDownload(task.jobId) },
                                onResume = { viewModel.resumeDownload(task.jobId) },
                                onCancel = { viewModel.cancelDownload(task.jobId) },
                                onRetry = { viewModel.retryDownload(task.jobId) },
                                onRunInBackground = { viewModel.dismissActiveProgressModal() },
                                onPlayMedia = {
                                    viewModel.dismissActiveProgressModal()
                                    viewModel.openPlaybackPreview(task)
                                },
                                onShareMedia = {
                                    FormatUtils.shareDownloadedFile(
                                        context = context,
                                        task = task,
                                        onError = viewModel::showToast
                                    )
                                },
                                onDownloadAnother = {
                                    viewModel.dismissActiveProgressModal()
                                    viewModel.selectTab(PrimaryTab.HOME)
                                }
                            )
                        }
                    }

                    // Overlay 3: Download Job Details Sheet (Spring Animated)
                    val detailsTask = uiState.inspectJobDetailsId?.let { id ->
                        allDownloads.find { it.jobId == id }
                    }
                    AnimatedVisibility(
                        visible = detailsTask != null,
                        enter = fadeIn() + scaleIn(
                            initialScale = 0.90f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        exit = fadeOut() + scaleOut(targetScale = 0.90f)
                    ) {
                        detailsTask?.let { task ->
                            DownloadJobDetailsSheet(
                                task = task,
                                onClose = viewModel::closeJobDetails,
                                onOpenLiveProgress = {
                                    val id = task.jobId
                                    viewModel.closeJobDetails()
                                    viewModel.openActiveProgressModal(id)
                                },
                                onPlayFile = {
                                    viewModel.closeJobDetails()
                                    viewModel.openPlaybackPreview(task)
                                },
                                onShareFile = {
                                    FormatUtils.shareDownloadedFile(
                                        context = context,
                                        task = task,
                                        onError = viewModel::showToast
                                    )
                                },
                                onDeleteFile = { viewModel.deleteDownloadItem(task.jobId) }
                            )
                        }
                    }

                    // Overlay 4: Media Playback Modal (Spring Animated)
                    AnimatedVisibility(
                        visible = uiState.playbackPreviewTask != null,
                        enter = fadeIn() + scaleIn(
                            initialScale = 0.90f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        exit = fadeOut() + scaleOut(targetScale = 0.90f)
                    ) {
                        uiState.playbackPreviewTask?.let { playTask ->
                            MediaPlaybackModal(
                                task = playTask,
                                reducedMotion = preferences.reducedMotion,
                                onClose = viewModel::closePlaybackPreview,
                                onShare = {
                                    FormatUtils.shareDownloadedFile(
                                        context = context,
                                        task = playTask,
                                        onError = viewModel::showToast
                                    )
                                }
                            )
                        }
                    }

                    // Overlay 5: Supported Sources Sheet (Spring Animated)
                    AnimatedVisibility(
                        visible = uiState.showSupportedSourcesSheet,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                    ) {
                        SupportedSourcesSheet(
                            adapters = viewModel.providerAdapters,
                            onClose = { viewModel.setShowSupportedSourcesSheet(false) }
                        )
                    }

                    // Overlay 6: Privacy & Terms Sheet (Spring Animated)
                    AnimatedVisibility(
                        visible = uiState.showPrivacyTermsSheet,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                    ) {
                        PrivacyAndTermsSheet(
                            onClose = { viewModel.setShowPrivacyTermsSheet(false) }
                        )
                    }

                    // Overlay 7: About Dialog
                    if (uiState.showAboutDialog) {
                        AboutLinkFlowDialog(
                            brandName = preferences.appBrandName,
                            onDismiss = { viewModel.setShowAboutDialog(false) }
                        )
                    }

                    // Overlay 8: Full-Screen OTA Software Update Modal (GitHub Releases)
                    AnimatedVisibility(
                        visible = otaUpdateState.showModal && otaUpdateState.releaseInfo != null,
                        enter = fadeIn(tween(280)) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { it / 3 }
                        ),
                        exit = fadeOut(tween(220)) + scaleOut(targetScale = 0.92f) + slideOutVertically(targetOffsetY = { it / 3 })
                    ) {
                        SoftwareUpdateModal(
                            otaState = otaUpdateState,
                            brandName = preferences.appBrandName,
                            reducedMotion = preferences.reducedMotion,
                            onInstallNow = viewModel::startOtaUpdateInstall,
                            onCancelDownload = viewModel::cancelOtaUpdateDownload,
                            onGrantInstallPermission = viewModel::grantUnknownSourcesAndInstallOta,
                            onDismissLater = viewModel::dismissOtaUpdateLater
                        )
                    }

                    // Top Floating Liquid-Glass Toast Notification
                    AnimatedVisibility(
                        visible = uiState.toastMessage != null,
                        enter = fadeIn() + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            initialOffsetY = { -it }
                        ) + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }) + scaleOut(targetScale = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(top = 12.dp)
                    ) {
                        uiState.toastMessage?.let { msg ->
                            com.example.ui.components.LiquidGlassCard(
                                cornerRadius = 24.dp,
                                isHighlighted = true
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
