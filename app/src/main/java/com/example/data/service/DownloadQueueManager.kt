package com.example.data.service

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat as AndroidMediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.LinkFlowDao
import com.example.data.model.DownloadJobState
import com.example.data.model.MediaAnalysisResult
import com.example.data.model.MediaFormat
import com.example.data.model.QualityOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

class DownloadQueueManager(
    private val context: Context,
    private val dao: LinkFlowDao,
    private val analyzerEngine: MediaAnalyzerEngine = MediaAnalyzerEngine()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addNetworkInterceptor { chain ->
            val req = chain.request()
            if (MediaAnalyzerEngine.isPrivateOrLoopbackHost(req.url.host)) {
                throw IOException("SSRF Protection: Blocked connection to private host '${req.url.host}'")
            }
            chain.proceed(req)
        }
        .build()

    fun getDownloadsDirectory(): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val baseDir = externalDir ?: File(context.filesDir, "downloads")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    suspend fun enqueueDownload(
        analysis: MediaAnalysisResult,
        option: QualityOption
    ): String {
        val existing = dao.findActiveDuplicate(analysis.normalizedUrl, option.label)
        if (existing != null) {
            return existing.jobId
        }

        val jobId = "job_${UUID.randomUUID().toString().take(8)}"
        val sanitizedTitle = analysis.title
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(40)
            .ifBlank { "media_download" }
        val uniqueSuffix = jobId.takeLast(4)
        val fileName = "${sanitizedTitle}_${uniqueSuffix}.${option.format.extension}"

        val now = System.currentTimeMillis()
        val initialEntity = DownloadTaskEntity(
            jobId = jobId,
            mediaId = analysis.mediaId,
            sourceUrl = analysis.normalizedUrl,
            targetDownloadUrl = option.downloadUrl,
            companionAudioUrl = option.companionAudioUrl,
            title = analysis.title,
            providerName = analysis.providerName,
            format = option.format.name,
            qualityLabel = option.label,
            resolutionOrBitrate = option.resolutionOrBitrate,
            codec = option.codec,
            durationFormatted = analysis.durationFormatted,
            thumbnailUrl = analysis.thumbnailUrl,
            state = DownloadJobState.QUEUED.name,
            progressPercent = 0,
            downloadedBytes = 0L,
            totalBytes = max(option.estimatedSizeBytes, 0L),
            speedBytesPerSec = 0L,
            etaSeconds = -1L,
            localFilePath = null,
            fileName = fileName,
            errorMessage = null,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )

        dao.upsertDownload(initialEntity)
        startJobExecution(jobId)
        return jobId
    }

    fun pauseDownload(jobId: String) {
        pausedFlags[jobId] = true
        activeJobs[jobId]?.cancel()
        activeJobs.remove(jobId)
        scope.launch {
            val current = dao.getDownloadById(jobId) ?: return@launch
            if (current.state != DownloadJobState.COMPLETED.name) {
                dao.upsertDownload(
                    current.copy(
                        state = DownloadJobState.PAUSED.name,
                        speedBytesPerSec = 0L,
                        etaSeconds = -1L,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun resumeDownload(jobId: String) {
        pausedFlags[jobId] = false
        startJobExecution(jobId)
    }

    fun cancelDownload(jobId: String) {
        pausedFlags.remove(jobId)
        activeJobs[jobId]?.cancel()
        activeJobs.remove(jobId)
        scope.launch {
            val current = dao.getDownloadById(jobId) ?: return@launch
            current.localFilePath?.let { path ->
                runCatching { File(path).delete() }
            }
            dao.upsertDownload(
                current.copy(
                    state = DownloadJobState.CANCELLED.name,
                    speedBytesPerSec = 0L,
                    etaSeconds = -1L,
                    errorMessage = "Cancelled by user",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun retryDownload(jobId: String) {
        pausedFlags[jobId] = false
        scope.launch {
            val current = dao.getDownloadById(jobId) ?: return@launch
            current.localFilePath?.let { path ->
                runCatching { File(path).delete() }
            }
            dao.upsertDownload(
                current.copy(
                    state = DownloadJobState.QUEUED.name,
                    progressPercent = 0,
                    downloadedBytes = 0L,
                    speedBytesPerSec = 0L,
                    etaSeconds = -1L,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            startJobExecution(jobId)
        }
    }

    suspend fun deleteDownloadAndFile(jobId: String) {
        activeJobs[jobId]?.cancel()
        activeJobs.remove(jobId)
        val current = dao.getDownloadById(jobId)
        current?.localFilePath?.let { path ->
            runCatching { File(path).delete() }
        }
        dao.deleteDownloadById(jobId)
    }

    private fun startJobExecution(jobId: String) {
        if (activeJobs.containsKey(jobId)) return

        val job = scope.launch {
            try {
                var task = dao.getDownloadById(jobId) ?: return@launch
                val destinationFile = File(getDownloadsDirectory(), task.fileName)

                if (task.downloadedBytes == 0L) {
                    task = task.copy(
                        state = DownloadJobState.PREPARING.name,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.upsertDownload(task)
                }

                // Resolve candidate download URLs so MP4/MP3 streams never fail due to expired CDN tokens or HTML wrappers
                val candidateUrls = buildOrderedCandidateStreamUrls(task)

                task = task.copy(
                    state = DownloadJobState.DOWNLOADING.name,
                    localFilePath = destinationFile.absolutePath,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
                dao.upsertDownload(task)

                var lastError: Exception? = null
                var downloadSucceeded = false

                for ((attemptIdx, candidateUrl) in candidateUrls.withIndex()) {
                    if (!isActive || pausedFlags[jobId] == true) return@launch
                    try {
                        if (attemptIdx > 0) {
                            // Reset partial file when switching to a fresh candidate stream URL
                            if (destinationFile.exists()) {
                                runCatching { destinationFile.delete() }
                            }
                            task = (dao.getDownloadById(jobId) ?: task).copy(
                                targetDownloadUrl = candidateUrl,
                                downloadedBytes = 0L,
                                progressPercent = 0,
                                updatedAt = System.currentTimeMillis()
                            )
                            dao.upsertDownload(task)
                        }

                        executeRealHttpStreamDownload(
                            task = task.copy(targetDownloadUrl = candidateUrl),
                            destinationFile = destinationFile
                        )

                        if (!isActive || pausedFlags[jobId] == true) return@launch

                        if (destinationFile.exists() && destinationFile.length() > 512L && !isHtmlErrorPageFile(destinationFile)) {
                            downloadSucceeded = true
                            break
                        } else {
                            throw IOException("Stream returned invalid or HTML payload instead of binary media")
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        lastError = e
                    }
                }

                if (!downloadSucceeded) {
                    throw lastError ?: IOException("Unable to download MP4/MP3 stream from any mirror")
                }

                if (!isActive || pausedFlags[jobId] == true) return@launch

                // If this high-resolution video option (e.g. 4K / 2K / 1080p60 / Reddit DASH) has a separate companion audio track,
                // download the companion audio stream and hardware-mux Video + Audio into a single playable MP4 container!
                val companionAudio = task.companionAudioUrl
                if (!companionAudio.isNullOrBlank() &&
                    companionAudio.startsWith("http") &&
                    companionAudio != task.targetDownloadUrl &&
                    task.format.equals(MediaFormat.MP4.name, ignoreCase = true)
                ) {
                    val processingTask = (dao.getDownloadById(jobId) ?: task).copy(
                        state = DownloadJobState.PROCESSING.name,
                        progressPercent = 96,
                        speedBytesPerSec = 0L,
                        etaSeconds = 1L,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.upsertDownload(processingTask)

                    val tempAudioFile = File(getDownloadsDirectory(), "${task.jobId}_companion_audio.m4a")
                    val tempMuxedFile = File(getDownloadsDirectory(), "${task.jobId}_muxed_output.mp4")
                    try {
                        downloadRawStreamToFile(
                            targetUrl = companionAudio,
                            sourceUrl = task.sourceUrl,
                            outputFile = tempAudioFile
                        )
                        if (tempAudioFile.exists() && tempAudioFile.length() > 512L && !isHtmlErrorPageFile(tempAudioFile)) {
                            val muxOk = muxVideoAndAudioToMp4(
                                videoFile = destinationFile,
                                audioFile = tempAudioFile,
                                outputFile = tempMuxedFile
                            )
                            if (muxOk && tempMuxedFile.exists() && tempMuxedFile.length() > destinationFile.length()) {
                                destinationFile.delete()
                                tempMuxedFile.renameTo(destinationFile)
                            }
                        }
                    } catch (_: Exception) {
                        // Keep downloaded video stream if companion audio muxing is unsupported for this codec
                    } finally {
                        runCatching { tempAudioFile.delete() }
                        runCatching { if (tempMuxedFile.exists()) tempMuxedFile.delete() }
                    }
                }

                val latestBeforeProcess = dao.getDownloadById(jobId) ?: return@launch
                val finalSize = destinationFile.length()

                // Export completed MP4/MP3 to Android MediaStore (Movies/LinkFlow or Music/LinkFlow & Downloads/LinkFlow)
                // so it appears immediately in the user's Gallery, Files, and Video/Music player apps.
                runCatching {
                    exportCompletedFileToSystemMediaStore(
                        file = destinationFile,
                        fileName = latestBeforeProcess.fileName,
                        format = latestBeforeProcess.format
                    )
                }

                val completedNow = System.currentTimeMillis()
                dao.upsertDownload(
                    latestBeforeProcess.copy(
                        state = DownloadJobState.COMPLETED.name,
                        progressPercent = 100,
                        downloadedBytes = finalSize,
                        totalBytes = finalSize,
                        speedBytesPerSec = 0L,
                        etaSeconds = 0L,
                        localFilePath = destinationFile.absolutePath,
                        errorMessage = null,
                        updatedAt = completedNow,
                        completedAt = completedNow
                    )
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                val failedTask = dao.getDownloadById(jobId)
                if (failedTask != null && pausedFlags[jobId] != true) {
                    dao.upsertDownload(
                        failedTask.copy(
                            state = DownloadJobState.FAILED.name,
                            speedBytesPerSec = 0L,
                            etaSeconds = -1L,
                            errorMessage = e.localizedMessage ?: "Network transfer failed",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } finally {
                activeJobs.remove(jobId)
            }
        }
        activeJobs[jobId] = job
    }

    private suspend fun buildOrderedCandidateStreamUrls(task: DownloadTaskEntity): List<String> {
        val candidates = mutableListOf<String>()
        if (task.targetDownloadUrl.startsWith("http")) {
            candidates.add(task.targetDownloadUrl)
        }

        // Re-analyze sourceUrl in case the social platform's signed CDN URL expired or needs fresh extraction
        if (task.sourceUrl.startsWith("http") && task.sourceUrl != task.targetDownloadUrl) {
            runCatching {
                when (val outcome = analyzerEngine.analyzeUrl(task.sourceUrl)) {
                    is UrlAnalysisOutcome.Success -> {
                        val matchingOptions = if (task.format.equals(MediaFormat.MP3.name, ignoreCase = true)) {
                            outcome.result.audioOptions + outcome.result.videoOptions
                        } else {
                            outcome.result.videoOptions + outcome.result.audioOptions
                        }
                        matchingOptions.forEach { opt ->
                            if (opt.downloadUrl.startsWith("http")) {
                                candidates.add(opt.downloadUrl)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        return candidates.distinct()
    }

    private fun isHtmlErrorPageFile(file: File): Boolean {
        return try {
            val headerBytes = ByteArray(256)
            val readCount = FileInputStream(file).use { it.read(headerBytes) }
            if (readCount <= 0) return true
            val headStr = String(headerBytes, 0, readCount, Charsets.UTF_8).trimStart().lowercase()
            headStr.startsWith("<!doctype html") ||
                headStr.startsWith("<html") ||
                headStr.startsWith("<head") ||
                headStr.startsWith("{\"error\"")
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun executeRealHttpStreamDownload(
        task: DownloadTaskEntity,
        destinationFile: File
    ) {
        val targetUrl = task.targetDownloadUrl
        val refererOrigin = runCatching {
            val uri = URI(task.sourceUrl.ifBlank { targetUrl })
            "${uri.scheme ?: "https"}://${uri.host ?: ""}/"
        }.getOrDefault("https://www.google.com/")

        val existingBytes = if (destinationFile.exists()) destinationFile.length() else 0L
        val useRangeResume = existingBytes > 0L && task.downloadedBytes > 0L

        var response = executeStreamRequest(
            targetUrl = targetUrl,
            referer = refererOrigin,
            rangeHeader = if (useRangeResume) "bytes=$existingBytes-" else null
        )

        // Many CDNs reject `Range` requests with 416 or 403; retry cleanly from byte 0
        if ((response.code == 416 || response.code == 403 || response.code == 400) && useRangeResume) {
            response.close()
            if (destinationFile.exists()) {
                runCatching { destinationFile.delete() }
            }
            response = executeStreamRequest(
                targetUrl = targetUrl,
                referer = refererOrigin,
                rangeHeader = null
            )
        }

        response.use { resp ->
            if (!resp.isSuccessful && resp.code != 206) {
                throw IOException("HTTP ${resp.code}: ${resp.message.ifBlank { "Unable to fetch media stream" }}")
            }

            val contentType = resp.header("Content-Type")?.lowercase().orEmpty()
            if (contentType.contains("text/html") || contentType.contains("application/xhtml")) {
                throw IOException("URL returned an HTML webpage instead of a binary MP4/MP3 stream")
            }

            val body = resp.body ?: throw IOException("Empty response body from server")

            // Check if this is an HLS M3U8 playlist stream
            if (contentType.contains("mpegurl") || targetUrl.substringBefore('?').lowercase().endsWith(".m3u8")) {
                val playlistText = body.string()
                downloadHlsM3u8StreamToFile(
                    playlistUrl = targetUrl,
                    playlistContent = playlistText,
                    referer = refererOrigin,
                    task = task,
                    destinationFile = destinationFile
                )
                return
            }

            val contentLength = body.contentLength()
            val appendMode = resp.code == 206 && useRangeResume
            val startingBytes = if (appendMode) existingBytes else 0L
            val totalExpected = if (contentLength > 0) {
                if (appendMode) contentLength + existingBytes else contentLength
            } else {
                task.totalBytes
            }

            val input = body.byteStream()
            val output = FileOutputStream(destinationFile, appendMode)
            val buffer = ByteArray(32 * 1024)
            var transferred = startingBytes
            var lastReportTime = System.currentTimeMillis()
            var bytesAtLastReport = transferred

            output.use { out ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    if (pausedFlags[task.jobId] == true) return
                    out.write(buffer, 0, read)
                    transferred += read

                    val now = System.currentTimeMillis()
                    val elapsedMs = now - lastReportTime
                    if (elapsedMs >= 140) {
                        val deltaBytes = transferred - bytesAtLastReport
                        val speed = (deltaBytes * 1000L) / max(elapsedMs, 1L)
                        val pct = if (totalExpected > 0) {
                            ((transferred.toDouble() / totalExpected.toDouble()) * 99.0)
                                .roundToInt()
                                .coerceIn(1, 99)
                        } else {
                            // Smooth indeterminate-to-determinate progress when server omits Content-Length
                            val pseudoMb = transferred.toDouble() / (2.5 * 1024 * 1024)
                            (pseudoMb * 85.0).roundToInt().coerceIn(5, 95)
                        }
                        val remainingBytes = if (totalExpected > transferred) totalExpected - transferred else -1L
                        val eta = if (speed > 0 && remainingBytes >= 0) remainingBytes / speed else -1L

                        dao.upsertDownload(
                            task.copy(
                                state = DownloadJobState.DOWNLOADING.name,
                                progressPercent = pct,
                                downloadedBytes = transferred,
                                totalBytes = if (totalExpected > 0) totalExpected else transferred,
                                speedBytesPerSec = speed,
                                etaSeconds = eta,
                                localFilePath = destinationFile.absolutePath,
                                updatedAt = now
                            )
                        )
                        lastReportTime = now
                        bytesAtLastReport = transferred
                    }
                }
                out.flush()
            }
        }
    }

    private fun executeStreamRequest(
        targetUrl: String,
        referer: String,
        rangeHeader: String?
    ): okhttp3.Response {
        val isGoogleVideo = targetUrl.contains("googlevideo.com", ignoreCase = true)
        val userAgent = if (isGoogleVideo) {
            "com.google.android.apps.youtube.vr.oculus/1.56.21 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
        } else {
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        val builder = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")

        if (!isGoogleVideo) {
            builder.header("Referer", referer)
        }

        if (rangeHeader != null) {
            builder.header("Range", rangeHeader)
        }
        return okHttpClient.newCall(builder.build()).execute()
    }

    private fun downloadRawStreamToFile(
        targetUrl: String,
        sourceUrl: String,
        outputFile: File
    ) {
        val refererOrigin = runCatching {
            val uri = URI(sourceUrl.ifBlank { targetUrl })
            "${uri.scheme ?: "https"}://${uri.host ?: ""}/"
        }.getOrDefault("https://www.google.com/")

        executeStreamRequest(targetUrl = targetUrl, referer = refererOrigin, rangeHeader = null).use { resp ->
            if (!resp.isSuccessful) return
            val body = resp.body ?: return
            body.byteStream().use { input ->
                FileOutputStream(outputFile, false).use { out ->
                    input.copyTo(out, bufferSize = 32 * 1024)
                    out.flush()
                }
            }
        }
    }

    /**
     * Uses Android's native hardware [MediaExtractor] and [MediaMuxer] to losslessly combine
     * a high-resolution adaptive MP4 video stream (e.g. 4K / 1080p60) and an M4A/AAC audio stream
     * into a single standard MP4 file without re-encoding.
     */
    private fun muxVideoAndAudioToMp4(
        videoFile: File,
        audioFile: File,
        outputFile: File
    ): Boolean {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        return try {
            videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }

            var videoTrackIndex = -1
            var videoFormat: AndroidMediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val fmt = videoExtractor.getTrackFormat(i)
                val mime = fmt.getString(AndroidMediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoTrackIndex = i
                    videoFormat = fmt
                    break
                }
            }

            var audioTrackIndex = -1
            var audioFormat: AndroidMediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val fmt = audioExtractor.getTrackFormat(i)
                val mime = fmt.getString(AndroidMediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }

            if (videoTrackIndex < 0 || videoFormat == null || audioTrackIndex < 0 || audioFormat == null) {
                return false
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxVideoTrack = muxer.addTrack(videoFormat)
            val muxAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val bufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // Copy all video samples
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                val sampleFlags = videoExtractor.sampleFlags
                bufferInfo.flags = if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }
                muxer.writeSampleData(muxVideoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Copy all audio samples
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                val sampleFlags = audioExtractor.sampleFlags
                bufferInfo.flags = if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }
                muxer.writeSampleData(muxAudioTrack, buffer, bufferInfo)
                audioExtractor.advance()
            }

            muxer.stop()
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { videoExtractor?.release() }
            runCatching { audioExtractor?.release() }
            runCatching { muxer?.release() }
        }
    }

    private suspend fun downloadHlsM3u8StreamToFile(
        playlistUrl: String,
        playlistContent: String,
        referer: String,
        task: DownloadTaskEntity,
        destinationFile: File
    ) {
        val baseUri = URI(playlistUrl)
        val lines = playlistContent.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Check if master playlist pointing to a variant .m3u8
        val variantPlaylists = lines.filter { !it.startsWith("#") && it.contains(".m3u8") }
        if (variantPlaylists.isNotEmpty()) {
            val resolvedVariantUrl = baseUri.resolve(variantPlaylists.last()).toString()
            executeStreamRequest(resolvedVariantUrl, referer, null).use { variantResp ->
                if (!variantResp.isSuccessful) throw IOException("Failed to load HLS variant playlist")
                val variantText = variantResp.body?.string().orEmpty()
                downloadHlsM3u8StreamToFile(
                    playlistUrl = resolvedVariantUrl,
                    playlistContent = variantText,
                    referer = referer,
                    task = task,
                    destinationFile = destinationFile
                )
            }
            return
        }

        val segmentUrls = lines
            .filter { !it.startsWith("#") }
            .map { seg -> if (seg.startsWith("http")) seg else baseUri.resolve(seg).toString() }

        if (segmentUrls.isEmpty()) {
            throw IOException("HLS playlist contained no playable media segments")
        }

        var transferred = 0L
        val startTime = System.currentTimeMillis()
        FileOutputStream(destinationFile, false).use { out ->
            val buffer = ByteArray(32 * 1024)
            for ((index, segUrl) in segmentUrls.withIndex()) {
                if (pausedFlags[task.jobId] == true) return
                executeStreamRequest(segUrl, referer, null).use { segResp ->
                    if (!segResp.isSuccessful) throw IOException("HLS segment HTTP ${segResp.code}")
                    val segIn = segResp.body?.byteStream() ?: return@use
                    var read: Int
                    while (segIn.read(buffer).also { read = it } != -1) {
                        if (pausedFlags[task.jobId] == true) return
                        out.write(buffer, 0, read)
                        transferred += read
                    }
                }
                val pct = (((index + 1).toDouble() / segmentUrls.size.toDouble()) * 99.0)
                    .roundToInt()
                    .coerceIn(1, 99)
                val elapsedSec = max((System.currentTimeMillis() - startTime) / 1000L, 1L)
                val speed = transferred / elapsedSec
                dao.upsertDownload(
                    task.copy(
                        state = DownloadJobState.DOWNLOADING.name,
                        progressPercent = pct,
                        downloadedBytes = transferred,
                        totalBytes = transferred,
                        speedBytesPerSec = speed,
                        etaSeconds = -1L,
                        localFilePath = destinationFile.absolutePath,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            out.flush()
        }
    }

    /**
     * Exports the downloaded MP4 or MP3 into Android's public MediaStore / Downloads collection
     * on API 29+ (or scans external media on older APIs) so the user can view the video/audio
     * directly in their system Gallery, Video Player, Music Player, and Files apps.
     */
    private fun exportCompletedFileToSystemMediaStore(
        file: File,
        fileName: String,
        format: String
    ) {
        if (!file.exists() || file.length() <= 0L) return
        val isAudio = format.equals("MP3", ignoreCase = true)
        val mimeType = if (isAudio) "audio/mpeg" else "video/mp4"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = if (isAudio) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val relativePath = if (isAudio) {
                "${Environment.DIRECTORY_MUSIC}/LinkFlow"
            } else {
                "${Environment.DIRECTORY_MOVIES}/LinkFlow"
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val insertedUri = resolver.insert(collection, values)
            if (insertedUri != null) {
                try {
                    resolver.openOutputStream(insertedUri)?.use { outStream ->
                        FileInputStream(file).use { inStream ->
                            inStream.copyTo(outStream, bufferSize = 32 * 1024)
                        }
                    }
                    val completeValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    resolver.update(insertedUri, completeValues, null, null)
                } catch (_: Exception) {
                    runCatching { resolver.delete(insertedUri, null, null) }
                }
            }

            // Also index into public Downloads/LinkFlow on API 29+
            val downloadsValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/LinkFlow")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val dlUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, downloadsValues)
            if (dlUri != null) {
                try {
                    resolver.openOutputStream(dlUri)?.use { outStream ->
                        FileInputStream(file).use { inStream ->
                            inStream.copyTo(outStream, bufferSize = 32 * 1024)
                        }
                    }
                    val completeDl = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(dlUri, completeDl, null, null)
                } catch (_: Exception) {
                    runCatching { resolver.delete(dlUri, null, null) }
                }
            }
        } else {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
                null
            )
        }
    }
}
