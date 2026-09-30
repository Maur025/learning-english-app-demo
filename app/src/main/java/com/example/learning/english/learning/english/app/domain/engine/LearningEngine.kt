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
 * Es la frontera que la UI usa para practicar: genera la sesión de hoy, califica
 * respuestas y registra la revisión. Las reglas de selección, balance y
 * presupuesto viven en [DailySessionPlanner]; las de progresión, en
 * `ReviewRecorder`. Ninguna de las dos se permite dentro de un ViewModel.
 *
 * Evaluar y registrar son operaciones separadas a propósito (§43.4): el
 * ejercicio se revela siempre tras responder y la calificación se confirma
 * siempre después, incluso cuando el evaluador ya sabe cuál sería. Si ambas
 * estuvieran juntas, en producción no se podría mostrar la respuesta sin
 * escribir antes la calificación.
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
     * Califica una respuesta **sin escribir nada**.
     *
     * Devuelve lo que la UI necesita para mostrar el veredicto y revelar la
     * respuesta natural, y [AnswerEvaluation.suggestedRating] con la calificación
     * que se propone por defecto. No toca la base de datos: si el proceso muere
     * entre evaluar y registrar, el ejercicio sigue sin responder y se repite.
     */
    suspend fun evaluateAnswer(
        session: LearningSession,
        exercise: Exercise,
        answer: UserAnswer,
    ): AnswerEvaluation

    /**
     * Persiste la calificación de un ejercicio y avanza el progreso de la sesión.
     *
     * [rating] es siempre explícito, incluso en los ejercicios calificados
     * automáticamente: el usuario sabe si la sabía despacio aunque la respuesta
     * comparara bien (§35).
     *
     * La respuesta se persiste en la misma operación: si el proceso muere, la
     * sesión guardada ya está advanced (§72). La posición se valida **antes** de
     * escribir para que un toque fuera de turno no deje una revisión huérfana.
     */
    suspend fun registerAnswer(
        session: LearningSession,
        exercise: Exercise,
        rating: ReviewRating,
        reviewedAt: Long,
        responseTimeMs: Long? = null,
    ): ReviewResult
}

/**
 * Resultado de registrar una respuesta.
 *
 * [learningState] lleva el estado ya persistido, así que la UI no necesita
 * releer nada para pintar el resultado.
 */
data class ReviewResult(
    val rating: ReviewRating,
    val learningState: LearningState,
    val session: LearningSession,
) {
    val isSessionCompleted: Boolean
        get() = session.isCompleted
}
