package com.example.learning.english.learning.english.app.domain.engine

import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluation
import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluator
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseGenerator
import com.example.learning.english.learning.english.app.domain.exercise.PracticeExercise
import com.example.learning.english.learning.english.app.domain.exercise.RecognitionExercise
import com.example.learning.english.learning.english.app.domain.exercise.WrittenExercise
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.SessionPlan
import com.example.learning.english.learning.english.app.domain.model.UserAnswer
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.domain.repository.ExpressionRepository
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import com.example.learning.english.learning.english.app.domain.repository.SessionRepository
import com.example.learning.english.learning.english.app.domain.service.ReviewRecorder
import java.util.UUID

/**
 * Orquestador del motor de aprendizaje (README §37).
 *
 * No decide nada: [DailySessionPlanner] decide qué practicar, [ReviewRecorder]
 * escribe el estado y [AnswerEvaluator] califica. Aquí solo se carga, se conecta
 * y se persiste, para que las reglas que importan sigan siendo puras y testeables
 * sin Android.
 *
 * La sesión guarda únicamente la referencia de cada ejercicio (expresión + tipo +
 * posición): el contenido se reconstruye con [ExerciseGenerator], que es
 * determinista, así que no hace falta serializar ejercicios en Room.
 */
class DefaultLearningEngine(
    private val planner: DailySessionPlanner,
    private val exerciseGenerator: ExerciseGenerator,
    private val answerEvaluator: AnswerEvaluator,
    private val reviewRecorder: ReviewRecorder,
    private val expressionRepository: ExpressionRepository,
    private val learningStateRepository: LearningStateRepository,
    private val sessionRepository: SessionRepository,
) : LearningEngine {

    override suspend fun createDailySession(preferences: UserPreferences, now: Long): LearningSession {
        val expressions = expressionRepository.getAll()
        val plan = planner.plan(
            expressions = expressions,
            states = learningStateRepository.getAll(),
            preferences = preferences,
            now = now,
        )
        val sessionId = SessionId(UUID.randomUUID().toString())
        val session = LearningSession(
            id = sessionId,
            startedAt = now,
            exercises = plan.toExercises(sessionId, expressions),
        )
        sessionRepository.save(session)
        return session
    }

    override suspend fun registerAnswer(
        session: LearningSession,
        exercise: Exercise,
        answer: UserAnswer,
        reviewedAt: Long,
        selfRating: ReviewRating?,
        responseTimeMs: Long?,
    ): ReviewResult {
        require(exercise in session.exercises) { "exercise does not belong to session ${session.id.value}" }
        val pool = expressionRepository.getAll()
        val expression = requireNotNull(expressionRepository.getById(exercise.expressionId)) {
            "expression ${exercise.expressionId.value} is no longer available"
        }
        val practiceExercise = requireNotNull(exerciseGenerator.generate(expression, exercise.reviewType, pool)) {
            "content of ${expression.phrase} cannot support ${exercise.reviewType}"
        }

        val evaluation = evaluate(practiceExercise, answer)
        val rating = ratingFor(evaluation, selfRating)
        val learningState = reviewRecorder.record(
            expressionId = exercise.expressionId,
            reviewType = exercise.reviewType,
            rating = rating,
            reviewedAt = reviewedAt,
            sessionId = session.id,
            responseTimeMs = responseTimeMs,
        )
        val advancedSession = session.advancedAfter(exercise.position, reviewedAt)
        sessionRepository.save(advancedSession)

        return ReviewResult(
            evaluation = evaluation,
            rating = rating,
            learningState = learningState,
            session = advancedSession,
        )
    }

    private fun SessionPlan.toExercises(sessionId: SessionId, expressions: List<Expression>): List<Exercise> {
        val byId = expressions.associateBy { it.id }
        return steps.mapIndexed { position, step ->
            val expression = requireNotNull(byId[step.expressionId]) {
                "planned expression ${step.expressionId.value} is missing from the catalog"
            }
            require(exerciseGenerator.generate(expression, step.reviewType, expressions) != null) {
                "planned step ${step.expressionId.value} is not renderable as ${step.reviewType}"
            }
            Exercise(
                sessionId = sessionId,
                position = position,
                expressionId = step.expressionId,
                reviewType = step.reviewType,
            )
        }
    }

    private fun evaluate(exercise: PracticeExercise, answer: UserAnswer): AnswerEvaluation = when (exercise) {
        is RecognitionExercise -> {
            require(answer is UserAnswer.Choice) { "recognition needs a choice answer" }
            answerEvaluator.evaluateChoice(exercise, answer)
        }
        is WrittenExercise -> {
            require(answer is UserAnswer.Text) { "written exercises need a text answer" }
            answerEvaluator.evaluateText(exercise, answer)
        }
    }

    /**
     * En los ejercicios calificados sola manda la evaluación; si el usuario
     * autocalifica, su criterio manda porque sabe si la acertó despacio. En
     * producción no hay evaluación automática, así que el rating es obligatorio
     * (§35): fingir que un comparador de cadenas entiende inglés sería mentira.
     */    private fun ratingFor(evaluation: AnswerEvaluation, selfRating: ReviewRating?): ReviewRating =
        when (evaluation) {
            is AnswerEvaluation.Graded -> selfRating ?: if (evaluation.isCorrect) {
                ReviewRating.GOOD
            } else {
                ReviewRating.FORGOT
            }
            is AnswerEvaluation.SelfAssessed -> requireNotNull(selfRating) {
                "production exercises are self-rated (README §35)"
            }
        }
}
