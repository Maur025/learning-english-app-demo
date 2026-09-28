package com.example.learning.english.learning.english.app.domain.engine

import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewSkill
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import kotlin.math.roundToInt

/**
 * Progresión de una expresión tras cada revisión (README §3.2, §3.3, §41).
 *
 * Se aplica **sobre el estado que ya programó el `ReviewScheduler`**: así el
 * `reviewCount` que ve la etapa es el incremented y el intervalo calculado no se
 * toca. El `ReviewRecorder` es quien lo invoca.
 *
 * Puntuaciones: media móvil exponencial
 *
 * ```text
 * score' = score + α · (calidad − score)
 * ```
 *
 * con `α = 0.35` y calidad `FORGOT=0`, `HARD=50`, `GOOD=85`, `EASY=100`. Solo
 * se actualiza el eje del ejercicio (reconocimiento o producción), de modo que
 * las dos puntuaciones avanzan al ritmo de sus propios ejercicios. Por eso no
 * hace falta un contador por eje, que el modelo no guarda.
 *
 * Etapas: se recalculan desde las puntuaciones y el número de revisiones, así
 * que pueden retroceder cuando la expresión se olvida repetidamente. `MASTERED`
 * exige producción: una expresión nunca se domina solo por reconocimiento.
 */
class LearningStateProgressor {

    fun apply(
        state: LearningState,
        reviewType: ReviewType,
        rating: ReviewRating,
    ): LearningState {
        val quality = qualityOf(rating)
        val scored = when (reviewType.skill) {
            ReviewSkill.RECOGNITION ->
                state.copy(recognitionScore = state.recognitionScore.nextScore(quality))
            ReviewSkill.PRODUCTION ->
                state.copy(productionScore = state.productionScore.nextScore(quality))
        }
        return scored.copy(stage = stageOf(scored))
    }

    private fun qualityOf(rating: ReviewRating): Int = when (rating) {
        ReviewRating.FORGOT -> QUALITY_FORGOT
        ReviewRating.HARD -> QUALITY_HARD
        ReviewRating.GOOD -> QUALITY_GOOD
        ReviewRating.EASY -> QUALITY_EASY
    }

    private fun Int.nextScore(quality: Int): Int =
        (this + SCORE_SMOOTHING * (quality - this))
            .roundToInt()
            .coerceIn(LearningState.SCORE_MIN, LearningState.SCORE_MAX)

    private fun stageOf(state: LearningState): LearningStage = when {
        state.reviewCount <= 0 -> LearningStage.NEW
        state.reviewCount < RECOGNIZED_MIN_REVIEWS -> LearningStage.SEEN
        state.recognitionScore < RECOGNIZED_SCORE -> LearningStage.SEEN
        state.recognitionScore < RECALLABLE_SCORE || state.reviewCount < RECALLABLE_MIN_REVIEWS -> LearningStage.RECOGNIZED
        state.productionScore < PRODUCTIVE_SCORE -> LearningStage.RECALLABLE
        state.productionScore < MASTERED_SCORE ||
            state.recognitionScore < MASTERED_SCORE ||
            state.reviewCount < MASTERED_MIN_REVIEWS -> LearningStage.PRODUCTIVE
        else -> LearningStage.MASTERED
    }

    companion object {
        /** Suavizado de la media móvil exponencial: ni brusco ni pegajoso. */
        const val SCORE_SMOOTHING = 0.35

        const val QUALITY_FORGOT = 0
        const val QUALITY_HARD = 50
        const val QUALITY_GOOD = 85
        const val QUALITY_EASY = 100

        const val RECOGNIZED_SCORE = 60
        const val RECOGNIZED_MIN_REVIEWS = 3
        const val RECALLABLE_SCORE = 70
        const val RECALLABLE_MIN_REVIEWS = 5
        const val PRODUCTIVE_SCORE = 60
        const val MASTERED_SCORE = 80
        const val MASTERED_MIN_REVIEWS = 10
    }
}
