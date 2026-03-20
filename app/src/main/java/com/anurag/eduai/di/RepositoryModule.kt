package com.anurag.eduai.di

import com.anurag.eduai.data.local.dao.ChapterDao
import com.anurag.eduai.data.local.dao.ConceptDao
import com.anurag.eduai.data.local.dao.StudentDao
import com.anurag.eduai.data.local.dao.SubjectDao
import com.anurag.eduai.repository.ChapterRepository
import com.anurag.eduai.repository.ConceptRepository
import com.anurag.eduai.repository.StudentLocalRepository
import com.anurag.eduai.repository.SubjectRepository
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
    ): ConceptRepository {
        return ConceptRepository(conceptDao)
    }

    @Provides
    @Singleton
    fun provideChapterRepository(
        chapterDao: ChapterDao,
    ): ChapterRepository {
        return ChapterRepository(chapterDao)
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
}
