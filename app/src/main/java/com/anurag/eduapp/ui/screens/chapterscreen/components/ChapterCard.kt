package com.anurag.eduapp.ui.screens.chapterscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anurag.eduapp.R
import com.anurag.eduapp.data.model.ProgressStatus
import com.anurag.eduapp.ui.models.ChapterUiModel
import com.anurag.eduapp.ui.models.ChapterProgressUiModel
import com.anurag.eduapp.ui.theme.CardBackground
import com.anurag.eduapp.ui.theme.ColorHint
import com.anurag.eduapp.ui.theme.CompleteTextColor
import com.anurag.eduapp.ui.theme.InProgressTextColor
import com.anurag.eduapp.ui.theme.LocalDimensions
import com.anurag.eduapp.ui.theme.TextPrimary
import com.anurag.eduapp.ui.theme.TextSecondary


/**
 * A card component to display chapter information
 * 1. Chapter ID and Name
 * 2. Status Badge (Completed, In Progress)
 * 3. Progress Bar showing completion percentage
 * 4. Action Buttons: Study, Simulations

 * @param chapter The chapter UI model to display.
 * @param onStudyClick Callback when the "Study" button is clicked.
 * @param onSimulationClick Callback when the "Simulation" button is clicked.
 * @param modifier Optional modifier for styling the card.
 */
@Composable
fun ChapterCard(
    modifier: Modifier = Modifier,
    chapter: ChapterUiModel,
    onStudyClick: () -> Unit = {},
    onSimulationClick: () -> Unit = {},
) {
    val dimens = LocalDimensions.current
    val progress = chapter.progressUiModel
        ?: throw IllegalStateException("ChapterProgressUiModel must be provided by ViewModel")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimens.cardPadding)
    ) {
        // Status Badge - positioned above the card
        if (chapter.status != ProgressStatus.NOT_STARTED) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = (-8).dp)
                    .zIndex(1f)
                    .background(
                        color = getChapterStatusColor(chapter.status),
                        shape = RoundedCornerShape(dimens.cornerRadiusSmall)
                    )
                    .padding(horizontal = dimens.statusTextBoxHorizontalPadding, vertical = dimens.statusTextBoxVerticalPadding)
            ) {
                Text(
                    text = getChapterStatusText(chapter.status),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        // Main Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimens.cornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimens.cardElevation
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.spaceMedium)
            ) {
                // Chapter Order + name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Chapter number
                    Text(
                        text = chapter.orderIndex.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(dimens.spaceSmall),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = chapter.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(dimens.spaceExtraSmall))

                        // Concept count
                        Text(
                            text = pluralStringResource(
                                R.plurals.concept_count,
                                chapter.totalConcepts,
                                chapter.totalConcepts
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(dimens.spaceMedium))

                        // Progress Bar with percentage
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)
                        ) {
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { progress.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dimens.progressIndicatorStrokeHeight)
                                    .clip(RoundedCornerShape(dimens.cornerRadiusSmall)),
                                trackColor = ColorHint,
                                color = getProgressBarColor(chapter.status)
                            )

                            // Progress text
                            Text(
                                text = stringResource(
                                    R.string.progress_status,
                                    progress.completed,
                                    progress.total,
                                    progress.remaining
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(dimens.spaceMedium))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                        ) {
                            // Buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                            ) {

                                ChapterActionButton(
                                    label = stringResource(R.string.simulation),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Science,
                                            contentDescription = stringResource(R.string.simulation),
                                            tint = TextPrimary
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onSimulationClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper functions for status badge
@Composable
private fun getChapterStatusText(status: ProgressStatus): String = when (status) {
    ProgressStatus.COMPLETED -> stringResource(R.string.status_completed)
    ProgressStatus.IN_PROGRESS -> stringResource(R.string.status_in_progress)
    ProgressStatus.NOT_STARTED -> stringResource(R.string.status_not_started)
    ProgressStatus.LOCKED -> stringResource(R.string.status_not_started)
}

private fun getChapterStatusColor(status: ProgressStatus): Color = when (status) {
    ProgressStatus.COMPLETED -> CompleteTextColor
    ProgressStatus.IN_PROGRESS -> InProgressTextColor
    ProgressStatus.NOT_STARTED -> Color.Gray
    ProgressStatus.LOCKED -> Color.Gray
}

private fun getProgressBarColor(status: ProgressStatus): Color = when (status) {
    ProgressStatus.COMPLETED -> CompleteTextColor
    ProgressStatus.IN_PROGRESS -> InProgressTextColor
    ProgressStatus.NOT_STARTED -> Color.Gray
    ProgressStatus.LOCKED -> Color.Gray
}

@Preview
@Composable
fun ChapterCardPreview() {
    ChapterCard(
        chapter = ChapterUiModel(
            id = "1",
            orderIndex = 1,
            name = "Number Systems",
            englishName = "Number Systems",
            totalConcepts = 8,
            completedConcepts = 5,
            status = ProgressStatus.IN_PROGRESS,
            progressUiModel = ChapterProgressUiModel(
                completed = 5,
                total = 8,
                progressFraction = 5f / 8f,
                progressPercentage = 62,
                remaining = 3
            )
        )
    )
}