package com.example.learning.english.learning.english.app.domain.exercise

import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.UserAnswer

/**
 * Evaluación determinista de respuestas (README §35).
 *
 * Recognition se califica por opción elegida; cloze, guided recall y translation
 * por comparación normalizada contra las respuestas aceptadas. Production no se
 * califica: devuelve [AnswerEvaluation.SelfAssessed] para que la UI revele las
 * respuestas naturales y el usuario se autoevalúe, en lugar de fingir que un
 * comparador de cadenas entiende inglés.
 *
 * Es sin estado y sin dependencias.
 */
class AnswerEvaluator {

    fun evaluateChoice(exercise: RecognitionExercise, answer: UserAnswer.Choice): AnswerEvaluation.Graded {
        val match = if (answer.optionId == exercise.correctOptionId) MatchKind.EXACT else MatchKind.INCORRECT
        return AnswerEvaluation.Graded(
            expectedAnswers = exercise.reveal.expectedAnswers,
            match = match,
            normalizedAnswer = null,
        )
    }

    fun evaluateText(exercise: WrittenExercise, answer: UserAnswer.Text): AnswerEvaluation {
        val normalized = TextNormalizer.normalize(answer.value)
        return if (exercise is ProductionExercise) {
            AnswerEvaluation.SelfAssessed(exercise.reveal.expectedAnswers, normalized)
        } else {
            grade(exercise.reveal.expectedAnswers, answer.value, normalized)
        }
    }

    private fun grade(
        expectedAnswers: List<String>,
        rawAnswer: String,
        normalized: String,
    ): AnswerEvaluation.Graded {
        val match = when {
            normalized.isEmpty() -> MatchKind.INCORRECT
            expectedAnswers.any { it.trim() == rawAnswer.trim() } -> MatchKind.EXACT
            expectedAnswers.any { TextNormalizer.matches(normalized, it) } -> MatchKind.NORMALIZED
            else -> MatchKind.INCORRECT
        }
        return AnswerEvaluation.Graded(expectedAnswers, match, normalized)
    }
}

/**
 * Resultado de evaluar una respuesta.
 *
 * [expectedAnswers] viaja siempre en el resultado porque la UI necesita revelar
 * la respuesta natural después de responder, tanto si acertó como si falló.
 */
sealed interface AnswerEvaluation {
    val expectedAnswers: List<String>

    /** Respuesta natural que se revela a la UI tras responder. */
    val expectedAnswer: String
        get() = expectedAnswers.first()

    /**
     * Calificación que la UI propone antes de que el usuario confirme.
     *
     * Existe para que el flujo sea el mismo en los cinco tipos (§43.4): el
     * ejercicio se revela siempre y la calificación se confirma siempre. En
     * producción es `null` porque no hay veredicto automático, así que la
     * elección es del usuario y no un dato que la app pueda deducir (§35).
     */
    val suggestedRating: ReviewRating?

    /** Calificación automática: recognition, cloze, guided recall o translation. */
    data class Graded(
        override val expectedAnswers: List<String>,
        val match: MatchKind,
        /** Respuesta del usuario ya normalizada; `null` cuando se eligió una opción. */
        val normalizedAnswer: String?,
    ) : AnswerEvaluation {
        val isCorrect: Boolean
            get() = match != MatchKind.INCORRECT

        override val suggestedRating: ReviewRating
            get() = if (isCorrect) ReviewRating.GOOD else ReviewRating.FORGOT
    }

    /** Production: la respuesta se muestra y el usuario se autocalifica (§35). */
    data class SelfAssessed(
        override val expectedAnswers: List<String>,
        val normalizedAnswer: String?,
    ) : AnswerEvaluation {
        override val suggestedRating: ReviewRating?
            get() = null
    }
}

enum class MatchKind {
    /** Idéntica al texto esperado, sin normalizar. */
    EXACT,

    /** Distingue solo por mayúsculas, espacios, tipografía o puntuación terminal. */
    NORMALIZED,

    /** Sin coincidencia: la UI revela la respuesta y el usuario se autocalifica. */
    INCORRECT,
}
