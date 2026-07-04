package com.ncert7.mathandsciencelab.service.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ncert7.mathandsciencelab.data.local.EduAiDatabase
import com.ncert7.mathandsciencelab.data.local.SharedPreferenceUtils
import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.ncert7.mathandsciencelab.utils.NetworkConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Central service for managing real-time and offline synchronization of
 * Progress, Analytics, and Session data to Firestore.
 *
 * Features:
 * - Real-time sync when data changes
 * - Automatic sync when device comes online
 * - Periodic background sync using WorkManager
 * - Exponential backoff retry on failures
 */
object DataSyncService {
    private const val TAG = "DataSyncService"
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + Dispatchers.IO)

    private var isInitialized = false
    private lateinit var applicationContext: Context  // Use app context, not activity context
    private lateinit var database: EduAiDatabase
    private lateinit var sharedPref: SharedPreferenceUtils
    private var syncManager: ProgressAnalyticsSessionSyncManager? = null
    private var simulationSyncManager: SimulationSyncManager? = null
    private var connectivityObserver: NetworkConnectivityObserver? = null
    private var networkListenerJob: Job? = null  // Track the network listener job

    /**
     * Initializes the DataSyncService
     * Must be called once from Application.onCreate() with application context
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        synchronized(this) {
            if (isInitialized) return

            // Store application context, not activity context
            this.applicationContext = context.applicationContext
            this.database = EduAiDatabase.getInstance(applicationContext)
            this.sharedPref = SharedPreferenceUtils(applicationContext)

            // Initialize network observer
            connectivityObserver = NetworkConnectivityObserver.getInstance(applicationContext)
            connectivityObserver?.register()

            // Get current student ID and initialize sync manager
            val currentStudentId = sharedPref.getUserId()
            if (!currentStudentId.isNullOrBlank()) {
                updateStudentId(currentStudentId)
            } else {
                simulationSyncManager = SimulationSyncManager(
                    interactionDao = database.simulationInteractionDao(),
                    sharedPreferenceUtils = sharedPref
                )
            }

            // Listen for network changes
            listenToNetworkChanges()

            isInitialized = true
            DebugLogger.debugLog(TAG, " DataSyncService initialized with studentId: $currentStudentId")
        }
    }

    /**
     * Syncs a single progress update immediately (real-time)
     * Falls back to offline queue if network is unavailable
     */
    fun syncProgressUpdate(progressId: Long, studentId: String) {
        scope.launch {
            try {
                if (syncManager == null) {
                    updateStudentId(studentId)
                }

                if (NetworkConnectivityObserver.isOnline(applicationContext)) {
                    DebugLogger.debugLog(TAG, " Real-time progress sync starting: $progressId")
                    try {
                        syncManager?.syncProgressUpdate(progressId, studentId)
                        DebugLogger.debugLog(TAG, " Real-time progress sync completed: $progressId")
                    } catch (e: Exception) {
                        DebugLogger.errorLog(TAG, " Real-time progress sync failed: ${e.message}")
                        scheduleBackgroundSync()
                    }
                } else {
                    DebugLogger.debugLog(TAG, " Device offline, progress update queued: $progressId")
                    scheduleBackgroundSync()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, " Progress sync error: ${e.message}")
                scheduleBackgroundSync()
            }
        }
    }

    /**
     * Syncs a single analytics update immediately (real-time)
     * Falls back to offline queue if network is unavailable
     */
    fun syncAnalyticsUpdate(analyticsId: Long) {
        scope.launch {
            try {
                if (syncManager == null) {
                    val studentId = sharedPref.getUserId() ?: return@launch
                    updateStudentId(studentId)
                }

                if (NetworkConnectivityObserver.isOnline(applicationContext)) {
                    DebugLogger.debugLog(TAG, " Real-time analytics sync starting: $analyticsId")
                    try {
                        syncManager?.syncAnalyticsUpdate(analyticsId)
                        DebugLogger.debugLog(TAG, " Real-time analytics sync completed: $analyticsId")
                    } catch (e: Exception) {
                        DebugLogger.errorLog(TAG, " Real-time analytics sync failed: ${e.message}")
                        scheduleBackgroundSync()
                    }
                } else {
                    DebugLogger.debugLog(TAG, " Device offline, analytics update queued: $analyticsId")
                    scheduleBackgroundSync()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, " Analytics sync error: ${e.message}")
                scheduleBackgroundSync()
            }
        }
    }

    /**
     * Syncs a single session update immediately (real-time)
     * Falls back to offline queue if network is unavailable
     */
    fun syncSessionUpdate(sessionId: String) {
        scope.launch {
            try {
                if (syncManager == null) {
                    val studentId = sharedPref.getUserId() ?: return@launch
                    updateStudentId(studentId)
                }

                // Immediately attempt sync (online or offline, let WorkManager handle it)
                DebugLogger.debugLog(TAG, "Syncing session: $sessionId")
                try {
                    syncManager?.syncSessionUpdate(sessionId)
                    DebugLogger.debugLog(TAG, "Session sync completed: $sessionId")
                } catch (e: Exception) {
                    DebugLogger.errorLog(TAG, " Session sync failed, queuing for retry: ${e.message}")
                    scheduleBackgroundSync()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "Session sync error: ${e.message}")
                scheduleBackgroundSync()
            }
        }
    }

    /**
     * Triggers a manual full sync of all unsynced data
     */
    fun triggerFullSync() {
        scope.launch {
            try {
                if (NetworkConnectivityObserver.isOnline(applicationContext)) {
                    DebugLogger.debugLog(TAG, " Triggering full sync...")
                    val result = syncManager?.syncAllUnsyncedData()
                    val simulationResult = simulationSyncManager?.syncTodayIfNeeded()
                    if (result != null) {
                        if (result.success) {
                            DebugLogger.debugLog(TAG, " Full sync completed:\n${result.message}")
                        } else {
                            DebugLogger.errorLog(TAG, " Full sync failed:\n${result.message}")
                        }
                    }
                    if (simulationResult != null) {
                        if (simulationResult.success) {
                            DebugLogger.debugLog(TAG, " Simulation sync completed: ${simulationResult.message}")
                        } else {
                            DebugLogger.errorLog(TAG, " Simulation sync failed: ${simulationResult.message}")
                        }
                    }
                } else {
                    DebugLogger.debugLog(TAG, " Device offline, scheduling background sync")
                    scheduleBackgroundSync()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, " Full sync error: ${e.message}")
                scheduleBackgroundSync()
            }
        }
    }

    fun syncSimulationInteractions() {
        scope.launch {
            try {
                if (simulationSyncManager == null) {
                    simulationSyncManager = SimulationSyncManager(
                        interactionDao = database.simulationInteractionDao(),
                        sharedPreferenceUtils = sharedPref
                    )
                }

                val result = simulationSyncManager?.syncTodayIfNeeded()
                if (result?.success == true) {
                    DebugLogger.debugLog(TAG, "Simulation interaction sync completed: ${result.message}")
                } else if (result != null) {
                    DebugLogger.errorLog(TAG, "Simulation interaction sync failed: ${result.message}")
                    scheduleBackgroundSync()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "Simulation interaction sync error: ${e.message}")
                scheduleBackgroundSync()
            }
        }
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Listens to network connectivity changes and triggers sync when online
     * Job is tracked for proper cleanup
     */
    private fun listenToNetworkChanges() {
        // Cancel previous job if exists
        networkListenerJob?.cancel()

        networkListenerJob = scope.launch {
            try {
                connectivityObserver?.isOnline?.collectLatest { isOnline ->
                    if (isOnline) {
                        DebugLogger.debugLog(TAG, " Device came online, checking for unsynced data...")
                        triggerFullSync()
                    } else {
                        DebugLogger.debugLog(TAG, " Device went offline")
                    }
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, " Network listener error: ${e.message}")
            }
        }
    }

    /**
     * Schedules a background sync task using WorkManager
     */
    private fun scheduleBackgroundSync() {
        try {
            val syncRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "DATA_SYNC_WORK_${System.currentTimeMillis()}",
                androidx.work.ExistingWorkPolicy.KEEP,
                syncRequest
            )

            DebugLogger.debugLog(TAG, " Background sync scheduled with WorkManager")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, " Failed to schedule background sync: ${e.message}")
        }
    }

    /**
     * Updates the sync manager with new student ID
     * Call this when user logs in
     */
    fun updateStudentId(studentId: String) {
        syncManager = ProgressAnalyticsSessionSyncManager(
            progressDao = database.progressDao(),
            analyticsDao = database.appAnalyticsDao(),
            sessionDao = database.sessionDao(),
            studentId = studentId
        )
        simulationSyncManager = SimulationSyncManager(
            interactionDao = database.simulationInteractionDao(),
            sharedPreferenceUtils = sharedPref
        )

        DebugLogger.debugLog(TAG, "👤 Student ID updated: $studentId")
    }

    /**
     * Cleanup - call this from Application.onTerminate()
     * DO NOT call from Activity.onDestroy() - this is a singleton
     */
    fun shutdown() {
        try {
            // Cancel the network listener job
            networkListenerJob?.cancel()
            networkListenerJob = null

            // Properly destroy observer to allow GC and remove context reference
            connectivityObserver?.destroy()
            connectivityObserver = null

            // Cancel all coroutines
            supervisorJob.cancel()

            // Clear references
            syncManager = null
            simulationSyncManager = null

            isInitialized = false
            DebugLogger.debugLog(TAG, " DataSyncService shutdown complete")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, " Error during shutdown: ${e.message}")
        }
    }
}
