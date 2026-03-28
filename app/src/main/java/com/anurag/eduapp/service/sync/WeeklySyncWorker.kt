package com.anurag.eduapp.service.sync

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anurag.eduapp.data.local.EduAiDatabase
import com.anurag.eduapp.debug.DebugLogger
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.pow


/**
 * Weekly background worker responsible for syncing new Firebase data
 * into the local Room database.
 * Runs once a week using WorkManager periodic request.
 *
 * Implements retry with exponential backoff on failures.
 */
class WeeklySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "WeeklySyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        return try {
            val retryAttempt = inputData.getInt("retry_attempt", 0)

            DebugLogger.debugLog(TAG, "Starting sync work (Attempt ${retryAttempt + 1}/$MAX_RETRY_ATTEMPTS)")

            val database = EduAiDatabase.getInstance(applicationContext)
            val syncManager = FirebaseSyncManager(
                subjectDao = database.subjectDao(),
                chapterDao = database.chapterDao(),
                conceptDao = database.conceptDao()
            )

            val result = syncManager.syncAllContent()
            if (result.success) {
                DebugLogger.debugLog(TAG, "Successfully synced with firebase: ${result.message}")
            } else {
                DebugLogger.errorLog(TAG, "Sync failed: ${result.message}")
                // Retry if sync was unsuccessful
                return handleRetry(retryAttempt)
            }

            val now = Timestamp.now()

            FirebaseFirestore.getInstance()
                .collection("worker_test")
                .add(
                    mapOf(
                        "time" to now,
                        "device" to Build.MODEL
                    )
                )
                .await()

            DebugLogger.debugLog(TAG, "Worker executed at $now")
            return Result.success()
        } catch (e: Exception) {
            val retryAttempt = inputData.getInt("retry_attempt", 0)
            DebugLogger.errorLog(TAG, "Exception during sync (Attempt ${retryAttempt + 1}/$MAX_RETRY_ATTEMPTS): ${e.message}\n${e.stackTrace.joinToString("\n")}")
            return handleRetry(retryAttempt)
        }
    }

    /**
     * Handles retry logic with exponential backoff.
     * @param currentAttempt The current retry attempt number (0-indexed)
     * @return Result.retry() if attempts remain, Result.failure() otherwise
     */
    private fun handleRetry(currentAttempt: Int): Result {
        return if (currentAttempt < MAX_RETRY_ATTEMPTS) {
            val nextAttempt = currentAttempt + 1
            val delayMillis = calculateBackoffDelay(nextAttempt)

            DebugLogger.debugLog(TAG, "Scheduling retry attempt $nextAttempt after ${delayMillis}ms")

            // Use exponential backoff: 2^n * base_delay
            Result.retry()
        } else {
            DebugLogger.errorLog(TAG, "Max retry attempts ($MAX_RETRY_ATTEMPTS) reached. Giving up.")
            Result.failure()
        }
    }

    /**
     * Calculates exponential backoff delay.
     * Attempt 1: 1 minute, Attempt 2: 2 minutes, Attempt 3: 4 minutes
     */
    private fun calculateBackoffDelay(attemptNumber: Int): Long {
        val baseDelayMinutes = 1L
        val delayMultiplier = 2.0.pow((attemptNumber - 1).toDouble()).toLong()
        return baseDelayMinutes * delayMultiplier * 60 * 1000 // Convert to milliseconds
    }
}

