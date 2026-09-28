package com.example.learning.english.learning.english.app.domain.exercise

import com.example.learning.english.learning.english.app.domain.model.CefrLevel
import com.example.learning.english.learning.english.app.domain.model.Difficulty
import com.example.learning.english.learning.english.app.domain.model.Example
import com.example.learning.english.learning.english.app.domain.model.ExampleId
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ExpressionPattern
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.PatternId

/** Expresiones de ejemplo para los tests del motor de ejercicios. */
internal object ExerciseFixtures {

    const val FIGURE_OUT_ID = "figure-out"
    const val PUT_OFF_ID = "put-off"
    const val RUN_INTO_ID = "run-into"

    val figureOut = expression(
        id = FIGURE_OUT_ID,
        phrase = "figure out",
        meaning = "averiguar, descubrir, resolver",
        explanation = "Use it when you find the answer to a question or the cause of a problem.",
        patterns = listOf("figure + object + out"),
        examples = listOf(
            example(
                english = "I'm trying to figure out what happened last night.",
                spanish = "Estoy intentando averiguar qué pasó anoche.",
                context = "general",
            ),
        ),
    )

    val putOff = expression(
        id = PUT_OFF_ID,
        phrase = "put off",
        meaning = "aplazar, posponer",
        examples = listOf(
            example(
                english = "I keep putting off writing the summary.",
                spanish = "Sigo aplazando escribir el resumen.",
                context = "work",
            ),
        ),
    )

    val runInto = expression(
        id = RUN_INTO_ID,
        phrase = "run into",
        meaning = "encontrarse con, toparse con (un problema)",
    )

    val withoutExamples = expression(
        id = "be-used-to",
        phrase = "be used to",
        meaning = "estar acostumbrado a",
    )

    val singleWord = expression(
        id = "deadline",
        phrase = "deadline",
        meaning = "fecha límite",
    )

    fun expression(
        id: String,
        phrase: String,
        meaning: String,
        explanation: String? = null,
        patterns: List<String> = emptyList(),
        examples: List<Example> = emptyList(),
    ) = Expression(
        id = ExpressionId(id),
        phrase = phrase,
        primaryMeaning = meaning,
        packId = PackId("core-english"),
        difficulty = Difficulty.MEDIUM,
        level = CefrLevel.B1,
        explanation = explanation,
        examples = examples,
        patterns = patterns.map { pattern ->
            ExpressionPattern(
                id = PatternId("$id-$pattern"),
                expressionId = ExpressionId(id),
                pattern = pattern,
            )
        },
    )

    fun example(
        english: String,
        spanish: String? = null,
        context: String? = null,
        isPrimary: Boolean = true,
        expressionId: String = FIGURE_OUT_ID,
    ) = Example(
        id = ExampleId("$expressionId-example"),
        expressionId = ExpressionId(expressionId),
        english = english,
        spanish = spanish,
        context = context,
        isPrimary = isPrimary,
    )

    /** Pool de distractores con significados distintos. */
    val distractorPool = listOf(putOff, runInto, withoutExamples, singleWord)
}
