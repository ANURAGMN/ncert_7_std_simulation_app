package com.ncert7.mathandsciencelab.repository

import com.ncert7.mathandsciencelab.data.local.SharedPreferenceUtils
import com.ncert7.mathandsciencelab.data.local.dao.SubjectDao
import com.ncert7.mathandsciencelab.data.local.entities.SubjectEntity
import com.ncert7.mathandsciencelab.utils.RetryHelper

/**
 * Repository class for managing subject data.
 */
class SubjectRepository(
    private val subjectDao: SubjectDao
) {
    /**
     * Retrieves all subjects for the configured content class level.
     * Always loads from CONTENT_CLASS_LEVEL regardless of input to ensure consistent data.
     * returns List of SubjectEntity
     */
    suspend fun getSubjectsForClass(classLevel: Int = SharedPreferenceUtils.CLASS_LEVEL): List<SubjectEntity> {
        return RetryHelper.executeWithRetryList(
            maxRetries = 3,
            functionName = "SubjectDao.getSubjectsForClassSync(${SharedPreferenceUtils.CLASS_LEVEL})"
        ) {
            subjectDao.getSubjectsForClassSync(SharedPreferenceUtils.CLASS_LEVEL)
        }
    }

    /**
     * Retrieves a specific subject by its ID.
     * returns SubjectEntity or null if not found
     */
    suspend fun getSubject(subjectId: String): SubjectEntity? {
        return RetryHelper.executeWithRetry(
            maxRetries = 3,
            functionName = "SubjectDao.getSubject($subjectId)"
        ) {
            subjectDao.getSubject(subjectId)
        }
    }
}
