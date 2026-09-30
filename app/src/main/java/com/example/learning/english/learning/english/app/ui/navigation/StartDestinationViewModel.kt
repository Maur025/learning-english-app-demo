package com.example.learning.english.learning.english.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning.english.learning.english.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Destino de inicio del grafo de navegación.
 *
 * Se resuelve una vez al abrir la app en lugar de usar siempre un destino fijo:
 * quien ya terminó el onboarding no debe ver la pantalla inicial otra vez
 * (README §44).
 *
 * `null` significa "todavía no lo sé", y la app no dibuja el grafo hasta que la
 * respuesta llega. Se lee una vez por proceso a propósito: leer la preferencia
 * es trabajo de datos y esta clase es su único puente con la UI.
 */
class StartDestinationViewModel(
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val startRoute: StateFlow<String?> = flow {
        val preferences = userPreferencesRepository.current()
        emit(if (preferences.onboardingCompleted) Routes.HOME else Routes.ONBOARDING)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )
}
