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

    const val PRACTICE_ARG_SESSION_ID = "sessionId"

    private const val PRACTICE_BASE = "practice"
    const val PRACTICE = "$PRACTICE_BASE/{$PRACTICE_ARG_SESSION_ID}"

    fun practice(sessionId: SessionId): String = "$PRACTICE_BASE/${sessionId.value}"
}