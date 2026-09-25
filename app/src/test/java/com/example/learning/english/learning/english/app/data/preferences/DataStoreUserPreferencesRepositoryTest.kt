package com.example.learning.english.learning.english.app.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.ThemePreference
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataStoreUserPreferencesRepositoryTest {

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var repository: DataStoreUserPreferencesRepository

    @Before
    fun setUp() {
        val file = temporaryFolder.newFile("user_preferences.preferences_pb")
        file.delete()
        repository = DataStoreUserPreferencesRepository(
            PreferenceDataStoreFactory.create { file },
        )
    }

    @After
    fun tearDown() = Unit

    @Test
    fun `returns defaults when nothing is stored`() = runBlocking {
        assertEquals(UserPreferences(), repository.current())
    }

    @Test
    fun `stores onboarding completion and daily goal`() = runBlocking {
        repository.update { it.copy(onboardingCompleted = true, dailyGoalMinutes = 45) }

        val stored = repository.current()
        assertEquals(true, stored.onboardingCompleted)
        assertEquals(45, stored.dailyGoalMinutes)
    }

    @Test
    fun `stores pack selection and theme`() = runBlocking {
        repository.update {
            it.copy(
                preferredPackIds = setOf(PackId("core-english"), PackId("developer-english")),
                theme = ThemePreference.DARK,
            )
        }

        val stored = repository.current()
        assertEquals(2, stored.preferredPackIds.size)
        assertEquals(ThemePreference.DARK, stored.theme)
    }

    @Test
    fun `update applies on top of the current values`() = runBlocking {
        repository.update { it.copy(dailyGoalMinutes = 60) }
        repository.update { it.copy(newExpressionsPerDay = 10) }

        val stored = repository.current()
        assertEquals(60, stored.dailyGoalMinutes)
        assertEquals(10, stored.newExpressionsPerDay)
    }

    @Test
    fun `observe emits the updated value`() = runBlocking {
        repository.update { it.copy(onboardingCompleted = true) }

        assertEquals(true, repository.observe().first().onboardingCompleted)
    }
}
