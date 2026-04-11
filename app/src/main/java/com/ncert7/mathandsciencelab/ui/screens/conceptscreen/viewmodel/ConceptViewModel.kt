package com.ncert7.mathandsciencelab.ui.screens.conceptscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.mathandsciencelab.data.local.SharedPreferenceUtils
import com.ncert7.mathandsciencelab.data.local.entities.ProgressEntity
import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.ncert7.mathandsciencelab.repository.ChapterRepository
import com.ncert7.mathandsciencelab.repository.ConceptRepository
import com.ncert7.mathandsciencelab.repository.StudentLocalRepository
import com.ncert7.mathandsciencelab.repository.SubjectRepository
import com.ncert7.mathandsciencelab.data.model.ProgressStatus
import com.ncert7.mathandsciencelab.ui.models.ConceptUiModel
import com.ncert7.mathandsciencelab.ui.screens.conceptscreen.dataclass.ConceptScreenState
import com.ncert7.mathandsciencelab.utils.getLocalizedName
import com.ncert7.mathandsciencelab.utils.buildProgressUiModel
import com.ncert7.mathandsciencelab.utils.isKannada
import com.ncert7.mathandsciencelab.service.sync.DataSyncService
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

    private val _showAdBeforeSimulation = MutableStateFlow(false)
    val showAdBeforeSimulation: StateFlow<Boolean> = _showAdBeforeSimulation.asStateFlow()

    private val _simulationTitle = MutableStateFlow("")
    val simulationTitle: StateFlow<String> = _simulationTitle.asStateFlow()

    private val _simulationUrl = MutableStateFlow("")
    val simulationUrl: StateFlow<String> = _simulationUrl.asStateFlow()
    
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
     * Track that a simulation has completed
     * Called when the simulation WebView finishes loading successfully
     * Sets status to COMPLETED and records both startedAt and completedAt times
     * This simplifies the flow - when simulation loads, it's considered completed by the user
     */
    fun markSimulationCompleted(conceptId: String) {
        viewModelScope.launch {
            try {
                val studentId = sharedPrefs.getUserId() ?: ""
                val currentTime = System.currentTimeMillis()
                DebugLogger.debugLog("ConceptViewModel", "markSimulationCompleted called for conceptId: $conceptId, studentId: $studentId")

                if (studentId.isNotEmpty() && conceptId.isNotEmpty()) {
                    conceptRepository.updateProgressStatus(
                        studentId = studentId,
                        itemType = "CONCEPT",
                        itemId = conceptId,
                        newStatus = "COMPLETED",
                        progressPercentage = 100,
                        timestamp = currentTime
                    )
                    DebugLogger.debugLog("ConceptViewModel", "✅ Simulation marked as COMPLETED for concept: $conceptId at $currentTime")

                    // Trigger real-time sync to Firestore
                    // Get the progress ID from the database to sync
                    val progress = conceptRepository.getProgress(studentId, "CONCEPT", conceptId)
                    if (progress != null) {
                        DataSyncService.syncProgressUpdate(progress.progressId, studentId)
                    }
                } else {
                    DebugLogger.errorLog("ConceptViewModel", "❌ Failed to mark simulation completed - studentId: $studentId, conceptId: $conceptId")
                }
            } catch (e: Exception) {
                DebugLogger.errorLog("ConceptViewModel", "❌ Error marking simulation completed: ${e.message} | ${e.stackTraceToString()}")
            }
        }
    }

    /**
     * Check if ad should be shown before viewing a simulation
     * Shows ad AFTER the first 3 simulations are completed
     * 1st, 2nd, 3rd simulations = NO AD
     * 4th simulation onwards = ALWAYS SHOW AD
     * Returns true if ad should be shown before simulation
     */
    suspend fun shouldShowAdBeforeSimulation(): Boolean {
        return try {
            val studentId = sharedPrefs.getUserId() ?: ""
            if (studentId.isEmpty()) return false

            val totalCompleted = conceptRepository.getTotalCompletedSimulations(studentId)

            // Show ad if user has completed 3 or more simulations (4th onwards)
            // This means: 1st 3 simulations = no ad, 4th onwards = always show ad
            val shouldShow = totalCompleted >= 3

            DebugLogger.debugLog(
                "ConceptViewModel",
                "shouldShowAdBeforeSimulation: $shouldShow | Total simulations completed: $totalCompleted"
            )

            shouldShow
        } catch (e: Exception) {
            DebugLogger.errorLog("ConceptViewModel", "Error checking ad before simulation: ${e.message}")
            false
        }
    }

    /**
     * Initialize ad display when entering simulation viewer
     *
     * @param conceptId The ID of the concept
     * @param simulationUrl Optional pre-computed URL (if provided, skips state search)
     * @param simulationTitle Optional title (if provided, uses this instead of searching state)
     *
     * If URL/title are not provided, searches in ViewModel state
     * If provided, uses them directly (useful for PracticeSimulationCard which has data ready)
     */
    fun initializeSimulationWithAdCheck(
        conceptId: String,
        simulationUrl: String? = null,
        simulationTitle: String? = null
    ) {
        viewModelScope.launch {
            try {
                if (simulationUrl != null && simulationTitle != null) {
                    _simulationTitle.value = simulationTitle
                    _simulationUrl.value = simulationUrl

                    DebugLogger.debugLog(
                        "ConceptViewModel",
                        "initializeSimulationWithAdCheck (external data) for $conceptId: title=$simulationTitle, url=$simulationUrl"
                    )
                } else {
                    val concept = _state.value.concepts.find { it.id == conceptId }
                    _simulationTitle.value = concept?.name ?: "Simulation"

                    if (concept == null) {
                        DebugLogger.errorLog("ConceptViewModel", "Concept not found in state for ID: $conceptId")
                        _simulationUrl.value = ""
                        return@launch
                    }

                    val selectedUrl = getSelectedSimulationUrl(concept.simulationUrl,concept.simulationUrlKannada)
                    _simulationUrl.value = selectedUrl ?: ""

                    DebugLogger.debugLog(
                        "ConceptViewModel",
                        "initializeSimulationWithAdCheck (state search) for $conceptId: title=${_simulationTitle.value}, url=${_simulationUrl.value}"
                    )
                }

                // Check if ad should be shown
                val shouldShowAd = shouldShowAdBeforeSimulation()
                _showAdBeforeSimulation.value = shouldShowAd

                DebugLogger.debugLog(
                    "ConceptViewModel",
                    "Ad check result: shouldShowAd=$shouldShowAd"
                )
            } catch (e: Exception) {
                DebugLogger.errorLog("ConceptViewModel", "Error initializing simulation ad: ${e.message} | ${e.stackTraceToString()}")
                _showAdBeforeSimulation.value = false
            }
        }
    }

    /**
     * Dismiss the ad and allow simulation to load
     */
    fun dismissAd() {
        _showAdBeforeSimulation.value = false
        DebugLogger.debugLog("ConceptViewModel", "Ad dismissed, showing simulation")
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