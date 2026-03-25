package com.anurag.eduapp.ui.screens.homescreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anurag.eduapp.R
import com.anurag.eduapp.data.local.entities.ConceptEntity
import com.anurag.eduapp.ui.models.ConceptStatus
import com.anurag.eduapp.ui.models.ConceptUiModel
import com.anurag.eduapp.ui.screens.conceptscreen.components.ConceptCard
import com.anurag.eduapp.ui.theme.BackgroundPrimary
import com.anurag.eduapp.ui.theme.LocalDimensions
import com.anurag.eduapp.ui.theme.TextPrimary
import com.anurag.eduapp.utils.getLocalizedName
import com.anurag.eduapp.data.local.entities.ProgressEntity

@Composable
fun PracticeSimulationCard(
    progressSimulations: List<Pair<ProgressEntity?, ConceptEntity?>>,
    onSimulationUrlClick: (String, String, String) -> Unit = { _, _, _ -> } // Click simulation button - opens URL viewer (title, url, conceptId)
) {
    val dimes = LocalDimensions.current

    // Don't show card if no simulations
    if (progressSimulations.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundPrimary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimes.cardElevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(dimes.screenPadding)) {
            Text(
                text = stringResource(R.string.practice_simulation),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(0.dp, dimes.spaceExtraSmall)
            )
            Spacer(modifier = Modifier.height(dimes.screenPadding))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimes.spaceSmall)
            ) {
                // Filter concepts that have valid simulation URLs
                val filteredSimulations = progressSimulations.mapIndexed { _, (progress, concept) ->
                    Pair(progress, concept)
                }.filter { (_, concept) ->
                    concept?.let { sim ->
                        (!sim.simulationUrl.isNullOrBlank() && sim.simulationUrl != "Not found") ||
                        (!sim.simulationUrlKannada.isNullOrBlank() && sim.simulationUrlKannada != "Not found")
                    } ?: false
                }

                filteredSimulations.forEachIndexed { index, (progress, concept) ->
                    concept?.let { sim ->
                        val conceptUiModel = ConceptUiModel(
                            id = sim.conceptId,
                            name = sim.getLocalizedName(),
                            order = sim.orderIndex,
                            status = when (progress?.status) {
                                "COMPLETED" -> ConceptStatus.COMPLETED
                                "IN_PROGRESS" -> ConceptStatus.IN_PROGRESS
                                else -> ConceptStatus.NOT_STARTED
                            },
                            type = sim.type,
                            simulationUrl = sim.simulationUrl,
                            simulationUrlKannada = sim.simulationUrlKannada,
                            simulationId = sim.simulationId
                        )

                        ConceptCard(
                            concept = conceptUiModel,
                            serialNumber = index + 1,
                            onSimulationClick = { title, url ->
                                // Clicking "Simulation" button opens URL viewer with conceptId
                                onSimulationUrlClick(title, url, sim.conceptId)
                            }
                        )
                    }
                }
            }
        }
    }
}