package com.example.learning.english.learning.english.app.domain.model

/**
 * Progresión conceptual de aprendizaje (README §3.2).
 *
 * El orden de avance y las transiciones pertenecen a `LearningEngine`;
 * este enum solo describe en qué etapa está una expresión.
 */
enum class LearningStage {
    NEW,
    SEEN,
    RECOGNIZED,
    RECALLABLE,
    PRODUCTIVE,
    MASTERED,
}
