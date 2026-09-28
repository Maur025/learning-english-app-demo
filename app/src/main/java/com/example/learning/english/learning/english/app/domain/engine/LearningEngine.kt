package com.example.learning.english.learning.english.app.domain.engine

import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluation
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.UserAnswer
import com.example.learning.english.learning.english.app.domain.model.UserPreferences

/**
 * Motor de aprendizaje (README §37).
 *
 * Es la frontera que la UI usa para practicar: genera la sesión de hoy y registra
 * cada respuesta. Las reglas de selección, balance y presupuesto viven en
 * [DailySessionPlanner]; las de progresión, en `ReviewRecorder`. Ninguna de las
 * dos se permite dentro de un ViewModel.
 *
 * El tiempo se recibe como parámetro en lugar de leerse del reloj: la sesión
 * depende de "ahora" y los tests necesitan fijarlo.
 */
interface LearningEngine {

    /**
     * Genera y persiste la sesión de hoy.
     *
     * Siempre guarda una sesión, incluso si el plan queda vacío: es el registro
     * honesto de que hoy no había nada vencido, y la UI decide qué mostrar.
     * Cada llamada compone un plan nuevo; reutilizar una sesión a medias es
     * decisión de la pantalla, no del motor.
     */
    suspend fun createDailySession(preferences: UserPreferences, now: Long): LearningSession

    /**
     * Califica una respuesta, actualiza el estado de aprendizaje y avanza el
     * progreso de la sesión.
     *
     * [selfRating] solo es obligatorio en los ejercicios de producción, que no se
     * autocalifican (§35). En el resto el motor deriva la calificación de la
     * evaluación, y un [selfRating] explícito manda: el usuario sabe si la sabía
     * despacio aunque la respuesta comparara bien.
     *
     * La respuesta se persiste en la misma operación: si el proceso muere, la
     * sesión guardada ya está advanced (§72).
     */
    suspend fun registerAnswer(
        session: LearningSession,
        exercise: Exercise,
        answer: UserAnswer,
        reviewedAt: Long,
        selfRating: ReviewRating? = null,
        responseTimeMs: Long? = null,
    ): ReviewResult
}

/**
 * Resultado de registrar una respuesta.
 *
 * [evaluation] lleva la respuesta natural que hay que revelar y [learningState]
 * el estado ya persistido, así que la UI no necesita releer nada para pintar el
 * resultado.
 */
data class ReviewResult(
    val evaluation: AnswerEvaluation,
    val rating: ReviewRating,
    val learningState: LearningState,
    val session: LearningSession,
) {
    val isSessionCompleted: Boolean
        get() = session.isCompleted

    val expectedAnswer: String
        get() = evaluation.expectedAnswer
}
