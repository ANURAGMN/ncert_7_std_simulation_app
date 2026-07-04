package com.ncert7.mathandsciencelab.service.sync

import com.ncert7.mathandsciencelab.data.local.dao.AppAnalyticsDao
import com.ncert7.mathandsciencelab.data.local.dao.ProgressDao
import com.ncert7.mathandsciencelab.data.local.dao.SessionDao
import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.jvm.Throws

/**
 * Manages real-time and offline sync of Progress, Analytics, and Session data to Firestore.
 * Handles both online (real-time) and offline (batch) synchronization.
 */
class ProgressAnalyticsSessionSyncManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val progressDao: ProgressDao,
    private val analyticsDao: AppAnalyticsDao,
    private val sessionDao: SessionDao,
    private val studentId: String
) {
    companion object {
        private const val TAG = "ProgressAnalyticsSyncManager"
        private const val PROGRESS_COLLECTION = "progress"
        private const val ANALYTICS_COLLECTION = "analytics"
        private const val SESSIONS_COLLECTION = "sessions"
        private const val BATCH_SIZE = 100 // Firestore batch write limit
    }

    /**
     * Syncs all unsynced progress, analytics, and sessions to Firestore
     */
    suspend fun syncAllUnsyncedData(): SyncResult {
        return try {
            DebugLogger.debugLog(TAG, "Starting full sync for student: $studentId")

            val progressResult = syncUnsyncedProgress()
            val analyticsResult = syncUnsyncedAnalytics()
            val sessionsResult = syncUnsyncedSessions()

            val allSuccess = progressResult.success && analyticsResult.success && sessionsResult.success
            val message = """
                Progress: ${progressResult.message}
                Analytics: ${analyticsResult.message}
                Sessions: ${sessionsResult.message}
            """.trimIndent()

            SyncResult(success = allSuccess, message = message)
        } catch (e: Exception) {
            val errorMsg = "Full sync failed: ${e.message}"
            DebugLogger.errorLog(TAG, errorMsg)
            SyncResult(success = false, message = errorMsg)
        }
    }

    /**
     * Syncs a single progress update to Firestore in real-time
     */
    @Throws(Exception::class)
    suspend fun syncProgressUpdate(progressId: Long, studentId: String): Boolean {
        return try {
            DebugLogger.debugLog(TAG, "Real-time sync triggered for progress: $progressId")

            // Try fetching from database directly by ID
            val allUnsyncedProgress = progressDao.getUnsyncedProgress()
            val progress = allUnsyncedProgress.find { it.progressId == progressId }

            if (progress != null) {
                val docRef = firestore
                    .collection(PROGRESS_COLLECTION)
                    .document(studentId)
                    .collection("records")
                    .document("${progress.itemType}_${progress.itemId}")

                val data = mapOf(
                    "progressId" to progress.progressId,
                    "studentId" to progress.studentId,
                    "itemType" to progress.itemType,
                    "itemId" to progress.itemId,
                    "status" to progress.status,
                    "progressPercentage" to progress.progressPercentage,
                    "startedAt" to progress.startedAt,
                    "completedAt" to progress.completedAt,
                    "lastAccessedAt" to progress.lastAccessedAt,
                    "updatedAt" to progress.updatedAt,
                    "appName" to progress.appName,
                    "syncedAt" to System.currentTimeMillis()
                )

                docRef.set(data).await()
                progressDao.markProgressAsSynced(listOf(progressId))
                DebugLogger.debugLog(TAG, " Progress synced to Firestore: $progressId, Type: ${progress.itemType}")
                true
            } else {
                DebugLogger.debugLog(TAG, " Progress not found or already synced: $progressId")
                false
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, " Progress real-time sync failed: ${e.message}")
            throw e
        }
    }

    /**
     * Syncs a single analytics update to Firestore in real-time
     */
    @Throws(Exception::class)
    suspend fun syncAnalyticsUpdate(analyticsId: Long): Boolean {
        return try {
            DebugLogger.debugLog(TAG, "Real-time sync triggered for analytics: $analyticsId")

            val allAnalytics = analyticsDao.getUnsyncedAnalytics()
            val analytics = allAnalytics.find { it.analyticsId == analyticsId }

            if (analytics != null) {
                val docRef = firestore
                    .collection(ANALYTICS_COLLECTION)
                    .document(studentId)
                    .collection("events")
                    .document(analytics.analyticsId.toString())

                val data = mapOf(
                    "analyticsId" to analytics.analyticsId,
                    "studentId" to studentId,
                    "sessionId" to analytics.sessionId,
                    "screenName" to analytics.screenName,
                    "eventType" to analytics.eventType,
                    "entryTime" to analytics.entryTime,
                    "exitTime" to analytics.exitTime,
                    "durationMillis" to analytics.durationMillis,
                    "appName" to analytics.appName,
                    "syncedAt" to System.currentTimeMillis()
                )

                docRef.set(data).await()
                analyticsDao.markAnalyticsAsSynced(analytics.analyticsId)
                DebugLogger.debugLog(TAG, " Analytics synced to Firestore: $analyticsId, Screen: ${analytics.screenName}")
                true
            } else {
                DebugLogger.debugLog(TAG, " Analytics not found or already synced: $analyticsId")
                false
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, " Analytics real-time sync failed: ${e.message}")
            throw e
        }
    }

    /**
     * Syncs a single session update to Firestore in real-time
     */
    @Throws(Exception::class)
    suspend fun syncSessionUpdate(sessionId: String): Boolean {
        return try {
            DebugLogger.debugLog(TAG, " Syncing session to Firebase: $sessionId")

            // IMPORTANT: Always read fresh data from database
            val session = sessionDao.getSession(sessionId)

            if (session != null) {
                val docRef = firestore
                    .collection(SESSIONS_COLLECTION)
                    .document(session.studentId)  // Use session's studentId, not sync manager's
                    .collection("records")
                    .document(session.sessionId)

                val data = mapOf(
                    "sessionId" to session.sessionId,
                    "studentId" to session.studentId,
                    "sessionDate" to session.sessionDate,
                    "sessionStartTime" to session.sessionStartTime,
                    "sessionEndTime" to session.sessionEndTime,
                    "durationMillis" to session.durationMillis,
                    "appName" to session.appName,
                    "syncedAt" to System.currentTimeMillis()
                )

                DebugLogger.debugLog(TAG, " Writing to Firebase: endTime=${session.sessionEndTime}, duration=${session.durationMillis}ms")

                // Use update() with merge semantics to properly update fields
                // If document doesn't exist, set it; if it exists, update only the specified fields
                try {
                    docRef.update(data).await()
                } catch (e: Exception) {
                    // If document doesn't exist yet (first sync), use set instead
                    if (e.message?.contains("No document to update") == true) {
                        DebugLogger.debugLog(TAG, " Document doesn't exist yet, creating with set()")
                        docRef.set(data).await()
                    } else {
                        throw e
                    }
                }

                // Only mark as synced AFTER successful Firebase write
                sessionDao.markSessionAsSynced(session.sessionId)

                DebugLogger.debugLog(TAG, " Session synced to Firebase: $sessionId, Duration: ${session.durationMillis}ms, EndTime: ${session.sessionEndTime}")
                true
            } else {
                DebugLogger.debugLog(TAG, " Session not found in database: $sessionId")
                false
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, " Session sync failed: ${e.message}")
            throw e
        }
    }

    // ==================== PRIVATE SYNC METHODS ====================

    /**
     * Syncs all unsynced progress records from local database to Firestore
     */
    private suspend fun syncUnsyncedProgress(): SyncResult {
        return try {
            val unsyncedProgress = progressDao.getUnsyncedProgress()

            if (unsyncedProgress.isEmpty()) {
                return SyncResult(success = true, message = "No unsynced progress records")
            }

            DebugLogger.debugLog(TAG, "Syncing ${unsyncedProgress.size} progress records...")

            // Process in batches
            unsyncedProgress.chunked(BATCH_SIZE).forEach { batch ->
                val batch_write = firestore.batch()

                batch.forEach { progress ->
                    val docRef = firestore
                        .collection(PROGRESS_COLLECTION)
                        .document(studentId)
                        .collection("records")
                        .document("${progress.itemType}_${progress.itemId}")

                    val data = mapOf(
                        "progressId" to progress.progressId,
                        "studentId" to progress.studentId,
                        "itemType" to progress.itemType,
                        "itemId" to progress.itemId,
                        "status" to progress.status,
                        "progressPercentage" to progress.progressPercentage,
                        "startedAt" to progress.startedAt,
                        "completedAt" to progress.completedAt,
                        "lastAccessedAt" to progress.lastAccessedAt,
                        "updatedAt" to progress.updatedAt,
                        "appName" to progress.appName,
                        "syncedAt" to System.currentTimeMillis()
                    )

                    batch_write.set(docRef, data)
                }

                batch_write.commit().await()
            }

            // Mark as synced
            progressDao.markProgressAsSynced(unsyncedProgress.map { it.progressId })

            val message = "Synced ${unsyncedProgress.size} progress records"
            DebugLogger.debugLog(TAG, message)
            SyncResult(success = true, message = message)
        } catch (e: Exception) {
            val errorMsg = "Progress sync failed: ${e.message}"
            DebugLogger.errorLog(TAG, errorMsg)
            SyncResult(success = false, message = errorMsg)
        }
    }

    /**
     * Syncs all unsynced analytics records from local database to Firestore
     */
    private suspend fun syncUnsyncedAnalytics(): SyncResult {
        return try {
            val unsyncedAnalytics = analyticsDao.getUnsyncedAnalytics()

            if (unsyncedAnalytics.isEmpty()) {
                return SyncResult(success = true, message = "No unsynced analytics records")
            }

            DebugLogger.debugLog(TAG, "Syncing ${unsyncedAnalytics.size} analytics records...")

            // Process in batches
            unsyncedAnalytics.chunked(BATCH_SIZE).forEach { batch ->
                val batch_write = firestore.batch()

                batch.forEach { analytics ->
                    val docRef = firestore
                        .collection(ANALYTICS_COLLECTION)
                        .document(studentId)
                        .collection("events")
                        .document(analytics.analyticsId.toString())

                    val data = mapOf(
                        "analyticsId" to analytics.analyticsId,
                        "studentId" to studentId,
                        "sessionId" to analytics.sessionId,
                        "screenName" to analytics.screenName,
                        "eventType" to analytics.eventType,
                        "entryTime" to analytics.entryTime,
                        "exitTime" to analytics.exitTime,
                        "durationMillis" to analytics.durationMillis,
                        "appName" to analytics.appName,
                        "syncedAt" to System.currentTimeMillis()
                    )

                    batch_write.set(docRef, data)
                }

                batch_write.commit().await()
            }

            // Mark as synced
            unsyncedAnalytics.forEach { analytics ->
                analyticsDao.markAnalyticsAsSynced(analytics.analyticsId)
            }

            val message = "Synced ${unsyncedAnalytics.size} analytics records"
            DebugLogger.debugLog(TAG, message)
            SyncResult(success = true, message = message)
        } catch (e: Exception) {
            val errorMsg = "Analytics sync failed: ${e.message}"
            DebugLogger.errorLog(TAG, errorMsg)
            SyncResult(success = false, message = errorMsg)
        }
    }

    /**
     * Syncs all unsynced session records from local database to Firestore
     */
    private suspend fun syncUnsyncedSessions(): SyncResult {
        return try {
            val unsyncedSessions = sessionDao.getUnsyncedSessions()

            if (unsyncedSessions.isEmpty()) {
                return SyncResult(success = true, message = "No unsynced session records")
            }

            DebugLogger.debugLog(TAG, "Syncing ${unsyncedSessions.size} session records...")

            // Process in batches
            unsyncedSessions.chunked(BATCH_SIZE).forEach { batch ->
                val batchWrite = firestore.batch()

                batch.forEach { session ->
                    val docRef = firestore
                        .collection(SESSIONS_COLLECTION)
                        .document(session.studentId)  // Use session's studentId
                        .collection("records")
                        .document(session.sessionId)

                    val data = mapOf(
                        "sessionId" to session.sessionId,
                        "studentId" to session.studentId,
                        "sessionDate" to session.sessionDate,
                        "sessionStartTime" to session.sessionStartTime,
                        "sessionEndTime" to session.sessionEndTime,
                        "durationMillis" to session.durationMillis,
                        "appName" to session.appName,
                        "syncedAt" to System.currentTimeMillis()
                    )

                    // Use merge=true to update only specified fields
                    batchWrite.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
                }

                batchWrite.commit().await()
            }

            // Mark as synced
            unsyncedSessions.forEach { session ->
                sessionDao.markSessionAsSynced(session.sessionId)
            }

            val message = "Synced ${unsyncedSessions.size} session records"
            DebugLogger.debugLog(TAG, message)
            SyncResult(success = true, message = message)
        } catch (e: Exception) {
            val errorMsg = "Session sync failed: ${e.message}"
            DebugLogger.errorLog(TAG, errorMsg)
            SyncResult(success = false, message = errorMsg)
        }
    }
}
