package com.example.data.model

enum class MediaFormat(val label: String, val extension: String) {
    MP4("MP4 Video", "mp4"),
    MP3("MP3 Audio", "mp3")
}

enum class DownloadJobState(val displayName: String) {
    QUEUED("Queued"),
    ANALYZING("Analyzing"),
    PREPARING("Preparing"),
    DOWNLOADING("Downloading"),
    PROCESSING("Processing"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

data class QualityOption(
    val id: String,
    val format: MediaFormat,
    val label: String,
    val subLabel: String,
    val badge: String? = null,
    val resolutionOrBitrate: String,
    val estimatedSizeBytes: Long, // -1L if unknown from HTTP HEAD
    val downloadUrl: String,
    val codec: String,
    val includesAudio: Boolean = true,
    val isAvailable: Boolean = true,
    val unavailableReason: String? = null
)

data class MediaAnalysisResult(
    val mediaId: String,
    val originalUrl: String,
    val normalizedUrl: String,
    val title: String,
    val authorOrChannel: String,
    val durationSeconds: Int,
    val durationFormatted: String,
    val providerId: String,
    val providerName: String,
    val providerBadgeColorHex: Long,
    val thumbnailUrl: String? = null,
    val videoOptions: List<QualityOption>,
    val audioOptions: List<QualityOption>,
    val isAuthorizedStream: Boolean = true,
    val securityNotice: String? = null,
    val analyzedAt: Long = System.currentTimeMillis()
)

data class ProviderStatusInfo(
    val id: String,
    val name: String,
    val domainPatterns: List<String>,
    val status: ProviderSupportLevel,
    val supportedFormats: List<MediaFormat>,
    val description: String,
    val limitationNote: String
)

enum class ProviderSupportLevel(val badgeText: String) {
    VERIFIED_ACTIVE("Active & Verified"),
    OEMBED_PREVIEW_ONLY("Metadata Only (DRM/TOS Protected)"),
    DIRECT_AUTHORIZED("Authorized Direct Stream")
}
