package com.ncert7.mathandsciencelab.service.analytics

import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.ncert7.mathandsciencelab.repository.SimulationInteractionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents a single interaction event captured from a simulation.
 *
 * timeTaken  — blank for all rows except the LAST row of a session,
 *              where it holds the total time spent in that simulation (e.g. "47s").
 * timestamp  — only populated on the FIRST row of a session; blank for all subsequent rows.
 */
data class InteractionEvent(
    val simulationTitle: String,
    val subjectName: String,
    val chapterName: String,
    val elementClicked: String,
    val elementType: String,   // "tap", "slider", "input"
    val givenAnswer: String,   // value entered/selected; for tap = the label tapped
    val isCorrect: String,     // "correct", "wrong", or "-" (no verdict for this event)
    val timeTaken: String,     // total session time, only on last row of session; else ""
    val timestamp: String      // HH:mm of session start, only on first row; else ""
)

/**
 * Lightweight singleton to track interaction events within simulations.
 * Temporary in-memory storage — cleared when the app is closed.
 */
object InteractionTracker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var repository: SimulationInteractionRepository? = null

    // --- Session state ---
    private var sessionTitle: String = ""
    private var sessionSubject: String = ""
    private var sessionChapter: String = ""
    private var sessionStartMs: Long = 0L
    private var sessionTimestamp: String = ""   // captured once at startSession()
    private var isFirstEventOfSession: Boolean = false

    // Index in _events where this session's first row sits
    private var sessionStartIndex: Int = -1

    // --- Public event list ---
    private val _events = MutableStateFlow<List<InteractionEvent>>(emptyList())
    val events: StateFlow<List<InteractionEvent>> = _events.asStateFlow()

    // --- Total count ---
    private val _totalInteractions = MutableStateFlow(0)
    val totalInteractions: StateFlow<Int> = _totalInteractions.asStateFlow()

    fun initialize(simulationInteractionRepository: SimulationInteractionRepository) {
        repository = simulationInteractionRepository
        DebugLogger.debugLog("InteractionTracker", "Initialized")
    }

    /**
     * Call when a simulation opens.
     * If a previous session is still open (user navigated directly to a new simulation
     * without pressing back), close it first so its total time is stamped correctly.
     * Resets session state and records start time + timestamp for the first row.
     */
    fun startSession(
        simulationTitle: String,
        subjectName: String,
        chapterName: String
    ) {
        // Auto-close any in-progress session before starting the new one
        if (sessionStartMs > 0L) {
            endSession()
        }
        sessionTitle = simulationTitle
        sessionSubject = subjectName
        sessionChapter = chapterName
        sessionStartMs = System.currentTimeMillis()
        sessionTimestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        isFirstEventOfSession = true
        sessionStartIndex = _events.value.size  // next event added will be the first of this session
    }

    /**
     * Call when the user navigates back from a simulation.
     * Stamps the total time spent into the last row of this session.
     */
    fun endSession() {
        if (sessionStartMs <= 0L) return

        val current = _events.value
        if (current.isEmpty()) return

        // Find the last event that belongs to this session
        val lastIndex = current.indexOfLast {
            it.simulationTitle == sessionTitle &&
                    it.subjectName == sessionSubject &&
                    it.chapterName == sessionChapter
        }
        if (lastIndex < 0) return

        val elapsedSeconds = ((System.currentTimeMillis() - sessionStartMs) / 1000).coerceAtLeast(0)
        val elapsedTime = "${elapsedSeconds}s"
        val updated = current.toMutableList()
        updated[lastIndex] = updated[lastIndex].copy(timeTaken = elapsedTime)
        _events.value = updated
        repository?.let { repo ->
            scope.launch {
                repo.updateLatestSessionTime(
                    simulationTitle = sessionTitle,
                    subjectName = sessionSubject,
                    chapterName = sessionChapter,
                    timeTaken = elapsedTime
                )
            }
        }
        sessionStartMs = 0L  // mark session as closed so startSession() doesn't re-close it
    }

    /**
     * Called from the JS bridge whenever the user interacts with a simulation element.
     *
     * Name patterns:
     *   "Slider [label] set to: value"  → type=slider, answer=value
     *   "Entered [label]: value"         → type=input,  answer=value
     *   anything else                    → type=tap,    answer=the label itself
     */
    fun logInteraction(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) return

        val elementType: String
        val elementClicked: String
        val givenAnswer: String

        when {
            name.startsWith("Slider [") -> {
                elementType = "slider"
                val labelEnd = name.indexOf(']')
                elementClicked = if (labelEnd > 8) name.substring(8, labelEnd) else name
                val setToIndex = name.indexOf("set to: ")
                givenAnswer = if (setToIndex >= 0) name.substring(setToIndex + 8) else ""
            }
            name.startsWith("Entered [") -> {
                elementType = "input"
                val labelEnd = name.indexOf(']')
                elementClicked = if (labelEnd > 9) name.substring(9, labelEnd) else name
                val colonIndex = name.indexOf("]: ")
                givenAnswer = if (colonIndex >= 0) name.substring(colonIndex + 3) else ""
            }
            else -> {
                elementType = "tap"
                elementClicked = name
                givenAnswer = name
            }
        }

        // Timestamp: only on the first event of this session, blank on all others
        val timestamp = if (isFirstEventOfSession) {
            isFirstEventOfSession = false
            sessionTimestamp
        } else {
            ""
        }

        val event = InteractionEvent(
            simulationTitle = sessionTitle,
            subjectName = sessionSubject,
            chapterName = sessionChapter,
            elementClicked = elementClicked,
            elementType = elementType,
            givenAnswer = givenAnswer,
            isCorrect = "-",
            timeTaken = "",  // filled by endSession() on the last row only
            timestamp = timestamp
        )

        _totalInteractions.value += 1
        _events.value = _events.value + event
        repository?.let { repo ->
            scope.launch {
                repo.saveInteraction(event)
            }
        }
    }

    /**
     * Called from the JS bridge when verdict text appears in the simulation DOM.
     * Finds the most recent event with no verdict and stamps it.
     * No-op if no pending event exists.
     */
    fun logVerdict(isCorrect: Boolean) {
        val current = _events.value
        val lastPendingIndex = current.indexOfLast { it.isCorrect == "-" }
        if (lastPendingIndex < 0) return

        val verdict = if (isCorrect) "correct" else "wrong"
        val updated = current.toMutableList()
        updated[lastPendingIndex] = updated[lastPendingIndex].copy(isCorrect = verdict)
        _events.value = updated
        repository?.let { repo ->
            scope.launch {
                repo.updateLatestPendingVerdict(verdict)
            }
        }
    }
}
