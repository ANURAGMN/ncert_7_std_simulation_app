package com.ncert7.mathandsciencelab.repository

import com.ncert7.mathandsciencelab.data.local.dao.ConceptDao
import com.ncert7.mathandsciencelab.data.local.dao.ProgressDao
import com.ncert7.mathandsciencelab.data.local.entities.ConceptEntity
import com.ncert7.mathandsciencelab.data.local.entities.ProgressEntity
import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.ncert7.mathandsciencelab.service.sync.DataSyncService
import com.ncert7.mathandsciencelab.utils.DatabaseRetryHelper

/**
 * Repository class for managing concepts and their progress.
 */
class ConceptRepository(
    private val conceptDao: ConceptDao,
    private val progressDao: ProgressDao
) {
    /**
     * Retrieves all concepts from the database.
     * Retries only if there's an actual failure.
     * returns List of ConceptEntity
     */
    suspend fun getAllConcepts(): List<ConceptEntity> {
        return DatabaseRetryHelper.retryIfFails(maxRetries = 3) {
            conceptDao.getAllConceptsSync()
        }
    }

    /**
     * Retrieves a list of concepts for a given chapter.
     * Retries only if there's an actual failure.
     * returns List of ConceptEntity
     */
    suspend fun getConceptsForChapter(chapterId: String, type: String): List<ConceptEntity> {
        return DatabaseRetryHelper.retryIfFails(maxRetries = 3) {
            conceptDao.getConceptsForChapterSync(chapterId, type)
        }
    }

    /**
     * Retrieves a specific concept by its ID.
     * Retries only if there's an actual failure.
     * returns ConceptEntity or null if not found
     */
    suspend fun getConcept(conceptId: String): ConceptEntity? {
        return DatabaseRetryHelper.retryIfFailsNullable(maxRetries = 3) {
            conceptDao.getConcept(conceptId)
        }
    }

    /**
     * Retrieves the progress of a student for a specific item.
     * Retries only if there's an actual failure.
     * returns ProgressEntity or null if not found
     */
    suspend fun getProgress(studentId: String, itemType: String, itemId: String): ProgressEntity? {
        return DatabaseRetryHelper.retryIfFailsNullable(maxRetries = 3) {
            progressDao.getProgress(studentId, itemType, itemId)
        }
    }

    /**
     * Updates the progress status of a specific item for a student.
     */
    suspend fun updateProgressStatus(
        studentId: String,
        itemType: String,
        itemId: String,
        newStatus: String,
        progressPercentage: Int,
        timestamp: Long
    ) {
        progressDao.updateProgressStatus(studentId, itemType, itemId, newStatus, progressPercentage, timestamp)

        // Get the updated progress and trigger sync
        val progress = progressDao.getProgress(studentId, itemType, itemId)
        if (progress != null) {
            DebugLogger.debugLog("ConceptRepository", "Syncing progress update: ${progress.progressId}")
            DataSyncService.syncProgressUpdate(progress.progressId, studentId)
        }
    }

    /**
     * Gets the total number of completed simulations for a student
     */
    suspend fun getTotalCompletedSimulations(studentId: String): Int {
        return DatabaseRetryHelper.retryIfFailsNullable(maxRetries = 3) {
            progressDao.getTotalCompletedSimulations(studentId)
        } ?: 0
    }

    /**
     * Gets the total number of simulations completed today by a student
     */
    suspend fun getTodayCompletedSimulations(studentId: String): Int {
        return DatabaseRetryHelper.retryIfFailsNullable(maxRetries = 3) {
            progressDao.getTodayCompletedSimulations(studentId)
        } ?: 0
    }
}