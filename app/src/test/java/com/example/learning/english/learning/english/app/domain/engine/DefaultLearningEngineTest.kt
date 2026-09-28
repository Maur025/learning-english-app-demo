package com.example.learning.english.learning.english.app.domain.engine

import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluation
import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluator
import com.example.learning.english.learning.english.app.domain.exercise.DefaultExerciseGenerator
import com.example.learning.english.learning.english.app.domain.exercise.RecognitionExercise
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.UserAnswer
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.domain.scheduler.Sm2ReviewScheduler
import com.example.learning.english.learning.english.app.domain.service.ReviewRecorder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLearningEngineTest {

    private val generator = DefaultExerciseGenerator()
    private val expressionRepository = FakeExpressionRepository(EngineFixtures.all)
    private val learningStateRepository = FakeLearningStateRepository()
    private val sessionRepository = FakeSessionRepository()
    private val reviewRepository = FakeReviewRepository()
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

    private val preferences = UserPreferences(onboardingCompleted = true, newExpressionsPerDay = 2)

    @Test
    fun `one call generates and persists today's session`() = runTest {
        val session = engine.createDailySession(preferences, NOW)

        assertEquals(2, session.exercises.size)
        assertEquals(listOf(0, 1), session.exercises.map { it.position })
        assertEquals(NOW, session.startedAt)
        assertEquals(0, session.currentPosition)
        assertEquals(session, sessionRepository.getById(session.id))
    }

    @Test
    fun `the session keeps the planned order`() = runTest {
        learningStateRepository.upsertAll(
            listOf(
                EngineFixtures.state("run-into", LearningStage.SEEN, nextReviewAt = NOW - 5_000),
                EngineFixtures.state("figure-out", LearningStage.SEEN, nextReviewAt = NOW - 50_000),
            ),
        )

        val session = engine.createDailySession(preferences.copy(newExpressionsPerDay = 0), NOW)

        assertEquals(
            listOf("figure-out", "run-into"),
            session.exercises.map { it.expressionId.value },
        )
    }

    @Test
    fun `a day with nothing to practice is still recorded`() = runTest {
        val session = engine.createDailySession(preferences.copy(newExpressionsPerDay = 0), NOW)

        assertTrue(session.exercises.isEmpty())
        assertEquals(session, sessionRepository.getById(session.id))
    }

    @Test
    fun `each call plans a new session instead of reusing an unfinished one`() = runTest {
        val first = engine.createDailySession(preferences, NOW)
        val second = engine.createDailySession(preferences, NOW)

        assertTrue(first.id != second.id)
        assertEquals(2, sessionRepository.sessions.size)
    }

    @Test
    fun `a correct recognition answer is graded and advances the session`() = runTest {
        val session = engine.createDailySession(preferences.copy(newExpressionsPerDay = 1), NOW)
        val step = session.nextExercise!!
        val practice = recognition(step)

        val result = engine.registerAnswer(
            session = session,
            exercise = step,
            answer = UserAnswer.Choice(practice.correctOptionId),
            reviewedAt = REVIEWED_AT,
        )

        assertTrue((result.evaluation as AnswerEvaluation.Graded).isCorrect)
        assertEquals(ReviewRating.GOOD, result.rating)
        assertEquals(1, result.learningState.reviewCount)
        assertEquals(LearningStage.SEEN, result.learningState.stage)
        assertEquals(1, result.session.currentPosition)
        assertTrue(result.isSessionCompleted)
        assertEquals(REVIEWED_AT + 2 * DAY_MILLIS, result.learningState.nextReviewAt)
        assertEquals(practice.reveal.expectedAnswers, result.evaluation.expectedAnswers)
    }

    @Test
    fun `a wrong answer forgets the expression and brings it back today`() = runTest {
        val session = engine.createDailySession(preferences.copy(newExpressionsPerDay = 1), NOW)
        val step = session.nextExercise!!
        val practice = recognition(step)
        val wrongOption = practice.options.first { it.id != practice.correctOptionId }.id

        val result = engine.registerAnswer(
            session = session,
            exercise = step,
            answer = UserAnswer.Choice(wrongOption),
            reviewedAt = REVIEWED_AT,
        )

        assertEquals(ReviewRating.FORGOT, result.rating)
        assertEquals(REVIEWED_AT, result.learningState.nextReviewAt)
        assertEquals(1, result.learningState.failedReviewCount)
        assertEquals(practice.reveal.expectedAnswers.first(), result.expectedAnswer)
    }

    @Test
    fun `the review history records the session and the response time`() = runTest {
        val session = engine.createDailySession(preferences.copy(newExpressionsPerDay = 1), NOW)

        engine.registerAnswer(
            session = session,
            exercise = session.nextExercise!!,
            answer = UserAnswer.Choice(recognition(session.nextExercise!!).correctOptionId),
            reviewedAt = REVIEWED_AT,
            responseTimeMs = 4_200L,
        )

        val event = reviewRepository.events.single()
        assertEquals(session.id, event.sessionId)
        assertEquals(4_200L, event.responseTimeMs)
        assertEquals(ReviewRating.GOOD, event.rating)
        assertEquals(LearningStage.NEW, event.previousStage)
        assertEquals(LearningStage.SEEN, event.newStage)
    }

    @Test
    fun `an explicit rating wins over the derived one`() = runTest {
        val session = engine.createDailySession(preferences.copy(newExpressionsPerDay = 1), NOW)
        val step = session.nextExercise!!

        val result = engine.registerAnswer(
            session = session,
            exercise = step,
            answer = UserAnswer.Choice(recognition(step).correctOptionId),
            reviewedAt = REVIEWED_AT,
            selfRating = ReviewRating.HARD,
        )

        assertEquals(ReviewRating.HARD, result.rating)
        assertEquals(ReviewRating.HARD, reviewRepository.events.single().rating)
    }

    @Test
    fun `a production exercise is self rated and never auto graded`() = runTest {
        val session = sessionWith(exercise(sessionId, 0, "figure-out", ReviewType.PRODUCTION))

        val result = engine.registerAnswer(
            session = session,
            exercise = session.nextExercise!!,
            answer = UserAnswer.Text("I'm trying to figure out why the app crashes."),
            reviewedAt = REVIEWED_AT,
            selfRating = ReviewRating.GOOD,
        )

        assertTrue(result.evaluation is AnswerEvaluation.SelfAssessed)
        assertEquals(ReviewRating.GOOD, result.rating)
        assertTrue(result.learningState.productionScore > 0)
        assertEquals(0, result.learningState.recognitionScore)
    }

    @Test
    fun `a production exercise without a self rating is rejected`() = runTest {
        val session = sessionWith(exercise(sessionId, 0, "figure-out", ReviewType.PRODUCTION))

        val failure = runCatching {
            engine.registerAnswer(
                session = session,
                exercise = session.nextExercise!!,
                answer = UserAnswer.Text("figure out"),
                reviewedAt = REVIEWED_AT,
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(reviewRepository.events.isEmpty())
    }

    @Test
    fun `a choice answer on a written exercise is rejected`() = runTest {
        val session = sessionWith(exercise(sessionId, 0, "figure-out", ReviewType.CLOZE))

        val failure = runCatching {
            engine.registerAnswer(
                session = session,
                exercise = session.nextExercise!!,
                answer = UserAnswer.Choice("figure-out"),
                reviewedAt = REVIEWED_AT,
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `answering the same step twice is rejected`() = runTest {
        val session = sessionWith(
            exercise(sessionId, 0, "figure-out", ReviewType.RECOGNITION),
            exercise(sessionId, 1, "run-into", ReviewType.RECOGNITION),
        )
        val first = session.nextExercise!!
        val answered = engine.registerAnswer(
            session = session,
            exercise = first,
            answer = UserAnswer.Choice(recognition(first).correctOptionId),
            reviewedAt = REVIEWED_AT,
        ).session

        val failure = runCatching {
            engine.registerAnswer(
                session = answered,
                exercise = first,
                answer = UserAnswer.Choice(recognition(first).correctOptionId),
                reviewedAt = REVIEWED_AT + 1,
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(1, answered.currentPosition)
    }

    @Test
    fun `an exercise from another session is rejected`() = runTest {
        val session = sessionWith(exercise(sessionId, 0, "figure-out", ReviewType.RECOGNITION))
        val foreign = exercise(SessionId("other-session"), 0, "figure-out", ReviewType.RECOGNITION)

        val failure = runCatching {
            engine.registerAnswer(
                session = session,
                exercise = foreign,
                answer = UserAnswer.Choice(recognition(foreign).correctOptionId),
                reviewedAt = REVIEWED_AT,
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `an interrupted session resumes where it stopped`() = runTest {
        val session = sessionWith(
            exercise(sessionId, 0, "figure-out", ReviewType.RECOGNITION),
            exercise(sessionId, 1, "run-into", ReviewType.RECOGNITION),
        ).copy(currentPosition = 1)
        sessionRepository.save(session)

        val restored = requireNotNull(sessionRepository.getById(sessionId))

        assertEquals(session, restored)
        assertEquals("run-into", restored.nextExercise?.expressionId?.value)
    }

    private fun sessionWith(vararg exercises: Exercise) = LearningSession(
        id = sessionId,
        startedAt = NOW,
        exercises = exercises.toList(),
    )

    private suspend fun recognition(exercise: Exercise): RecognitionExercise {
        val expression = requireNotNull(expressionRepository.getById(exercise.expressionId))
        return requireNotNull(generator.generate(expression, exercise.reviewType, EngineFixtures.all))
            as RecognitionExercise
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val REVIEWED_AT = 1_700_000_100_000L
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        val sessionId = SessionId("session-1")
    }
}
