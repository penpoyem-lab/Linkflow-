package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.DownloadTaskEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "—"
        return "${formatBytes(bytesPerSec)}/s"
    }

    fun formatEta(seconds: Long): String {
        if (seconds < 0L) return "Calculating…"
        if (seconds == 0L) return "Done"
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins > 0) "${mins}m ${secs}s left" else "${secs}s left"
    }

    fun formatTimestamp(epochMs: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US)
        return sdf.format(Date(epochMs))
    }

    fun shareDownloadedFile(
        context: Context,
        task: DownloadTaskEntity,
        onError: (String) -> Unit
    ) {
        try {
            val path = task.localFilePath
            val file = if (path != null) File(path) else null
            if (file != null && file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mimeType = if (task.format == "MP3") "audio/mpeg" else "video/mp4"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, task.title)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${task.title} (${task.qualityLabel}) • Saved with LinkFlow"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share ${task.fileName}")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                // Share metadata link fallback if file was moved
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, task.title)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${task.title} (${task.qualityLabel}) — ${task.sourceUrl}"
                    )
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share Media Link")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        } catch (e: Exception) {
            onError("Unable to open share sheet: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    fun openDownloadedFileExternally(
        context: Context,
        task: DownloadTaskEntity,
        onError: (String) -> Unit
    ) {
        try {
            val path = task.localFilePath
            val file = if (path != null) File(path) else null
            if (file != null && file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mimeType = if (task.format == "MP3") "audio/mpeg" else "video/mp4"
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
            } else {
                onError("Local file not found on disk: ${task.fileName}")
            }
        } catch (e: Exception) {
            onError("No external media player found: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
