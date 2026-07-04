package com.ncert7.mathandsciencelab.di

import com.ncert7.mathandsciencelab.data.local.dao.ChapterDao
import com.ncert7.mathandsciencelab.data.local.dao.ConceptDao
import com.ncert7.mathandsciencelab.data.local.dao.ProgressDao
import com.ncert7.mathandsciencelab.data.local.dao.SimulationInteractionDao
import com.ncert7.mathandsciencelab.data.local.dao.StudentDao
import com.ncert7.mathandsciencelab.data.local.dao.SubjectDao
import com.ncert7.mathandsciencelab.data.local.dao.StreakDao
import com.ncert7.mathandsciencelab.data.local.SharedPreferenceUtils
import com.ncert7.mathandsciencelab.repository.ChapterRepository
import com.ncert7.mathandsciencelab.repository.ConceptRepository
import com.ncert7.mathandsciencelab.repository.FirebaseRepository
import com.ncert7.mathandsciencelab.repository.SimulationInteractionRepository
import com.ncert7.mathandsciencelab.repository.StudentLocalRepository
import com.ncert7.mathandsciencelab.repository.SubjectRepository
import com.ncert7.mathandsciencelab.repository.StreakRepository
import com.ncert7.mathandsciencelab.utils.StreakManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideConceptRepository(
        conceptDao: ConceptDao,
        progressDao: ProgressDao
    ): ConceptRepository {
        return ConceptRepository(conceptDao, progressDao)
    }

    @Provides
    @Singleton
    fun provideChapterRepository(
        chapterDao: ChapterDao,
        progressDao: ProgressDao
    ): ChapterRepository {
        return ChapterRepository(chapterDao, progressDao)
    }

    @Provides
    @Singleton
    fun provideSubjectRepository(subjectDao: SubjectDao): SubjectRepository {
        return SubjectRepository(subjectDao)
    }

    @Provides
    @Singleton
    fun provideStudentRepository(studentDao: StudentDao): StudentLocalRepository {
        return StudentLocalRepository(studentDao)
    }

    @Provides
    @Singleton
    fun provideFirebaseRepository(): FirebaseRepository {
        return FirebaseRepository()
    }

    @Provides
    @Singleton
    fun provideStreakRepository(
        streakDao: StreakDao,
        firebaseRepository: FirebaseRepository
    ): StreakRepository {
        return StreakRepository(streakDao, firebaseRepository)
    }

    @Provides
    @Singleton
    fun provideSimulationInteractionRepository(
        simulationInteractionDao: SimulationInteractionDao,
        sharedPreferenceUtils: SharedPreferenceUtils
    ): SimulationInteractionRepository {
        return SimulationInteractionRepository(simulationInteractionDao, sharedPreferenceUtils)
    }

    @Provides
    @Singleton
    fun provideStreakManager(
        streakRepository: StreakRepository,
        sharedPreferenceUtils: SharedPreferenceUtils
    ): StreakManager {
        // Note: userId will be available when user logs in
        // This provides a default instance; actual userId should be injected when available
        return StreakManager(streakRepository, sharedPreferenceUtils.getUserId() ?: "")
    }
}
