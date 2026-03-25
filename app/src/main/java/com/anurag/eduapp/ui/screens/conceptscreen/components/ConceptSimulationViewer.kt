package com.anurag.eduapp.ui.screens.conceptscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduapp.R
import com.anurag.eduapp.ui.screens.conceptscreen.viewmodel.ConceptViewModel
import com.anurag.eduapp.ui.theme.HeaderGradientStart
import com.anurag.eduapp.ui.theme.LocalDimensions
import com.anurag.eduapp.ui.theme.TextOnPrimary

/**
 * ConceptSimulationViewer displays a simulation in a WebView for the concept screen.
 *
 * @param simulationUrl The HTML file url to load
 * @param simulationTitle The title of the simulation
 * @param conceptId The ID of the concept being viewed (used to mark progress)
 * @param onBackClick Callback function to be invoked when the back button is clicked
 */
@Composable
fun ConceptSimulationViewer(
    simulationUrl: String,
    simulationTitle: String,
    conceptId: String = "",
    onBackClick: () -> Unit = {},
    viewModel: ConceptViewModel = hiltViewModel()
) {
    val dimens = LocalDimensions.current
    val isSimulationMarked = remember { mutableStateOf(false) }

    // Mark simulation as viewed when component appears - same logic as ConceptScreen
    LaunchedEffect(conceptId) {
        if (conceptId.isNotEmpty() && !isSimulationMarked.value) {
            viewModel.markSimulationViewed(conceptId)
            isSimulationMarked.value = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HeaderGradientStart)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.spaceSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = TextOnPrimary,
                    modifier = Modifier.size(dimens.iconMedium)
                )
            }

            Text(
                text = simulationTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextOnPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // WebView
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            SimulationWebView(
                url = simulationUrl,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
