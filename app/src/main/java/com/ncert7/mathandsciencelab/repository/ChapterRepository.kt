package com.ncert7.mathandsciencelab.repository

import com.ncert7.mathandsciencelab.data.local.dao.ChapterDao
import com.ncert7.mathandsciencelab.data.local.dao.ChapterProgressSummary
import com.ncert7.mathandsciencelab.data.local.dao.ProgressDao
import com.ncert7.mathandsciencelab.data.local.entities.ChapterEntity
import com.ncert7.mathandsciencelab.utils.RetryHelper

/**
 * Repository class for managing chapter data and related progress.
 */
class ChapterRepository(
    private val chapterDao: ChapterDao,
    private val progressDao: ProgressDao
) {
    /**
     * Retrieves all chapters for a given subject ID.
     * returns List of ChapterEntity
     */
    suspend fun getChaptersForSubject(subjectId: String): List<ChapterEntity> {
        return RetryHelper.executeWithRetryList(
            maxRetries = 3,
            functionName = "ChapterDao.getChaptersForSubjectSync($subjectId)"
        ) {
            chapterDao.getChaptersForSubjectSync(subjectId)
        }
    }

    /**
     * Retrieves a specific chapter by its ID.
     * returns ChapterEntity or null if not found
     */
    suspend fun getChapter(chapterId: String): ChapterEntity? {
        return RetryHelper.executeWithRetry(
            maxRetries = 3,
            functionName = "ChapterDao.getChapter($chapterId)"
        ) {
            chapterDao.getChapter(chapterId)
        }
    }

    /**
     * Retrieves chapter-wise progress for a student in a specific subject and class level.
     * returns List of ChapterProgressSummary
     */
    suspend fun getChapterWiseProgress(
        studentId: String,
        classLevel: Int,
        subjectId: String
    ): List<ChapterProgressSummary> {
        return RetryHelper.executeWithRetryList(
            maxRetries = 3,
            functionName = "ProgressDao.getChapterWiseProgress($studentId, $classLevel, $subjectId)"
        ) {
            progressDao.getChapterWiseProgress(studentId, classLevel, subjectId)
        }
    }
}

