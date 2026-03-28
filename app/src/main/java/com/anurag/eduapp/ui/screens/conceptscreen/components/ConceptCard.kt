package com.anurag.eduapp.ui.screens.conceptscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.anurag.eduapp.R
import com.anurag.eduapp.data.model.ProgressStatus
import com.anurag.eduapp.ui.models.ConceptUiModel
import com.anurag.eduapp.ui.theme.AccentBlue
import com.anurag.eduapp.ui.theme.CardBackground
import com.anurag.eduapp.ui.theme.CompleteTextColor
import com.anurag.eduapp.ui.theme.InProgressTextColor
import com.anurag.eduapp.ui.theme.LocalDimensions
import com.anurag.eduapp.ui.theme.NotStartedTextColor
import com.anurag.eduapp.ui.theme.TextPrimary
import com.anurag.eduapp.ui.theme.White

/**
 * Composable function to display a Concept Card with status badge, title, concept completion status, and an icon.
 *
 * @param concept The Concept UI data to display.
 * @param serialNumber The order number of the concept.
 * @param onClick Lambda function to handle card click events.
 * @param onSimulationClick Lambda function to handle simulation button clicks with concept name and URL.
 */
@Composable
fun ConceptCard(
    concept: ConceptUiModel,
    serialNumber: Int = 1,
    onClick: () -> Unit = {},
    onSimulationClick: (String, String) -> Unit = { _, _ -> }
) {
    val dimens = LocalDimensions.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardDefaults.shape,
        colors = CardDefaults.cardColors(
            containerColor = CardBackground,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimens.cardElevation,
        )
    ) {
        // Left side: Badge + Content
        Row(
            modifier = Modifier
                .padding(dimens.spaceMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
        ) {
            // Status badge (Circle with icon/order)
            ConceptStatusBadge(
                conceptOrder = serialNumber.toString(),
                status = concept.status
            )

            // Content (Title + Status)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.inputHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
            ) {
                // Title
                Text(
                    text = concept.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                // Concept Completion status - pre-computed in ViewModel
                Text(
                    text = stringResource(concept.statusTextResId),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = getStatusColor(concept.status)
                )

                // Simulation Button
                if (concept.isSimulation && !concept.simulationButtonUrl.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimens.spaceSmall),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                    ) {
                        Button(
                            onClick = {
                                onSimulationClick(concept.name, concept.simulationButtonUrl)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = dimens.spaceExtraSmall),
                            shape = MaterialTheme.shapes.small,
                            colors = buttonColors(
                                containerColor = AccentBlue,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.simulation),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.fillMaxWidth(),
                                color = White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Maps ProgressStatus to its corresponding UI color
 * This is a pure UI utility function with no side effects
 */
private fun getStatusColor(status: ProgressStatus): Color = when (status) {
    ProgressStatus.COMPLETED -> CompleteTextColor
    ProgressStatus.IN_PROGRESS -> InProgressTextColor
    ProgressStatus.NOT_STARTED -> NotStartedTextColor
    ProgressStatus.LOCKED -> NotStartedTextColor
}