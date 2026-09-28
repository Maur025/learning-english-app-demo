package com.example.learning.english.learning.english.app.domain.model

/** Eje reconocimiento / producción. Nunca se domina una expresión solo por reconocimiento (§3.3). */
enum class ReviewSkill {
    RECOGNITION,
    PRODUCTION,
}

enum class ReviewType {
    RECOGNITION,
    CLOZE,
    GUIDED_RECALL,
    TRANSLATION,
    PRODUCTION,
    ;

    val skill: ReviewSkill
        get() = when (this) {
            RECOGNITION, CLOZE, GUIDED_RECALL -> ReviewSkill.RECOGNITION
            TRANSLATION, PRODUCTION -> ReviewSkill.PRODUCTION
        }

    /**
     * Coste estimado en segundos de un ejercicio de este tipo (§39).
     *
     * Heurística inicial: `LearningEngine` la usa para construir sesiones cerca
     * de la duración diaria configurada y debería afinarla con datos reales.
     */
    val estimatedSeconds: Int
        get() = when (this) {
            RECOGNITION -> RECOGNITION_SECONDS
            CLOZE -> CLOZE_SECONDS
            GUIDED_RECALL -> GUIDED_RECALL_SECONDS
            TRANSLATION -> TRANSLATION_SECONDS
            PRODUCTION -> PRODUCTION_SECONDS
        }

    companion object {
        const val RECOGNITION_SECONDS = 15
        const val CLOZE_SECONDS = 25
        const val GUIDED_RECALL_SECONDS = 30
        const val TRANSLATION_SECONDS = 45
        const val PRODUCTION_SECONDS = 60
    }
}
