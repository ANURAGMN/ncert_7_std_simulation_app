package com.anurag.eduapp.di

import android.content.Context
import com.anurag.eduapp.data.local.EduAiDatabase
import com.anurag.eduapp.data.local.SharedPreferenceUtils
import com.anurag.eduapp.data.local.dao.ChapterDao
import com.anurag.eduapp.data.local.dao.ConceptDao
import com.anurag.eduapp.data.local.dao.ProgressDao
import com.anurag.eduapp.data.local.dao.StudentDao
import com.anurag.eduapp.data.local.dao.SubjectDao
import com.anurag.eduapp.utils.StreakManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EduAiDatabase {
        return EduAiDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideConceptDao(database: EduAiDatabase): ConceptDao {
        return database.conceptDao()
    }

    @Provides
    @Singleton
    fun provideChapterDao(database: EduAiDatabase): ChapterDao {
        return database.chapterDao()
    }

    @Provides
    @Singleton
    fun provideSubjectDao(database: EduAiDatabase): SubjectDao {
        return database.subjectDao()
    }

    @Provides
    @Singleton
    fun provideStudentDao(database: EduAiDatabase): StudentDao {
        return database.studentDao()
    }

    @Provides
    @Singleton
    fun provideProgressDao(database: EduAiDatabase): ProgressDao {
        return database.progressDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferenceUtils(@ApplicationContext context: Context): SharedPreferenceUtils {
        return SharedPreferenceUtils(context)
    }

    @Provides
    @Singleton
    fun provideStreakManager(@ApplicationContext context: Context): StreakManager {
        return StreakManager(context)
    }
}