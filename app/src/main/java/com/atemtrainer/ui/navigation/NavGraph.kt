package com.atemtrainer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.atemtrainer.ui.screens.*
import com.atemtrainer.viewmodel.MainUiState
import com.atemtrainer.viewmodel.MainViewModel
import com.atemtrainer.viewmodel.TrainingViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TRAINING = "training/{targetSeconds}"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
    fun training(seconds: Int) = "training/$seconds"
}

@Composable
fun AppNavGraph() {
    val mainVm: MainViewModel = viewModel()
    val uiState by mainVm.uiState.collectAsState()

    when (val state = uiState) {
        is MainUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is MainUiState.Ready -> {
            val startDestination = if (state.onboardingDone) Routes.HOME else Routes.ONBOARDING
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = startDestination) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinish = { baseline ->
                            mainVm.completeOnboarding(baseline)
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        state = state,
                        onStartTraining = {
                            navController.navigate(Routes.training(state.currentTargetSeconds))
                        },
                        onNavigateStats = { navController.navigate(Routes.STATISTICS) },
                        onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                        onAcceptIncrease = mainVm::acceptIncrease,
                        onDismissIncrease = mainVm::dismissIncrease,
                    )
                }

                composable(
                    Routes.TRAINING,
                    arguments = listOf(navArgument("targetSeconds") { type = NavType.IntType })
                ) { backStack ->
                    val targetSeconds = backStack.arguments?.getInt("targetSeconds")
                        ?: state.currentTargetSeconds
                    val trainingVm: TrainingViewModel = viewModel()
                    TrainingScreen(
                        targetSeconds = targetSeconds,
                        viewModel = trainingVm,
                        onDone = {
                            mainVm.recordCompletedSession(targetSeconds)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }

                composable(Routes.STATISTICS) {
                    StatisticsScreen(
                        sessions = state.sessions,
                        maxDuration = state.maxDuration,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = mainVm,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
