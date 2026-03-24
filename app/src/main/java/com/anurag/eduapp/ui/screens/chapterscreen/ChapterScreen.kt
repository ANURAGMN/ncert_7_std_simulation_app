package com.anurag.eduapp.ui.screens.chapterscreen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduapp.R
import com.anurag.eduapp.debug.DebugLogger
import com.anurag.eduapp.service.analytics.ScreenName
import com.anurag.eduapp.service.analytics.TrackScreenEvent
import com.anurag.eduapp.ui.screens.chapterscreen.components.ChapterCard
import com.anurag.eduapp.ui.screens.chapterscreen.components.ChapterScreenHeader
import com.anurag.eduapp.ui.screens.chapterscreen.viewmodel.ChapterViewModel
import com.anurag.eduapp.ui.theme.BackgroundPrimary
import com.anurag.eduapp.ui.theme.LocalDimensions


@Composable
fun ChapterScreen(
    subjectId: String,
    onBackClick: () -> Unit = {},
    onStudyClick: (String, String) -> Unit = {_, _ -> },
    onSimulationClick: (String, String) -> Unit = {_, _ -> },
    onGoHome: () -> Unit = {},
    onGoSetting: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    viewModel: ChapterViewModel = hiltViewModel(),
) {
    // Analytics Tracking
    TrackScreenEvent(screenName = ScreenName.CHAPTER)

    val dimens = LocalDimensions.current
    val state by viewModel.state.collectAsState()


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
                items(count = state.chapters.size, key = { state.chapters[it].id }) { index ->
                    val chapterUiModel = state.chapters[index]
                    ChapterCard(
                        chapter = chapterUiModel,
                        subjectName = state.subjectName,
                        onStudyClick = { onStudyClick(chapterUiModel.id, "STUDY") },
                        onSimulationClick = { onSimulationClick(chapterUiModel.id, "SIMULATION") }
                    )
                }
            }
        }
    }
}