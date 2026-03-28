package com.anurag.eduapp.ui.screens.conceptscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anurag.eduapp.data.local.SharedPreferenceUtils
import com.anurag.eduapp.data.local.entities.ProgressEntity
import com.anurag.eduapp.debug.DebugLogger
import com.anurag.eduapp.repository.ChapterRepository
import com.anurag.eduapp.repository.ConceptRepository
import com.anurag.eduapp.repository.StudentLocalRepository
import com.anurag.eduapp.repository.SubjectRepository
import com.anurag.eduapp.data.model.ProgressStatus
import com.anurag.eduapp.ui.models.ConceptUiModel
import com.anurag.eduapp.ui.screens.conceptscreen.dataclass.ConceptScreenState
import com.anurag.eduapp.utils.getLocalizedName
import com.anurag.eduapp.utils.buildProgressUiModel
import com.anurag.eduapp.utils.isKannada
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.count
import kotlin.collections.isNotEmpty
import kotlin.collections.mapIndexed
import kotlin.let

@HiltViewModel
class ConceptViewModel @Inject constructor(
    private val conceptRepository: ConceptRepository,
    private val chapterRepository: ChapterRepository,
    private val subjectRepository: SubjectRepository,
    private val studentRepository: StudentLocalRepository,
    private val sharedPrefs: SharedPreferenceUtils
) : ViewModel() {

    private val _state = MutableStateFlow(ConceptScreenState())
    val state: StateFlow<ConceptScreenState> = _state.asStateFlow()

    fun loadConcepts(chapterId: String, type: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val concepts = conceptRepository.getConceptsForChapter(chapterId, type)
                val chapter = chapterRepository.getChapter(chapterId)
                val studentId = sharedPrefs.getUserId() ?: ""

                // Get subject and class level information
                val subject = chapter?.let { subjectRepository.getSubject(it.subjectId) }
                val student = studentRepository.getStudentSync(studentId)
                val classLevel = student?.classLevel ?: 7

                // Convert to UI models with status
                val conceptUiModels = concepts.mapIndexed { index, concept ->
                    val progress = conceptRepository.getProgress(
                        studentId = studentId,
                        itemType = "CONCEPT",
                        itemId = concept.conceptId
                    )

                    // Determine status with sequential unlocking logic
                    val status = determineConceptStatus(
                        progress = progress,
                        isFirstConcept = index == 0,
                        previousConceptStatus = if (index > 0) {
                            conceptRepository.getProgress(
                                studentId = studentId,
                                itemType = "CONCEPT",
                                itemId = concepts[index - 1].conceptId
                            )?.status
                        } else null
                    )

                    val progressStatus = when (status) {
                        "COMPLETED" -> ProgressStatus.COMPLETED
                        "IN_PROGRESS", "STARTED" -> ProgressStatus.IN_PROGRESS
                        else -> ProgressStatus.NOT_STARTED
                    }

                    // Compute other properties in ViewModel
                    val isSimulation = concept.type.equals("SIMULATION", ignoreCase = true)
                    val simulationButtonUrl = getSelectedSimulationUrl(
                        concept.simulationUrl,
                        concept.simulationUrlKannada
                    )

                    ConceptUiModel(
                        id = concept.conceptId,
                        name = concept.getLocalizedName(),
                        order = concept.orderIndex,
                        status = progressStatus,
                        type = concept.type,
                        simulationUrl = concept.simulationUrl,
                        simulationUrlKannada = concept.simulationUrlKannada,
                        simulationId = concept.simulationId,
                        isSimulation = isSimulation,
                        simulationButtonUrl = simulationButtonUrl
                    )
                }

                // Auto-unlock first concept if not started
                if (conceptUiModels.isNotEmpty() &&
                    conceptUiModels[0].status == ProgressStatus.NOT_STARTED) {
                    unlockFirstConcept(studentId, conceptUiModels[0].id)
                }

                // Filter concepts with simulation URLs (visible concepts)
                // IMPORTANT: This filtering MUST match ConceptScreen filtering exactly
                val visibleConcepts = conceptUiModels.filter { concept ->
                    (!concept.simulationUrl.isNullOrBlank() && concept.simulationUrl != "Not found") ||
                    (!concept.simulationUrlKannada.isNullOrBlank() && concept.simulationUrlKannada != "Not found")
                }

                // Count completed visible concepts
                val completedCount = visibleConcepts.count { it.status == ProgressStatus.COMPLETED }
                val totalCount = visibleConcepts.size  // Only visible concepts

                // Log for debugging - shows actual visible count
                DebugLogger.debugLog(
                    "ConceptViewModel",
                    "Chapter: $chapterId | Total Concepts: ${conceptUiModels.size} | Visible with Simulation: $totalCount | Completed: $completedCount"
                )

                // progress UI model
                val progressUiModel = buildProgressUiModel(
                    completed = completedCount,
                    total = totalCount
                )

                _state.value = _state.value.copy(
                    concepts = conceptUiModels,
                    chapterName = chapter?.getLocalizedName() ?: "",
                    chapterId = chapterId,
                    type = type,
                    progressUiModel = progressUiModel,
                    subjectName = subject?.getLocalizedName() ?: "",
                    classLevel = classLevel,
                    isLoading = false,
                    error = null
                )

                DebugLogger.debugLog("ConceptViewModel", "Loaded ${conceptUiModels.size} concepts")
            } catch (e: Exception) {
                DebugLogger.debugLog("ConceptViewModel", "Error: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun determineConceptStatus(
        progress: ProgressEntity?,
        isFirstConcept: Boolean,
        previousConceptStatus: String?
    ): String {
        // If progress exists, use its status
        if (progress != null) {
            return progress.status
        }

        // First concept is always unlocked (IN_PROGRESS)
        if (isFirstConcept) {
            return "IN_PROGRESS"
        }

        // Unlock next concept only if previous is completed
        if (previousConceptStatus == "COMPLETED") {
            return "IN_PROGRESS"
        }

        // Otherwise, keep locked
        return "NOT_STARTED"
    }

    private suspend fun unlockFirstConcept(studentId: String, conceptId: String) {
        try {
            conceptRepository.updateProgressStatus(
                studentId = studentId,
                itemType = "CONCEPT",
                itemId = conceptId,
                newStatus = "IN_PROGRESS",
                progressPercentage = 0,
                timestamp = System.currentTimeMillis()
            )
            DebugLogger.debugLog("ConceptViewModel", "First concept unlocked: $conceptId")
        } catch (e: Exception) {
            DebugLogger.debugLog("ConceptViewModel", "Error unlocking first concept: ${e.message}")
        }
    }

    /**
     * Track that a concept's simulation was viewed
     * Called when user opens and views a simulation
     */
    fun markSimulationViewed(conceptId: String) {
        viewModelScope.launch {
            try {
                val studentId = sharedPrefs.getUserId() ?: ""
                if (studentId.isNotEmpty() && conceptId.isNotEmpty()) {
                    conceptRepository.updateProgressStatus(
                        studentId = studentId,
                        itemType = "CONCEPT",
                        itemId = conceptId,
                        newStatus = "COMPLETED",
                        progressPercentage = 100,
                        timestamp = System.currentTimeMillis()
                    )
                    DebugLogger.debugLog("ConceptViewModel", "Simulation viewed for concept: $conceptId")
                }
            } catch (e: Exception) {
                DebugLogger.errorLog("ConceptViewModel", "Error marking simulation viewed: ${e.message}")
            }
        }
    }

    /**
     * Selects the appropriate simulation URL based on device language preference
     * Prioritizes Kannada URL if available and device language is Kannada, otherwise uses English URL
     */
    private fun getSelectedSimulationUrl(
        englishUrl: String?,
        kannadaUrl: String?
    ): String? {
        // Select URL based on current app language
        val selectedUrl = if (isKannada()) {
            // Use Kannada URL if available, fallback to English URL
            kannadaUrl?.takeIf { it.isNotBlank() } ?: englishUrl
        } else {
            // Use English URL
            englishUrl
        }

        // Validate URL is not empty or placeholder
        return selectedUrl?.takeIf { it.isNotBlank() && it != "Not found" }
    }
}