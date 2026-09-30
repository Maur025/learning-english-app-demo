package com.example.learning.english.learning.english.app.domain.repository

import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeHistory(expressionId: ExpressionId): Flow<List<ReviewEvent>>

    fun observeAll(): Flow<List<ReviewEvent>>

    /**
     * Revisiones de una sesión, en orden de respuesta.
     *
     * Es la fuente del resumen de sesión (§43.5): los contadores se derivan de la
     * historia persistida, no de un contador paralelo que habría que mantener.
     */
    suspend fun getBySession(sessionId: SessionId): List<ReviewEvent>

    suspend fun record(event: ReviewEvent)
}
