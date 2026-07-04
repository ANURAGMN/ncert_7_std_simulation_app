package com.ncert7.mathandsciencelab.ui.screens.conceptscreen.dataclass

import com.ncert7.mathandsciencelab.data.local.SharedPreferenceUtils
import com.ncert7.mathandsciencelab.ui.models.ChapterProgressUiModel
import com.ncert7.mathandsciencelab.ui.models.ConceptUiModel


data class ConceptScreenState(
    val concepts: List<ConceptUiModel> = emptyList(),
    val visibleConcepts: List<ConceptUiModel> = emptyList(), // Filtered based on current language
    val chapterName: String = "",
    val chapterId: String = "",
    val progressUiModel: ChapterProgressUiModel = ChapterProgressUiModel(
        completed = 0,
        total = 0,
        progressFraction = 0f,
        progressPercentage = 0,
        remaining = 0
    ),
    val type :String = "",
    val subjectName: String = "",
    val classLevel: Int = SharedPreferenceUtils.CLASS_LEVEL,
    val isLoading: Boolean = false,
    val error: String? = null
)
