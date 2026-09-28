package com.example.learning.english.learning.english.app.domain.service

import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.ReviewId
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.engine.LearningStateProgressor
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import com.example.learning.english.learning.english.app.domain.repository.ReviewRepository
import com.example.learning.english.learning.english.app.domain.scheduler.ReviewScheduler
import java.util.UUID

/**
 * Orquesta un repaso de una expresión (Fase 4).
 *
 * 1. Carga el [LearningState] actual (o lo crea si la expresión es nueva).
 * 2. Calcula el próximo repaso con [ReviewScheduler].
 * 3. Actualiza puntuaciones y etapa con [LearningStateProgressor] sobre el estado
 *    ya programado: el intervalo no se toca y el `reviewCount` ya está contado.
 * 4. Persiste el nuevo estado (la fecha futura sobrevive al reinicio, §72).
 * 5. Registra el [ReviewEvent] histórico, append-only (§32), para que las
 *    métricas y el debugging del scheduler no dependan del último score.
 *
 * Es el único camino de escritura del estado de aprendizaje: el
 * `LearningEngine.registerAnswer` (Fase 6) delega aquí todo su trabajo.
 */
class ReviewRecorder(
    private val scheduler: ReviewScheduler,
    private val progressor: LearningStateProgressor,
    private val learningStateRepository: LearningStateRepository,
    private val reviewRepository: ReviewRepository,
) {

    suspend fun record(
        expressionId: ExpressionId,
        reviewType: ReviewType,
        rating: ReviewRating,
        reviewedAt: Long,
        sessionId: SessionId? = null,
        responseTimeMs: Long? = null,
    ): LearningState {
        val previous = learningStateRepository.getByExpression(expressionId) ?: LearningState.new(expressionId)
        val updated = progressor.apply(scheduler.schedule(previous, rating, reviewedAt), reviewType, rating)
        learningStateRepository.upsert(updated)
        reviewRepository.record(
            ReviewEvent(
                id = ReviewId(UUID.randomUUID().toString()),
                expressionId = expressionId,
                reviewType = reviewType,
                rating = rating,
                reviewedAt = reviewedAt,
                sessionId = sessionId,
                responseTimeMs = responseTimeMs,
                previousStage = previous.stage,
                newStage = updated.stage,
            ),
        )
        return updated
    }
}