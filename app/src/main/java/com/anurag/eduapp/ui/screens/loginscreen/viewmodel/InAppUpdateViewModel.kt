package com.anurag.eduapp.ui.screens.loginscreen.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anurag.eduapp.utils.update.FakeUpdateManager
import com.anurag.eduapp.utils.update.GooglePlayUpdateManager
import com.anurag.eduapp.utils.update.UpdateCallback
import com.anurag.eduapp.utils.update.UpdateManager
import com.anurag.eduapp.utils.update.UpdateType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateState(
    val updateAvailable: Boolean = false,
    val updateInfo: Any? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val error: String? = null,
    val updateInstalled: Boolean = false
)

@HiltViewModel
class InAppUpdateViewModel @Inject constructor() : ViewModel(), UpdateCallback {
    companion object {
        private const val TAG = "InAppUpdateViewModel"
        private const val USE_FAKE_UPDATES = true // Set to false to use real Play Store updates
    }

    private val updateManager: UpdateManager = if (USE_FAKE_UPDATES) {
        Log.d(TAG, "Using FakeUpdateManager for testing")
        FakeUpdateManager(shouldShowUpdate = true)
    } else {
        Log.d(TAG, "Using GooglePlayUpdateManager for production")
        GooglePlayUpdateManager()
    }

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized, setting callback")
        updateManager.setUpdateCallback(this)
        Log.d(TAG, "Callback set successfully")
    }

    fun checkForUpdate(activity: Activity) {
        Log.d(TAG, "checkForUpdate called from LoginScreen")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting update check in coroutine...")
                updateManager.checkForUpdate(activity)
                Log.d(TAG, "Update check completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for update", e)
                _updateState.value = _updateState.value.copy(
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun startUpdate(activity: Activity, updateType: UpdateType = UpdateType.FLEXIBLE) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting update with type: $updateType")
                _updateState.value = _updateState.value.copy(isDownloading = true)
                updateManager.startUpdate(activity, updateType)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting update", e)
                _updateState.value = _updateState.value.copy(
                    error = e.message ?: "Unknown error occurred",
                    isDownloading = false
                )
            }
        }
    }

    fun completeUpdate(activity: Activity) {
        try {
            updateManager.completeUpdate(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing update", e)
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState()
    }

    override fun onUpdateAvailable(appUpdateInfo: Any) {
        Log.d(TAG, "onUpdateAvailable called with: $appUpdateInfo")
        _updateState.value = _updateState.value.copy(
            updateAvailable = true,
            updateInfo = appUpdateInfo
        )
        Log.d(TAG, "State updated: updateAvailable = true")
    }

    override fun onUpdateInProgress(bytesDownloaded: Long, totalBytes: Long) {
        val progress = if (totalBytes > 0) {
            (bytesDownloaded.toFloat() / totalBytes.toFloat())
        } else {
            0f
        }
        Log.d(TAG, "Update progress: ${(progress * 100).toInt()}% ($bytesDownloaded/$totalBytes bytes)")
        _updateState.value = _updateState.value.copy(
            downloadProgress = progress
        )
    }

    override fun onUpdateInstalled() {
        Log.d(TAG, "onUpdateInstalled called")
        _updateState.value = _updateState.value.copy(
            updateInstalled = true,
            isDownloading = false
        )
    }

    override fun onUpdateFailed(exception: Exception) {
        Log.e(TAG, "Update failed: ${exception.message}", exception)
        _updateState.value = _updateState.value.copy(
            error = exception.message ?: "Update failed",
            isDownloading = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        if (updateManager is GooglePlayUpdateManager) {
            updateManager.unregisterListener()
        }
    }
}
