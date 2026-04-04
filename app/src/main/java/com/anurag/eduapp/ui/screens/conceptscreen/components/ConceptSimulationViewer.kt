package com.anurag.eduapp.ui.screens.conceptscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduapp.debug.DebugLogger
import com.anurag.eduapp.ui.components.AdDialog
import com.anurag.eduapp.ui.screens.conceptscreen.viewmodel.ConceptViewModel
import java.net.URLDecoder

/**
 * ConceptSimulationViewer - Pure UI Component
 * Displays:
 * - Ad dialog (if needed)
 * - Simulation header
 * - WebView with simulation
 *
 * @param conceptId The ID of the concept being viewed (for progress tracking)
 * @param simulationTitle The title of the simulation to display (URL-encoded)
 * @param simulationUrl The URL of the simulation to load (URL-encoded)
 * @param onBackClick Callback when back button is clicked
 * @param viewModel ViewModel for handling ad display and progress tracking
 */
@Composable
fun ConceptSimulationViewer(
    conceptId: String = "",
    simulationTitle: String = "",
    simulationUrl: String = "",
    onBackClick: () -> Unit = {},
    viewModel: ConceptViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val showAdDialog by viewModel.showAdBeforeSimulation.collectAsState()

    // Decode URL-encoded title to show original name (URL stays encoded for web requests)
    val decodedTitle = try {
        URLDecoder.decode(simulationTitle, "UTF-8")
    } catch (e: Exception) {
        simulationTitle
    }

    val decodedUrl = try {
        URLDecoder.decode(simulationUrl, "UTF-8")
    } catch (e: Exception) {
        simulationUrl
    }

    DebugLogger.debugLog(
        "ConceptSimulationViewer",
        "UI Render: conceptId=$conceptId, title=$decodedTitle (encoded: $simulationTitle), url=$decodedUrl, showAd=$showAdDialog"
    )

    LaunchedEffect(conceptId, decodedUrl, decodedTitle) {
        if (conceptId.isNotEmpty() && decodedUrl.isNotEmpty() && decodedTitle.isNotEmpty()) {
            DebugLogger.debugLog(
                "ConceptSimulationViewer",
                "LaunchedEffect: Initializing ad check for conceptId=$conceptId"
            )
            viewModel.initializeSimulationWithAdCheck(
                conceptId = conceptId,
                simulationUrl = decodedUrl,
                simulationTitle = decodedTitle
            )
        }
    }

    // Handle page loaded
    val handlePageLoaded = {
        if (conceptId.isNotEmpty()) {
            viewModel.markSimulationCompleted(conceptId)
            DebugLogger.debugLog("ConceptSimulationViewer", "Simulation page loaded for concept: $conceptId")
        }
    }

    val handleBackClick = {
        onBackClick()
    }

    val handleAdDismiss = {
        viewModel.dismissAd()
    }

    // Show ad dialog if needed
    if (showAdDialog) {
        AdDialog(
            context = context,
            onDismiss = handleAdDismiss
        )
    }

    // Show simulation UI (only when ad is dismissed)
    if (!showAdDialog) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            SimulationHeader(
                title = decodedTitle,
                onBackClick = handleBackClick
            )

            // WebView
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                SimulationWebView(
                    url = decodedUrl,
                    modifier = Modifier.fillMaxSize(),
                    onPageLoaded = handlePageLoaded
                )
            }
        }
    }
}