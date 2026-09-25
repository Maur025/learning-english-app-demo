package com.example.learning.english.learning.english.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.ThemePreference
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Implementación de DataStore de [UserPreferencesRepository].
 *
 * Guarda un único objeto de preferencias por clave, no datos de aprendizaje.
 */
class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            // Un archivo corrupto no debe tumbar la app: se vuelve a empezar
            // con los valores por defecto.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toUserPreferences() }

    override fun observe(): Flow<UserPreferences> = preferences

    override suspend fun current(): UserPreferences = preferences.first()

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.edit { stored ->
            val updated = transform(stored.toUserPreferences())
            stored[Keys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
            stored[Keys.DAILY_GOAL_MINUTES] = updated.dailyGoalMinutes
            stored[Keys.NEW_EXPRESSIONS_PER_DAY] = updated.newExpressionsPerDay
            stored[Keys.PREFERRED_PACK_IDS] = updated.preferredPackIds.map { it.value }.toSet()
            stored[Keys.THEME] = updated.theme.name
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
        onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: false,
        dailyGoalMinutes = this[Keys.DAILY_GOAL_MINUTES] ?: UserPreferences.DEFAULT_DAILY_GOAL_MINUTES,
        newExpressionsPerDay = this[Keys.NEW_EXPRESSIONS_PER_DAY]
            ?: UserPreferences.DEFAULT_NEW_EXPRESSIONS_PER_DAY,
        preferredPackIds = this[Keys.PREFERRED_PACK_IDS].orEmpty().map { PackId(it) }.toSet(),
        theme = this[Keys.THEME]?.toThemePreference() ?: ThemePreference.SYSTEM,
    )

    private fun String.toThemePreference(): ThemePreference =
        enumValueOfOrDefault(this, ThemePreference.SYSTEM)

    private inline fun <reified T : Enum<T>> enumValueOfOrDefault(value: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val NEW_EXPRESSIONS_PER_DAY = intPreferencesKey("new_expressions_per_day")
        val PREFERRED_PACK_IDS = stringSetPreferencesKey("preferred_pack_ids")
        val THEME = stringPreferencesKey("theme")
    }
}
