package com.example.learning.english.learning.english.app.ui.practice

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning.english.learning.english.app.R
import com.example.learning.english.learning.english.app.domain.engine.LearningEngine
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseGenerator
import com.example.learning.english.learning.english.app.domain.exercise.PracticeExercise
import com.example.learning.english.learning.english.app.domain.exercise.RecognitionExercise
import com.example.learning.english.learning.english.app.domain.exercise.WrittenExercise
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.SessionSummary
import com.example.learning.english.learning.english.app.domain.model.UserAnswer
import com.example.learning.english.learning.english.app.domain.repository.ExpressionRepository
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import com.example.learning.english.learning.english.app.domain.repository.ReviewRepository
import com.example.learning.english.learning.english.app.domain.repository.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Práctica: un ejercicio, una respuesta y una calificación por paso (README §43.4).
 *
 * El motor decide qué se practica; esta pantalla solo encadena sus tres pasos —
 * comprobar, revelar y calificar — y pinta el estado. Ninguna regla de
 * planificación ni de progresión vive aquí.
 *
 * El avance se lee de `currentPosition` y lo persiste el motor en cada
 * calificación, así que una sesión interrumpida se retoma donde se dejó (§72). Lo
 * único que no sobrevive al proceso es el borrador sin calificar: al reabrir se
 * repite el ejercicio, en vez de fingir que se respondió.
 */
class PracticeViewModel(
    private val sessionId: SessionId,
    private val sessionRepository: SessionRepository,
    private val expressionRepository: ExpressionRepository,
    private val exerciseGenerator: ExerciseGenerator,
    private val learningEngine: LearningEngine,
    private val reviewRepository: ReviewRepository,
    private val learningStateRepository: LearningStateRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val state = MutableStateFlow(SessionState())

    /** Cuándo se mostró el ejercicio actual, para medir cuánto costó responder (§32). */
    private val shownAtMillis = MutableStateFlow(now())

    private val _effects = Channel<PracticeEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<PracticeEffect> = _effects.receiveAsFlow()

    val uiState = state.map { it.toUiState() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PracticeUiState.Loading,
    )

    init {
        viewModelScope.launch { load() }
    }

    // region eventos de la pantalla

    /**
     * La presentación de una expresión nueva no escribe nada: presentar no es
     * recordar. La revisión se registra cuando el usuario responde de verdad.
     */
    fun onIntroductionSeen() {
        state.update { it.copy(introduction = null) }
    }

    fun onOptionSelected(optionId: String) {
        if (state.value.prompt !is RecognitionExercise) return
        state.update { it.answeringDraft(UserAnswer.Choice(optionId)) }
    }

    fun onTextChanged(value: String) {
        if (state.value.prompt !is WrittenExercise) return
        state.update { it.answeringDraft(UserAnswer.Text(value)) }
    }

    /**
     * Comprueba la respuesta y revela el veredicto sin escribir nada todavía.
     *
     * Persistir viene después, al confirmar la calificación: así el usuario ve
     * siempre la respuesta natural antes de decidir cómo le fue (§35), también en
     * los ejercicios que el evaluador ya sabe calificar.
     */
    fun onCheck() {
        val current = state.value
        val draft = current.answering?.draft
        if (!current.canCheck || draft == null) return
        // El guardia se levanta antes de lanzar el coroutine: leerlo después
        // dejaría pasar un segundo toque dentro del mismo frame y la misma
        // respuesta se comprobaría dos veces.
        state.update { it.copyBusy(true) }
        viewModelScope.launch {
            val exercise = requireNotNull(current.exercise)
            val evaluation = guarding(
                failureRes = R.string.practice_exercise_unavailable,
                recover = { state.update { it.copyBusy(false) } },
            ) {
                learningEngine.evaluateAnswer(requireNotNull(current.session), exercise, draft)
            } ?: return@launch

            state.update {
                it.copy(turn = AnswerTurn.Revealed(evaluation, draft, evaluation.suggestedRating))
            }
        }
    }

    /** Confirma la calificación, la persiste y pasa al siguiente ejercicio. */
    fun onRate(rating: ReviewRating) {
        val current = state.value
        if (!current.canRate) return
        state.update { it.copyBusy(true) }
        viewModelScope.launch {
            val exercise = requireNotNull(current.exercise)
            val result = guarding(
                failureRes = R.string.practice_exercise_unavailable,
                recover = { state.update { it.copyBusy(false) } },
            ) {
                learningEngine.registerAnswer(
                    session = requireNotNull(current.session),
                    exercise = exercise,
                    rating = rating,
                    reviewedAt = now(),
                    responseTimeMs = (now() - shownAtMillis.value).coerceAtLeast(0L),
                )
            } ?: return@launch

            if (result.isSessionCompleted) summarize(result.session) else showNext(result.session)
        }
    }

    /** Cierra la pantalla: el resumen ya está a la vista, o no hay nada que ver. */
    fun onFinish() {
        _effects.trySend(PracticeEffect.Finish)
    }

    // endregion

    private suspend fun load() {
        val session = sessionRepository.getById(sessionId)
        when {
            session == null -> state.update { it.copy(failureRes = R.string.practice_session_missing) }
            session.isCompleted -> summarize(session)
            // Una sesión guardada con el plan vacío es real: se creó sin nada que
            // practicar. Se informa en vez de dejar un spinner infinito.
            session.nextExercise == null -> state.update { it.copy(session = session) }
            else -> showNext(session)
        }
    }

    /**
     * Deja listo el siguiente paso de la sesión.
     *
     * El ejercicio concreto se reconstruye con [ExerciseGenerator] en vez de
     * guardarse: es determinista, así que sale el mismo que se planificó.
     */
    private suspend fun showNext(session: LearningSession) {
        val exercise = requireNotNull(session.nextExercise)
        val expression = expressionRepository.getById(exercise.expressionId)
        val prompt = expression?.let {
            exerciseGenerator.generate(it, exercise.reviewType, expressionRepository.getAll())
        }
        if (prompt == null) {
            state.update {
                it.copy(session = session, failureRes = R.string.practice_exercise_unavailable)
            }
            return
        }
        state.value = SessionState(
            session = session,
            exercise = exercise,
            prompt = prompt,
            turn = AnswerTurn.Answering(draft = null),
            introduction = requireNotNull(expression).introIfUnseen(),
        )
        shownAtMillis.value = now()
    }

    /** Solo se presenta lo que no se ha visto nunca: repetirla sería ruido (§43.3). */
    private suspend fun Expression.introIfUnseen(): ExpressionIntroUiModel? {
        if (!isUnseen(id)) return null
        return ExpressionIntroUiModel(
            phrase = phrase,
            meaning = primaryMeaning,
            explanation = explanation,
            pattern = patterns.firstOrNull()?.pattern,
            examples = examples.map { it.english },
        )
    }

    private suspend fun summarize(session: LearningSession) {
        val reviews = reviewRepository.getBySession(session.id)
        state.update {
            it.copy(session = session, exercise = null, prompt = null, summary = SessionSummary.from(session, reviews))
        }
    }

    /**
     * Ejecuta un paso del motor traduciendo un fallo a estado visible.
     *
     * Devuelve `null` cuando el paso falló. La cancelación se propaga: tragarse
     * un `CancellationException` dejaría la pantalla colgada.
     */
    private suspend fun <T> guarding(
        @StringRes failureRes: Int,
        recover: () -> Unit,
        step: suspend () -> T,
    ): T? = try {
        step()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        recover()
        // `IllegalArgumentException` es una invariante del dominio —un tipo de
        // respuesta que no corresponde al ejercicio— y tiene que verse al
        // desarrollo. Lo que falla de verdad, una expresión que ya no está o un
        // contenido insuficiente, se muestra al usuario.
        if (failure is IllegalArgumentException) throw failure
        state.update { it.copy(failureRes = failureRes) }
        null
    }

    /** `true` si la expresión nunca se ha revisado: hay que presentarla (§43.3). */
    private suspend fun isUnseen(expressionId: ExpressionId): Boolean =
        (learningStateRepository.getByExpression(expressionId)?.reviewCount ?: 0) == 0

    /**
     * Estado interno de la sesión.
     *
     * Guarda la sesión entera porque el motor la necesita para registrar, pero de
     * ella a la pantalla solo salen [SessionProgress] y el ejercicio actual.
     */
    private data class SessionState(
        val session: LearningSession? = null,
        val exercise: Exercise? = null,
        val prompt: PracticeExercise? = null,
        val turn: AnswerTurn = AnswerTurn.Answering(draft = null),
        val summary: SessionSummary? = null,
        val introduction: ExpressionIntroUiModel? = null,
        val failureRes: Int? = null,
    ) {
        val answering: AnswerTurn.Answering?
            get() = turn as? AnswerTurn.Answering

        val canCheck: Boolean
            get() = exercise != null && !turn.isChecking && isAnswerable

        val canRate: Boolean
            get() = exercise != null && turn is AnswerTurn.Revealed && !turn.isRegistering

        /** Respondido y sin blanco: un hueco vacío no se comprueba. */
        private val isAnswerable: Boolean
            get() = answering?.draft?.hasContent() == true

        fun copyBusy(busy: Boolean) = copy(turn = turn.copyBusy(busy))

        fun answeringDraft(answer: UserAnswer) = copy(
            turn = AnswerTurn.Answering(answer),
            introduction = null,
        )

        private fun AnswerTurn.copyBusy(busy: Boolean) = when (this) {
            is AnswerTurn.Answering -> copy(checking = busy)
            is AnswerTurn.Revealed -> copy(registering = busy)
        }

        fun toUiState(): PracticeUiState = when {
            failureRes != null -> PracticeUiState.Failed(failureRes)
            session == null -> PracticeUiState.Loading
            summary != null -> PracticeUiState.Completed(summary)
            exercise == null || prompt == null -> PracticeUiState.NothingToPractice
            else -> PracticeUiState.Active(
                exercise = exercise,
                prompt = prompt,
                progress = SessionProgress(
                    answered = session.currentPosition,
                    total = session.exercises.size,
                ),
                turn = turn,
                introduction = introduction,
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Respondido y sin blanco: un hueco vacío no se comprueba. */
private fun UserAnswer.hasContent(): Boolean = when (this) {
    is UserAnswer.Choice -> optionId.isNotBlank()
    is UserAnswer.Text -> value.isNotBlank()
}