package com.ncert7.mathandsciencelab.service.sync

import com.ncert7.mathandsciencelab.data.local.entities.ProgressEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.ncert7.mathandsciencelab.debug.DebugLogger

/**
 * Maps Firestore progress documents to local Room ProgressEntity objects.
 * Handles conversion from Firestore data structure to local database entity format.
 *
 * Firestore progress document structure (progress/{studentId}/records/{itemType}_{itemId}):
 * - itemId: ID of the concept/simulation being tracked
 * - itemType: "CONCEPT" or "SIMULATION"
 * - status: "NOT_STARTED", "IN_PROGRESS", or "COMPLETED"
 * - progressPercentage: Integer from 0-100
 * - startedAt: Long timestamp (milliseconds) when user started, optional
 * - completedAt: Long timestamp (milliseconds) when user completed, optional
 * - lastAccessedAt: Long timestamp (milliseconds) of last access
 * - updatedAt: Long timestamp (milliseconds) of last update
 * - syncedAt: Long timestamp (milliseconds) when synced to Firestore
 */
object FirebaseProgressMapper {
    private const val TAG = "FirebaseProgressMapper"

    fun map(document: DocumentSnapshot, studentId: String): ProgressEntity {
        try {
            val itemId = document.getString("itemId")
                ?: error("itemId missing for progress document ${document.id}")

            val itemType = document.getString("itemType")
                ?: error("itemType missing for progress document ${document.id}")

            val status = document.getString("status")
                ?: error("status missing for progress document ${document.id}")

            val progressPercentage = document.getLong("progressPercentage")?.toInt() ?: 0
            val startedAt = document.getLong("startedAt")
            val completedAt = document.getLong("completedAt")
            val lastAccessedAt = document.getLong("lastAccessedAt") ?: System.currentTimeMillis()
            val updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis()

            return ProgressEntity(
                studentId = studentId,
                itemType = itemType,
                itemId = itemId,
                status = status,
                progressPercentage = progressPercentage.coerceIn(0, 100),
                startedAt = startedAt,
                completedAt = completedAt,
                lastAccessedAt = lastAccessedAt,
                updatedAt = updatedAt,
                isSynced = true // Firestore data is already synced
            )
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Error mapping progress document ${document.id}: ${e.message}")
            throw e
        }
    }
}
