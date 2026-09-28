package com.example.learning.english.learning.english.app.domain.service

import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.ReviewId
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType
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
 * 3. Persiste el nuevo estado (la fecha futura sobrevive al reinicio, §72).
 * 4. Registra el [ReviewEvent] histórico, append-only (§32), para que las
 *    métricas y el debugging del scheduler no dependan del último score.
 *
 * `LearningEngine.registerAnswer` (Fase 6) delegará aquí parte de su trabajo.
 */
class ReviewRecorder(
    private val scheduler: ReviewScheduler,
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
        val updated = scheduler.schedule(previous, rating, reviewedAt)
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