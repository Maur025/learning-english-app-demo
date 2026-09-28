package com.example.learning.english.learning.english.app.domain.model

/**
 * Respuesta del usuario a un ejercicio (README §37).
 *
 * `Choice` solo tiene sentido en ejercicios de opción múltiple y `Text` en los
 * escritos; `ExerciseGenerator` y `AnswerEvaluator` usan esa distinción para que
 * una respuesta escrita no se pueda evaluar contra un ejercicio de opciones.
 */
sealed interface UserAnswer {

    /** Texto libre: cloze, guided recall, translation o production. */
    data class Text(val value: String) : UserAnswer

    /** Opción elegida en recognition. */
    data class Choice(val optionId: String) : UserAnswer
}
