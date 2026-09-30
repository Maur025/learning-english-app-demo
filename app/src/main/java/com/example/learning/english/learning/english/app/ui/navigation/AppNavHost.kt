package com.example.learning.english.learning.english.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.ui.AppViewModelFactory
import com.example.learning.english.learning.english.app.ui.home.HomeEffect
import com.example.learning.english.learning.english.app.ui.home.HomeScreen
import com.example.learning.english.learning.english.app.ui.home.HomeViewModel
import com.example.learning.english.learning.english.app.ui.onboarding.OnboardingEffect
import com.example.learning.english.learning.english.app.ui.onboarding.OnboardingScreen
import com.example.learning.english.learning.english.app.ui.onboarding.OnboardingViewModel
import com.example.learning.english.learning.english.app.ui.practice.PracticeScreen

/**
 * Grafo de navegación: onboarding → home → práctica (README §44).
 *
 * Cada destino es delgado: observa su ViewModel, pinta su estado y ejecuta las
 * navegaciones que ese ViewModel pide. Ninguna decisión de dominio vive aquí.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestinationViewModel: StartDestinationViewModel = viewModel(
        factory = AppViewModelFactory.StartDestination,
    ),
) {
    val startRoute by startDestinationViewModel.startRoute.collectAsStateWithLifecycle()

    if (startRoute == null) {
        WaitingForStartDestination(modifier = modifier)
        return
    }

    NavHost(
        navController = navController,
        startDestination = requireNotNull(startRoute),
        modifier = modifier,
    ) {
        composable(route = Routes.ONBOARDING) {
            OnboardingRoute(onCompleted = { navController.toHomeFromOnboarding() })
        }
        composable(route = Routes.HOME) {
            HomeRoute(onOpenSession = { sessionId -> navController.navigate(Routes.practice(sessionId)) })
        }
        composable(route = Routes.PRACTICE) { entry ->
            val sessionId = entry.arguments?.getString(Routes.PRACTICE_ARG_SESSION_ID)
            PracticeScreen(sessionId = SessionId(sessionId.orEmpty()))
        }
    }
}

@Composable
private fun OnboardingRoute(onCompleted: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel(factory = AppViewModelFactory.Onboarding)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.Completed -> onCompleted()
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onDailyGoalSelected = viewModel::onDailyGoalSelected,
        onPackToggled = viewModel::onPackToggled,
        onStart = viewModel::onStart,
    )
}

@Composable
private fun HomeRoute(onOpenSession: (SessionId) -> Unit) {
    val viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory.Home)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.OpenSession -> onOpenSession(effect.sessionId)
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onStartSession = viewModel::onStartSession,
        onContinueSession = viewModel::onContinueSession,
    )
}

@Composable
private fun WaitingForStartDestination(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Home no debe quedar en la pila de atrás: volver atrás no tiene sentido. */
private fun NavHostController.toHomeFromOnboarding() {
    navigate(Routes.HOME) {
        popUpTo(Routes.ONBOARDING) { inclusive = true }
    }
}
