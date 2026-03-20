package com.anurag.eduai.service.sync

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anurag.eduai.debug.DebugLogger
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * Weekly background worker responsible for syncing new Firebase data
 * into the local Room database.
 * Runs once a week using WorkManager periodic request.
 */
@HiltWorker
class WeeklySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: FirebaseSyncManager,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = syncManager.syncAllContent()

            if (result.success) {
                DebugLogger.debugLog("WeeklySync", "Successfully synced with firebase")
            }

            val now = Timestamp.now()

            firestore.collection("worker_test")
                .add(
                    mapOf(
                        "time" to now,
                        "device" to Build.MODEL
                    )
                )
                .await()

            DebugLogger.debugLog("WorkerTest", "Worker executed at $now")
            Result.success()

        } catch (e: Exception) {
            DebugLogger.debugLog("WeeklySyncWorker", "Error: \n ${e.message}")
            Result.retry()
        }
    }
}
