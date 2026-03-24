package com.anurag.eduapp.repository

import com.anurag.eduapp.data.local.dao.StudentDao
import com.anurag.eduapp.data.local.entities.StudentEntity

class StudentLocalRepository(
    private val dao: StudentDao
) {

    suspend fun saveStudentLocally(student: StudentEntity) {
        dao.insertStudent(student)
    }

    fun getStudent(studentId: String) = dao.getStudent(studentId)

    suspend fun getStudentSync(studentId: String): StudentEntity? {
        return dao.getStudentSync(studentId)
    }
}

