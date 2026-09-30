package com.example.learning.english.learning.english.app.ui.navigation

import com.example.learning.english.learning.english.app.domain.model.SessionId

/**
 * Destinos de navegación.
 *
 * Solo viajan identificadores estables entre pantallas; el estado se carga desde
 * los repositorios al llegar (README §44).
 */
object Routes {

    const val ONBOARDING = "onboarding"
    const val HOME = "home"

    const val PRACTICE = "practice/{sessionId}"
    const val PRACTICE_ARG_SESSION_ID = "sessionId"

    fun practice(sessionId: SessionId): String = "practice/${sessionId.value}"
}
