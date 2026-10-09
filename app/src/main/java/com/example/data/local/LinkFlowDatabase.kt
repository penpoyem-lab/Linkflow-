package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkFlowDao {
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun observeAllDownloads(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE jobId = :jobId LIMIT 1")
    fun observeDownloadById(jobId: String): Flow<DownloadTaskEntity?>

    @Query("SELECT * FROM download_tasks WHERE jobId = :jobId LIMIT 1")
    suspend fun getDownloadById(jobId: String): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE sourceUrl = :url AND qualityLabel = :qualityLabel AND state IN ('QUEUED', 'ANALYZING', 'PREPARING', 'DOWNLOADING', 'PROCESSING') LIMIT 1")
    suspend fun findActiveDuplicate(url: String, qualityLabel: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET fileName = :newFileName, updatedAt = :updatedAt WHERE jobId = :jobId")
    suspend fun renameDownloadFile(jobId: String, newFileName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM download_tasks WHERE jobId = :jobId")
    suspend fun deleteDownloadById(jobId: String)

    @Query("DELETE FROM download_tasks WHERE state = 'COMPLETED'")
    suspend fun clearCompletedDownloads()

    @Query("DELETE FROM download_tasks WHERE state IN ('FAILED', 'CANCELLED')")
    suspend fun clearFailedOrCancelledDownloads()

    @Query("DELETE FROM download_tasks")
    suspend fun clearAllDownloads()

    @Query("SELECT * FROM recent_analyses ORDER BY analyzedAt DESC LIMIT 20")
    fun observeRecentAnalyses(): Flow<List<RecentAnalysisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentAnalysis(item: RecentAnalysisEntity)

    @Query("DELETE FROM recent_analyses")
    suspend fun clearRecentAnalyses()
}

@Database(
    entities = [DownloadTaskEntity::class, RecentAnalysisEntity::class],
    version = 3,
    exportSchema = false
)
abstract class LinkFlowDatabase : RoomDatabase() {
    abstract fun linkFlowDao(): LinkFlowDao

    companion object {
        @Volatile
        private var INSTANCE: LinkFlowDatabase? = null

        fun getInstance(context: Context): LinkFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LinkFlowDatabase::class.java,
                    "linkflow_media.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
