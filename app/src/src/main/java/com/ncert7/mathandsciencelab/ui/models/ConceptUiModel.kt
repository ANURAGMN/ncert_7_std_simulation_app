package com.ncert7.mathandsciencelab.ui.models

import androidx.annotation.StringRes
import com.ncert7.mathandsciencelab.R
import com.ncert7.mathandsciencelab.data.model.ProgressStatus

/**
 * UI Model for Concept data
 */
data class ConceptUiModel(
    val id: String,
    val name: String,
    val order: Int,
    val status: ProgressStatus,
    val type: String = "study",
    val simulationUrl: String? = null,
    val simulationUrlKannada: String? = null,
    val simulationId: String? = null,
    val isSimulation: Boolean = false,
    val simulationButtonUrl: String? = null
) {
    /**
     * Computed property: Maps status to string resource ID
     * No warning because it's derived, not a parameter
     */
    @get:StringRes
    val statusTextResId: Int
        get() = when (status) {
            ProgressStatus.COMPLETED -> R.string.completed
            ProgressStatus.IN_PROGRESS -> R.string.in_progress_continue_learning
            ProgressStatus.NOT_STARTED -> R.string.start_learning
            ProgressStatus.LOCKED -> R.string.start_learning
        }
}

