package com.ncert7.mathandsciencelab.ui.screens.chapterscreen.dataclass

import com.ncert7.mathandsciencelab.data.local.SharedPreferenceUtils
import com.ncert7.mathandsciencelab.ui.models.ChapterProgressUiModel
import com.ncert7.mathandsciencelab.ui.models.ChapterUiModel

/**
 * UI State for Chapter Screen
 */
data class ChapterUiState(
    val isLoading: Boolean = false,
    val classLevel: Int = SharedPreferenceUtils.CLASS_LEVEL,
    val subjectName: String = "",
    val progressUiModel: ChapterProgressUiModel = ChapterProgressUiModel(
        completed = 0,
        total = 0,
        progressFraction = 0f,
        progressPercentage = 0,
        remaining = 0
    ),    val chapters: List<ChapterUiModel> = emptyList(),
    val error: String? = null
)