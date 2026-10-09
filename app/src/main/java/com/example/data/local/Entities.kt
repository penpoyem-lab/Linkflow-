package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val jobId: String,
    val mediaId: String,
    val sourceUrl: String,
    val targetDownloadUrl: String,
    val title: String,
    val providerName: String,
    val format: String, // "MP4" or "MP3"
    val qualityLabel: String,
    val resolutionOrBitrate: String,
    val codec: String,
    val durationFormatted: String,
    val thumbnailUrl: String?,
    val state: String, // DownloadJobState.name
    val progressPercent: Int, // 0..100
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Long, // -1 if unknown
    val localFilePath: String?,
    val fileName: String,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?
)

@Entity(tableName = "recent_analyses")
data class RecentAnalysisEntity(
    @PrimaryKey val mediaId: String,
    val originalUrl: String,
    val title: String,
    val providerName: String,
    val durationFormatted: String,
    val thumbnailUrl: String?,
    val highestQualityLabel: String,
    val availableFormatsLabel: String,
    val estimatedSizeBytes: Long,
    val isSupportedForDownload: Boolean,
    val statusNote: String,
    val analyzedAt: Long
)
