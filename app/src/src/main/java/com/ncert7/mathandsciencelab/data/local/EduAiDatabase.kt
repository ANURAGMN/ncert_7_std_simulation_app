package com.ncert7.mathandsciencelab.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ncert7.mathandsciencelab.data.local.entities.AppAnalyticsEntity
import com.ncert7.mathandsciencelab.data.local.entities.SessionEntity
import com.ncert7.mathandsciencelab.data.local.dao.AppAnalyticsDao
import com.ncert7.mathandsciencelab.data.local.dao.ChapterDao
import com.ncert7.mathandsciencelab.data.local.dao.ConceptDao
import com.ncert7.mathandsciencelab.data.local.dao.ProgressDao
import com.ncert7.mathandsciencelab.data.local.dao.SessionDao
import com.ncert7.mathandsciencelab.data.local.dao.SimulationInteractionDao
import com.ncert7.mathandsciencelab.data.local.dao.StudentDao
import com.ncert7.mathandsciencelab.data.local.dao.SubjectDao
import com.ncert7.mathandsciencelab.data.local.dao.StreakDao
import com.ncert7.mathandsciencelab.data.local.entities.ChapterEntity
import com.ncert7.mathandsciencelab.data.local.entities.ConceptEntity
import com.ncert7.mathandsciencelab.data.local.entities.ProgressEntity
import com.ncert7.mathandsciencelab.data.local.entities.SimulationInteractionEntity
import com.ncert7.mathandsciencelab.data.local.entities.StudentEntity
import com.ncert7.mathandsciencelab.data.local.entities.SubjectEntity
import com.ncert7.mathandsciencelab.data.local.entities.StreakEntity
import com.ncert7.mathandsciencelab.data.local.migrations.Migration_1_To_2
import com.ncert7.mathandsciencelab.data.local.migrations.Migration_1_To_3
import com.ncert7.mathandsciencelab.data.local.migrations.Migration_2_To_3
import com.ncert7.mathandsciencelab.data.local.migrations.Migration_3_To_4

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
        StreakEntity::class,
        SimulationInteractionEntity::class,
    ],
    version = 4,
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
    abstract fun streakDao(): StreakDao
    abstract fun simulationInteractionDao(): SimulationInteractionDao

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
                    .addMigrations(
                        Migration_1_To_2,      // v1 → v2 (users on 1.0 → 1.0.1)
                        Migration_1_To_3,      // v1 → v3 direct path (users on 1.0 → 1.0.2)
                        Migration_2_To_3,
                        Migration_3_To_4
                    )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
