package com.example.learning.english.learning.english.app.domain.model

/**
 * Media de los dos ejes sobre lo ya revisado (README §43.2, §57).
 *
 * Los porcentajes son `null` mientras no exista ninguna revisión: promediar
 * sobre cero expresiones no daría 0 %, daría un dato inventado. La UI decide
 * qué mostrar en ese caso.
 *
 * El redondeo vive aquí y no en la consulta: los scores ya van de 0 a 100, así
 * que la media en ese rango es directamente el porcentaje.
 */
data class SkillAverages(
    val recognitionPercent: Int?,
    val productionPercent: Int?,
) {
    val hasData: Boolean
        get() = recognitionPercent != null

    companion object {
        val EMPTY = SkillAverages(recognitionPercent = null, productionPercent = null)

        fun fromScores(recognition: Double?, production: Double?): SkillAverages = SkillAverages(
            recognitionPercent = recognition?.toPercent(),
            productionPercent = production?.toPercent(),
        )

        private fun Double.toPercent(): Int =
            kotlin.math.round(this).toInt().coerceIn(LearningState.SCORE_MIN, LearningState.SCORE_MAX)
    }
}
