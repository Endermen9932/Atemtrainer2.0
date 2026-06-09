package com.atemtrainer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.atemtrainer.ui.screens.*
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
    val navController = rememberNavController()
    val mainVm: MainViewModel = viewModel()
    val state by mainVm.uiState.collectAsState()

    val startDestination = if (state.onboardingDone) Routes.HOME else Routes.ONBOARDING

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

        composable(Routes.TRAINING) { backStack ->
            val targetSeconds = backStack.arguments?.getString("targetSeconds")?.toIntOrNull() ?: state.currentTargetSeconds
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
