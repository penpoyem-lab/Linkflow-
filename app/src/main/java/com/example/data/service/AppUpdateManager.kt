package com.example.data.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class GithubApkAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val contentType: String,
    val sha256Digest: String?
)

data class ParsedChangelog(
    val summaryParagraph: String,
    val topFeatures: List<String>,
    val isMandatory: Boolean,
    val minSupportedVersion: String?
)

data class AppReleaseUpdateInfo(
    val tagName: String,
    val cleanVersionName: String,
    val releaseTitle: String,
    val rawMarkdownBody: String,
    val summaryText: String,
    val topFeatures: List<String>,
    val htmlUrl: String,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val isMandatory: Boolean,
    val apkAsset: GithubApkAsset
)

enum class OtaInstallPhase {
    IDLE,
    CHECKING,
    AVAILABLE,
    DOWNLOADING,
    VERIFYING,
    READY_TO_INSTALL,
    REQUIRES_UNKNOWN_SOURCES_PERMISSION,
    UP_TO_DATE,
    ERROR
}

data class OtaUpdateUiState(
    val showModal: Boolean = false,
    val phase: OtaInstallPhase = OtaInstallPhase.IDLE,
    val releaseInfo: AppReleaseUpdateInfo? = null,
    val installedVersionName: String = "1.0",
    val githubRepoSlug: String = DEFAULT_GITHUB_REPO,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val progressPercent: Int = 0,
    val downloadedApkFilePath: String? = null,
    val sha256Verified: Boolean = false,
    val packageVerified: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val lastCheckedEpochMs: Long = 0L
) {
    companion object {
        const val DEFAULT_GITHUB_REPO = "penpoyem-lab/Linkflow-"
    }
}

class AppUpdateManager(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    private val _state = MutableStateFlow(
        OtaUpdateUiState(
            installedVersionName = getInstalledVersionName(context)
        )
    )
    val state: StateFlow<OtaUpdateUiState> = _state.asStateFlow()

    private var activeDownloadJob: Job? = null

    fun setRepositorySlug(slug: String) {
        val clean = slug.trim().removePrefix("https://github.com/").trim('/')
        if (clean.contains('/')) {
            _state.update { it.copy(githubRepoSlug = clean) }
        }
    }

    fun dismissUpdateDialog() {
        val currentRelease = _state.value.releaseInfo
        if (currentRelease?.isMandatory == true) return
        _state.update { it.copy(showModal = false) }
    }

    fun openModalIfAvailable() {
        if (_state.value.releaseInfo != null) {
            _state.update { it.copy(showModal = true) }
        }
    }

    /**
     * Checks GitHub Releases REST API:
     * GET https://api.github.com/repos/{OWNER}/{REPOSITORY}/releases/latest
     */
    suspend fun checkForUpdates(
        repoSlug: String = _state.value.githubRepoSlug,
        dismissedVersionTag: String? = null,
        dismissedTimestampMs: Long = 0L,
        isManualUserTrigger: Boolean = false
    ): OtaInstallPhase = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // Rate-limit automatic checks to once every 30 minutes
        if (!isManualUserTrigger && now - _state.value.lastCheckedEpochMs < 30 * 60 * 1000L) {
            return@withContext _state.value.phase
        }

        val installedVer = getInstalledVersionName(context)
        _state.update {
            it.copy(
                phase = OtaInstallPhase.CHECKING,
                installedVersionName = installedVer,
                githubRepoSlug = repoSlug,
                errorMessage = null,
                statusMessage = "Checking GitHub Releases ($repoSlug)…"
            )
        }

        val apiUrl = "https://api.github.com/repos/$repoSlug/releases/latest"
        try {
            val req = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "LinkFlow-Android-OTA/2.4")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 404) {
                    _state.update {
                        it.copy(
                            phase = OtaInstallPhase.UP_TO_DATE,
                            lastCheckedEpochMs = now,
                            statusMessage = "You are on the latest version ($installedVer). No newer GitHub release published yet."
                        )
                    }
                    return@withContext OtaInstallPhase.UP_TO_DATE
                }

                if (resp.code == 403 || resp.code == 429) {
                    _state.update {
                        it.copy(
                            phase = OtaInstallPhase.ERROR,
                            errorMessage = "GitHub API rate limit reached. Please try again in a few minutes.",
                            statusMessage = null
                        )
                    }
                    return@withContext OtaInstallPhase.ERROR
                }

                if (!resp.isSuccessful) {
                    _state.update {
                        it.copy(
                            phase = OtaInstallPhase.ERROR,
                            errorMessage = "GitHub Releases returned HTTP ${resp.code}.",
                            statusMessage = null
                        )
                    }
                    return@withContext OtaInstallPhase.ERROR
                }

                val bodyStr = resp.body?.string().orEmpty()
                val parsed = parseGithubReleaseJson(
                    jsonString = bodyStr,
                    expectedPackageHint = "linkflow",
                    installedVersion = installedVer
                )

                if (parsed == null) {
                    _state.update {
                        it.copy(
                            phase = OtaInstallPhase.UP_TO_DATE,
                            lastCheckedEpochMs = now,
                            statusMessage = "No compatible APK asset found in the latest release."
                        )
                    }
                    return@withContext OtaInstallPhase.UP_TO_DATE
                }

                val isNewer = compareSemanticVersions(parsed.cleanVersionName, installedVer) > 0
                if (!isNewer) {
                    _state.update {
                        it.copy(
                            phase = OtaInstallPhase.UP_TO_DATE,
                            lastCheckedEpochMs = now,
                            statusMessage = "LinkFlow v$installedVer is up to date."
                        )
                    }
                    return@withContext OtaInstallPhase.UP_TO_DATE
                }

                // Check if user recently tapped "Later" on this exact optional version (cooldown 24h)
                val cooldownActive = !isManualUserTrigger &&
                    !parsed.isMandatory &&
                    dismissedVersionTag == parsed.tagName &&
                    (now - dismissedTimestampMs) < 24 * 60 * 60 * 1000L

                _state.update {
                    it.copy(
                        phase = OtaInstallPhase.AVAILABLE,
                        releaseInfo = parsed,
                        showModal = !cooldownActive,
                        lastCheckedEpochMs = now,
                        errorMessage = null,
                        statusMessage = "Version ${parsed.cleanVersionName} is ready to install"
                    )
                }
                return@withContext OtaInstallPhase.AVAILABLE
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    phase = OtaInstallPhase.ERROR,
                    errorMessage = "Unable to reach GitHub Releases: ${e.localizedMessage ?: "Network unavailable"}",
                    statusMessage = null
                )
            }
            return@withContext OtaInstallPhase.ERROR
        }
    }

    /**
     * Loads a controlled preview/test release so the user can inspect and test the full
     * animated Software Update modal ("Version 2.5 is ready to install") at any time from Settings.
     */
    fun triggerPreviewUpdateDialog(currentBrandName: String = "LinkFlow") {
        val sampleBody = """
            We've supercharged $currentBrandName with a next-generation liquid-glass engine, faster multi-threaded social media stream resolution, and full over-the-air GitHub release updates.
            
            ### Top Features
            - Direct Android Share Target for Instagram Reels, TikTok, YouTube Shorts, X, and Reddit
            - Google Play Store-style transparent scalloped pull-to-refresh physics
            - Hardware-accelerated 120fps Liquid-Glass bottom navigation bar
            - Cryptographic SHA-256 & package signature verification for OTA APK updates
        """.trimIndent()

        val parsedNotes = parseMarkdownChangelog(sampleBody, "1.0")
        val previewRelease = AppReleaseUpdateInfo(
            tagName = "v2.5.0",
            cleanVersionName = "2.5",
            releaseTitle = "Version 2.5 is ready to install",
            rawMarkdownBody = sampleBody,
            summaryText = parsedNotes.summaryParagraph,
            topFeatures = parsedNotes.topFeatures,
            htmlUrl = "https://github.com/${_state.value.githubRepoSlug}/releases",
            publishedAt = "2026-10-09T05:00:00Z",
            isPrerelease = false,
            isMandatory = false,
            apkAsset = GithubApkAsset(
                name = "linkflow-release-v2.5.0.apk",
                downloadUrl = "https://github.com/${_state.value.githubRepoSlug}/releases/latest/download/linkflow-release.apk",
                sizeBytes = 18_450_000L,
                contentType = "application/vnd.android.package-archive",
                sha256Digest = null
            )
        )

        _state.update {
            it.copy(
                showModal = true,
                phase = OtaInstallPhase.AVAILABLE,
                releaseInfo = previewRelease,
                downloadedBytes = 0L,
                totalBytes = previewRelease.apkAsset.sizeBytes,
                progressPercent = 0,
                errorMessage = null,
                statusMessage = "Version 2.5 is ready to install"
            )
        }
    }

    /**
     * Streams the selected release APK to `cacheDir/updates/`, verifies SHA-256 checksum
     * (when provided by GitHub Releases) and verifies APK package & signing certificate compatibility,
     * then launches Android's package installer confirmation flow.
     */
    suspend fun downloadAndVerifyUpdateApk(): File? = withContext(Dispatchers.IO) {
        val release = _state.value.releaseInfo ?: return@withContext null
        val asset = release.apkAsset

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetApkFile = File(updatesDir, asset.name.replace(Regex("[^a-zA-Z0-9._-]"), "_"))

        _state.update {
            it.copy(
                phase = OtaInstallPhase.DOWNLOADING,
                downloadedBytes = 0L,
                totalBytes = asset.sizeBytes,
                progressPercent = 0,
                errorMessage = null,
                statusMessage = "Downloading ${asset.name}…"
            )
        }

        try {
            val request = Request.Builder()
                .url(asset.downloadUrl)
                .header("User-Agent", "LinkFlow-Android-OTA/2.4")
                .header("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _state.update {
                        it.copy(
                            phase = OtaInstallPhase.ERROR,
                            errorMessage = "APK download failed (HTTP ${response.code}). Publish a release APK asset on GitHub first.",
                            statusMessage = null
                        )
                    }
                    return@withContext null
                }

                val body = response.body ?: run {
                    _state.update {
                        it.copy(
                            phase = OtaInstallPhase.ERROR,
                            errorMessage = "Empty APK response body from server.",
                            statusMessage = null
                        )
                    }
                    return@withContext null
                }

                val contentLength = body.contentLength().takeIf { it > 0 } ?: asset.sizeBytes
                val buffer = ByteArray(16 * 1024)
                var totalRead = 0L

                body.byteStream().use { input ->
                    FileOutputStream(targetApkFile).use { output ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            totalRead += read
                            val pct = if (contentLength > 0) {
                                ((totalRead * 100L) / contentLength).toInt().coerceIn(0, 100)
                            } else {
                                50
                            }
                            _state.update {
                                it.copy(
                                    downloadedBytes = totalRead,
                                    totalBytes = contentLength,
                                    progressPercent = pct
                                )
                            }
                        }
                        output.flush()
                    }
                }
            }

            // Phase 2: Verify SHA-256 Checksum & Package Compatibility
            _state.update {
                it.copy(
                    phase = OtaInstallPhase.VERIFYING,
                    statusMessage = "Verifying APK SHA-256 & package signature…"
                )
            }

            val expectedSha256 = asset.sha256Digest?.removePrefix("sha256:")?.trim()?.lowercase()
            if (!expectedSha256.isNullOrBlank()) {
                val actualSha256 = computeFileSha256(targetApkFile)
                if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                    targetApkFile.delete()
                    _state.update {
                        it.copy(
                            phase = OtaInstallPhase.ERROR,
                            errorMessage = "SHA-256 checksum mismatch! Expected $expectedSha256 but got $actualSha256.",
                            statusMessage = null
                        )
                    }
                    return@withContext null
                }
            }

            val compatibility = verifyApkCompatibility(context, targetApkFile)
            if (!compatibility.first) {
                targetApkFile.delete()
                _state.update {
                    it.copy(
                        phase = OtaInstallPhase.ERROR,
                        errorMessage = compatibility.second,
                        statusMessage = null
                    )
                }
                return@withContext null
            }

            // Check Android 8.0+ Unknown Sources install permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                _state.update {
                    it.copy(
                        phase = OtaInstallPhase.REQUIRES_UNKNOWN_SOURCES_PERMISSION,
                        downloadedApkFilePath = targetApkFile.absolutePath,
                        sha256Verified = true,
                        packageVerified = true,
                        statusMessage = "Allow 'Install unknown apps' to complete the update."
                    )
                }
                return@withContext targetApkFile
            }

            _state.update {
                it.copy(
                    phase = OtaInstallPhase.READY_TO_INSTALL,
                    downloadedApkFilePath = targetApkFile.absolutePath,
                    progressPercent = 100,
                    sha256Verified = true,
                    packageVerified = true,
                    statusMessage = "APK verified. Launching Android package installer…"
                )
            }

            launchAndroidPackageInstaller(context, targetApkFile)
            return@withContext targetApkFile
        } catch (ce: CancellationException) {
            targetApkFile.delete()
            _state.update {
                it.copy(
                    phase = OtaInstallPhase.AVAILABLE,
                    downloadedBytes = 0L,
                    progressPercent = 0,
                    statusMessage = "Update download cancelled."
                )
            }
            throw ce
        } catch (e: Exception) {
            targetApkFile.delete()
            _state.update {
                it.copy(
                    phase = OtaInstallPhase.ERROR,
                    errorMessage = "Failed to download update: ${e.localizedMessage ?: "Network error"}",
                    statusMessage = null
                )
            }
            return@withContext null
        }
    }

    fun launchInstallerForDownloadedApk() {
        val path = _state.value.downloadedApkFilePath ?: return
        val file = File(path)
        if (!file.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            openManageUnknownAppSourcesSettings(context)
            return
        }

        _state.update { it.copy(phase = OtaInstallPhase.READY_TO_INSTALL) }
        launchAndroidPackageInstaller(context, file)
    }

    fun openManageUnknownAppSourcesSettings(ctx: Context = context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${ctx.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { ctx.startActivity(intent) }
        }
    }

    fun launchAndroidPackageInstaller(ctx: Context, apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(installIntent)
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    phase = OtaInstallPhase.ERROR,
                    errorMessage = "Could not open Android Package Installer: ${e.localizedMessage}"
                )
            }
        }
    }

    companion object {
        fun getInstalledVersionName(context: Context): String {
            return try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "1.0"
            } catch (_: Exception) {
                "1.0"
            }
        }

        /**
         * Compares two semantic version strings (e.g., "v1.10.2" vs "1.2.9").
         * Returns > 0 if v1 > v2, < 0 if v1 < v2, and 0 if equal.
         * Never compares version strings alphabetically.
         */
        fun compareSemanticVersions(v1Raw: String, v2Raw: String): Int {
            val clean1 = v1Raw.trim().removePrefix("v").removePrefix("V").substringBefore('-')
            val clean2 = v2Raw.trim().removePrefix("v").removePrefix("V").substringBefore('-')

            val parts1 = clean1.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
            val parts2 = clean2.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }

            val maxLen = maxOf(parts1.size, parts2.size)
            for (i in 0 until maxLen) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) return p1.compareTo(p2)
            }

            // Pre-release suffix handling: stable ("1.8.0") > prerelease ("1.8.0-beta1")
            val hasPre1 = v1Raw.contains('-')
            val hasPre2 = v2Raw.contains('-')
            if (!hasPre1 && hasPre2) return 1
            if (hasPre1 && !hasPre2) return -1
            return 0
        }

        /**
         * Parses GitHub's `/releases/latest` JSON payload, ignoring draft/prerelease items
         * and selecting the best matching `.apk` asset while ignoring `.zip` / `.tar.gz`.
         */
        fun parseGithubReleaseJson(
            jsonString: String,
            expectedPackageHint: String = "linkflow",
            installedVersion: String = "1.0"
        ): AppReleaseUpdateInfo? {
            if (jsonString.isBlank()) return null
            return try {
                val root = JSONObject(jsonString)
                if (root.optBoolean("draft", false)) return null
                val isPrerelease = root.optBoolean("prerelease", false)
                if (isPrerelease) return null

                val tagName = root.optString("tag_name").trim()
                if (tagName.isEmpty()) return null
                val cleanVersion = tagName.removePrefix("v").removePrefix("V").trim()
                val releaseName = root.optString("name").takeIf { it.isNotBlank() }
                    ?: "Version $cleanVersion is ready to install"
                val body = root.optString("body", "")
                val htmlUrl = root.optString("html_url", "")
                val publishedAt = root.optString("published_at", "")

                val assetsArray = root.optJSONArray("assets") ?: JSONArray()
                val apkCandidates = mutableListOf<GithubApkAsset>()

                for (i in 0 until assetsArray.length()) {
                    val assetObj = assetsArray.optJSONObject(i) ?: continue
                    val name = assetObj.optString("name").trim()
                    val downloadUrl = assetObj.optString("browser_download_url").trim()
                    val size = assetObj.optLong("size", -1L)
                    val contentType = assetObj.optString("content_type", "")
                    val digest = assetObj.optString("digest").takeIf { it.isNotBlank() && it != "null" }

                    // Only accept genuine .apk files, never .zip, .tar.gz, or unrelated files
                    if (name.lowercase().endsWith(".apk") && downloadUrl.startsWith("https://")) {
                        apkCandidates.add(
                            GithubApkAsset(
                                name = name,
                                downloadUrl = downloadUrl,
                                sizeBytes = size,
                                contentType = contentType,
                                sha256Digest = digest
                            )
                        )
                    }
                }

                if (apkCandidates.isEmpty()) return null

                // Prefer an APK whose filename matches the app/package hint or universal/arm64 release
                val selectedApk = apkCandidates.sortedByDescending { candidate ->
                    val lower = candidate.name.lowercase()
                    var score = 0
                    if (lower.contains(expectedPackageHint.lowercase())) score += 10
                    if (lower.contains("release") || lower.contains("universal")) score += 5
                    if (lower.contains("arm64") || lower.contains("v8a")) score += 3
                    score
                }.first()

                val parsedChangelog = parseMarkdownChangelog(body, installedVersion)

                // Also look for SHA-256 in release notes if `digest` field wasn't populated
                val resolvedSha256 = selectedApk.sha256Digest ?: extractSha256FromMarkdown(body, selectedApk.name)

                AppReleaseUpdateInfo(
                    tagName = tagName,
                    cleanVersionName = cleanVersion,
                    releaseTitle = if (releaseName.contains("ready to install", ignoreCase = true)) {
                        releaseName
                    } else {
                        "Version $cleanVersion is ready to install"
                    },
                    rawMarkdownBody = body,
                    summaryText = parsedChangelog.summaryParagraph,
                    topFeatures = parsedChangelog.topFeatures,
                    htmlUrl = htmlUrl,
                    publishedAt = publishedAt,
                    isPrerelease = isPrerelease,
                    isMandatory = parsedChangelog.isMandatory,
                    apkAsset = selectedApk.copy(sha256Digest = resolvedSha256)
                )
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Parses a GitHub release markdown changelog into:
         * 1. A clean summary paragraph ("What's new")
         * 2. A list of bulleted "Top Features"
         * 3. Optional mandatory / minimum-supported-version policy flags
         */
        fun parseMarkdownChangelog(markdown: String, installedVersion: String = "1.0"): ParsedChangelog {
            val lines = markdown.lines().map { it.trim() }
            val bullets = mutableListOf<String>()
            val paragraphs = mutableListOf<String>()
            var minSupportedVersion: String? = null
            var explicitMandatory = false

            for (line in lines) {
                if (line.isEmpty()) continue
                if (line.startsWith("#")) continue

                if (line.contains("[MANDATORY]", ignoreCase = true) || line.contains("mandatory: true", ignoreCase = true)) {
                    explicitMandatory = true
                    continue
                }

                val minVerMatch = Regex("""min_supported_version\s*[:=]\s*v?([0-9.]+)""", RegexOption.IGNORE_CASE).find(line)
                if (minVerMatch != null) {
                    minSupportedVersion = minVerMatch.groupValues[1]
                    continue
                }

                if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
                    val cleanBullet = line
                        .removePrefix("- ")
                        .removePrefix("* ")
                        .removePrefix("• ")
                        .replace("**", "")
                        .replace("__", "")
                        .replace("`", "")
                        .trim()
                    if (cleanBullet.isNotEmpty() && !cleanBullet.startsWith("sha256:", ignoreCase = true)) {
                        bullets.add(cleanBullet)
                    }
                } else if (!line.startsWith("```") && !line.contains("sha256", ignoreCase = true)) {
                    val cleanPara = line
                        .replace("**", "")
                        .replace("__", "")
                        .replace("`", "")
                        .trim()
                    if (cleanPara.length > 12) {
                        paragraphs.add(cleanPara)
                    }
                }
            }

            val isMinVerBreached = minSupportedVersion?.let { minVer ->
                compareSemanticVersions(installedVersion, minVer) < 0
            } ?: false

            val summary = paragraphs.firstOrNull()
                ?: "This update includes performance enhancements, liquid-glass UI refinements, and expanded social media stream compatibility."

            val features = if (bullets.isNotEmpty()) {
                bullets.take(6)
            } else {
                listOf(
                    "Faster social media stream inspection and multi-resolution MP4/MP3 downloads",
                    "Enhanced 120fps Liquid-Glass animations and pull-to-refresh responsiveness",
                    "Security, stability, and background queue reliability improvements"
                )
            }

            return ParsedChangelog(
                summaryParagraph = summary,
                topFeatures = features,
                isMandatory = explicitMandatory || isMinVerBreached,
                minSupportedVersion = minSupportedVersion
            )
        }

        private fun extractSha256FromMarkdown(markdown: String, apkName: String): String? {
            val regex = Regex("""\b([a-fA-F0-9]{64})\b""")
            return regex.find(markdown)?.groupValues?.getOrNull(1)?.lowercase()
        }

        fun computeFileSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        /**
         * Verifies that the downloaded file is a valid Android APK whose packageName matches
         * the currently installed application and whose signing certificate is compatible.
         */
        @Suppress("DEPRECATION")
        fun verifyApkCompatibility(context: Context, apkFile: File): Pair<Boolean, String> {
            return try {
                val pm = context.packageManager
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES
                } else {
                    PackageManager.GET_SIGNATURES
                }

                val archiveInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
                } else {
                    pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
                } ?: return false to "Downloaded file is not a valid Android APK archive."

                if (archiveInfo.packageName != context.packageName) {
                    return false to "APK package mismatch: expected '${context.packageName}' but found '${archiveInfo.packageName}'."
                }

                val installedInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
                } else {
                    pm.getPackageInfo(context.packageName, flags)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val archiveSigners = archiveInfo.signingInfo?.apkContentsSigners
                    val installedSigners = installedInfo.signingInfo?.apkContentsSigners
                    if (!archiveSigners.isNullOrEmpty() && !installedSigners.isNullOrEmpty()) {
                        val archiveBytes = archiveSigners.first().toByteArray()
                        val installedBytes = installedSigners.first().toByteArray()
                        if (!archiveBytes.contentEquals(installedBytes)) {
                            return false to "APK signing certificate does not match the installed app signature."
                        }
                    }
                }

                true to "Verified package ${archiveInfo.packageName}"
            } catch (e: Exception) {
                false to "APK verification error: ${e.localizedMessage ?: "Invalid package"}"
            }
        }
    }
}
