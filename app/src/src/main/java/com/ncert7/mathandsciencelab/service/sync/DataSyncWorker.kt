package com.ncert7.mathandsciencelab.service.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ncert7.mathandsciencelab.data.local.EduAiDatabase
import com.ncert7.mathandsciencelab.data.local.SharedPreferenceUtils
import com.ncert7.mathandsciencelab.debug.DebugLogger
import kotlin.math.pow

/**
 * Background worker responsible for syncing Progress, Analytics, and Session data
 * to Firestore when the app is offline or periodically.
 * Uses WorkManager with exponential backoff retry strategy.
 */
class DataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DataSyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        return try {
            val retryAttempt = inputData.getInt("retry_attempt", 0)

            DebugLogger.debugLog(TAG, "Starting data sync (Attempt ${retryAttempt + 1}/$MAX_RETRY_ATTEMPTS)")

            val database = EduAiDatabase.getInstance(applicationContext)
            val sharedPref = SharedPreferenceUtils(applicationContext)

            // Get current student ID
            val studentId = sharedPref.getUserId()
            if (studentId.isNullOrBlank()) {
                DebugLogger.debugLog(TAG, "No student ID found. Skipping sync.")
                return Result.success()
            }

            // Create sync manager
            val syncManager = ProgressAnalyticsSessionSyncManager(
                progressDao = database.progressDao(),
                analyticsDao = database.appAnalyticsDao(),
                sessionDao = database.sessionDao(),
                studentId = studentId
            )

            // Perform sync
            val result = syncManager.syncAllUnsyncedData()
            val simulationResult = SimulationSyncManager(
                interactionDao = database.simulationInteractionDao(),
                sharedPreferenceUtils = sharedPref
            ).syncTodayIfNeeded()

            if (result.success && simulationResult.success) {
                DebugLogger.debugLog(TAG, "Successfully synced data:\n${result.message}\n${simulationResult.message}")
                return Result.success()
            } else {
                DebugLogger.errorLog(TAG, "Sync failed: ${result.message}\n${simulationResult.message}")
                return handleRetry(retryAttempt)
            }
        } catch (e: Exception) {
            val retryAttempt = inputData.getInt("retry_attempt", 0)
            DebugLogger.errorLog(
                TAG,
                "Exception during sync (Attempt ${retryAttempt + 1}/$MAX_RETRY_ATTEMPTS): ${e.message}\n${e.stackTrace.joinToString("\n")}"
            )
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
