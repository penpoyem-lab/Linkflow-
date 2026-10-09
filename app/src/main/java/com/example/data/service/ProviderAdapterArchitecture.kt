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
    YOUTUBE("youtube_official_metadata", "YouTube", 0xFFEF4444),
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
                host.contains("youtube-nocookie.com") ||
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

    /**
     * Accurately extracts the YouTube Video ID from standard watch URLs, mobile m.youtube.com,
     * music.youtube.com, youtu.be short links, /shorts/, /live/, /embed/, /v/, and share links with ?si=...
     */
    fun extractYouTubeVideoId(url: String): String? {
        return try {
            val trimmed = url.trim()
            val uri = URI(if (trimmed.startsWith("http", ignoreCase = true)) trimmed else "https://$trimmed")
            val host = uri.host?.lowercase().orEmpty()
            val path = uri.path.orEmpty()
            val candidate = when {
                host.contains("youtu.be") -> path.trim('/').substringBefore('/').substringBefore('?')
                path.startsWith("/shorts/") -> path.removePrefix("/shorts/").substringBefore('/').substringBefore('?')
                path.startsWith("/live/") -> path.removePrefix("/live/").substringBefore('/').substringBefore('?')
                path.startsWith("/embed/") -> path.removePrefix("/embed/").substringBefore('/').substringBefore('?')
                path.startsWith("/v/") -> path.removePrefix("/v/").substringBefore('/').substringBefore('?')
                else -> {
                    val query = uri.rawQuery.orEmpty()
                    query.split('&')
                        .map { it.split('=', limit = 2) }
                        .firstOrNull { it.firstOrNull() == "v" || it.firstOrNull() == "video_id" }
                        ?.getOrNull(1)
                }
            }?.trim()
            candidate?.takeIf { it.length in 6..20 && it.all { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '-' } }
        } catch (_: Exception) {
            null
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

            // Inspect binary magic signatures (MP4/M4A ftyp, MP3 ID3 or ADTS/MPEG frame sync, WebM EBML, Ogg, WAV RIFF)
            val detectedFormat = when {
                readCount >= 8 &&
                    headerBytes[4] == 'f'.code.toByte() &&
                    headerBytes[5] == 't'.code.toByte() &&
                    headerBytes[6] == 'y'.code.toByte() &&
                    headerBytes[7] == 'p'.code.toByte() -> "MP4/M4A"
                readCount >= 3 &&
                    headerBytes[0] == 'I'.code.toByte() &&
                    headerBytes[1] == 'D'.code.toByte() &&
                    headerBytes[2] == '3'.code.toByte() -> "MP3"
                readCount >= 2 &&
                    (headerBytes[0].toInt() and 0xFF) == 0xFF &&
                    ((headerBytes[1].toInt() and 0xE0) == 0xE0) -> "MP3/AAC"
                readCount >= 4 &&
                    (headerBytes[0].toInt() and 0xFF) == 0x1A &&
                    (headerBytes[1].toInt() and 0xFF) == 0x45 &&
                    (headerBytes[2].toInt() and 0xFF) == 0xDF &&
                    (headerBytes[3].toInt() and 0xFF) == 0xA3 -> "WEBM/MKV"
                readCount >= 4 &&
                    headerBytes[0] == 'R'.code.toByte() &&
                    headerBytes[1] == 'I'.code.toByte() &&
                    headerBytes[2] == 'F'.code.toByte() &&
                    headerBytes[3] == 'F'.code.toByte() -> "WAV"
                readCount >= 4 &&
                    headerBytes[0] == 'O'.code.toByte() &&
                    headerBytes[1] == 'g'.code.toByte() &&
                    headerBytes[2] == 'g'.code.toByte() &&
                    headerBytes[3] == 'S'.code.toByte() -> "OGG"
                else -> "BINARY_MEDIA"
            }

            MediaFileValidationResult(
                isValid = true,
                detectedFormat = detectedFormat,
                reason = "Verified $detectedFormat binary media container ($length bytes)"
            )
        } catch (e: Exception) {
            MediaFileValidationResult(isValid = false, reason = "File validation error: ${e.localizedMessage}")
        }
    }
}

data class MediaFileValidationResult(
    val isValid: Boolean,
    val isHtmlWebpage: Boolean = false,
    val detectedFormat: String? = null,
    val reason: String
)

enum class DownloadErrorCategory {
    HTML_WEBPAGE_INSTEAD_OF_MEDIA,
    UNAUTHORIZED_401,
    FORBIDDEN_OR_EXPIRED_URL_403,
    RATE_LIMITED_429,
    NOT_FOUND_404,
    SERVER_ERROR_5XX,
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
    fun classifyHttpStatus(statusCode: Int, statusMessage: String, providerName: String): ClassifiedDownloadError {
        return when (statusCode) {
            401 -> ClassifiedDownloadError(
                category = DownloadErrorCategory.UNAUTHORIZED_401,
                userFriendlyMessage = "HTTP 401: Authentication or valid authorization token required by $providerName.",
                recoveryActionHint = "Verify the link is publicly accessible or provide an authorized signed link.",
                shouldReResolveSourceUrl = true,
                isRetryable = true
            )
            403 -> ClassifiedDownloadError(
                category = DownloadErrorCategory.FORBIDDEN_OR_EXPIRED_URL_403,
                userFriendlyMessage = "HTTP 403: Signed media stream expired or access was restricted by $providerName CDN.",
                recoveryActionHint = "Tap Retry Job to refresh the signed stream token from the original link.",
                shouldReResolveSourceUrl = true,
                isRetryable = true
            )
            404 -> ClassifiedDownloadError(
                category = DownloadErrorCategory.NOT_FOUND_404,
                userFriendlyMessage = "HTTP 404: The media stream is no longer available at this URL ($providerName).",
                recoveryActionHint = "Verify the post or file has not been deleted or moved.",
                shouldReResolveSourceUrl = true,
                isRetryable = false
            )
            429 -> ClassifiedDownloadError(
                category = DownloadErrorCategory.RATE_LIMITED_429,
                userFriendlyMessage = "HTTP 429: Rate limit reached on $providerName.",
                recoveryActionHint = "Wait a few seconds and tap Retry Job.",
                shouldReResolveSourceUrl = true,
                isRetryable = true
            )
            in 500..599 -> ClassifiedDownloadError(
                category = DownloadErrorCategory.SERVER_ERROR_5XX,
                userFriendlyMessage = "HTTP $statusCode: Upstream server error at $providerName (${statusMessage.ifBlank { "Service unavailable" }}).",
                recoveryActionHint = "Wait a moment and tap Retry Job to reconnect.",
                shouldReResolveSourceUrl = true,
                isRetryable = true
            )
            else -> ClassifiedDownloadError(
                category = DownloadErrorCategory.UNKNOWN_ERROR,
                userFriendlyMessage = "HTTP $statusCode: ${statusMessage.ifBlank { "Unable to fetch media stream" }}",
                recoveryActionHint = "Check the URL and try again.",
                shouldReResolveSourceUrl = true,
                isRetryable = true
            )
        }
    }

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
            lower.contains("http 401") || lower.contains("unauthorized") -> {
                ClassifiedDownloadError(
                    category = DownloadErrorCategory.UNAUTHORIZED_401,
                    userFriendlyMessage = "HTTP 401: Authentication or valid authorization token required by $providerName.",
                    recoveryActionHint = "Verify the link is publicly accessible or provide an authorized signed link.",
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
            lower.contains("http 500") || lower.contains("http 502") || lower.contains("http 503") ||
                lower.contains("http 504") || lower.contains("server error") -> {
                ClassifiedDownloadError(
                    category = DownloadErrorCategory.SERVER_ERROR_5XX,
                    userFriendlyMessage = "Server Error (5xx): The upstream media server at $providerName is temporarily unavailable.",
                    recoveryActionHint = "Tap Retry Job in a moment to reconnect or use an alternate mirror.",
                    shouldReResolveSourceUrl = true,
                    isRetryable = true
                )
            }
            throwable is java.net.SocketTimeoutException ||
                throwable is java.net.UnknownHostException ||
                throwable is java.net.ConnectException ||
                lower.contains("timeout") ||
                lower.contains("timed out") ||
                lower.contains("unable to resolve host") ||
                lower.contains("connection") -> {
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
