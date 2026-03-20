package com.anurag.eduai.ui.screens.chapterscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.R
import com.anurag.eduai.debug.DebugLogger
import com.anurag.eduai.ui.theme.BackgroundPrimary
import com.anurag.eduai.ui.theme.LocalDimensions


@Composable
fun ChapterScreen(
    subjectId: String,
    onBackClick: () -> Unit = {},
    onStudyClick: (String, String) -> Unit = {_, _ -> },
    onSimulationClick: (String, String) -> Unit = {_, _ -> },
    onRevisionClick: (String) -> Unit = {},
    onGoHome: () -> Unit = {},
    onGoSetting: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    viewModel: ChapterViewModel = hiltViewModel(),
    revisionViewModel: RevisionViewModel = hiltViewModel()
) {
    // Analytics Tracking
    TrackScreenEvent(screenName = ScreenName.CHAPTER)

    val dimens = LocalDimensions.current
    val state by viewModel.state.collectAsState()

    // State for revision dialog
    var showRevisionDialog by remember { mutableStateOf(false) }
    var pendingRevisionChapter by remember { mutableStateOf<String?>(null) }

    // Load chapters when subjectId changes
    LaunchedEffect(subjectId) {
        viewModel.loadChapters(subjectId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        ChapterScreenHeader(
            classLevel = state.classLevel,
            subjectName = state.subjectName,
            onBackClick = onBackClick,
            onGoHome = onGoHome,
            onGoSetting = onGoSetting,
            onProgressClick = onProgressClick
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            DebugLogger.errorLog("ChapterScreen", "Error loading chapters: ${state.error}")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.unable_to_load_chapters))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
            ) {
                items(state.chapters, { it.id }) { chapterUiModel ->
                    ChapterCard(
                        chapter = chapterUiModel,
                        subjectName = state.subjectName, // Pass subject name for conditional rendering
                        onStudyClick = { onStudyClick(chapterUiModel.id, "STUDY") },
                        onSimulationClick = { onSimulationClick(chapterUiModel.id, "SIMULATION") },
                        onRevisionClick = {
                            // Use the English name from the model to get the API-compatible name
                            val chapterName = chapterUiModel.englishName.getRevisionChapterName()
                            DebugLogger.debugLog("ChapterScreen", "Revision button clicked for chapter: ${chapterUiModel.name}, english name: ${chapterUiModel.englishName}, mapped: $chapterName")

                            // Check if session exists
                            if (revisionViewModel.hasExistingSession(chapterName)) {
                                DebugLogger.debugLog("ChapterScreen", "Existing revision session found, showing dialog")
                                pendingRevisionChapter = chapterName
                                showRevisionDialog = true
                            } else {
                                DebugLogger.debugLog("ChapterScreen", "No existing revision session, navigating directly")
                                onRevisionClick(chapterName)
                            }
                        }
                    )
                }
            }
        }
