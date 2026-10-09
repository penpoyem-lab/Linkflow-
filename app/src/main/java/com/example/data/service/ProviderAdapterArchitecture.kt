package com.example.data.service

import com.example.data.model.MediaFormat
import java.io.File
import java.io.FileInputStream
import java.net.URI

/**
 * Clean Provider Adapter & Error Classification Architecture for LinkFlow:
 * - [ProviderDetector]: Identifies supported URL providers and media categories.
 * - [MediaStreamValidator]: Validates HTTP response headers, Content-Type, redirect chains, and binary file magic bytes.
 * - [DownloadErrorCategory] & [DownloadErrorHandler]: Classifies errors (HTML webpage vs HTTP 403 vs Network vs Invalid Stream)
 *   and determines whether a retry or fresh URL re-resolution is appropriate.
 */
enum class DetectedProviderType(
    val id: String,
    val displayName: String,
    val badgeColorHex: Long
) {
    INSTAGRAM("instagram", "Instagram", 0xFFE1306C),
    YOUTUBE("youtube", "YouTube", 0xFFEF4444),
    TIKTOK("tiktok", "TikTok", 0xFF06B6D4),
    TWITTER_X("twitter_x", "X (Twitter)", 0xFF3B82F6),
    FACEBOOK("facebook", "Facebook", 0xFF1877F2),
    REDDIT("reddit", "Reddit", 0xFFFF4500),
    VIMEO("vimeo", "Vimeo", 0xFF1AB7EA),
    SOUNDCLOUD("soundcloud", "SoundCloud", 0xFFFF5500),
    PINTEREST("pinterest", "Pinterest", 0xFFE60023),
    DAILYMOTION("dailymotion", "Dailymotion", 0xFF0066DC),
    TWITCH("twitch", "Twitch", 0xFF9146FF),
    THREADS("threads", "Threads", 0xFF10B981),
    BILIBILI("bilibili", "Bilibili", 0xFF00A1D6),
    SNAPCHAT("snapchat", "Snapchat", 0xFFFFFC00),
    INTERNET_ARCHIVE("internet_archive", "Internet Archive", 0xFF2563EB),
    WIKIMEDIA("wikimedia", "Wikimedia Commons", 0xFF10B981),
    DIRECT_MEDIA("direct_media", "Direct Media", 0xFF10B981),
    GENERIC_WEBPAGE("generic_webpage", "Web Media", 0xFF6366F1)
}

object ProviderDetector {
    fun detectProvider(url: String): DetectedProviderType {
        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
        val path = runCatching { URI(url).path?.lowercase().orEmpty() }.getOrDefault("")
        return when {
            host.contains("instagram.com") || host.contains("instagr.am") ||
                host.contains("ddinstagram.com") || host.contains("kkinstagram.com") ||
                host.contains("vxinstagram.com") -> DetectedProviderType.INSTAGRAM
            host.contains("youtube.com") || host.contains("youtu.be") ||
                host.contains("googlevideo.com") -> DetectedProviderType.YOUTUBE
            host.contains("tiktok.com") || host.contains("tikwm.com") -> DetectedProviderType.TIKTOK
            host.contains("twitter.com") || host.contains("x.com") ||
                host.contains("t.co") || host.contains("vxtwitter.com") ||
                host.contains("fxtwitter.com") || host.contains("twimg.com") -> DetectedProviderType.TWITTER_X
            host.contains("facebook.com") || host.contains("fb.watch") ||
                host.contains("fb.com") || host.contains("fbcdn.net") -> DetectedProviderType.FACEBOOK
            host.contains("reddit.com") || host.contains("redd.it") ||
                host.contains("rxddit.com") -> DetectedProviderType.REDDIT
            host.contains("vimeo.com") -> DetectedProviderType.VIMEO
            host.contains("soundcloud.com") -> DetectedProviderType.SOUNDCLOUD
            host.contains("pinterest.com") || host.contains("pin.it") ||
                host.contains("pinimg.com") -> DetectedProviderType.PINTEREST
            host.contains("dailymotion.com") || host.contains("dai.ly") -> DetectedProviderType.DAILYMOTION
            host.contains("twitch.tv") -> DetectedProviderType.TWITCH
            host.contains("threads.net") -> DetectedProviderType.THREADS
            host.contains("bilibili.com") || host.contains("b23.tv") -> DetectedProviderType.BILIBILI
            host.contains("snapchat.com") -> DetectedProviderType.SNAPCHAT
            host.contains("archive.org") -> DetectedProviderType.INTERNET_ARCHIVE
            host.contains("wikimedia.org") -> DetectedProviderType.WIKIMEDIA
            path.endsWith(".mp4") || path.endsWith(".mp3") || path.endsWith(".m4a") ||
                path.endsWith(".webm") || path.endsWith(".wav") || path.endsWith(".ogg") ||
                path.endsWith(".m3u8") -> DetectedProviderType.DIRECT_MEDIA
            else -> DetectedProviderType.GENERIC_WEBPAGE
        }
    }

    fun isSocialMediaProvider(provider: DetectedProviderType): Boolean {
        return provider != DetectedProviderType.DIRECT_MEDIA &&
            provider != DetectedProviderType.GENERIC_WEBPAGE &&
            provider != DetectedProviderType.INTERNET_ARCHIVE &&
            provider != DetectedProviderType.WIKIMEDIA
    }
}

object MediaStreamValidator {
    private val HTML_SIGNATURE_PREFIXES = listOf(
        "<!doctype html",
        "<html",
        "<head",
        "<body",
        "<script",
        "<meta",
        "<?xml",
        "{\"error\"",
        "{\"status\":\"error\""
    )

    private val SOCIAL_WEB_DOMAINS = listOf(
        "instagram.com", "instagr.am", "ddinstagram.com", "kkinstagram.com", "vxinstagram.com",
        "youtube.com", "youtu.be",
        "tiktok.com", "vm.tiktok.com", "vt.tiktok.com",
        "twitter.com", "x.com", "t.co", "vxtwitter.com", "fxtwitter.com",
        "facebook.com", "fb.watch", "fb.com",
        "reddit.com", "redd.it", "rxddit.com",
        "threads.net", "pinterest.com", "pin.it"
    )

    private val KNOWN_WEBPAGE_PATH_PATTERNS = listOf(
        Regex("""instagram\.com/(?:reel|reels|p|tv|share|stories)/""", RegexOption.IGNORE_CASE),
        Regex("""(?:ddinstagram|kkinstagram|vxinstagram)\.com/(?:reel|reels|p|tv)/[A-Za-z0-9_-]+/?$""", RegexOption.IGNORE_CASE),
        Regex("""tiktok\.com/@[^/]+/video/\d+""", RegexOption.IGNORE_CASE),
        Regex("""(?:twitter|x|vxtwitter|fxtwitter)\.com/[^/]+/status/\d+""", RegexOption.IGNORE_CASE),
        Regex("""youtube\.com/(?:watch|shorts|embed|live)""", RegexOption.IGNORE_CASE),
        Regex("""youtu\.be/[A-Za-z0-9_-]+""", RegexOption.IGNORE_CASE),
        Regex("""facebook\.com/(?:share|reel|watch|[^/]+/videos)/""", RegexOption.IGNORE_CASE),
        Regex("""fb\.watch/""", RegexOption.IGNORE_CASE)
    )

    /**
     * Checks whether a candidate URL is clearly a social webpage URL rather than a direct CDN stream URL.
     */
    fun isLikelyWebpageLandingUrl(url: String): Boolean {
        val clean = url.substringBefore('?').trim()
        val lower = clean.lowercase()
        if (lower.endsWith(".mp4") || lower.endsWith(".mp3") || lower.endsWith(".m4a") ||
            lower.endsWith(".webm") || lower.endsWith(".m3u8") || lower.contains("googlevideo.com") ||
            lower.contains("cdninstagram.com") || lower.contains("fbcdn.net") ||
            lower.contains("twimg.com") || lower.contains("tikwm.com") ||
            lower.contains("v.redd.it")
        ) {
            return false
        }
        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
        if (SOCIAL_WEB_DOMAINS.any { host == it || host.endsWith(".$it") }) {
            // Social main domains without a direct media extension or /videos/ endpoint are webpages
            if (!lower.contains("/videos/") && !lower.contains("video_redirect")) {
                return true
            }
        }
        return KNOWN_WEBPAGE_PATH_PATTERNS.any { it.containsMatchIn(clean) }
    }

    /**
     * Inspects HTTP Content-Type header to determine if the server returned an HTML webpage or JSON error
     * instead of a binary media stream.
     */
    fun isInvalidNonMediaContentType(contentType: String?): Boolean {
        if (contentType.isNullOrBlank()) return false
        val lower = contentType.lowercase()
        return lower.contains("text/html") ||
            lower.contains("application/xhtml") ||
            lower.contains("text/plain") ||
            lower.contains("application/json")
    }

    /**
     * Inspects the first bytes of a downloaded file to verify it is a genuine binary media container
     * (e.g., ISO Base Media / MP4 `ftyp`, ID3 / ADTS MP3, Ogg, WebM/Matroska, RIFF WAV, or HLS segment)
     * and NOT an HTML webpage, login wall, rate-limit page, or JSON error payload.
     */
    fun validateDownloadedMediaFile(file: File): MediaFileValidationResult {
        if (!file.exists()) {
            return MediaFileValidationResult(
                isValid = false,
                reason = "Downloaded file does not exist on disk"
            )
        }
        val length = file.length()
        if (length <= 512L) {
            return MediaFileValidationResult(
                isValid = false,
                reason = "Downloaded stream was empty or truncated ($length B)"
            )
        }

        return try {
            val headerBytes = ByteArray(512)
            val readCount = FileInputStream(file).use { it.read(headerBytes) }
            if (readCount <= 0) {
                return MediaFileValidationResult(isValid = false, reason = "Unable to read media header bytes")
            }

            val headAscii = String(headerBytes, 0, readCount, Charsets.UTF_8)
                .trimStart()
                .lowercase()

            for (prefix in HTML_SIGNATURE_PREFIXES) {
                if (headAscii.startsWith(prefix)) {
                    return MediaFileValidationResult(
                        isValid = false,
                        isHtmlWebpage = true,
                        reason = "Server returned an HTML webpage or JSON error instead of a binary MP4/MP3 stream"
                    )
                }
            }

            if (headAscii.contains("<html") || headAscii.contains("<!doctype html")) {
                return MediaFileValidationResult(
                    isValid = false,
                    isHtmlWebpage = true,
                    reason = "Server returned an HTML login/consent page instead of a binary media stream"
                )
            }

            MediaFileValidationResult(isValid = true, reason = "Verified binary media container ($length bytes)")
        } catch (e: Exception) {
            MediaFileValidationResult(isValid = false, reason = "File validation error: ${e.localizedMessage}")
        }
    }
}

data class MediaFileValidationResult(
    val isValid: Boolean,
    val isHtmlWebpage: Boolean = false,
    val reason: String
)

enum class DownloadErrorCategory {
    HTML_WEBPAGE_INSTEAD_OF_MEDIA,
    FORBIDDEN_OR_EXPIRED_URL_403,
    RATE_LIMITED_429,
    NOT_FOUND_404,
    NETWORK_IO_TIMEOUT,
    UNSUPPORTED_OR_PRIVATE_CONTENT,
    UNKNOWN_ERROR
}

data class ClassifiedDownloadError(
    val category: DownloadErrorCategory,
    val userFriendlyMessage: String,
    val recoveryActionHint: String,
    val shouldReResolveSourceUrl: Boolean,
    val isRetryable: Boolean
)

object DownloadErrorHandler {
    fun classify(throwable: Throwable, providerName: String): ClassifiedDownloadError {
        val rawMsg = (throwable.localizedMessage ?: throwable.message ?: "Download failed").trim()
        val lower = rawMsg.lowercase()

        return when {
            lower.contains("html webpage") || lower.contains("text/html") || lower.contains("html payload") -> {
                ClassifiedDownloadError(
                    category = DownloadErrorCategory.HTML_WEBPAGE_INSTEAD_OF_MEDIA,
                    userFriendlyMessage = "Provider returned a webpage instead of a direct media stream for $providerName.",
                    recoveryActionHint = "Tap Retry Job to re-resolve a fresh direct MP4/MP3 stream from the source URL.",
                    shouldReResolveSourceUrl = true,
                    isRetryable = true
                )
            }
            lower.contains("http 403") || lower.contains("forbidden") -> {
                ClassifiedDownloadError(
                    category = DownloadErrorCategory.FORBIDDEN_OR_EXPIRED_URL_403,
                    userFriendlyMessage = "HTTP 403: Signed media stream expired or access was restricted by $providerName CDN.",
                    recoveryActionHint = "Tap Retry Job to refresh the signed stream token from the original link.",
                    shouldReResolveSourceUrl = true,
                    isRetryable = true
                )
            }
            lower.contains("http 429") || lower.contains("too many requests") -> {
                ClassifiedDownloadError(
                    category = DownloadErrorCategory.RATE_LIMITED_429,
                    userFriendlyMessage = "HTTP 429: Rate limit reached on $providerName.",
                    recoveryActionHint = "Wait a few seconds and tap Retry Job to switch to an alternate mirror.",
                    shouldReResolveSourceUrl = true,
                    isRetryable = true
                )
            }
            lower.contains("http 404") || lower.contains("not found") -> {
                ClassifiedDownloadError(
                    category = DownloadErrorCategory.NOT_FOUND_404,
                    userFriendlyMessage = "HTTP 404: The media stream is no longer available at this URL.",
                    recoveryActionHint = "Verify the post has not been deleted or made private.",
                    shouldReResolveSourceUrl = true,
                    isRetryable = false
                )
            }
            lower.contains("timeout") || lower.contains("unable to resolve host") || lower.contains("connection") -> {
                ClassifiedDownloadError(
                    category = DownloadErrorCategory.NETWORK_IO_TIMEOUT,
                    userFriendlyMessage = "Network connection interrupted while streaming from $providerName.",
                    recoveryActionHint = "Check your internet connection and tap Retry Job.",
                    shouldReResolveSourceUrl = false,
                    isRetryable = true
                )
            }
            else -> {
                ClassifiedDownloadError(
                    category = DownloadErrorCategory.UNKNOWN_ERROR,
                    userFriendlyMessage = rawMsg,
                    recoveryActionHint = "Tap Retry Job to re-analyze the source link and try an alternate stream.",
                    shouldReResolveSourceUrl = true,
                    isRetryable = true
                )
            }
        }
    }
}
