package com.example.learning.english.learning.english.app.domain.engine

import com.example.learning.english.learning.english.app.domain.exercise.ExerciseGenerator
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.ReviewSkill
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionPlan
import com.example.learning.english.learning.english.app.domain.model.SessionReason
import com.example.learning.english.learning.english.app.domain.model.SessionStep
import com.example.learning.english.learning.english.app.domain.model.UserPreferences

/**
 * Compone la sesión diaria a partir del contenido y del estado de aprendizaje
 * (README §38–41).
 *
 * Es una función pura: recibe las expresiones, sus estados, las preferencias y
 * `now`, y devuelve el plan. No toca repositorios ni reloj, así que se prueba
 * sin instrumentación ni dobles de test.
 *
 * Prioridad de los candidatos, de más a menos apremiante:
 *
 * ```text
 * 1. Revisiones vencidas       la más vencida primero
 * 2. Expresiones débiles       no vencidas con olvidos acumulados
 * 3. Recordatorio productivo  no vencidas y flojas produciendo
 * 4. Contenido nuevo           limitado por la presión de revisiones
 * ```
 *
 * Cada expresión aparece una sola vez, y el presupuesto se rellena saltando el
 * paso que no cabe en lugar de recortar la cola: así se respeta la prioridad sin
 * dejar la sesión corta.
 *
 * El planificador decide *qué* practicar, no *si* se puede mostrar: valida cada
 * paso con [ExerciseGenerator] porque un tipo que el contenido no puede sostener
 * (un recognition sin distractores, un cloze sin frase que lo sostenga) no es un
 * paso válido. El ejercicio concreto se descarta y la UI lo reconstruye de
 * forma determinista a partir del mismo par expresión + tipo.
 */
class DailySessionPlanner(
    private val exerciseGenerator: ExerciseGenerator,
) {

    fun plan(
        expressions: List<Expression>,
        states: List<LearningState>,
        preferences: UserPreferences,
        now: Long,
    ): SessionPlan {
        val selected = expressions.filter { it.packId in selectedPackIds(expressions, preferences) }
        if (selected.isEmpty()) return SessionPlan()

        val statesById = states.associateBy { it.expressionId }
        val productionDeficit = hasProductionDeficit(states)
        val due = selected.dueCandidates(statesById, now, productionDeficit)
        val newLimit = newContentLimit(due.size, preferences.newExpressionsPerDay)
        val candidates = buildList {
            addAll(due)
            addAll(selected.weakCandidates(statesById, now, productionDeficit))
            addAll(selected.productionCandidates(statesById, now, productionDeficit))
            addAll(selected.newCandidates(statesById, newLimit))
        }

        val budgetSeconds = preferences.dailyGoalMinutes * SECONDS_PER_MINUTE
        val steps = mutableListOf<SessionStep>()
        val planned = mutableSetOf<ExpressionId>()
        var spentSeconds = 0
        for (candidate in candidates) {
            if (steps.size >= MAX_STEPS || !planned.add(candidate.expression.id)) continue
            val reviewType = candidate.renderableType(selected) ?: continue
            val estimatedSeconds = candidate.costOf(reviewType)
            if (spentSeconds + estimatedSeconds > budgetSeconds) continue
            spentSeconds += estimatedSeconds
            steps += SessionStep(
                expressionId = candidate.expression.id,
                reviewType = reviewType,
                reason = candidate.reason,
                estimatedSeconds = estimatedSeconds,
            )
        }
        return SessionPlan(steps)
    }

    private fun selectedPackIds(expressions: List<Expression>, preferences: UserPreferences): Set<PackId> =
        preferences.preferredPackIds.ifEmpty { expressions.mapTo(mutableSetOf()) { it.packId } }

    /** 1. Vencidas: primero la que más se aplazó. */
    private fun List<Expression>.dueCandidates(
        statesById: Map<ExpressionId, LearningState>,
        now: Long,
        productionDeficit: Boolean,
    ) = mapNotNull { expression ->
        val state = statesById[expression.id]
        if (state != null && state.isDue(now)) {
            Candidate(expression, state, SessionReason.OVERDUE_REVIEW, productionDeficit)
        } else {
            null
        }
    }.sortedWith(compareBy({ it.state?.nextReviewAt }, { it.expression.id.value }))

    /** 2. Débiles: no vencidas, con olvidos acumulados y todavía no dominadas. */
    private fun List<Expression>.weakCandidates(
        statesById: Map<ExpressionId, LearningState>,
        now: Long,
        productionDeficit: Boolean,
    ) = mapNotNull { expression ->
        val state = statesById[expression.id] ?: return@mapNotNull null
        val isWeak = !state.isDue(now) &&
            state.failedReviewCount > 0 &&
            state.stage != LearningStage.MASTERED
        if (isWeak) Candidate(expression, state, SessionReason.WEAK, productionDeficit) else null
    }.sortedWith(compareBy({ it.targetScore }, { it.expression.id.value }))

    /** 3. Recordatorio productivo: ya recallables y con producción floja. */
    private fun List<Expression>.productionCandidates(
        statesById: Map<ExpressionId, LearningState>,
        now: Long,
        productionDeficit: Boolean,
    ) = mapNotNull { expression ->
        val state = statesById[expression.id] ?: return@mapNotNull null
        val needsProduction = !state.isDue(now) &&
            state.stage >= LearningStage.RECALLABLE &&
            state.stage != LearningStage.MASTERED &&
            state.productionScore < PRODUCTION_TARGET_SCORE
        if (needsProduction) {
            Candidate(expression, state, SessionReason.PRODUCTION_RECALL, productionDeficit)
        } else {
            null
        }
    }.sortedWith(compareBy({ it.targetScore }, { it.expression.id.value }))

    /** 4. Nuevo contenido: lo más fácil primero, y solo el margen que sobra. */
    private fun List<Expression>.newCandidates(
        statesById: Map<ExpressionId, LearningState>,
        limit: Int,
    ) = mapNotNull { expression ->
        val state = statesById[expression.id]
        if (state == null || state.reviewCount == 0) {
            Candidate(expression, state, SessionReason.NEW)
        } else {
            null
        }
    }.sortedWith(
        compareBy(
            { it.expression.difficulty },
            { it.expression.level },
            { it.expression.phrase },
            { it.expression.id.value },
        ),
    ).take(limit)

    /**
     * El contenido nuevo se reduce cuando ya hay revisiones acumuladas (§38, §41):
     * no se amplía material mientras lo pendiente se acumula.
     */
    private fun newContentLimit(dueCount: Int, configuredLimit: Int): Int = when {
        configuredLimit == 0 -> 0
        dueCount >= NEW_BLOCKED_DUE_COUNT -> 0
        dueCount >= NEW_REDUCED_DUE_COUNT -> (configuredLimit / 2).coerceAtLeast(1)
        else -> configuredLimit
    }

    /** ¿La producción va rezagada respecto al reconocimiento en el conjunto? */
    private fun hasProductionDeficit(states: List<LearningState>): Boolean {
        val reviewed = states.filter { it.reviewCount > 0 }
        if (reviewed.isEmpty()) return false
        val recognition = reviewed.sumOf { it.recognitionScore }.toDouble() / reviewed.size
        val production = reviewed.sumOf { it.productionScore }.toDouble() / reviewed.size
        return recognition - production >= PRODUCTION_DEFICIT
    }

    private fun Candidate.renderableType(pool: List<Expression>): ReviewType? =
        (listOf(reviewType) + FALLBACK_TYPES.getValue(reviewType))
            .firstOrNull { exerciseGenerator.generate(expression, it, pool) != null }

    private fun Candidate.costOf(reviewType: ReviewType): Int =
        if (isNewExpression) NEW_EXPRESSION_SECONDS else reviewType.estimatedSeconds

    /** Puntuación en el eje que se va a practicar: define qué es más débil. */
    private val Candidate.targetScore: Int
        get() = when (reviewType.skill) {
            ReviewSkill.RECOGNITION -> state?.recognitionScore ?: 0
            ReviewSkill.PRODUCTION -> state?.productionScore ?: 0
        }

    private fun LearningState.isDue(now: Long): Boolean = nextReviewAt != null && nextReviewAt <= now

    private data class Candidate(
        val expression: Expression,
        val state: LearningState?,
        val reason: SessionReason,
        private val productionDeficit: Boolean = false,
    ) {
        val isNewExpression: Boolean
            get() = state == null || state.reviewCount == 0

        val reviewType: ReviewType
            get() = ladderTypeFor()

        /**
         * Escalera de exigencia por etapa (§33): cada etapa se practica con el
         * nivel de recuerdo que le corresponde, y los olvidos recentran el tipo en
         * guided recall porque una pista es lo que devuelve la sensación de haberla
         * visto antes (§41).
         */
        private fun ladderTypeFor(): ReviewType {
            val stage = state?.stage ?: return ReviewType.RECOGNITION
            if (state.failedReviewCount > 0 && stage <= LearningStage.RECOGNIZED) return ReviewType.GUIDED_RECALL
            val base = when (stage) {
                LearningStage.NEW -> ReviewType.RECOGNITION
                LearningStage.SEEN -> ReviewType.CLOZE
                LearningStage.RECOGNIZED -> ReviewType.GUIDED_RECALL
                LearningStage.RECALLABLE -> ReviewType.TRANSLATION
                LearningStage.PRODUCTIVE, LearningStage.MASTERED -> ReviewType.PRODUCTION
            }
            return if (productionDeficit && stage >= LearningStage.RECOGNIZED) {
                base.productionAlternative
            } else {
                base
            }
        }
    }

    companion object {
        const val SECONDS_PER_MINUTE = 60

        /** Presentar una expresión nueva cuesta más que practicarla (§39). */
        const val NEW_EXPRESSION_SECONDS = 90

        /** Tope de pasos por sesión, para que el plan siempre sea usable. */
        const val MAX_STEPS = 60

        const val NEW_REDUCED_DUE_COUNT = 10
        const val NEW_BLOCKED_DUE_COUNT = 20

        /** Diferencia media entre reconocimiento y producción que dispara el balance. */
        const val PRODUCTION_DEFICIT = 20.0

        /** Por debajo de este score la producción necesita recordatorio aunque no toque revisar. */
        const val PRODUCTION_TARGET_SCORE = 70

        /**
         * Orden en que se prueba otro tipo cuando el contenido no sostiene el
         * preferido. En recognition y cloze baja la exigencia porque son los
         * únicos que dependen de material externo.
         */
        private val FALLBACK_TYPES: Map<ReviewType, List<ReviewType>> = mapOf(
            ReviewType.RECOGNITION to listOf(ReviewType.CLOZE, ReviewType.GUIDED_RECALL, ReviewType.TRANSLATION),
            ReviewType.CLOZE to listOf(ReviewType.GUIDED_RECALL, ReviewType.RECOGNITION),
            ReviewType.GUIDED_RECALL to listOf(ReviewType.CLOZE, ReviewType.RECOGNITION),
            ReviewType.TRANSLATION to listOf(ReviewType.GUIDED_RECALL, ReviewType.CLOZE),
            ReviewType.PRODUCTION to listOf(ReviewType.TRANSLATION, ReviewType.GUIDED_RECALL),
        )
    }
}
