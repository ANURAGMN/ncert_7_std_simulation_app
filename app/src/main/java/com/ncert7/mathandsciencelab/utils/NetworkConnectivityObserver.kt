package com.ncert7.mathandsciencelab.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.ncert7.mathandsciencelab.debug.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Observes network connectivity changes and provides real-time updates
 * Used to trigger synchronization when device comes online
 *
 * IMPORTANT: Uses WeakReference to prevent memory leaks
 * Does NOT hold strong reference to Context in static fields
 */
class NetworkConnectivityObserver(private val context: Context) {
    companion object {
        private const val TAG = "NetworkConnectivityObserver"
        // Use WeakReference to avoid memory leaks - if context is destroyed, instance can be GC'd
        private var instanceRef: WeakReference<NetworkConnectivityObserver>? = null

        fun getInstance(context: Context): NetworkConnectivityObserver {
            val instance = instanceRef?.get()

            // If instance doesn't exist or was GC'd, create new one
            if (instance == null) {
                val newInstance = NetworkConnectivityObserver(context)
                instanceRef = WeakReference(newInstance)
                return newInstance
            }

            return instance
        }

        fun isOnline(context: Context): Boolean {
            return try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                caps != null && (
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        )
            } catch (_: Exception) {
                false
            }
        }
    }

    // Use application context to avoid holding activity reference
    private val appContext = context.applicationContext

    private val _isOnline = MutableStateFlow(isOnline(appContext))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            DebugLogger.debugLog(TAG, " Network available - device is ONLINE")
        }

        override fun onLost(network: Network) {
            // Check if there are other networks available
            _isOnline.value = isOnline(appContext)
            if (!_isOnline.value) {
                DebugLogger.debugLog(TAG, " Network lost - device is OFFLINE")
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _isOnline.value = isOnline(appContext)
            DebugLogger.debugLog(TAG, " Network capabilities changed - Online: ${_isOnline.value}")
        }
    }

    private var isRegistered = false

    fun register() {
        try {
            if (!isRegistered) {
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
                isRegistered = true
                DebugLogger.debugLog(TAG, " Network callback registered")
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, " Failed to register network callback: ${e.message}")
        }
    }

    fun unregister() {
        try {
            if (isRegistered) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                isRegistered = false
                DebugLogger.debugLog(TAG, " Network callback unregistered")
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, " Failed to unregister network callback: ${e.message}")
        }
    }

    /**
     * Cleanup - call this when shutting down
     * Allows instance to be garbage collected
     */
    fun destroy() {
        unregister()
        instanceRef?.clear()
        instanceRef = null
        DebugLogger.debugLog(TAG, " NetworkConnectivityObserver destroyed")
    }
}

