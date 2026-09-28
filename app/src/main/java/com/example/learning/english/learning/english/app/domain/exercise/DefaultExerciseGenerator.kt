package com.example.learning.english.learning.english.app.domain.exercise

import com.example.learning.english.learning.english.app.domain.model.Example
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ReviewType

/**
 * Generador determinista de los cinco tipos de ejercicio (README §33).
 *
 * Reglas de contenido:
 * - recognition necesita al menos un significado distinto como distractor;
 * - cloze oculta la frase dentro de un ejemplo real; si la frase no aparece,
 *   cae a un cloze de la propia expresión, y si tampoco es viable devuelve `null`;
 * - guided recall construye la pista de iniciales sobre el mismo ejemplo;
 * - translation usa la traducción del ejemplo y, si falta, el significado;
 * - production nunca se autocalifica: solo describe la situación y las
 *   respuestas naturales (README §35).
 *
 * No usa aleatoriedad: los distractores y la posición de la respuesta correcta
 * se derivan del `id` de la expresión, de modo que el mismo contenido produce
 * siempre el mismo ejercicio y los tests son estables.
 */
class DefaultExerciseGenerator : ExerciseGenerator {

    override fun generate(
        expression: Expression,
        reviewType: ReviewType,
        distractorPool: List<Expression>,
    ): PracticeExercise? = when (reviewType) {
        ReviewType.RECOGNITION -> recognitionExercise(expression, distractorPool)
        ReviewType.CLOZE -> clozeExercise(expression)
        ReviewType.GUIDED_RECALL -> guidedRecallExercise(expression)
        ReviewType.TRANSLATION -> translationExercise(expression)
        ReviewType.PRODUCTION -> productionExercise(expression)
    }

    private fun recognitionExercise(expression: Expression, pool: List<Expression>): RecognitionExercise? {
        val distractors = pickDistractors(expression, pool)
        if (distractors.isEmpty()) return null
        val correct = AnswerOption(id = expression.id.value, text = expression.primaryMeaning)
        val options = listOf(correct) + distractors.map { AnswerOption(it.id.value, it.primaryMeaning) }
        return RecognitionExercise(
            expressionId = expression.id,
            prompt = expression.phrase,
            options = placeCorrectOption(options, correct.id, expression.id.value.hashCode()),
            correctOptionId = correct.id,
            reveal = reveal(expression, listOf(expression.primaryMeaning)),
        )
    }

    private fun clozeExercise(expression: Expression): ClozeExercise? {
        val example = expression.examples.firstOrNull()
        val span = example?.let { findSpan(it.english, expression.phrase) }
        if (example != null && span != null) {
            val sentence = span.blankIn(example.english)
            if (hasVisibleWords(sentence)) {
                val hidden = span.substringIn(example.english)
                return ClozeExercise(
                    expressionId = expression.id,
                    sentence = sentence,
                    hiddenAnswer = hidden,
                    // El hueco cubre la frase entera, así que la frase es la respuesta;
                    // el evaluador normaliza y acepta también la forma del ejemplo.
                    reveal = reveal(expression, listOf(expression.phrase), example),
                )
            }
        }
        return phraseCloze(expression)
    }

    /**
     * Fallback del cloze: oculta solo la primera palabra, p. ej. `______ out`.
     * La respuesta sigue siendo la frase entera, no la palabra oculta.
     */
    private fun phraseCloze(expression: Expression): ClozeExercise? {
        val firstWordEnd = expression.phrase.indexOf(' ')
        if (firstWordEnd <= 0) return null
        val visible = expression.phrase.substring(firstWordEnd).trim()
        if (visible.isEmpty()) return null
        return ClozeExercise(
            expressionId = expression.id,
            sentence = "$BLANK $visible",
            hiddenAnswer = expression.phrase.substring(0, firstWordEnd),
            reveal = reveal(expression, listOf(expression.phrase)),
        )
    }

    private fun guidedRecallExercise(expression: Expression): GuidedRecallExercise {
        val example = expression.examples.firstOrNull()
        val span = example?.let { findSpan(it.english, expression.phrase) }
        val hint = if (example != null && span != null) {
            span.replaceIn(example.english, skeleton(span.substringIn(example.english)))
        } else {
            skeleton(expression.phrase)
        }
        return GuidedRecallExercise(
            expressionId = expression.id,
            meaning = expression.primaryMeaning,
            hint = hint,
            reveal = reveal(expression, listOf(expression.phrase), example),
        )
    }

    private fun translationExercise(expression: Expression): TranslationExercise {
        val example = expression.examples.firstOrNull()
        val prompt = example?.spanish?.takeIf { it.isNotBlank() } ?: expression.primaryMeaning
        val expected = example?.let { listOf(it.english) } ?: listOf(expression.phrase)
        return TranslationExercise(
            expressionId = expression.id,
            prompt = prompt,
            reveal = reveal(expression, expected, example),
        )
    }

    private fun productionExercise(expression: Expression): ProductionExercise {
        val example = expression.examples.firstOrNull()
        return ProductionExercise(
            expressionId = expression.id,
            situation = expression.explanation?.takeIf { it.isNotBlank() } ?: expression.primaryMeaning,
            guidance = example?.context?.takeIf { it.isNotBlank() },
            reveal = reveal(expression, listOfNotNull(example?.english, expression.phrase), example),
        )
    }

    private fun reveal(expression: Expression, expectedAnswers: List<String>, example: Example? = null) =
        RevealContent(
            expectedAnswers = expectedAnswers.distinct(),
            explanation = expression.explanation,
            pattern = expression.patterns.firstOrNull()?.pattern,
            example = example ?: expression.examples.firstOrNull(),
        )

    /**
     * Distractores con un significado distinto al correcto, en orden estable y
     * desde un desplazamiento derivado del id de la expresión.
     */
    private fun pickDistractors(expression: Expression, pool: List<Expression>): List<Expression> {
        val correctMeaning = TextNormalizer.normalize(expression.primaryMeaning)
        val candidates = pool
            .asSequence()
            .filter { it.id != expression.id }
            .filter { it.primaryMeaning.isNotBlank() }
            .filter { TextNormalizer.normalize(it.primaryMeaning) != correctMeaning }
            .sortedBy { it.id.value }
            .toList()
        if (candidates.size <= MAX_DISTRACTORS) return candidates
        val start = Math.floorMod(expression.id.value.hashCode(), candidates.size)
        return List(MAX_DISTRACTORS) { index -> candidates[(start + index) % candidates.size] }
    }

    /** Mueve la respuesta correcta a una posición estable, pero no siempre la misma. */
    private fun placeCorrectOption(
        options: List<AnswerOption>,
        correctOptionId: String,
        seed: Int,
    ): List<AnswerOption> {
        val target = Math.floorMod(seed, options.size)
        val current = options.indexOfFirst { it.id == correctOptionId }
        val shift = Math.floorMod(target - current, options.size)
        return options.drop(shift) + options.take(shift)
    }

    /** `out` -> `o--`, `that...` -> `t---...`: conserva los signos de puntuación. */
    private fun skeleton(text: String): String = text.split(' ').joinToString(" ") { word ->
        val core = word.takeWhile { it.isLetterOrDigit() }
        if (core.isEmpty()) {
            word
        } else {
            val hidden = HIDDEN_CHARACTER.repeat(core.length - 1)
            core.first() + hidden + word.substring(core.length)
        }
    }

    private fun findSpan(text: String, phrase: String): PhraseSpan? {
        val start = text.indexOf(phrase, ignoreCase = true)
        return if (start < 0) null else PhraseSpan(start, start + phrase.length)
    }

    /** Un cloze solo sirve si deja palabras visibles que den contexto. */
    private fun hasVisibleWords(sentence: String): Boolean = sentence.any { it.isLetter() }

    /** Zona de texto ocupada por la frase buscada. */
    private data class PhraseSpan(val start: Int, val endExclusive: Int) {
        fun substringIn(text: String): String = text.substring(start, endExclusive)

        fun blankIn(text: String): String = replaceIn(text, BLANK)

        fun replaceIn(text: String, replacement: String): String =
            text.replaceRange(start, endExclusive, replacement)
    }

    private companion object {
        const val BLANK = "______"
        const val HIDDEN_CHARACTER = "-"
        const val MAX_DISTRACTORS = 3
    }
}
