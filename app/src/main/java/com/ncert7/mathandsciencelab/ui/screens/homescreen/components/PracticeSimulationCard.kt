package com.ncert7.mathandsciencelab.ui.screens.homescreen.components

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
import com.ncert7.mathandsciencelab.R
import com.ncert7.mathandsciencelab.data.local.entities.ConceptEntity
import com.ncert7.mathandsciencelab.data.local.entities.ProgressEntity
import com.ncert7.mathandsciencelab.data.model.ProgressStatus
import com.ncert7.mathandsciencelab.ui.models.ConceptUiModel
import com.ncert7.mathandsciencelab.ui.screens.conceptscreen.components.ConceptCard
import com.ncert7.mathandsciencelab.ui.theme.BackgroundPrimary
import com.ncert7.mathandsciencelab.ui.theme.LocalDimensions
import com.ncert7.mathandsciencelab.ui.theme.TextPrimary
import com.ncert7.mathandsciencelab.utils.getLocalizedName
import com.ncert7.mathandsciencelab.utils.isKannada

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
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = dimes.spaceExtraSmall)
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
                        // Determine status from progress entity
                        val status = when (progress?.status) {
                            "COMPLETED" -> ProgressStatus.COMPLETED
                            "IN_PROGRESS", "STARTED" -> ProgressStatus.IN_PROGRESS
                            else -> ProgressStatus.NOT_STARTED
                        }

                        // Select simulation URL based on language preference
                        val selectedSimulationUrl = if (isKannada()) {
                            sim.simulationUrlKannada?.takeIf { it.isNotBlank() && it != "Not found" }
                                ?: sim.simulationUrl
                        } else {
                            sim.simulationUrl
                        }

                        // Validate URL
                        val hasValidUrl = !selectedSimulationUrl.isNullOrBlank() && selectedSimulationUrl != "Not found"
                        val isSimulation = sim.type.equals("SIMULATION", ignoreCase = true)

                        val conceptUiModel = ConceptUiModel(
                            id = sim.conceptId,
                            name = sim.getLocalizedName(),
                            order = sim.orderIndex,
                            status = status,
                            type = sim.type,
                            simulationUrl = sim.simulationUrl,
                            simulationUrlKannada = sim.simulationUrlKannada,
                            simulationId = sim.simulationId,
                            isSimulation = isSimulation,
                            simulationButtonUrl = if (hasValidUrl) selectedSimulationUrl else null
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