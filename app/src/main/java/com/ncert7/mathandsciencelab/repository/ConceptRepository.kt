package com.ncert7.mathandsciencelab.repository

import com.ncert7.mathandsciencelab.data.local.dao.ConceptDao
import com.ncert7.mathandsciencelab.data.local.dao.ProgressDao
import com.ncert7.mathandsciencelab.data.local.entities.ConceptEntity
import com.ncert7.mathandsciencelab.data.local.entities.ProgressEntity
import com.ncert7.mathandsciencelab.utils.RetryHelper

/**
 * Repository class for managing concepts and their progress.
 */
class ConceptRepository(
    private val conceptDao: ConceptDao,
    private val progressDao: ProgressDao
) {
    /**
     * Retrieves all concepts from the database.
     * returns List of ConceptEntity
     */
    suspend fun getAllConcepts(): List<ConceptEntity> {
        return RetryHelper.executeWithRetryList(
            maxRetries = 3,
            functionName = "ConceptDao.getAllConceptsSync"
        ) {
            conceptDao.getAllConceptsSync()
        }
    }

    /**
     * Retrieves a list of concepts for a given chapter.
     * returns List of ConceptEntity
     */
    suspend fun getConceptsForChapter(chapterId: String, type: String): List<ConceptEntity> {
        return RetryHelper.executeWithRetryList(
            maxRetries = 3,
            functionName = "ConceptDao.getConceptsForChapterSync($chapterId, $type)"
        ) {
            conceptDao.getConceptsForChapterSync(chapterId, type)
        }
    }

    /**
     * Retrieves a specific concept by its ID.
     * returns ConceptEntity or null if not found
     */
    suspend fun getConcept(conceptId: String): ConceptEntity? {
        return RetryHelper.executeWithRetry(
            maxRetries = 3,
            functionName = "ConceptDao.getConcept($conceptId)"
        ) {
            conceptDao.getConcept(conceptId)
        }
    }

    /**
     * Retrieves the progress of a student for a specific item.
     * returns ProgressEntity or null if not found
     */
    suspend fun getProgress(studentId: String, itemType: String, itemId: String): ProgressEntity? {
        return RetryHelper.executeWithRetry(
            maxRetries = 3,
            functionName = "ProgressDao.getProgress($studentId, $itemType, $itemId)"
        ) {
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
    }

    /**
     * Gets the total number of completed simulations for a student
     */
    suspend fun getTotalCompletedSimulations(studentId: String): Int {
        return RetryHelper.executeWithRetry(
            maxRetries = 3,
            functionName = "ProgressDao.getTotalCompletedSimulations($studentId)"
        ) {
            progressDao.getTotalCompletedSimulations(studentId)
        } ?: 0
    }
}