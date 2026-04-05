package com.ncert7.mathandsciencelab.ui.screens.subjectscreen.dataclass

import com.ncert7.mathandsciencelab.ui.models.SubjectUiModel

data class SubjectScreenState(
    val subjects: List<SubjectUiModel> = emptyList(),
    val classLevel: Int = 7,
    val isLoading: Boolean = false,
    val error: String? = null
)
