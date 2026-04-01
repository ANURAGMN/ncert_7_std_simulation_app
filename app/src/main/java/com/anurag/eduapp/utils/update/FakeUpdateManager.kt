package com.anurag.eduapp.utils.update

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import kotlinx.coroutines.delay

/**
 * Fake Update Manager for testing in-app updates without Play Store
 * Simulates update availability and download progress
 */
class FakeUpdateManager(private val shouldShowUpdate: Boolean = true) : UpdateManager {
    companion object {
        private const val TAG = "FakeUpdateManager"
    }

    private var updateCallback: UpdateCallback? = null

    override suspend fun checkForUpdate(activity: Activity) {
        try {
            Log.d(TAG, "Fake: Checking for update...")
            delay(500) // Simulate network delay

            if (shouldShowUpdate) {
                Log.d(TAG, "Fake: Update available - v2 (simulated)")
                updateCallback?.onUpdateAvailable(FakeAppUpdateInfo())
            } else {
                Log.d(TAG, "Fake: No update available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fake: Exception while checking for update", e)
            updateCallback?.onUpdateFailed(e)
        }
    }

    override suspend fun startUpdate(activity: Activity, updateType: UpdateType) {
        try {
            Log.d(TAG, "Fake: Starting update - Type: $updateType")

            // Simulate download progress
            val totalBytes = 50_000_000L
            val steps = 20
            val bytesPerStep = totalBytes / steps

            repeat(steps) {
                delay(200) // Simulate download progress
                val bytesDownloaded = bytesPerStep * (it + 1)
                updateCallback?.onUpdateInProgress(bytesDownloaded, totalBytes)
                Log.d(TAG, "Fake: Downloaded $bytesDownloaded / $totalBytes bytes")
            }

            Log.d(TAG, "Fake: Download complete")
            updateCallback?.onUpdateInstalled()

            if (updateType == UpdateType.IMMEDIATE) {
                delay(500)
                Log.d(TAG, "Fake: IMMEDIATE update - would restart app")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fake: Exception while starting update", e)
            updateCallback?.onUpdateFailed(e)
        }
    }

    override fun completeUpdate(activity: Activity) {
        Log.d(TAG, "Fake: Complete update called")
    }

    override fun getUpdateInfo(): AppUpdateInfo? = null

    override fun setUpdateCallback(callback: UpdateCallback?) {
        this.updateCallback = callback
    }
}

/**
 * Fake data class representing app update info for testing
 */
data class FakeAppUpdateInfo(
    val currentVersion: Int = 1,
    val availableVersion: Int = 2,
    val isUpdateAvailable: Boolean = true,
    val updateSize: Long = 50_000_000L
)
