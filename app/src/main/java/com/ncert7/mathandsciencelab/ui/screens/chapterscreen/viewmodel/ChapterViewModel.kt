package com.ncert7.mathandsciencelab.ui.screens.chapterscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.mathandsciencelab.data.local.SharedPreferenceUtils
import com.ncert7.mathandsciencelab.repository.ChapterRepository
import com.ncert7.mathandsciencelab.repository.StudentLocalRepository
import com.ncert7.mathandsciencelab.repository.SubjectRepository
import com.ncert7.mathandsciencelab.data.model.ProgressStatus
import com.ncert7.mathandsciencelab.ui.models.ChapterUiModel
import com.ncert7.mathandsciencelab.ui.screens.chapterscreen.dataclass.ChapterUiState
import com.ncert7.mathandsciencelab.utils.getLocalizedName
import com.ncert7.mathandsciencelab.utils.buildProgressUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.associateBy

@HiltViewModel
class ChapterViewModel @Inject constructor(
    private val chapterRepository: ChapterRepository,
    private val subjectRepository: SubjectRepository,
    private val studentRepository: StudentLocalRepository,
    private val sharedPrefs: SharedPreferenceUtils
) : ViewModel() {

    private val _state = MutableStateFlow(ChapterUiState())
    val state: StateFlow<ChapterUiState> = _state.asStateFlow()

    fun loadChapters(subjectId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // Get student ID and class level
                val userId = sharedPrefs.getUserId() ?: ""
                val student = studentRepository.getStudentSync(userId)
                val classLevel = student?.classLevel ?: 7

                // Fetch data from repositories
                val chapters = chapterRepository.getChaptersForSubject(subjectId)
                val subject = subjectRepository.getSubject(subjectId)
                val progressList = chapterRepository.getChapterWiseProgress(
                    studentId = userId,
                    classLevel = classLevel,
                    subjectId = subjectId
                )

                // Convert to map for easy lookup
                val progressMap = progressList.associateBy { it.chapterId }

                // Filter chapters to only show those with simulation concepts that have valid URLs
                val chapterUiModels = chapters.mapNotNull { chapter ->
                    val progress = progressMap[chapter.chapterId]
                    val completedConcepts = progress?.completedConcepts ?: 0
                    val totalConcepts = progress?.totalConcepts ?: 0

                    // Skip chapters with no simulation concepts that have valid URLs
                    if (totalConcepts == 0) {
                        return@mapNotNull null
                    }

                    // Determine status based on completion
                    val status = when {
                        completedConcepts == 0 -> ProgressStatus.NOT_STARTED
                        completedConcepts >= totalConcepts -> ProgressStatus.COMPLETED
                        else -> ProgressStatus.IN_PROGRESS
                    }

                    // Compute progress UI model
                    val progressUiModel = buildProgressUiModel(
                        completed = completedConcepts,
                        total = totalConcepts
                    )

                    ChapterUiModel(
                        id = chapter.chapterId,
                        orderIndex = chapter.orderIndex,
                        name = chapter.getLocalizedName(),  // Display name (localized)
                        englishName = chapter.chapterName,  // API name (always English)
                        totalConcepts = totalConcepts,
                        completedConcepts = completedConcepts,
                        status = status,
                        progressUiModel = progressUiModel
                    )
                }

                _state.value = _state.value.copy(
                    chapters = chapterUiModels,
                    subjectName = subject?.getLocalizedName() ?: "",
                    classLevel = classLevel,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}