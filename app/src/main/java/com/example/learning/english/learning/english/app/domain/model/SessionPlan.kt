package com.example.learning.english.learning.english.app.domain.model

/**
 * Por qué una expresión entra en la sesión de hoy (README §38).
 */
enum class SessionReason {
    /** Su revisión programada era ahora o antes: es lo prioritario. */
    OVERDUE_REVIEW,

    /** Todavía no vencida, pero acumula olvidos: se refuerza con una pista. */
    WEAK,

    /** Ya reconocible y no vencida: lo que flojea es producir. */
    PRODUCTION_RECALL,

    /** Sin ninguna revisión todavía. */
    NEW,
}

/**
 * Un paso del plan: qué expresión practicar, con qué nivel de exigencia y por qué.
 *
 * [estimatedSeconds] es el presupuesto que consume el paso (§39): una expresión
 * nueva cuesta más porque hay que presentarla, no solo reconocerla.
 */
data class SessionStep(
    val expressionId: ExpressionId,
    val reviewType: ReviewType,
    val reason: SessionReason,
    val estimatedSeconds: Int,
) {
    init {
        require(estimatedSeconds > 0) { "estimatedSeconds must be positive" }
    }
}

/**
 * Plan de una sesión diaria: los pasos ordenados que caben en el presupuesto
 * diario (README §38–39).
 *
 * Es un resultado intermedio: la sesión persistida solo guarda la referencia de
 * cada ejercicio, porque el contenido se reconstruye de forma determinista.
 */
data class SessionPlan(
    val steps: List<SessionStep> = emptyList(),
) {
    val estimatedSeconds: Int
        get() = steps.sumOf { it.estimatedSeconds }

    val isEmpty: Boolean
        get() = steps.isEmpty()

    fun stepsOf(reason: SessionReason): List<SessionStep> = steps.filter { it.reason == reason }
}
