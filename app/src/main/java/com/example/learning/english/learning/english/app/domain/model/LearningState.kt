package com.example.learning.english.learning.english.app.domain.model

/**
 * Estado de aprendizaje de una expresión.
 *
 * Reconocimiento y producción se puntúan por separado (§3.3). Los campos de
 * planificación (`nextReviewAt`, `currentIntervalDays`, `easeFactor`) están
 * aislados aquí para poder migrar de algoritmo de spaced repetition.
 */
data class LearningState(
    val expressionId: ExpressionId,
    val stage: LearningStage = LearningStage.NEW,
    val recognitionScore: Int = 0,
    val productionScore: Int = 0,
    /** `null` mientras la expresión no esté programada para revisión. */
    val nextReviewAt: Long? = null,
    val lastReviewedAt: Long? = null,
    val reviewCount: Int = 0,
    val successfulReviewCount: Int = 0,
    val failedReviewCount: Int = 0,
    val currentIntervalDays: Int = 0,
    val easeFactor: Double = DEFAULT_EASE_FACTOR,
    val updatedAt: Long = 0L,
) {
    init {
        require(recognitionScore in SCORE_RANGE) { "recognitionScore must be in $SCORE_RANGE" }
        require(productionScore in SCORE_RANGE) { "productionScore must be in $SCORE_RANGE" }
        require(reviewCount >= 0) { "reviewCount must not be negative" }
        require(successfulReviewCount >= 0) { "successfulReviewCount must not be negative" }
        require(failedReviewCount >= 0) { "failedReviewCount must not be negative" }
        require(currentIntervalDays >= 0) { "currentIntervalDays must not be negative" }
        require(easeFactor > 0.0) { "easeFactor must be positive" }
    }

    companion object {
        const val SCORE_MIN = 0
        const val SCORE_MAX = 100
        val SCORE_RANGE = SCORE_MIN..SCORE_MAX
        const val DEFAULT_EASE_FACTOR = 2.5

        fun new(expressionId: ExpressionId): LearningState = LearningState(expressionId)
    }
}
