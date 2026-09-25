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
}
