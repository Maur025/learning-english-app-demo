package com.example.learning.english.learning.english.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning.english.learning.english.app.domain.model.ContentPack
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.domain.model.selectedPackIds
import com.example.learning.english.learning.english.app.domain.repository.ContentPackRepository
import com.example.learning.english.learning.english.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Onboarding: duración diaria y packs con los que practicar (README §43.1).
 *
 * No guarda nada hasta el final: el usuario puede cambiar de opinión mientras ve
 * la pantalla, y solo un "Comenzar" explícito deja la marca de que ya eligió.
 *
 * La selección de packs vive aquí, no en las preferencias, hasta que se guarda.
 * Antes de ese momento las preferencias sirven como valor inicial, así que
 * volver al onboarding no pierde lo que ya se había elegido.
 */
class OnboardingViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val contentPackRepository: ContentPackRepository,
) : ViewModel() {

    /** Elecciones del usuario en esta pantalla; `null` mientras no haya tocado nada. */
    private val draft = MutableStateFlow(Draft())

    private val _effects = Channel<OnboardingEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = _effects.receiveAsFlow()

    val uiState = combine(
        contentPackRepository.observeAll(),
        userPreferencesRepository.observe(),
        draft,
    ) { packs, preferences, draft ->
        OnboardingUiState(
            packs = packs,
            selectedPackIds = draft.selectedPackIds
                ?: preferences.selectedPackIds(packs.mapTo(mutableSetOf()) { it.id }),
            dailyGoalMinutes = draft.dailyGoalMinutes ?: preferences.dailyGoalMinutes,
            isSaving = draft.isSaving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OnboardingUiState(),
    )

    fun onDailyGoalSelected(minutes: Int) {
        if (uiState.value.isEditable) {
            draft.value = draft.value.copy(dailyGoalMinutes = minutes)
        }
    }

    fun onPackToggled(packId: PackId) {
        if (!uiState.value.isEditable) return
        val current = uiState.value.selectedPackIds
        val updated = if (packId in current) current - packId else current + packId
        draft.value = draft.value.copy(selectedPackIds = updated)
    }

    fun onStart() {
        val state = uiState.value
        if (!state.canStart) return
        // El guardia se levanta antes de lanzar el coroutine: leer `isSaving`
        // después dejaría pasar un segundo toque dentro del mismo frame.
        draft.value = draft.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                userPreferencesRepository.update { preferences ->
                    preferences.copy(
                        onboardingCompleted = true,
                        dailyGoalMinutes = state.dailyGoalMinutes,
                        preferredPackIds = state.selectedPackIds,
                    )
                }
                _effects.send(OnboardingEffect.Completed)
            } finally {
                draft.value = draft.value.copy(isSaving = false)
            }
        }
    }

    private data class Draft(
        val selectedPackIds: Set<PackId>? = null,
        val dailyGoalMinutes: Int? = null,
        val isSaving: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Estado de la pantalla de onboarding.
 *
 * [isContentReady] vale `false` mientras el contenido empaquetado se importa en
 * segundo plano: no es un error, es el arranque normal de la app (README §29).
 */
data class OnboardingUiState(
    val packs: List<ContentPack> = emptyList(),
    val selectedPackIds: Set<PackId> = emptySet(),
    val dailyGoalMinutes: Int = UserPreferences.DEFAULT_DAILY_GOAL_MINUTES,
    val isSaving: Boolean = false,
) {
    val isContentReady: Boolean
        get() = packs.isNotEmpty()

    /** Sin packs no hay nada que practicar, así que no se puede empezar. */
    val canStart: Boolean
        get() = isContentReady && selectedPackIds.isNotEmpty() && !isSaving

    val isEditable: Boolean
        get() = !isSaving

    companion object {
        val DAILY_GOAL_OPTIONS = listOf(15, 30, 45, 60)
    }
}

/** Lo que la pantalla ordena hacer y no puede decidir sola. */
sealed interface OnboardingEffect {
    data object Completed : OnboardingEffect
}
