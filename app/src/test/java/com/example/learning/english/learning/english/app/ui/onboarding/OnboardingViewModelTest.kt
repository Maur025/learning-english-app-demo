package com.example.learning.english.learning.english.app.ui.onboarding

import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.ui.FakeContentPackRepository
import com.example.learning.english.learning.english.app.ui.FakeUserPreferencesRepository
import com.example.learning.english.learning.english.app.ui.MainDispatcherRule
import com.example.learning.english.learning.english.app.ui.contentPack
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Los ViewModel se crean dentro de cada test, no como campo.
 *
 * `viewModelScope` captura `Dispatchers.Main` en el momento de construir el
 * ViewModel, y el regla inyecta el dispatcher de test después: un ViewModel
 * construido en el constructor de la clase quedaría atado al hilo principal real.
 */
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesRepository = FakeUserPreferencesRepository()
    private val contentPackRepository = FakeContentPackRepository()
    private val installedPacks = listOf(contentPack("core-english"), contentPack("developer-english"))

    @Test
    fun `waits for the bundled content to be installed`() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.first { it.packs.isEmpty() }.isContentReady)
        assertFalse(viewModel.uiState.value.canStart)

        contentPackRepository.install(installedPacks)

        assertTrue(viewModel.uiState.first { it.isContentReady }.canStart)
    }

    @Test
    fun `starts with every installed pack selected`() = runTest {
        contentPackRepository.install(installedPacks)

        val state = createViewModel().uiState.first { it.isContentReady }

        assertEquals(setOf(PackId("core-english"), PackId("developer-english")), state.selectedPackIds)
    }

    @Test
    fun `toggling a pack updates the selection`() = runTest {
        contentPackRepository.install(installedPacks)
        val viewModel = createViewModel()
        viewModel.uiState.first { it.isContentReady }

        viewModel.onPackToggled(PackId("developer-english"))
        assertEquals(
            setOf(PackId("core-english")),
            viewModel.uiState.first { it.selectedPackIds.size == 1 }.selectedPackIds,
        )

        viewModel.onPackToggled(PackId("developer-english"))
        assertEquals(
            installedPacks.size,
            viewModel.uiState.first { it.selectedPackIds.size == 2 }.selectedPackIds.size,
        )
    }

    @Test
    fun `cannot start without a selected pack`() = runTest {
        contentPackRepository.install(listOf(contentPack("core-english")))
        val viewModel = createViewModel()
        viewModel.uiState.first { it.isContentReady }

        viewModel.onPackToggled(PackId("core-english"))

        assertFalse(viewModel.uiState.first { it.selectedPackIds.isEmpty() }.canStart)
    }

    @Test
    fun `changes the daily goal`() = runTest {
        contentPackRepository.install(installedPacks)
        val viewModel = createViewModel()
        viewModel.uiState.first { it.isContentReady }

        viewModel.onDailyGoalSelected(45)

        assertEquals(45, viewModel.uiState.first { it.dailyGoalMinutes == 45 }.dailyGoalMinutes)
    }

    @Test
    fun `saving stores the goal, the packs and the onboarding flag`() = runTest {
        contentPackRepository.install(installedPacks)
        val viewModel = createViewModel()
        viewModel.uiState.first { it.isContentReady }
        viewModel.onDailyGoalSelected(45)
        viewModel.onPackToggled(PackId("developer-english"))
        viewModel.uiState.first { it.selectedPackIds.size == 1 }

        viewModel.onStart()

        val saved = preferencesRepository.current()
        assertTrue(saved.onboardingCompleted)
        assertEquals(45, saved.dailyGoalMinutes)
        assertEquals(setOf(PackId("core-english")), saved.preferredPackIds)
    }

    @Test
    fun `announces that onboarding finished`() = runTest {
        contentPackRepository.install(installedPacks)
        val viewModel = createViewModel()
        viewModel.uiState.first { it.isContentReady }

        viewModel.onStart()

        assertEquals(OnboardingEffect.Completed, viewModel.effects.first())
    }

    @Test
    fun `does not save twice on a double tap`() = runTest {
        // El guardado queda a medias: solo así el segundo toque llega mientras el
        // primero sigue en curso, como pasa en un dispositivo.
        val gate = CompletableDeferred<Unit>()
        val preferences = FakeUserPreferencesRepository(saveGate = gate)
        contentPackRepository.install(installedPacks)
        val viewModel = createViewModel(preferences)
        viewModel.uiState.first { it.isContentReady }

        viewModel.onStart()
        viewModel.onStart()
        gate.complete(Unit)

        assertEquals(OnboardingEffect.Completed, viewModel.effects.first())
        assertEquals(1, preferences.saveCount)
    }

    @Test
    fun `keeps a previous choice when onboarding is opened again`() = runTest {
        val previous = UserPreferences(
            onboardingCompleted = true,
            dailyGoalMinutes = 60,
            preferredPackIds = setOf(PackId("developer-english")),
        )

        val reopened = OnboardingViewModel(
            FakeUserPreferencesRepository(previous),
            FakeContentPackRepository(installedPacks),
        )

        val state = reopened.uiState.first { it.isContentReady }

        assertEquals(setOf(PackId("developer-english")), state.selectedPackIds)
        assertEquals(60, state.dailyGoalMinutes)
    }

    private fun createViewModel(
        preferences: FakeUserPreferencesRepository = preferencesRepository,
    ) = OnboardingViewModel(preferences, contentPackRepository)
}