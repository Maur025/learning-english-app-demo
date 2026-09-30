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
import com.example.learning.english.learning.english.app.ui.practice.PracticeEffect
import com.example.learning.english.learning.english.app.ui.practice.PracticeScreen
import com.example.learning.english.learning.english.app.ui.practice.PracticeViewModel

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
        composable(route = Routes.PRACTICE) {
            PracticeRoute(onFinish = { navController.toHomeFromPractice() })
        }
    }
}

/**
 * Práctica: observa su ViewModel y ejecuta el efecto de salida.
 *
 * Volver atrás desde la práctica dejaría Home con un contador obsoleto —los
 *ercise ya están calificados—, así que al terminar se salta a Home en lugar de
 * deshacer la navegación.
 */
@Composable
private fun PracticeRoute(onFinish: () -> Unit) {
    val viewModel: PracticeViewModel = viewModel(factory = AppViewModelFactory.Practice)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PracticeEffect.Finish -> onFinish()
            }
        }
    }

    PracticeScreen(
        uiState = uiState,
        onIntroductionSeen = viewModel::onIntroductionSeen,
        onOptionSelected = viewModel::onOptionSelected,
        onTextChanged = viewModel::onTextChanged,
        onCheck = viewModel::onCheck,
        onRate = viewModel::onRate,
        onFinish = viewModel::onFinish,
    )
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

/**
 * Al terminar la práctica se vuelve a Home sin apilarlo otra vez.
 *
 * Se hace `popBackStack` en lugar de navegar: la práctica se abrió desde Home, así
 * que deshacerse devuelve a la misma instancia, ya con los contadores al día.
 */
private fun NavHostController.toHomeFromPractice() {
    popBackStack(route = Routes.HOME, inclusive = false)
}
