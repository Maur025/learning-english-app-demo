package com.example.learning.english.learning.english.app.domain.scheduler

import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import kotlin.math.roundToInt

/**
 * Scheduler determinista inspirado en SM-2 (§4.3, candidato 2).
 *
 * Reglas:
 * - Primer repaso (sin revisiones previas): HARD 1d, GOOD 2d, EASY 4d.
 * - Repasos siguientes: HARD es 1.2x el intervalo anterior; GOOD y EASY
 *   multiplican por el ease factor (EASY además por 1.3). Un éxito nunca
 *   programa para antes de mañana.
 * - FORGOT deja la expresión vencida de inmediato (intervalo 0) para que
 *   reaparezca en la siguiente consulta de vencidos.
 * - El ease factor empieza en 2.5: FORGOT −0.20, HARD −0.15, EASY +0.15,
 *   GOOD sin cambio, con piso en 1.3.
 *
 * Las transiciones de [LearningStage] no pertenecen a este scheduler:
 * son responsabilidad de `LearningEngine` (Fase 6).
 */
class Sm2ReviewScheduler : ReviewScheduler {

    override fun schedule(state: LearningState, rating: ReviewRating, reviewedAt: Long): LearningState {
        val newEaseFactor = (state.easeFactor + easeDelta(rating)).coerceAtLeast(MIN_EASE_FACTOR)
        val intervalDays = intervalDays(state, rating, newEaseFactor)
        val correct = rating.isCorrect
        return state.copy(
            nextReviewAt = reviewedAt + intervalDays * MILLIS_PER_DAY,
            lastReviewedAt = reviewedAt,
            reviewCount = state.reviewCount + 1,
            successfulReviewCount = state.successfulReviewCount + if (correct) 1 else 0,
            failedReviewCount = state.failedReviewCount + if (correct) 0 else 1,
            currentIntervalDays = intervalDays,
            easeFactor = newEaseFactor,
            updatedAt = reviewedAt,
        )
    }

    private fun intervalDays(state: LearningState, rating: ReviewRating, newEaseFactor: Double): Int {
        if (rating == ReviewRating.FORGOT) return 0
        val rawDays = if (state.reviewCount == 0) {
            firstIntervalDays(rating).toDouble()
        } else {
            when (rating) {
                ReviewRating.HARD -> state.currentIntervalDays * HARD_MULTIPLIER_DAYS
                ReviewRating.GOOD -> state.currentIntervalDays * newEaseFactor
                ReviewRating.EASY -> state.currentIntervalDays * newEaseFactor * EASY_MULTIPLIER_DAYS
                ReviewRating.FORGOT -> error("FORGOT handled above")
            }
        }
        return rawDays.roundToInt().coerceIn(MIN_SUCCESS_INTERVAL_DAYS, MAX_INTERVAL_DAYS)
    }

    private fun firstIntervalDays(rating: ReviewRating): Int = when (rating) {
        ReviewRating.HARD -> FIRST_HARD_INTERVAL_DAYS
        ReviewRating.GOOD -> FIRST_GOOD_INTERVAL_DAYS
        ReviewRating.EASY -> FIRST_EASY_INTERVAL_DAYS
        ReviewRating.FORGOT -> error("FORGOT handled above")
    }

    private fun easeDelta(rating: ReviewRating): Double = when (rating) {
        ReviewRating.FORGOT -> EASE_DELTA_FORGOT
        ReviewRating.HARD -> EASE_DELTA_HARD
        ReviewRating.GOOD -> 0.0
        ReviewRating.EASY -> EASE_DELTA_EASY
    }

    companion object {
        const val FIRST_HARD_INTERVAL_DAYS = 1
        const val FIRST_GOOD_INTERVAL_DAYS = 2
        const val FIRST_EASY_INTERVAL_DAYS = 4

        const val MIN_SUCCESS_INTERVAL_DAYS = 1
        const val MAX_INTERVAL_DAYS = 365

        const val HARD_MULTIPLIER_DAYS = 1.2
        const val EASY_MULTIPLIER_DAYS = 1.3

        const val MIN_EASE_FACTOR = 1.3
        const val EASE_DELTA_FORGOT = -0.20
        const val EASE_DELTA_HARD = -0.15
        const val EASE_DELTA_EASY = 0.15

        val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}