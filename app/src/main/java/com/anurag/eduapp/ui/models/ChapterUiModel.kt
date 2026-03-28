package com.anurag.eduapp.ui.models

import com.anurag.eduapp.data.model.ProgressStatus
/**
 * UI Model for Chapter data
 */
data class ChapterUiModel(
    val id: String,
    val name: String,  // Localized name for display (English or Kannada)
    val englishName: String,  // English name for API calls (always English)
    val orderIndex:Int,
    val totalConcepts: Int,
    val completedConcepts: Int,
    val status: ProgressStatus,
    val progressUiModel: ChapterProgressUiModel? = null
)

