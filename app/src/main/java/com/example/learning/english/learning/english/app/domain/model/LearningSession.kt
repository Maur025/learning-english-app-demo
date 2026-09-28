package com.example.learning.english.learning.english.app.domain.model

/**
 * Sesión con su punto de avance.
 *
 * [currentPosition] es el índice del siguiente ejercicio sin responder: sobrevive
 * a la muerte del proceso porque se persiste en `learning_sessions`, de modo que
 * "continuar sesión" no depende de reconstruir nada (README §37, §72).
 *
 * Un listado de sesiones recientes llega sin ejercicios, así que el avance solo
 * tiene sentido con la lista cargada; [nextExercise] y [advancedAfter] son `null`
 * o fallan en ese caso por diseño.
 */
data class LearningSession(
    val id: SessionId,
    val startedAt: Long,
    val completedAt: Long? = null,
    val currentPosition: Int = 0,
    val exercises: List<Exercise> = emptyList(),
) {
    init {
        require(currentPosition >= 0) { "currentPosition must not be negative" }
        require(exercises.map { it.position } == exercises.indices.toList()) {
            "exercises must be ordered with contiguous positions starting at 0"
        }
    }

    val isCompleted: Boolean
        get() = completedAt != null

    val nextExercise: Exercise?
        get() = exercises.getOrNull(currentPosition)

    /**
     * Avanza el progreso tras responder al ejercicio de [answeredPosition].
     *
     * Exigir que sea el ejercicio actual evita que un doble toque registre dos
     * respuestas sobre el mismo paso. La sesión se sella al responder el último.
     */
    fun advancedAfter(answeredPosition: Int, answeredAt: Long): LearningSession {
        require(answeredPosition == currentPosition) {
            "cannot answer position $answeredPosition while the session is at $currentPosition"
        }
        val nextPosition = currentPosition + 1
        return copy(
            currentPosition = nextPosition,
            completedAt = if (nextPosition == exercises.size) answeredAt else completedAt,
        )
    }
}
