package com.example.learning.english.learning.english.app.domain.model

data class Example(
    val id: ExampleId,
    val expressionId: ExpressionId,
    val english: String,
    val spanish: String? = null,
    val context: String? = null,
    /** `null` significa que hereda la dificultad de la expresión. */
    val difficulty: Difficulty? = null,
    val isPrimary: Boolean = false,
) {
    init {
        require(english.isNotBlank()) { "english must not be blank" }
    }
}
