package com.example.learning.english.learning.english.app.domain.model

/**
 * Evento de revisión histórico (§32).
 *
 * Se guarda cada revisión, no solo el último score: alimenta el progreso,
 * el debugging del scheduler y una futura migración de algoritmo.
 */
data class ReviewEvent(
    val id: ReviewId,
    val expressionId: ExpressionId,
    val reviewType: ReviewType,
    val rating: ReviewRating,
    val reviewedAt: Long,
    val sessionId: SessionId? = null,
    val responseTimeMs: Long? = null,
    val previousStage: LearningStage = LearningStage.NEW,
    val newStage: LearningStage = LearningStage.NEW,
) {
    /** Derivado de la calificación para que ambos nunca se contradigan. */
    val isCorrect: Boolean
        get() = rating.isCorrect
}
