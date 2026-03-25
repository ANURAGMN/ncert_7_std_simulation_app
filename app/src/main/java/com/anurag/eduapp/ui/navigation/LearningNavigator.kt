package com.anurag.eduapp.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anurag.eduapp.ui.screens.chapterscreen.ChapterScreen
import com.anurag.eduapp.ui.screens.conceptscreen.ConceptScreen
import com.anurag.eduapp.ui.screens.conceptscreen.components.ConceptSimulationViewer
import com.anurag.eduapp.ui.screens.subjectscreen.SubjectScreen
import androidx.compose.runtime.Composable

object LearningRoutes {
    const val HOME = "home"
    const val SUBJECTS = "subjects"
    const val CHAPTERS = "chapters/{subjectId}"
    const val CONCEPTS = "concepts/{chapterId}/{type}"
    const val SIMULATION_VIEWER = "simulation_viewer/{simulationId}/{htmlFileName}/{simulationTitle}"
}

@Composable
fun LearningNavigator(
    navController: NavHostController = rememberNavController(),
    onBackToHome: () -> Unit,
    onGoHome: () -> Unit = {},
    onGoSetting: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = LearningRoutes.SUBJECTS
    ) {
        composable(LearningRoutes.SUBJECTS) {
            SubjectScreen(
                onBackClick = onBackToHome,
                onSubjectClick = { subjectId ->
                    navController.navigate("chapters/${subjectId}")
                },
                onGoHome = onGoHome,
                onGoSetting = onGoSetting
            )
        }

        composable(LearningRoutes.CHAPTERS) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: return@composable
            ChapterScreen(
                subjectId = subjectId,
                onBackClick = { navController.popBackStack() },
                onStudyClick = { chapterId, type ->
                    navController.navigate("concepts/$chapterId/$type")
                },
                onSimulationClick = { chapterId, type, conceptId ->
                    navController.navigate("concepts/$chapterId/$type")
                },
                onGoHome = onGoHome,
                onGoSetting = onGoSetting
            )
        }

        composable(LearningRoutes.CONCEPTS) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
            val type = backStackEntry.arguments?.getString("type") ?: "STUDY"
            ConceptScreen(
                chapterId = chapterId,
                type = type,
                onBackClick = { navController.popBackStack() },
                onSimulationClick = { title, url, conceptId ->
                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                    navController.navigate("concept_sim_view/$encodedUrl/$encodedTitle/$conceptId")
                },
                onGoHome = onGoHome,
                onGoSetting = onGoSetting
            )
        }

        composable(
            route = "concept_sim_view/{url}/{title}/{conceptId}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("conceptId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: "Simulation"
            val conceptId = backStackEntry.arguments?.getString("conceptId") ?: ""

            ConceptSimulationViewer(
                simulationUrl = url,
                simulationTitle = title,
                conceptId = conceptId,
                onBackClick = { navController.popBackStack() }
            )
        }

    }
}

