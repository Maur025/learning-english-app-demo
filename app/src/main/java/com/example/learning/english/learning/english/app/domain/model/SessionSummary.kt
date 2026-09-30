package com.example.learning.english.learning.english.app.domain.model

import kotlin.math.roundToInt

/**
 * Resumen de una sesión terminada (README §43.5).
 *
 * Todas las cifras se derivan de la historia de revisiones persistida (§57): no
 * hay contadores paralelos que puedan desincronizarse de lo que realmente se
 * practicó. Las que no tienen dato se dejan a `null` en lugar de a cero, porque un
 * promedio sobre cero ejercicios no daría 0 %, daría un número inventado — el
 * mismo criterio que [SkillAverages].
 *
 * [durationMillis] es el tiempo transcurrido de la sesión, no el tiempo
 * practicando: cuenta también los ratos con la app abierta. Es el único dato de
 * duración real disponible, así que no se disfraza de precisión que no tiene.
 */
data class SessionSummary(
    /** Tiempo transcurrido entre el inicio y el final; `null` si sigue abierta. */
    val durationMillis: Long?,
    val reviewedCount: Int,
    /** Expresiones que entran en la sesión sin haber sido revisadas nunca. */
    val newExpressions: Int,
    /** Acierto por opción o comparación normalizada; `null` si no hubo tales ejercicios. */
    val recognitionPercent: Int?,
    /**
     * Acierto según la autocalificación del usuario (§35): en producción no hay
     * veredicto automático, así que esta cifra es su valoración, no la de la app.
     */
    val productionPercent: Int?,
    /** Expresiones cuya etapa avanzó durante la sesión. */
    val improvedExpressions: Int,
) {
    companion object {
        val EMPTY = SessionSummary(
            durationMillis = null,
            reviewedCount = 0,
            newExpressions = 0,
            recognitionPercent = null,
            productionPercent = null,
            improvedExpressions = 0,
        )

        /**
         * Calcula el resumen de [session] a partir de sus revisiones.
         *
         * Función pura: el reloj ya viene resuelto en [LearningSession.startedAt] y
         * [LearningSession.completedAt], así que se prueba sin instrumentación.
         */
        fun from(session: LearningSession, reviews: List<ReviewEvent>): SessionSummary {
            val answered = reviews.filter { it.sessionId == session.id }
            return SessionSummary(
                durationMillis = session.completedAt?.let { finishedAt -> finishedAt - session.startedAt },
                reviewedCount = answered.size,
                newExpressions = answered.count { it.previousStage == LearningStage.NEW },
                recognitionPercent = accuracyOf(answered.filter { it.reviewType.skill == ReviewSkill.RECOGNITION }),
                productionPercent = accuracyOf(answered.filter { it.reviewType.skill == ReviewSkill.PRODUCTION }),
                improvedExpressions = answered.count { it.newStage.advancedFrom(it.previousStage) },
            )
        }

        /** Porcentaje de acierto, o `null` si no hay ejercicios de ese eje. */
        private fun accuracyOf(reviews: List<ReviewEvent>): Int? {
            if (reviews.isEmpty()) return null
            val percent = reviews.count { it.isCorrect } * 100.0 / reviews.size
            return percent.roundToInt().coerceIn(LearningState.SCORE_MIN, LearningState.SCORE_MAX)
        }
    }
}