package com.ncert7.mathandsciencelab.ui.screens.conceptscreen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.mathandsciencelab.R
import com.ncert7.mathandsciencelab.debug.DebugLogger
import com.ncert7.mathandsciencelab.service.analytics.ScreenName
import com.ncert7.mathandsciencelab.service.analytics.TrackScreenEvent
import com.ncert7.mathandsciencelab.ui.screens.conceptscreen.components.ConceptCard
import com.ncert7.mathandsciencelab.ui.screens.conceptscreen.components.ConceptScreenHeader
import com.ncert7.mathandsciencelab.ui.screens.conceptscreen.viewmodel.ConceptViewModel
import com.ncert7.mathandsciencelab.ui.theme.BackgroundPrimary
import com.ncert7.mathandsciencelab.ui.theme.LocalDimensions
import com.ncert7.mathandsciencelab.ui.theme.TextPrimary

/**
 * Composable screen to display concepts of a chapter.
 * chapterId: ID of the chapter whose concepts are to be displayed.
 * type: Type of concepts to load (STUDY or SIMULATION).
 * onBackClick: Lambda function to handle back navigation.
 * onGoHome: Lambda function to navigate to home screen.
 * onGoSetting: Lambda function to navigate to settings screen.
 *
 * loads concepts from the database using ConceptViewModel and displays them in a list.
 */


@Composable
fun ConceptScreen(
    chapterId: String,
    type: String,
    onBackClick: () -> Unit = {},
    // Added subjectName and chapterName so callers can pass them into the simulation viewer
    onSimulationClick: (title: String, url: String, conceptId: String, subjectName: String, chapterName: String) -> Unit = { _, _, _, _, _ -> },
    onGoHome:() -> Unit = {},
    onGoSetting:() -> Unit = {},
    viewModel: ConceptViewModel = hiltViewModel(),
) {
    TrackScreenEvent(screenName = ScreenName.CONCEPT)

    val dimens = LocalDimensions.current
    val state by viewModel.state.collectAsState()

    // Load concepts and update streak (streak update happens in viewModel)
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

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                ) {
                    items(count = state.visibleConcepts.size, key = { state.visibleConcepts[it].id }) { index ->
                        val conceptUiModel = state.visibleConcepts[index]
                        ConceptCard(
                            concept = conceptUiModel,
                            serialNumber = index + 1,
                            onClick = {
                            },
                            onSimulationClick = { title, url ->
                                // Navigate to simulation viewer with conceptId, subjectName, chapterName
                                onSimulationClick(title, url, conceptUiModel.id, state.subjectName, state.chapterName)
                            }
                        )
                    }
                }
            }
        }
    }
}