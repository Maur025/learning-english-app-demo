package com.example.learning.english.learning.english.app.domain.exercise

import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ReviewType

/**
 * Convierte contenido en práctica (Fase 5, README §13 "domain").
 *
 * Es una función pura: recibe la expresión, el tipo de ejercicio y el material
 * que la UI ya tiene cargada. `distractorPool` son las expresiones candidatas a
 * significado incorrecto de un reconocimiento; si no hay al menos una con
 * significado distinto, se devuelve `null`.
 *
 * Devolver `null` significa "este contenido no da para este tipo de ejercicio".
 * Quien compone la sesión (Fase 6) decide entonces usar otro `ReviewType` en
 * lugar de construir un ejercicio degenerado.
 */
interface ExerciseGenerator {
    fun generate(
        expression: Expression,
        reviewType: ReviewType,
        distractorPool: List<Expression> = emptyList(),
    ): PracticeExercise?
}
