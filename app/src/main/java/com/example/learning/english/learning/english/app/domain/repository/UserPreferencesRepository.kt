package com.example.learning.english.learning.english.app.domain.repository

import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Preferencias de usuario. Implementado sobre DataStore, no sobre Room:
 * son datos pequeños y no relacionales (README §19).
 */
interface UserPreferencesRepository {
    fun observe(): Flow<UserPreferences>

    suspend fun current(): UserPreferences

    suspend fun update(transform: (UserPreferences) -> UserPreferences)
}
