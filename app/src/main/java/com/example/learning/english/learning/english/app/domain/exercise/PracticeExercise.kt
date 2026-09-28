package com.example.learning.english.learning.english.app.domain.exercise

import com.example.learning.english.learning.english.app.domain.model.Example
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ReviewType

/**
 * Ejercicio listo para renderizar, derivado de una `Expression` y un [ReviewType].
 *
 * No se persiste: `ExerciseGenerator` lo reconstruye de forma determinista a
 * partir del contenido, así que sobrevive a la muerte de proceso sin guardar
 * payload en Room. El `Exercise` de `session_exercises` sigue siendo solo la
 * referencia (expresión + tipo + posición).
 */
sealed interface PracticeExercise {
    val expressionId: ExpressionId
    val reviewType: ReviewType

    /** Contenido que se muestra después de responder, para Reveal + autoevaluación. */
    val reveal: RevealContent

    /** Coste estimado del ejercicio, derivado de [ReviewType.estimatedSeconds] (§39). */
    val estimatedSeconds: Int
        get() = reviewType.estimatedSeconds
}

/** Ejercicios que el usuario responde escribiendo texto libre. */
sealed interface WrittenExercise : PracticeExercise

/** Recognition: el usuario elige entre significados. */
data class RecognitionExercise(
    override val expressionId: ExpressionId,
    val prompt: String,
    val options: List<AnswerOption>,
    val correctOptionId: String,
    override val reveal: RevealContent,
) : PracticeExercise {
    override val reviewType: ReviewType = ReviewType.RECOGNITION

    init {
        require(prompt.isNotBlank()) { "prompt must not be blank" }
        require(options.size >= 2) { "recognition needs the correct option plus a distractor" }
        require(options.map { it.id }.toSet().size == options.size) { "option ids must be unique" }
        require(options.any { it.id == correctOptionId }) { "correctOptionId must be one of the options" }
    }

    val correctOption: AnswerOption
        get() = options.first { it.id == correctOptionId }
}

/** Cloze: parte de la frase oculta dentro de una frase real (§33). */
data class ClozeExercise(
    override val expressionId: ExpressionId,
    val sentence: String,
    val hiddenAnswer: String,
    override val reveal: RevealContent,
) : WrittenExercise {
    override val reviewType: ReviewType = ReviewType.CLOZE

    init {
        require(sentence.isNotBlank()) { "sentence must not be blank" }
        require(hiddenAnswer.isNotBlank()) { "hiddenAnswer must not be blank" }
    }
}

/** Guided recall: significado + esqueleto de iniciales como pista (§33). */
data class GuidedRecallExercise(
    override val expressionId: ExpressionId,
    val meaning: String,
    val hint: String,
    override val reveal: RevealContent,
) : WrittenExercise {
    override val reviewType: ReviewType = ReviewType.GUIDED_RECALL

    init {
        require(meaning.isNotBlank()) { "meaning must not be blank" }
        require(hint.isNotBlank()) { "hint must not be blank" }
    }
}

/** Translation: se da el enunciado en español y se produce inglés (§33). */
data class TranslationExercise(
    override val expressionId: ExpressionId,
    val prompt: String,
    override val reveal: RevealContent,
) : WrittenExercise {
    override val reviewType: ReviewType = ReviewType.TRANSLATION

    init {
        require(prompt.isNotBlank()) { "prompt must not be blank" }
    }
}

/** Production: se da una situación y se produce una frase propia (§33, §35). */
data class ProductionExercise(
    override val expressionId: ExpressionId,
    val situation: String,
    val guidance: String? = null,
    override val reveal: RevealContent,
) : WrittenExercise {
    override val reviewType: ReviewType = ReviewType.PRODUCTION

    init {
        require(situation.isNotBlank()) { "situation must not be blank" }
    }
}

data class AnswerOption(
    val id: String,
    val text: String,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(text.isNotBlank()) { "text must not be blank" }
    }
}

/**
 * Contenido de revelado posterior a la respuesta.
 *
 * `expectedAnswers` son las respuestas aceptadas; la primera es la respuesta
 * natural que se muestra como modelo. La evaluación automática solo es válida
 * para recognition, cloze, guided recall y translation (§35).
 */
data class RevealContent(
    val expectedAnswers: List<String>,
    val explanation: String? = null,
    val pattern: String? = null,
    val example: Example? = null,
) {
    init {
        require(expectedAnswers.isNotEmpty()) { "expectedAnswers must not be empty" }
        require(expectedAnswers.all { it.isNotBlank() }) { "expectedAnswers must not be blank" }
    }

    val expectedAnswer: String
        get() = expectedAnswers.first()
}
