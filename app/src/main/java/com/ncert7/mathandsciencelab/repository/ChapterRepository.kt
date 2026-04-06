package com.ncert7.mathandsciencelab.repository

import com.ncert7.mathandsciencelab.data.local.dao.ChapterDao
import com.ncert7.mathandsciencelab.data.local.dao.ChapterProgressSummary
import com.ncert7.mathandsciencelab.data.local.dao.ProgressDao
import com.ncert7.mathandsciencelab.data.local.entities.ChapterEntity
import com.ncert7.mathandsciencelab.utils.DatabaseRetryHelper

/**
 * Repository class for managing chapter data and related progress.
 */
class ChapterRepository(
    private val chapterDao: ChapterDao,
    private val progressDao: ProgressDao
) {
    /**
     * Retrieves all chapters for a given subject ID.
     * Retries only if there's an actual failure.
     * returns List of ChapterEntity
     */
    suspend fun getChaptersForSubject(subjectId: String): List<ChapterEntity> {
        return DatabaseRetryHelper.retryIfFails(maxRetries = 3) {
            chapterDao.getChaptersForSubjectSync(subjectId)
        }
    }

    /**
     * Retrieves a specific chapter by its ID.
     * Retries only if there's an actual failure.
     * returns ChapterEntity or null if not found
     */
    suspend fun getChapter(chapterId: String): ChapterEntity? {
        return DatabaseRetryHelper.retryIfFailsNullable(maxRetries = 3) {
            chapterDao.getChapter(chapterId)
        }
    }

    /**
     * Retrieves chapter-wise progress for a student in a specific subject and class level.
     * Retries only if there's an actual failure.
     * returns List of ChapterProgressSummary
     */
    suspend fun getChapterWiseProgress(
        studentId: String,
        classLevel: Int,
        subjectId: String
    ): List<ChapterProgressSummary> {
        return DatabaseRetryHelper.retryIfFails(maxRetries = 3) {
            progressDao.getChapterWiseProgress(studentId, classLevel, subjectId)
        }
    }
}
