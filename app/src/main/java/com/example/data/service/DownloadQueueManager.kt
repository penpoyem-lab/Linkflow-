package com.example.data.service

import android.content.Context
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.LinkFlowDao
import com.example.data.model.DownloadJobState
import com.example.data.model.MediaAnalysisResult
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
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

class DownloadQueueManager(
    private val context: Context,
    private val dao: LinkFlowDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun getDownloadsDirectory(): File {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
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
        val fileName = "${sanitizedTitle}.${option.format.extension}"

        val now = System.currentTimeMillis()
        val initialEntity = DownloadTaskEntity(
            jobId = jobId,
            mediaId = analysis.mediaId,
            sourceUrl = analysis.normalizedUrl,
            targetDownloadUrl = option.downloadUrl,
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

                task = task.copy(
                    state = DownloadJobState.DOWNLOADING.name,
                    localFilePath = destinationFile.absolutePath,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
                dao.upsertDownload(task)

                executeRealHttpStreamDownload(task, destinationFile)

                if (!isActive || pausedFlags[jobId] == true) return@launch

                val latestBeforeProcess = dao.getDownloadById(jobId) ?: return@launch
                if (!destinationFile.exists() || destinationFile.length() == 0L) {
                    throw IOException("Server returned an empty media stream.")
                }

                val finalSize = destinationFile.length()
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

    private suspend fun executeRealHttpStreamDownload(
        task: DownloadTaskEntity,
        destinationFile: File
    ) {
        val requestBuilder = Request.Builder()
            .url(task.targetDownloadUrl)
            .header("User-Agent", "LinkFlow-Android/2.4")

        val existingBytes = if (destinationFile.exists()) destinationFile.length() else 0L
        if (existingBytes > 0 && task.downloadedBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw IOException("HTTP ${response.code}: ${response.message.ifBlank { "Unable to fetch media stream" }}")
            }
            val body = response.body ?: throw IOException("Empty response body from server")
            val contentLength = body.contentLength()
            val totalExpected = if (contentLength > 0) {
                if (response.code == 206) contentLength + existingBytes else contentLength
            } else {
                task.totalBytes
            }

            val appendMode = response.code == 206 && existingBytes > 0
            val input = body.byteStream()
            val output = FileOutputStream(destinationFile, appendMode)
            val buffer = ByteArray(16 * 1024)
            var transferred = if (appendMode) existingBytes else 0L
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
                    if (elapsedMs >= 150) {
                        val deltaBytes = transferred - bytesAtLastReport
                        val speed = (deltaBytes * 1000L) / max(elapsedMs, 1L)
                        val pct = if (totalExpected > 0) {
                            ((transferred.toDouble() / totalExpected.toDouble()) * 99.0)
                                .roundToInt()
                                .coerceIn(1, 99)
                        } else {
                            0
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
            }
        }
    }
}
