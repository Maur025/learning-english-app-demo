package com.example.learning.english.learning.english.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.learning.english.learning.english.app.di.appContainer
import com.example.learning.english.learning.english.app.ui.home.HomeViewModel
import com.example.learning.english.learning.english.app.ui.navigation.StartDestinationViewModel
import com.example.learning.english.learning.english.app.ui.onboarding.OnboardingViewModel

/**
 * Fábricas de los ViewModels de la app.
 *
 * Vive en `ui` y no en `di` a propósito: el grafo de dependencias no debe
 * conocer las pantallas. Cada ViewModel pide solo lo que usa (README §13).
 */
object AppViewModelFactory {

    val StartDestination: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            StartDestinationViewModel(userPreferencesRepository = appContainer.userPreferencesRepository)
        }
    }

    val Home: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = appContainer
            HomeViewModel(
                userPreferencesRepository = container.userPreferencesRepository,
                contentPackRepository = container.contentPackRepository,
                learningStateRepository = container.learningStateRepository,
                expressionRepository = container.expressionRepository,
                sessionRepository = container.sessionRepository,
                learningEngine = container.learningEngine,
            )
        }
    }

    val Onboarding: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = appContainer
            OnboardingViewModel(
                userPreferencesRepository = container.userPreferencesRepository,
                contentPackRepository = container.contentPackRepository,
            )
        }
    }
}
