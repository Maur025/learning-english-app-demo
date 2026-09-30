package com.example.learning.english.learning.english.app.domain.repository

import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeRecent(limit: Int): Flow<List<LearningSession>>

    /**
     * Última sesión sin completar, o `null` si no hay ninguna.
     *
     * Llega sin ejercicios: sirve para ofrecer "continuar" y saber por dónde iba
     * la sesión, no para practicar. Quien necesite el contenido la pide por id.
     */
    fun observeInProgress(): Flow<LearningSession?>

    suspend fun getById(sessionId: SessionId): LearningSession?

    suspend fun save(session: LearningSession)
}
