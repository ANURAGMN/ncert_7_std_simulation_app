package com.ncert7.mathandsciencelab.repository

import com.ncert7.mathandsciencelab.data.local.dao.ConceptDao
import com.ncert7.mathandsciencelab.data.local.entities.ConceptEntity
import com.ncert7.mathandsciencelab.utils.RetryHelper

/**
 * Repository for managing simulation data
 * Currently hardcoded for Unit 8, can be extended to database later
 */
class SimulationRepository(
    private val conceptDao: ConceptDao
) {

    /**
     * Get all simulations for a specific chapter
     * @param chapterId The chapter ID (e.g., "8")
     * @return List of simulations for the chapter
     */
    suspend fun getSimulationsForChapter(chapterId: String): List<ConceptEntity> {
        return RetryHelper.executeWithRetryList(
            maxRetries = 3,
            functionName = "ConceptDao.getConceptsForChapterSync($chapterId, SIMULATION)"
        ) {
            conceptDao.getConceptsForChapterSync(chapterId = chapterId, type = "SIMULATION")
        }
    }

}