package com.example.ui

import android.content.Context
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.LinkFlowDao
import com.example.data.local.LinkFlowDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.RecentAnalysisEntity
import com.example.data.local.UserPreferencesState
import com.example.data.model.MediaAnalysisResult
import com.example.data.model.MediaFormat
import com.example.data.model.ProviderStatusInfo
import com.example.data.model.QualityOption
import com.example.data.service.AppUpdateManager
import com.example.data.service.DownloadQueueManager
import com.example.data.service.MediaAnalyzerEngine
import com.example.data.service.OtaInstallPhase
import com.example.data.service.OtaUpdateUiState
import com.example.data.service.UrlAnalysisOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PrimaryTab(val route: String, val label: String) {
    HOME("home", "Home"),
    DOWNLOADS("downloads", "Downloads"),
    LIBRARY("library", "Library"),
    SETTINGS("settings", "Settings")
}

enum class LibrarySortOption(val label: String) {
    DATE_DESC("Newest First"),
    NAME_ASC("Name (A–Z)"),
    SIZE_DESC("Largest Size"),
    TYPE("Media Type")
}

data class StorageMetrics(
    val usedByAppBytes: Long = 0L,
    val availableDeviceBytes: Long = 0L,
    val totalDeviceBytes: Long = 0L
)

data class LinkFlowUiState(
    val showSplash: Boolean = true,
    val currentTab: PrimaryTab = PrimaryTab.HOME,
    val urlInput: String = "",
    val quickFormatSelection: MediaFormat = MediaFormat.MP4,
    val isAnalyzingUrl: Boolean = false,
    val analysisError: UrlAnalysisOutcome.Error? = null,
    val currentAnalysis: MediaAnalysisResult? = null,
    val showQualitySheet: Boolean = false,
    val selectedQualityOption: QualityOption? = null,
    val activeModalJobId: String? = null,
    val inspectJobDetailsId: String? = null,
    val playbackPreviewTask: DownloadTaskEntity? = null,
    val isAudioPreviewPlaying: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastRefreshTimestamp: Long = System.currentTimeMillis(),
    val librarySearchQuery: String = "",
    val libraryFormatFilter: MediaFormat? = null,
    val librarySortOption: LibrarySortOption = LibrarySortOption.DATE_DESC,
    val libraryGridMode: Boolean = true,
    val downloadsSearchQuery: String = "",
    val showAboutDialog: Boolean = false,
    val showSupportedSourcesSheet: Boolean = false,
    val showPrivacyTermsSheet: Boolean = false,
    val toastMessage: String? = null,
    val storageMetrics: StorageMetrics = StorageMetrics()
)

class LinkFlowViewModel(
    private val appContext: Context,
    private val dao: LinkFlowDao,
    private val preferencesRepository: PreferencesRepository,
    private val analyzerEngine: MediaAnalyzerEngine,
    private val queueManager: DownloadQueueManager,
    private val appUpdateManager: AppUpdateManager = AppUpdateManager(appContext)
) : ViewModel() {

    private var otaDownloadJob: Job? = null

    private val _uiState = MutableStateFlow(LinkFlowUiState())
    val uiState: StateFlow<LinkFlowUiState> = _uiState.asStateFlow()

    val otaUpdateState: StateFlow<OtaUpdateUiState> = appUpdateManager.state

    val preferences: StateFlow<UserPreferencesState> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesState()
        )

    val allDownloads: StateFlow<List<DownloadTaskEntity>> = dao.observeAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentAnalyses: StateFlow<List<RecentAnalysisEntity>> = dao.observeRecentAnalyses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val providerAdapters: List<ProviderStatusInfo> = analyzerEngine.providerAdapters

    init {
        cleanupStaleZeroByteDownloadsOnStartup()
        queueManager.resumeInterruptedDownloadsOnStartup()
        refreshStorageMetrics()
    }

    private fun cleanupStaleZeroByteDownloadsOnStartup() {
        viewModelScope.launch {
            runCatching {
                val dir = queueManager.getDownloadsDirectory()
                dir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".part") || file.length() == 0L) {
                        runCatching { file.delete() }
                    }
                }
            }
        }
    }

    fun completeSplash() {
        _uiState.update { it.copy(showSplash = false) }
        checkForAppUpdates(isManualUserTrigger = false)
    }

    fun checkForAppUpdates(isManualUserTrigger: Boolean = false) {
        viewModelScope.launch {
            val prefs = preferences.value
            val phase = appUpdateManager.checkForUpdates(
                repoSlug = prefs.githubRepoSlug,
                dismissedVersionTag = prefs.dismissedUpdateTag,
                dismissedTimestampMs = prefs.dismissedUpdateTimestampMs,
                isManualUserTrigger = isManualUserTrigger
            )
            if (isManualUserTrigger) {
                when (phase) {
                    OtaInstallPhase.AVAILABLE -> {
                        val ver = appUpdateManager.state.value.releaseInfo?.cleanVersionName ?: ""
                        showToast("Update v$ver available!")
                    }
                    OtaInstallPhase.UP_TO_DATE -> {
                        val msg = appUpdateManager.state.value.statusMessage ?: "You are on the latest version."
                        showToast(msg)
                    }
                    OtaInstallPhase.ERROR -> {
                        val err = appUpdateManager.state.value.errorMessage ?: "Could not check GitHub Releases."
                        showToast(err)
                    }
                    else -> {}
                }
            }
        }
    }

    fun triggerPreviewOtaUpdateDialog() {
        appUpdateManager.triggerPreviewUpdateDialog(preferences.value.appBrandName)
    }

    fun startOtaUpdateInstall() {
        otaDownloadJob?.cancel()
        otaDownloadJob = viewModelScope.launch {
            appUpdateManager.downloadAndVerifyUpdateApk()
        }
    }

    fun cancelOtaUpdateDownload() {
        otaDownloadJob?.cancel()
        otaDownloadJob = null
        showToast("Update download cancelled")
    }

    fun grantUnknownSourcesAndInstallOta() {
        appUpdateManager.launchInstallerForDownloadedApk()
    }

    fun dismissOtaUpdateLater() {
        val tag = appUpdateManager.state.value.releaseInfo?.tagName
        appUpdateManager.dismissUpdateDialog()
        if (!tag.isNullOrBlank()) {
            viewModelScope.launch {
                preferencesRepository.recordDismissedUpdate(tag)
            }
        }
    }

    fun setGithubRepoSlug(slug: String) {
        viewModelScope.launch {
            preferencesRepository.setGithubRepoSlug(slug)
            appUpdateManager.setRepositorySlug(slug)
            showToast("GitHub Release repository updated to $slug")
        }
    }

    fun handleIncomingSharedContent(rawSharedText: String) {
        val extractedUrl = MediaAnalyzerEngine.extractUrlFromSharedText(rawSharedText)
        if (extractedUrl.isBlank()) return
        _uiState.update {
            it.copy(
                showSplash = false,
                currentTab = PrimaryTab.HOME,
                urlInput = extractedUrl,
                analysisError = null
            )
        }
        showToast("Link received from Share sheet • Analyzing…")
        analyzeCurrentUrl(urlOverride = extractedUrl, openSheetImmediately = true)
    }

    fun selectTab(tab: PrimaryTab) {
        _uiState.update {
            it.copy(
                currentTab = tab,
                activeModalJobId = null,
                inspectJobDetailsId = null
            )
        }
        if (tab == PrimaryTab.LIBRARY || tab == PrimaryTab.SETTINGS) {
            refreshStorageMetrics()
        }
    }

    fun updateUrlInput(newText: String) {
        _uiState.update {
            it.copy(
                urlInput = newText,
                analysisError = null
            )
        }
    }

    fun clearUrlInput() {
        _uiState.update {
            it.copy(
                urlInput = "",
                analysisError = null,
                currentAnalysis = null,
                selectedQualityOption = null
            )
        }
    }

    fun selectQuickFormat(format: MediaFormat) {
        _uiState.update { state ->
            val updatedDefaultOption = state.currentAnalysis?.let { analysis ->
                chooseDefaultOption(analysis, format)
            }
            state.copy(
                quickFormatSelection = format,
                selectedQualityOption = updatedDefaultOption ?: state.selectedQualityOption
            )
        }
        viewModelScope.launch {
            preferencesRepository.setPreferredFormat(format)
        }
    }

    fun analyzeCurrentUrl(urlOverride: String? = null, openSheetImmediately: Boolean = true) {
        val targetUrl = urlOverride ?: _uiState.value.urlInput
        if (urlOverride != null) {
            _uiState.update { it.copy(urlInput = urlOverride, analysisError = null) }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingUrl = true, analysisError = null) }
            when (val outcome = analyzerEngine.analyzeUrl(targetUrl)) {
                is UrlAnalysisOutcome.Success -> {
                    val result = outcome.result
                    val preferredFmt = _uiState.value.quickFormatSelection
                    val defaultOption = chooseDefaultOption(result, preferredFmt)

                    val bestLabel = defaultOption?.label ?: "Metadata Preview"
                    val estBytes = defaultOption?.estimatedSizeBytes ?: 0L
                    dao.upsertRecentAnalysis(
                        RecentAnalysisEntity(
                            mediaId = result.mediaId,
                            originalUrl = result.originalUrl,
                            title = result.title,
                            providerName = result.providerName,
                            durationFormatted = result.durationFormatted,
                            thumbnailUrl = result.thumbnailUrl,
                            highestQualityLabel = bestLabel,
                            availableFormatsLabel = buildString {
                                if (result.videoOptions.isNotEmpty()) append("MP4")
                                if (result.videoOptions.isNotEmpty() && result.audioOptions.isNotEmpty()) append(" • ")
                                if (result.audioOptions.isNotEmpty()) append("Audio")
                            }.ifBlank { "Metadata Preview" },
                            estimatedSizeBytes = estBytes,
                            isSupportedForDownload = result.isAuthorizedStream && (result.videoOptions.isNotEmpty() || result.audioOptions.isNotEmpty()),
                            statusNote = if (result.isAuthorizedStream && (result.videoOptions.isNotEmpty() || result.audioOptions.isNotEmpty())) {
                                "Verified & Ready"
                            } else {
                                "Metadata Preview Only"
                            },
                            analyzedAt = System.currentTimeMillis()
                        )
                    )

                    _uiState.update {
                        it.copy(
                            isAnalyzingUrl = false,
                            currentAnalysis = result,
                            selectedQualityOption = defaultOption,
                            showQualitySheet = openSheetImmediately,
                            analysisError = null
                        )
                    }
                }
                is UrlAnalysisOutcome.Error -> {
                    _uiState.update {
                        it.copy(
                            isAnalyzingUrl = false,
                            analysisError = outcome,
                            showQualitySheet = false
                        )
                    }
                }
            }
        }
    }

    private fun chooseDefaultOption(
        analysis: MediaAnalysisResult,
        preferredFormat: MediaFormat
    ): QualityOption? {
        return if (preferredFormat == MediaFormat.MP3) {
            analysis.audioOptions.firstOrNull() ?: analysis.videoOptions.firstOrNull()
        } else {
            analysis.videoOptions.firstOrNull() ?: analysis.audioOptions.firstOrNull()
        }
    }

    fun openQualitySelector() {
        if (_uiState.value.currentAnalysis != null) {
            _uiState.update { it.copy(showQualitySheet = true) }
        }
    }

    fun closeQualitySelector() {
        _uiState.update {
            it.copy(
                showQualitySheet = false,
                isAudioPreviewPlaying = false
            )
        }
    }

    fun selectQualityOption(option: QualityOption) {
        _uiState.update {
            it.copy(
                selectedQualityOption = option,
                quickFormatSelection = option.format
            )
        }
    }

    fun toggleAudioPreview() {
        _uiState.update { it.copy(isAudioPreviewPlaying = !it.isAudioPreviewPlaying) }
    }

    fun startSelectedDownload() {
        val analysis = _uiState.value.currentAnalysis ?: return
        val option = _uiState.value.selectedQualityOption ?: chooseDefaultOption(
            analysis,
            _uiState.value.quickFormatSelection
        )
        if (option == null || !analysis.isAuthorizedStream) {
            showToast(analysis.authorizationLimitationNotice ?: "Direct file download is not authorized by ${analysis.providerName} API.")
            return
        }

        viewModelScope.launch {
            val jobId = queueManager.enqueueDownload(analysis, option)
            _uiState.update {
                it.copy(
                    showQualitySheet = false,
                    isAudioPreviewPlaying = false,
                    activeModalJobId = jobId
                )
            }
            refreshStorageMetrics()
        }
    }

    fun openActiveProgressModal(jobId: String) {
        _uiState.update { it.copy(activeModalJobId = jobId) }
    }

    fun dismissActiveProgressModal() {
        _uiState.update { it.copy(activeModalJobId = null) }
        refreshStorageMetrics()
    }

    fun openJobDetails(jobId: String) {
        _uiState.update { it.copy(inspectJobDetailsId = jobId) }
    }

    fun closeJobDetails() {
        _uiState.update { it.copy(inspectJobDetailsId = null) }
    }

    fun openPlaybackPreview(task: DownloadTaskEntity) {
        _uiState.update { it.copy(playbackPreviewTask = task) }
    }

    fun closePlaybackPreview() {
        _uiState.update { it.copy(playbackPreviewTask = null) }
    }

    fun pauseDownload(jobId: String) {
        queueManager.pauseDownload(jobId)
        showToast("Download paused")
    }

    fun resumeDownload(jobId: String) {
        queueManager.resumeDownload(jobId)
        showToast("Resuming download…")
    }

    fun cancelDownload(jobId: String) {
        queueManager.cancelDownload(jobId)
        showToast("Download cancelled")
    }

    fun retryDownload(jobId: String) {
        queueManager.retryDownload(jobId)
        showToast("Retrying download…")
    }

    fun deleteDownloadItem(jobId: String) {
        viewModelScope.launch {
            queueManager.deleteDownloadAndFile(jobId)
            _uiState.update {
                it.copy(
                    activeModalJobId = if (it.activeModalJobId == jobId) null else it.activeModalJobId,
                    inspectJobDetailsId = if (it.inspectJobDetailsId == jobId) null else it.inspectJobDetailsId,
                    playbackPreviewTask = if (it.playbackPreviewTask?.jobId == jobId) null else it.playbackPreviewTask
                )
            }
            refreshStorageMetrics()
            showToast("File removed from library")
        }
    }

    fun renameLibraryFile(jobId: String, newName: String) {
        val clean = newName.trim().replace(Regex("[^a-zA-Z0-9_.\\- ]"), "")
        if (clean.isBlank()) return
        viewModelScope.launch {
            dao.renameDownloadFile(jobId, clean)
            showToast("Renamed to $clean")
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            dao.clearCompletedDownloads()
            refreshStorageMetrics()
            showToast("Cleared completed history")
        }
    }

    fun clearFailedDownloads() {
        viewModelScope.launch {
            dao.clearFailedOrCancelledDownloads()
            refreshStorageMetrics()
            showToast("Cleared failed & cancelled tasks")
        }
    }

    fun clearAllHistoryAndAnalyses() {
        viewModelScope.launch {
            dao.clearRecentAnalyses()
            dao.clearCompletedDownloads()
            refreshStorageMetrics()
            showToast("History & analysis cache cleared")
        }
    }

    fun triggerPullToRefresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(650)
            refreshStorageMetrics()
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    lastRefreshTimestamp = System.currentTimeMillis()
                )
            }
            showToast("Queue & storage synchronized")
        }
    }

    fun updateLibrarySearch(query: String) {
        _uiState.update { it.copy(librarySearchQuery = query) }
    }

    fun updateLibraryFormatFilter(format: MediaFormat?) {
        _uiState.update { it.copy(libraryFormatFilter = format) }
    }

    fun updateLibrarySort(sort: LibrarySortOption) {
        _uiState.update { it.copy(librarySortOption = sort) }
    }

    fun toggleLibraryGridMode() {
        _uiState.update { it.copy(libraryGridMode = !it.libraryGridMode) }
    }

    fun updateDownloadsSearch(query: String) {
        _uiState.update { it.copy(downloadsSearchQuery = query) }
    }

    fun setShowAboutDialog(show: Boolean) {
        _uiState.update { it.copy(showAboutDialog = show) }
    }

    fun setShowSupportedSourcesSheet(show: Boolean) {
        _uiState.update { it.copy(showSupportedSourcesSheet = show) }
    }

    fun setShowPrivacyTermsSheet(show: Boolean) {
        _uiState.update { it.copy(showPrivacyTermsSheet = show) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
        viewModelScope.launch {
            delay(2800)
            _uiState.update { state ->
                if (state.toastMessage == message) state.copy(toastMessage = null) else state
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAccentPreset(preset: String) {
        viewModelScope.launch { preferencesRepository.setAccentPreset(preset) }
    }

    fun setAppBrandName(name: String) {
        viewModelScope.launch {
            preferencesRepository.setAppBrandName(name)
            showToast("Brand identity updated")
        }
    }

    fun setPreferredVideoQuality(quality: String) {
        viewModelScope.launch { preferencesRepository.setPreferredVideoQuality(quality) }
    }

    fun setPreferredAudioBitrate(bitrate: String) {
        viewModelScope.launch { preferencesRepository.setPreferredAudioBitrate(bitrate) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setReducedMotion(enabled) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticFeedback(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setWifiOnlyDownloads(enabled) }
    }

    private fun refreshStorageMetrics() {
        viewModelScope.launch {
            val dir = queueManager.getDownloadsDirectory()
            val filesUsed = dir.listFiles()?.sumOf { it.length() } ?: 0L
            val stat = runCatching { StatFs(appContext.filesDir.absolutePath) }.getOrNull()
            val avail = stat?.availableBytes ?: 0L
            val total = stat?.totalBytes ?: 0L
            _uiState.update {
                it.copy(
                    storageMetrics = StorageMetrics(
                        usedByAppBytes = filesUsed,
                        availableDeviceBytes = avail,
                        totalDeviceBytes = total
                    )
                )
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            val db = LinkFlowDatabase.getInstance(appCtx)
            val dao = db.linkFlowDao()
            val prefs = PreferencesRepository(appCtx)
            val analyzer = MediaAnalyzerEngine()
            val queue = DownloadQueueManager(appCtx, dao, analyzer)
            val updater = AppUpdateManager(appCtx)
            return LinkFlowViewModel(appCtx, dao, prefs, analyzer, queue, updater) as T
        }
    }
}
