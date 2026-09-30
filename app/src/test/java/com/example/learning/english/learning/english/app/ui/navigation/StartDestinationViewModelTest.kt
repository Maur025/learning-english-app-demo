package com.example.learning.english.learning.english.app.ui.navigation

import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.ui.FakeUserPreferencesRepository
import com.example.learning.english.learning.english.app.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartDestinationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `opens onboarding until it has been completed`() = runTest {
        val viewModel = StartDestinationViewModel(FakeUserPreferencesRepository())

        assertEquals(Routes.ONBOARDING, viewModel.startRoute.first { it != null })
    }

    @Test
    fun `opens home once the onboarding has been completed`() = runTest {
        val preferences = FakeUserPreferencesRepository(UserPreferences(onboardingCompleted = true))

        val viewModel = StartDestinationViewModel(preferences)

        assertEquals(Routes.HOME, viewModel.startRoute.first { it != null })
    }
}