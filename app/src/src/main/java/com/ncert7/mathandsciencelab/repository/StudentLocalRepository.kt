package com.ncert7.mathandsciencelab.repository

import com.ncert7.mathandsciencelab.data.local.dao.StudentDao
import com.ncert7.mathandsciencelab.data.local.entities.StudentEntity
import com.ncert7.mathandsciencelab.utils.RetryHelper

class StudentLocalRepository(
    private val dao: StudentDao
) {

    suspend fun saveStudentLocally(student: StudentEntity) {
        dao.insertStudent(student)
    }

    suspend fun getStudentSync(studentId: String): StudentEntity? {
        return RetryHelper.executeWithRetry(
            maxRetries = 3,
            initialDelayMs = 100L,
            functionName = "StudentDao.getStudentSync($studentId)"
        ) {
            dao.getStudentSync(studentId)
        }
    }
}

