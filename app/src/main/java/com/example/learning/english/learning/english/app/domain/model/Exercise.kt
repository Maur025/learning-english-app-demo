package com.example.learning.english.learning.english.app.domain.model

/** Ejercicio de una sesión. El contenido concreto lo genera `LearningEngine` más adelante. */
data class Exercise(
    val sessionId: SessionId,
    val position: Int,
    val expressionId: ExpressionId,
    val reviewType: ReviewType,
) {
    init {
        require(position >= 0) { "position must not be negative" }
    }

    val skill: ReviewSkill
        get() = reviewType.skill
}
