package com.anurag.eduapp.utils.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo

/**
 * Interface for managing in-app updates
 * Allows for both real Play Store updates and fake testing
 */
interface UpdateManager {
    /**
     * Check for available updates
     */
    suspend fun checkForUpdate(activity: Activity)

    /**
     * Start the update flow (immediate or flexible)
     */
    suspend fun startUpdate(activity: Activity, updateType: UpdateType = UpdateType.FLEXIBLE)

    /**
     * Complete the flexible update flow
     */
    fun completeUpdate(activity: Activity)

    /**
     * Get the current update info
     */
    fun getUpdateInfo(): AppUpdateInfo?

    /**
     * Set a callback for update state changes
     */
    fun setUpdateCallback(callback: UpdateCallback?)
}

enum class UpdateType {
    IMMEDIATE,
    FLEXIBLE
}

interface UpdateCallback {
    fun onUpdateAvailable(appUpdateInfo: Any)
    fun onUpdateInProgress(bytesDownloaded: Long, totalBytes: Long)
    fun onUpdateInstalled()
    fun onUpdateFailed(exception: Exception)
}
