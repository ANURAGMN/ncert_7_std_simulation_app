package com.anurag.eduapp.ui.screens.conceptscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduapp.R
import com.anurag.eduapp.debug.DebugLogger
import com.anurag.eduapp.service.analytics.ScreenName
import com.anurag.eduapp.service.analytics.TrackScreenEvent
import com.anurag.eduapp.ui.screens.conceptscreen.components.ConceptCard
import com.anurag.eduapp.ui.screens.conceptscreen.components.ConceptScreenHeader
import com.anurag.eduapp.ui.screens.conceptscreen.viewmodel.ConceptViewModel
import com.anurag.eduapp.ui.theme.BackgroundPrimary
import com.anurag.eduapp.ui.theme.LocalDimensions
import com.anurag.eduapp.ui.theme.TextPrimary
import com.anurag.eduapp.utils.StreakManager

/**
 * Composable screen to display concepts of a chapter.
 * chapterId: ID of the chapter whose concepts are to be displayed.
 * type: Type of concepts to load (STUDY or SIMULATION).
 * onBackClick: Lambda function to handle back navigation.
 * onConceptClick: Lambda function to handle concept item clicks.
 *
 * loads concepts from the database using ConceptViewModel and displays them in a list.
 */


@Composable
fun ConceptScreen(
    chapterId: String,
    type: String,
    onBackClick: () -> Unit = {},
    onSimulationClick: (String, String) -> Unit = { _, _ -> },
    onGoHome:() -> Unit = {},
    onGoSetting:() -> Unit = {},
    viewModel: ConceptViewModel = hiltViewModel(),
) {
    TrackScreenEvent(screenName = ScreenName.CONCEPT)

    val dimens = LocalDimensions.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()


    // streak update
    val streakManager = remember { StreakManager(context) }

    // updating streak on concept opening
    LaunchedEffect(Unit) {
        streakManager.onConceptOpened()
    }
    LaunchedEffect(chapterId, type) {
        viewModel.loadConcepts(chapterId, type)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        ConceptScreenHeader(
            classLevel = state.classLevel,
            subjectName = state.subjectName,
            chapterName = state.chapterName,
            progress = state.progressUiModel,
            onBackClick = onBackClick,
            onGoHome = onGoHome,
            onGoSetting = onGoSetting
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            DebugLogger.errorLog("ConceptScreen", "Error loading concepts: ${state.error}")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.unable_to_load_concepts), color = TextPrimary)
            }
        } else {
            Column(
                modifier = Modifier.padding(dimens.spaceMedium),
            ) {
                Text(
                    text = if (state.type.equals("SIMULATION", ignoreCase = true))
                        stringResource(R.string.simulations_to_explore)
                    else
                        stringResource(R.string.lessons_to_master),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )

                Spacer(modifier = Modifier.height(dimens.spaceSmall))

                // Filter concepts that have a valid simulation URL
                val filteredConcepts = state.concepts.filter { concept ->
                    (!concept.simulationUrl.isNullOrBlank() && concept.simulationUrl != "Not found") ||
                    (!concept.simulationUrlKannada.isNullOrBlank() && concept.simulationUrlKannada != "Not found")
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                ) {
                    items(count = filteredConcepts.size, key = { filteredConcepts[it].id }) { index ->
                        val conceptUiModel = filteredConcepts[index]
                        ConceptCard(
                            concept = conceptUiModel,
                            serialNumber = index + 1,
                            onClick = {
                            },
                            onSimulationClick = { title, url ->
                                // Pass everything back to the navigator
                                onSimulationClick( title, url)
                            }
                        )
                    }
                }
            }
        }
    }
}