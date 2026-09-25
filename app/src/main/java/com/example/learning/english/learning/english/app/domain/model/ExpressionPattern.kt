package com.example.learning.english.learning.english.app.domain.model

/** Estructura reutilizable de una expresión, por ejemplo `figure + object + out`. */
data class ExpressionPattern(
    val id: PatternId,
    val expressionId: ExpressionId,
    val pattern: String,
    val explanation: String? = null,
) {
    init {
        require(pattern.isNotBlank()) { "pattern must not be blank" }
    }
}
