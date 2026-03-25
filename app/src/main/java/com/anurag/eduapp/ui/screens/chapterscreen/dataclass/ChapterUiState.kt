package com.anurag.eduapp.ui.screens.chapterscreen.dataclass

import com.anurag.eduapp.ui.models.ChapterProgressUiModel
import com.anurag.eduapp.ui.models.ChapterUiModel

/**
 * UI State for Chapter Screen
 */
data class ChapterUiState(
    val isLoading: Boolean = false,
    val classLevel: Int = 7,
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