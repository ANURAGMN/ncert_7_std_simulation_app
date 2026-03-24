package com.anurag.eduapp.ui.screens.subjectscreen.dataclass

import com.anurag.eduapp.ui.models.SubjectUiModel

data class SubjectScreenState(
    val subjects: List<SubjectUiModel> = emptyList(),
    val classLevel: Int = 7,
    val isLoading: Boolean = false,
    val error: String? = null
)
