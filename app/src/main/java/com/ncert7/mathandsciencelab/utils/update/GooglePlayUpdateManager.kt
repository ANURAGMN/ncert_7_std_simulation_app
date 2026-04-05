package com.ncert7.mathandsciencelab.utils.update

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Real Google Play implementation of UpdateManager
 * Uses Google Play In-App Update API
 */
class GooglePlayUpdateManager : UpdateManager {
    companion object {
        private const val TAG = "GooglePlayUpdateManager"
        private const val REQUEST_CODE_UPDATE = 100
    }

    private var appUpdateManager: AppUpdateManager? = null
    private var updateCallback: UpdateCallback? = null
    private var appUpdateInfo: AppUpdateInfo? = null
    private val installListener = InstallStateUpdatedListener { installState ->
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                Log.d(TAG, "Update downloaded, ready to install")
                updateCallback?.onUpdateInstalled()
            }
            InstallStatus.DOWNLOADING -> {
                Log.d(TAG, "Update downloading: ${installState.bytesDownloaded()}/${installState.totalBytesToDownload()}")
                updateCallback?.onUpdateInProgress(
                    installState.bytesDownloaded(),
                    installState.totalBytesToDownload()
                )
            }
            InstallStatus.INSTALLED -> {
                Log.d(TAG, "Update installed")
            }
            else -> {
                Log.d(TAG, "Install state: ${installState.installStatus()}")
            }
        }
    }

    override suspend fun checkForUpdate(activity: Activity) = suspendCancellableCoroutine { continuation ->
        try {
            if (appUpdateManager == null) {
                appUpdateManager = AppUpdateManagerFactory.create(activity)
            }

            appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
                this.appUpdateInfo = appUpdateInfo
                Log.d(TAG, "Update check successful. Available version code: ${appUpdateInfo.availableVersionCode()}")

                if (appUpdateInfo.updateAvailability() == com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE) {
                    updateCallback?.onUpdateAvailable(appUpdateInfo)
                }
                continuation.resume(Unit)
            }?.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check for update", exception)
                updateCallback?.onUpdateFailed(exception)
                continuation.resume(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while checking for update", e)
            updateCallback?.onUpdateFailed(e)
            continuation.resume(Unit)
        }
    }

    override suspend fun startUpdate(
        activity: Activity,
        updateType: UpdateType
    ) = suspendCancellableCoroutine { continuation ->
        try {
            appUpdateInfo?.let { appUpdateInfo ->
                if (appUpdateManager == null) {
                    appUpdateManager = AppUpdateManagerFactory.create(activity)
                }

                appUpdateManager?.registerListener(installListener)

                val updateTypeInt = when (updateType) {
                    UpdateType.IMMEDIATE -> AppUpdateType.IMMEDIATE
                    UpdateType.FLEXIBLE -> AppUpdateType.FLEXIBLE
                }

                // Use modern API - this will handle the update flow
                @Suppress("DEPRECATION")
                appUpdateManager?.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateTypeInt,
                    activity,
                    REQUEST_CODE_UPDATE
                )
                continuation.resume(Unit)
            } ?: run {
                Log.w(TAG, "No update info available")
                continuation.resume(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while starting update", e)
            updateCallback?.onUpdateFailed(e)
            continuation.resume(Unit)
        }
    }

    override fun completeUpdate(activity: Activity) {
        try {
            appUpdateManager?.completeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while completing update", e)
        }
    }

    override fun getUpdateInfo(): AppUpdateInfo? = appUpdateInfo

    override fun setUpdateCallback(callback: UpdateCallback?) {
        this.updateCallback = callback
    }

    fun unregisterListener() {
        appUpdateManager?.unregisterListener(installListener)
    }
}
