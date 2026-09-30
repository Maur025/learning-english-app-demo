package com.example.learning.english.learning.english.app.ui.practice

import com.example.learning.english.learning.english.app.R
import com.example.learning.english.learning.english.app.domain.engine.DailySessionPlanner
import com.example.learning.english.learning.english.app.domain.engine.DefaultLearningEngine
import com.example.learning.english.learning.english.app.domain.engine.EngineFixtures
import com.example.learning.english.learning.english.app.domain.engine.FakeExpressionRepository
import com.example.learning.english.learning.english.app.domain.engine.FakeLearningStateRepository
import com.example.learning.english.learning.english.app.domain.engine.FakeReviewRepository
import com.example.learning.english.learning.english.app.domain.engine.FakeSessionRepository
import com.example.learning.english.learning.english.app.domain.engine.LearningStateProgressor
import com.example.learning.english.learning.english.app.domain.engine.exercise
import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluation
import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluator
import com.example.learning.english.learning.english.app.domain.exercise.DefaultExerciseGenerator
import com.example.learning.english.learning.english.app.domain.exercise.RecognitionExercise
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.ReviewId
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.SessionSummary
import com.example.learning.english.learning.english.app.domain.scheduler.Sm2ReviewScheduler
import com.example.learning.english.learning.english.app.domain.service.ReviewRecorder
import com.example.learning.english.learning.english.app.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PracticeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionRepository = FakeSessionRepository()
    private val expressionRepository = FakeExpressionRepository(EngineFixtures.all)
    private val learningStateRepository = FakeLearningStateRepository()
    private val reviewRepository = FakeReviewRepository()
    private val generator = DefaultExerciseGenerator()

    /** El reloj solo avanza al comprobar: responder cuesta tiempo, mostrar no. */
    private var clock = NOW
    private val now = { clock }

    /**
     * Motor real sobre repositorios en memoria.
     *
     * Un doble que devuelve lo que le conviene no probaría nada: el encadenado
     * comprobar-revelar-calificar es justamente lo que depende del motor, y el
     * motor ya tiene sus propios tests.
     */
    private val engine = DefaultLearningEngine(
        planner = DailySessionPlanner(generator),
        exerciseGenerator = generator,
        answerEvaluator = AnswerEvaluator(),
        reviewRecorder = ReviewRecorder(
            scheduler = Sm2ReviewScheduler(),
            progressor = LearningStateProgressor(),
            learningStateRepository = learningStateRepository,
            reviewRepository = reviewRepository,
        ),
        expressionRepository = expressionRepository,
        learningStateRepository = learningStateRepository,
        sessionRepository = sessionRepository,
    )

    @Test
    fun `a session with exercises opens on the first one`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())

        val active = viewModel.active()
        assertEquals(0, active.progress.answered)
        assertEquals(2, active.progress.total)
        assertEquals(1, active.progress.current)
        assertTrue(active.prompt is RecognitionExercise)
    }

    @Test
    fun `a session that does not exist fails instead of spinning`() = runTest {
        val viewModel = viewModel(sessionId = SessionId("missing"))

        val state = viewModel.uiState.first { it !is PracticeUiState.Loading }
        assertEquals(PracticeUiState.Failed(R.string.practice_session_missing), state)
    }

    @Test
    fun `a session with an empty plan says there is nothing to do`() = runTest {
        val viewModel = viewModel(sessionWith())

        val state = viewModel.uiState.first { it !is PracticeUiState.Loading }
        assertEquals(PracticeUiState.NothingToPractice, state)
    }

    @Test
    fun `an unseen expression is introduced before it is asked`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())

        assertTrue(viewModel.active().isIntroductionPending)
        assertFalse(viewModel.active().canAnswer)

        viewModel.onIntroductionSeen()

        assertFalse(viewModel.active().isIntroductionPending)
        assertTrue(viewModel.active().canAnswer)
    }

    @Test
    fun `a reviewed expression is asked straight away`() = runTest {
        val session = sessionWithTwoRecognitionSteps()
        markAsReviewed(session.exercises.first().expressionId)

        val viewModel = viewModel(session)

        assertFalse(viewModel.active().isIntroductionPending)
    }

    @Test
    fun `reading the introduction writes nothing`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())

        viewModel.onIntroductionSeen()

        assertTrue(reviewRepository.events.isEmpty())
        assertNull(learningStateRepository.getByExpression(expressionIdOf(0)))
    }

    @Test
    fun `checking reveals the verdict without writing anything yet`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())
        viewModel.answerFirstOption()

        viewModel.checkAnswer()

        val revealed = viewModel.revealed()
        assertTrue((revealed.evaluation as AnswerEvaluation.Graded).isCorrect)
        assertEquals(ReviewRating.GOOD, revealed.suggestedRating)
        // Nada se persiste hasta que el usuario confirma cómo le fue (§35).
        assertTrue(reviewRepository.events.isEmpty())
        assertEquals(0, savedSession().currentPosition)
    }

    @Test
    fun `confirming a rating persists the review and moves to the next exercise`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())
        viewModel.answerFirstOption()
        viewModel.checkAnswer()

        viewModel.onRate(ReviewRating.GOOD)

        assertEquals(1, reviewRepository.events.size)
        assertEquals(1, viewModel.active().progress.answered)
    }

    @Test
    fun `the next expression is introduced when it has never been seen`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())
        viewModel.answerFirstOption()
        viewModel.checkAnswer()

        viewModel.onRate(ReviewRating.GOOD)

        // El paso anterior ya no se presenta, y el nuevo sí: es otra expresión que
        // el usuario no ha visto todavía.
        assertTrue(viewModel.active().isIntroductionPending)
        assertEquals(expressionIdOf(1), viewModel.active().exercise.expressionId)
        assertEquals(1, learningStateRepository.getByExpression(expressionIdOf(0))!!.reviewCount)
        assertNull(learningStateRepository.getByExpression(expressionIdOf(1)))
    }

    @Test
    fun `confirming can override the rating the app suggested`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())
        viewModel.answerFirstOption()
        viewModel.checkAnswer()

        viewModel.onRate(ReviewRating.HARD)

        assertEquals(ReviewRating.HARD, reviewRepository.events.single().rating)
    }

    @Test
    fun `a wrong answer is forgotten and offered again today`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())
        viewModel.answerWrongOption()
        viewModel.checkAnswer()

        viewModel.onRate(viewModel.revealed().suggestedRating!!)

        assertEquals(ReviewRating.FORGOT, reviewRepository.events.single().rating)
        assertEquals(REVIEWED_AT, learningStateRepository.getByExpression(expressionIdOf(0))!!.nextReviewAt)
    }

    @Test
    fun `a production exercise waits for the user to choose a rating`() = runTest {
        val viewModel = viewModel(sessionWith(exercise(SESSION_ID, 0, "figure-out", ReviewType.PRODUCTION)))
        viewModel.onTextChanged("I'm trying to figure out why it crashes.")

        viewModel.checkAnswer()

        assertNull(viewModel.revealed().suggestedRating)
        assertTrue(reviewRepository.events.isEmpty())
    }

    @Test
    fun `the last rating completes the session and shows its summary`() = runTest {
        val viewModel = viewModel(sessionWith(exercise(SESSION_ID, 0, "figure-out", ReviewType.RECOGNITION)))
        viewModel.answerFirstOption()
        viewModel.checkAnswer()

        viewModel.onRate(ReviewRating.GOOD)

        val completed = viewModel.uiState.first { it is PracticeUiState.Completed } as PracticeUiState.Completed
        assertEquals(1, completed.summary.reviewedCount)
        assertEquals(1, completed.summary.newExpressions)
        assertEquals(100, completed.summary.recognitionPercent)
        assertEquals(1, reviewRepository.events.size)
    }

    @Test
    fun `an interrupted session resumes on the exercise it stopped at`() = runTest {
        val interrupted = sessionWith(
            exercise(SESSION_ID, 0, "figure-out", ReviewType.RECOGNITION),
            exercise(SESSION_ID, 1, "run-into", ReviewType.RECOGNITION),
        ).copy(currentPosition = 1)
        sessionRepository.save(interrupted)

        val viewModel = viewModel(interrupted)

        assertEquals(1, viewModel.active().progress.answered)
    }

    @Test
    fun `a session already completed summarises without practising it again`() = runTest {
        val finished = sessionWith(exercise(SESSION_ID, 0, "figure-out", ReviewType.RECOGNITION))
            .advancedAfter(0, REVIEWED_AT)
        sessionRepository.save(finished)
        reviewRepository.record(reviewEvent(finished))

        val viewModel = viewModel(finished)

        val completed = viewModel.uiState.first { it is PracticeUiState.Completed } as PracticeUiState.Completed
        assertEquals(SessionSummary.from(finished, reviewRepository.events), completed.summary)
    }

    @Test
    fun `finishing asks the screen to close`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())

        viewModel.onFinish()

        assertEquals(PracticeEffect.Finish, viewModel.effects.first())
    }

    @Test
    fun `checking twice is ignored while the first check is in flight`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())
        viewModel.answerFirstOption()

        viewModel.checkAnswer()
        viewModel.checkAnswer()

        // El segundo toque llega con la fase ya en «revelado», así que se descarta.
        assertTrue(viewModel.active().turn is AnswerTurn.Revealed)
        assertTrue(reviewRepository.events.isEmpty())
    }

    @Test
    fun `rating twice is ignored while the first rating is in flight`() = runTest {
        val viewModel = viewModel(sessionWithTwoRecognitionSteps())
        viewModel.answerFirstOption()
        viewModel.checkAnswer()

        viewModel.onRate(ReviewRating.GOOD)
        viewModel.onRate(ReviewRating.GOOD)

        assertEquals(1, reviewRepository.events.size)
    }

    @Test
    fun `a blank answer cannot be checked`() = runTest {
        val viewModel = viewModel(sessionWith(exercise(SESSION_ID, 0, "figure-out", ReviewType.CLOZE)))

        viewModel.onTextChanged("   ")
        viewModel.checkAnswer()

        assertTrue(viewModel.active().turn is AnswerTurn.Answering)
        assertTrue(reviewRepository.events.isEmpty())
    }

    @Test
    fun `a choice is ignored on a written exercise`() = runTest {
        val viewModel = viewModel(sessionWith(exercise(SESSION_ID, 0, "figure-out", ReviewType.CLOZE)))

        viewModel.onOptionSelected("figure-out")

        assertNull(viewModel.active().answering?.draft)
    }

    @Test
    fun `text is ignored on a recognition exercise`() = runTest {
        val viewModel = viewModel(sessionWith(exercise(SESSION_ID, 0, "figure-out", ReviewType.RECOGNITION)))

        viewModel.onTextChanged("figure out")

        assertNull(viewModel.active().answering?.draft)
    }

    // region ayudantes

    /**
     * El ViewModel carga la sesión al construirse, así que se construye dentro del
     * test: `viewModelScope` captura `Dispatchers.Main` en el constructor y la
     * regla lo inyecta después.
     */
    private fun viewModel(
        session: LearningSession? = null,
        sessionId: SessionId = SESSION_ID,
    ) = PracticeViewModel(
        sessionId = session?.id ?: sessionId,
        sessionRepository = sessionRepository,
        expressionRepository = expressionRepository,
        exerciseGenerator = generator,
        learningEngine = engine,
        reviewRepository = reviewRepository,
        learningStateRepository = learningStateRepository,
        now = now,
    )

    private suspend fun sessionWith(vararg exercises: Exercise) = LearningSession(
        id = SESSION_ID,
        startedAt = NOW,
        exercises = exercises.toList(),
    ).also { session -> sessionRepository.save(session) }

    private suspend fun sessionWithTwoRecognitionSteps() = sessionWith(
        exercise(SESSION_ID, 0, "figure-out", ReviewType.RECOGNITION),
        exercise(SESSION_ID, 1, "run-into", ReviewType.RECOGNITION),
    )

    private suspend fun markAsReviewed(expressionId: ExpressionId) {
        learningStateRepository.upsert(LearningState.new(expressionId).copy(reviewCount = 1))
    }

    private fun reviewEvent(session: LearningSession) = ReviewEvent(
        id = ReviewId("review-1"),
        expressionId = session.exercises.first().expressionId,
        reviewType = ReviewType.RECOGNITION,
        rating = ReviewRating.GOOD,
        reviewedAt = REVIEWED_AT,
        sessionId = session.id,
        previousStage = LearningStage.NEW,
        newStage = LearningStage.SEEN,
    )

    private suspend fun expressionIdOf(position: Int) =
        requireNotNull(sessionWithTwoRecognitionSteps().exercises[position].expressionId)

    private suspend fun savedSession() = requireNotNull(sessionRepository.getById(SESSION_ID))

    private suspend fun PracticeViewModel.active(): PracticeUiState.Active =
        uiState.first { it is PracticeUiState.Active } as PracticeUiState.Active

    private suspend fun PracticeViewModel.revealed(): AnswerTurn.Revealed = active().turn as AnswerTurn.Revealed

    private suspend fun PracticeViewModel.answerFirstOption() {
        val prompt = active().prompt as RecognitionExercise
        onOptionSelected(prompt.correctOptionId)
    }

    private suspend fun PracticeViewModel.answerWrongOption() {
        val prompt = active().prompt as RecognitionExercise
        onOptionSelected(prompt.options.first { it.id != prompt.correctOptionId }.id)
    }

    /** Comprobar es cuando el usuario ha terminado de responder. */
    private fun PracticeViewModel.checkAnswer() {
        clock = REVIEWED_AT
        onCheck()
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val REVIEWED_AT = 1_700_000_100_000L
        val SESSION_ID = SessionId("session-1")
    }
}