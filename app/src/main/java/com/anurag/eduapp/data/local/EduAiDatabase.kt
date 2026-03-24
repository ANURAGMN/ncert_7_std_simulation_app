package com.anurag.eduapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.anurag.eduai.data.local.entities.AppAnalyticsEntity
import com.anurag.eduai.data.local.entities.SessionEntity
import com.anurag.eduapp.data.local.dao.AppAnalyticsDao
import com.anurag.eduapp.data.local.dao.ChapterDao
import com.anurag.eduapp.data.local.dao.ConceptDao
import com.anurag.eduapp.data.local.dao.ProgressDao
import com.anurag.eduapp.data.local.dao.SessionDao
import com.anurag.eduapp.data.local.dao.StudentDao
import com.anurag.eduapp.data.local.dao.SubjectDao
import com.anurag.eduapp.data.local.entities.ChapterEntity
import com.anurag.eduapp.data.local.entities.ConceptEntity
import com.anurag.eduapp.data.local.entities.ProgressEntity
import com.anurag.eduapp.data.local.entities.StudentEntity
import com.anurag.eduapp.data.local.entities.SubjectEntity

/**
 * Main Room Database for EduAi App
 */
@Database(
    entities = [
        StudentEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        ConceptEntity::class,
        SessionEntity::class,
        AppAnalyticsEntity::class,
        ProgressEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class EduAiDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun conceptDao(): ConceptDao
    abstract fun progressDao(): ProgressDao
    abstract fun sessionDao(): SessionDao
    abstract fun appAnalyticsDao(): AppAnalyticsDao

    companion object {
        @Volatile
        private var INSTANCE: EduAiDatabase? = null

        private const val DATABASE_NAME = "eduai_database"

        fun getInstance(context: Context): EduAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EduAiDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}