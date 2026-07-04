package com.ncert7.mathandsciencelab.repository

import com.ncert7.mathandsciencelab.data.firebase.model.User
import com.ncert7.mathandsciencelab.data.firebase.model.Streak
import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.ncert7.mathandsciencelab.config.AppConfig
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.jvm.java
import kotlin.text.contains
import kotlin.text.isBlank
import kotlin.text.lowercase
import kotlin.to

class FirebaseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("users")
    private val streakCollection = firestore.collection("streak")

    /**
     * Check if a user exists in Firestore
     * @return UserCheckResult indicating Found, NotFound, or Error
     */
    suspend fun checkUserExists(userId: String): UserCheckResult {
        return try {
            // Validate user ID is not empty
            if (userId.isBlank()) {
                DebugLogger.errorLog("FirebaseRepository", "Cannot check user: User ID is empty")
                return UserCheckResult.Error(IllegalArgumentException("User ID cannot be empty"))
            }

            val snapshot = usersCollection.document(userId).get().await()

            if (!snapshot.exists()) {
                DebugLogger.debugLog("FirebaseRepository", "User not found: $userId")
                UserCheckResult.NotFound
            } else {
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    DebugLogger.debugLog("FirebaseRepository", "User found: $userId")
                    UserCheckResult.Found(user)
                } else {
                    DebugLogger.errorLog("FirebaseRepository", "Failed to parse user data for: $userId")
                    UserCheckResult.Error(Exception("Failed to parse user data"))
                }
            }
        } catch (e: FirebaseNetworkException) {
            DebugLogger.errorLog("FirebaseRepository", "Network error checking user: ${e.message}")
            UserCheckResult.Error(NetworkException("Network error. Please check your connection and try again.", e))
        } catch (e: FirebaseFirestoreException) {
            DebugLogger.errorLog("FirebaseRepository", "Firestore error checking user: ${e.message}")
            // Check if it's a network-related Firestore error
            if (isNetworkError(e)) {
                UserCheckResult.Error(NetworkException("Network error. Please check your connection and try again.", e))
            } else {
                UserCheckResult.Error(e)
            }
        } catch (e: SocketTimeoutException) {
            DebugLogger.errorLog("FirebaseRepository", "Connection timeout checking user: ${e.message}")
            UserCheckResult.Error(NetworkException("Connection timeout. Please try again.", e))
        } catch (e: UnknownHostException) {
            DebugLogger.errorLog("FirebaseRepository", "No internet connection: ${e.message}")
            UserCheckResult.Error(NetworkException("No internet connection. Please check your network.", e))
        } catch (e: IOException) {
            DebugLogger.errorLog("FirebaseRepository", "I/O error checking user: ${e.message}")
            UserCheckResult.Error(NetworkException("Network error occurred. Please try again.", e))
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Unexpected error checking user: ${e.message}")
            UserCheckResult.Error(e)
        }
    }

    suspend fun createNewUser(user: User): Boolean {
        return try {
            // Validate user ID is not empty
            if (user.id.isBlank()) {
                DebugLogger.errorLog("FirebaseRepository", "Cannot create user: User ID is empty")
                throw IllegalArgumentException("User ID cannot be empty")
            }

            val data = mapOf(
                "id" to user.id,
                "email" to user.email,
                "displayName" to user.displayName,
                "profilePictureUri" to user.profilePictureUri,
                "schoolName" to user.schoolName,
                "phoneNumber" to user.phoneNumber,
                "studentClass" to user.studentClass,
                "language" to user.language,
                "appName" to user.appName,
                "createdAt" to user.createdAt,
                "updatedAt" to user.lastLogin
            )

            usersCollection.document(user.id).set(data).await()
            DebugLogger.debugLog("FirebaseRepository", "User created successfully: ${user.id}")
            true
        } catch (e: FirebaseNetworkException) {
            DebugLogger.errorLog("FirebaseRepository", "Network error creating user: ${e.message}")
            throw NetworkException("Network error. Please check your connection and try again.", e)
        } catch (e: FirebaseFirestoreException) {
            DebugLogger.errorLog("FirebaseRepository", "Firestore error creating user: ${e.message}")
            if (isNetworkError(e)) {
                throw NetworkException("Network error. Please check your connection and try again.", e)
            } else {
                throw e
            }
        } catch (e: SocketTimeoutException) {
            DebugLogger.errorLog("FirebaseRepository", "Connection timeout creating user: ${e.message}")
            throw NetworkException("Connection timeout. Please try again.", e)
        } catch (e: UnknownHostException) {
            DebugLogger.errorLog("FirebaseRepository", "No internet connection: ${e.message}")
            throw NetworkException("No internet connection. Please check your network.", e)
        } catch (e: IOException) {
            DebugLogger.errorLog("FirebaseRepository", "I/O error creating user: ${e.message}")
            throw NetworkException("Network error occurred. Please try again.", e)
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error creating user: ${e.message}")
            throw e
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        name: String,
        phone: String,
        school: String,
        studentClass: Int,
        updatedAt: Long
    ): Boolean {
        return try {
            usersCollection.document(userId)
                .update(
                    mapOf(
                        "displayName" to name,
                        "phoneNumber" to phone,
                        "schoolName" to school,
                        "studentClass" to studentClass,
                        "updatedAt" to updatedAt
                    )
                )
                .await()
            DebugLogger.debugLog("FirebaseRepository", "User profile updated: $userId")
            true
        } catch (e: FirebaseNetworkException) {
            DebugLogger.errorLog("FirebaseRepository", "Network error updating profile: ${e.message}")
            throw NetworkException("Network error. Please check your connection and try again.", e)
        } catch (e: FirebaseFirestoreException) {
            DebugLogger.errorLog("FirebaseRepository", "Firestore error updating profile: ${e.message}")
            if (isNetworkError(e)) {
                throw NetworkException("Network error. Please check your connection and try again.", e)
            } else {
                throw e
            }
        } catch (e: SocketTimeoutException) {
            DebugLogger.errorLog("FirebaseRepository", "Connection timeout updating profile: ${e.message}")
            throw NetworkException("Connection timeout. Please try again.", e)
        } catch (e: UnknownHostException) {
            DebugLogger.errorLog("FirebaseRepository", "No internet connection: ${e.message}")
            throw NetworkException("No internet connection. Please check your network.", e)
        } catch (e: IOException) {
            DebugLogger.errorLog("FirebaseRepository", "I/O error updating profile: ${e.message}")
            throw NetworkException("Network error occurred. Please try again.", e)
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error updating profile: ${e.message}")
            throw e
        }
    }

    /**
     * Check if a Firestore exception is network-related
     */
    private fun isNetworkError(exception: FirebaseFirestoreException): Boolean {
        return when (exception.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> true
            else -> {
                val message = exception.message?.lowercase() ?: ""
                message.contains("network") ||
                        message.contains("timeout") ||
                        message.contains("connection") ||
                        message.contains("unavailable")
            }
        }
    }

    // ===== STREAK OPERATIONS =====

    /**
     * Get user's streak from Firestore
     */
    suspend fun getStreak(userId: String): Streak? {
        return try {
            if (userId.isBlank()) {
                DebugLogger.errorLog("FirebaseRepository", "Cannot get streak: User ID is empty")
                return null
            }

            val snapshot = streakCollection.document(userId).get().await()
            if (snapshot.exists()) {
                val streak = snapshot.toObject(Streak::class.java)
                DebugLogger.debugLog("FirebaseRepository", "Streak retrieved: $userId - count: ${streak?.streakCount}")
                streak
            } else {
                DebugLogger.debugLog("FirebaseRepository", "No streak found for user: $userId")
                null
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error getting streak: ${e.message}")
            null
        }
    }

    /**
     * Create or update user's streak in Firestore
     */
    suspend fun updateStreak(userId: String, streakCount: Int, lastStreakDate: Long): Boolean {
        return try {
            if (userId.isBlank()) {
                DebugLogger.errorLog("FirebaseRepository", "Cannot update streak: User ID is empty")
                return false
            }

            val streak = Streak(
                userId = userId,
                streakCount = streakCount,
                lastStreakDate = lastStreakDate,
                updatedAt = System.currentTimeMillis(),
                appName = AppConfig.APP_NAME
            )

            streakCollection.document(userId).set(streak).await()
            DebugLogger.debugLog("FirebaseRepository", "Streak updated: $userId - count: $streakCount")
            true
        } catch (e: FirebaseNetworkException) {
            DebugLogger.errorLog("FirebaseRepository", "Network error updating streak: ${e.message}")
            false
        } catch (e: FirebaseFirestoreException) {
            DebugLogger.errorLog("FirebaseRepository", "Firestore error updating streak: ${e.message}")
            false
        } catch (e: Exception) {
            DebugLogger.errorLog("FirebaseRepository", "Error updating streak: ${e.message}")
            false
        }
    }

}

/**
 * Result type for user existence check
 */
sealed class UserCheckResult {
    data class Found(val user: User) : UserCheckResult()
    object NotFound : UserCheckResult()
    data class Error(val exception: Throwable) : UserCheckResult()
}

/**
 * Custom exception for network-related errors
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)