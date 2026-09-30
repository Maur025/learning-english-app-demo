package com.example.learning.english.learning.english.app.ui.practice

import androidx.annotation.StringRes
import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluation
import com.example.learning.english.learning.english.app.domain.exercise.PracticeExercise
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.SessionSummary
import com.example.learning.english.learning.english.app.domain.model.UserAnswer

/**
 * Estado de la pantalla de práctica (README §45).
 *
 * Los cuatro casos del enunciado —cargando, activa, completada y error— son la
 * única forma de pintar la pantalla, así que no existen estados intermedios que
 * la pantalla tenga que adivinar. Los dos que no aparecen allí porque describen lo
 * que hay que hacer con el estado, no cómo se ve: [PracticeUiState.NothingToPractice]
 * para una sesión sin ejercicios y [PracticeUiState.Failed] para una sesión que no
 * existe o un ejercicio que el contenido ya no sostiene.
 */
sealed interface PracticeUiState {

    data object Loading : PracticeUiState

    data class Active(
        val exercise: Exercise,
        /**
         * El ejercicio concreto ya generado. Viaja como modelo de dominio porque
         * ya es un valor puro, separado de Room y de la pantalla, y duplicar su
         * jerarquía sellada en la capa de UI no aportaría nada (README §14).
         */
        val prompt: PracticeExercise,
        val progress: SessionProgress,
        val turn: AnswerTurn,
        /**
         * Presentación de una expresión nunca vista, o `null` si ya se conoce.
         * Se muestra antes de preguntar (§43.3): presentar no es recordar, así
         * que no deja revisión registrada.
         */
        val introduction: ExpressionIntroUiModel?,
    ) : PracticeUiState {
        /** El turno mientras se responde; `null` en cuanto se ha revelado. */
        val answering: AnswerTurn.Answering?
            get() = turn as? AnswerTurn.Answering

        val isIntroductionPending: Boolean
            get() = introduction != null

        val canAnswer: Boolean
            get() = turn is AnswerTurn.Answering && introduction == null && !turn.isChecking

        val isRating: Boolean
            get() = turn is AnswerTurn.Revealed && turn.isRegistering
    }

    /** La sesión ya estaba cerrada al entrar: se resume sin volver a practicarla. */
    data class Completed(val summary: SessionSummary) : PracticeUiState

    /** La sesión se guardó pero el plan quedó vacío: no hay nada que hacer. */
    data object NothingToPractice : PracticeUiState

    data class Failed(@param:StringRes val messageRes: Int) : PracticeUiState
}

/**
 * Momento de la respuesta dentro de un ejercicio.
 *
 * Es sellada porque el orden es fijo: no se puede calificar sin haber respondido
 * ni volver a responder después de haber revelado. Un borrador y una evaluación
 * nunca conviven.
 */
sealed interface AnswerTurn {

    val isChecking: Boolean
        get() = this is Answering && checking

    val isRegistering: Boolean
        get() = this is Revealed && registering

    /** Respondiendo. [draft] es lo escrito o elegido; `null` si todavía no hay nada. */
    data class Answering(val draft: UserAnswer?, val checking: Boolean = false) : AnswerTurn

    /**
     * Ya respondió: [evaluation] lleva el veredicto y la respuesta a revelar,
     * [suggestedRating] la calificación que la app propone, `null` en producción.
     *
     * [draft] se conserva para poder repetir lo que el usuario escribió tal cual.
     * [AnswerEvaluation.normalizedAnswer] no sirve para eso: es la respuesta ya
     * normalizada, y enseñarla haría creer que el usuario Tecleó «figure out» cuando
     * escribió «Figure Out!».
     */
    data class Revealed(
        val evaluation: AnswerEvaluation,
        val draft: UserAnswer?,
        val suggestedRating: ReviewRating?,
        val registering: Boolean = false,
    ) : AnswerTurn
}

/** Avance de la sesión: cuántos ejercicios quedan y cuál es el actual. */
data class SessionProgress(
    val answered: Int,
    val total: Int,
) {
    val current: Int
        get() = (answered + 1).coerceAtMost(total)

    val fraction: Float
        get() = if (total == 0) 0f else answered.toFloat() / total
}

/**
 * Presentación de una expresión que el usuario todavía no ha visto (§43.3).
 *
 * Es el único modelo de UI que se copia de `Expression`: el resto viaja como
 * modelo de dominio porque ya son valores puros con la forma que la pantalla
 * necesita. Aquí el copia aporta algo —la pantalla presenta una expresión, no un
 * ejercicio, y para eso el tipo del ejercicio da una forma distinta cada vez.
 */
data class ExpressionIntroUiModel(
    val phrase: String,
    val meaning: String,
    val explanation: String?,
    val pattern: String?,
    val examples: List<String>,
)

/** Salidas que la pantalla ejecuta y no puede decidir sola. */
sealed interface PracticeEffect {
    /** Se acabó: volver a Home. */
    data object Finish : PracticeEffect
}