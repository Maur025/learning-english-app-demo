package com.example.learning.english.learning.english.app.domain.engine

import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures
import com.example.learning.english.learning.english.app.domain.model.CefrLevel
import com.example.learning.english.learning.english.app.domain.model.Difficulty
import com.example.learning.english.learning.english.app.domain.model.Example
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.LearningState

/**
 * Contenido y estados de prueba del motor de sesión: dos packs con expresiones
 * de significado distinto (para que recognition tenga distractores), una palabra
 * suelta sin ejemplos (que no sostiene ni recognition ni cloze) y estados en
 * varias etapas de la escalera.
 */
internal object EngineFixtures {

    const val CORE_PACK = "core-english"
    const val DEVELOPER_PACK = "developer-english"

    val figureOut = expression(
        id = "figure-out",
        phrase = "figure out",
        meaning = "averiguar, descubrir, resolver",
        examples = listOf(example("figure-out", "I'm trying to figure out what happened last night.")),
    )

    val runInto = expression(
        id = "run-into",
        phrase = "run into",
        meaning = "encontrarse con, toparse con (un problema)",
        examples = listOf(example("run-into", "We ran into a problem with the build.")),
    )

    val putOff = expression(
        id = "put-off",
        phrase = "put off",
        meaning = "aplazar, posponer",
        difficulty = Difficulty.HARD,
        level = CefrLevel.B2,
        examples = listOf(example("put-off", "I keep putting off writing the summary.")),
    )

    /** Palabra suelta y sin ejemplos: no da para recognition ni para cloze. */
    val deadline = expression(
        id = "deadline",
        phrase = "deadline",
        meaning = "fecha límite",
        difficulty = Difficulty.EASY,
        level = CefrLevel.A2,
    )

    val shipIt = expression(
        id = "ship-it",
        phrase = "ship it",
        meaning = "lanzarlo, ponerlo en producción",
        packId = DEVELOPER_PACK,
        examples = listOf(example("ship-it", "Let's ship it before the release window closes.")),
    )

    val codeReview = expression(
        id = "code-review",
        phrase = "code review",
        meaning = "revisión de código",
        packId = DEVELOPER_PACK,
        difficulty = Difficulty.EASY,
    )

    val all = listOf(figureOut, runInto, putOff, deadline, shipIt, codeReview)

    fun state(
        expressionId: String,
        stage: LearningStage,
        nextReviewAt: Long? = null,
        recognitionScore: Int = 0,
        productionScore: Int = 0,
        reviewCount: Int = 1,
        failedReviewCount: Int = 0,
    ) = LearningState(
        expressionId = ExpressionId(expressionId),
        stage = stage,
        recognitionScore = recognitionScore,
        productionScore = productionScore,
        nextReviewAt = nextReviewAt,
        reviewCount = reviewCount,
        failedReviewCount = failedReviewCount,
    )

    fun expressions(count: Int, packId: String = CORE_PACK): List<Expression> = (1..count).map { index ->
        expression(
            id = "bulk-$index",
            phrase = "bulk phrase $index",
            meaning = "significado $index",
            packId = packId,
        )
    }

    private fun expression(
        id: String,
        phrase: String,
        meaning: String,
        packId: String = CORE_PACK,
        difficulty: Difficulty = Difficulty.MEDIUM,
        level: CefrLevel? = CefrLevel.B1,
        examples: List<Example> = emptyList(),
    ) = ExerciseFixtures.expression(
        id = id,
        phrase = phrase,
        meaning = meaning,
        packId = packId,
        difficulty = difficulty,
        level = level,
        examples = examples,
    )

    private fun example(expressionId: String, english: String) =
        ExerciseFixtures.example(expressionId = expressionId, english = english)
}
