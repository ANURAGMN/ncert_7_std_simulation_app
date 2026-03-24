package com.anurag.eduapp.di

import com.anurag.eduapp.data.local.dao.ChapterDao
import com.anurag.eduapp.data.local.dao.ConceptDao
import com.anurag.eduapp.data.local.dao.ProgressDao
import com.anurag.eduapp.data.local.dao.StudentDao
import com.anurag.eduapp.data.local.dao.SubjectDao
import com.anurag.eduapp.repository.ChapterRepository
import com.anurag.eduapp.repository.ConceptRepository
import com.anurag.eduapp.repository.FirebaseRepository
import com.anurag.eduapp.repository.StudentLocalRepository
import com.anurag.eduapp.repository.SubjectRepository
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
}
