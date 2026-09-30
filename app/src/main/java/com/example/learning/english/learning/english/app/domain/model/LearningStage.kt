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

/**
 * `true` cuando llegar a esta etapa desde [stage] es avanzar en la escalera.
 *
 * El orden de las constantes es la progresión conceptual (§3.2), así que el
 * ordinal compara etapas. Se expone como función para que quien lee no tenga que
 * saber eso: la progresión puede retroceder cuando la expresión se olvida, y una
 * comparación desnuda no lo distingue de un empate.
 */
fun LearningStage.advancedFrom(stage: LearningStage): Boolean = stage.ordinal < ordinal
