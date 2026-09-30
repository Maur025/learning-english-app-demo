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

    override suspend fun evaluateAnswer(
        session: LearningSession,
        exercise: Exercise,
        answer: UserAnswer,
    ): AnswerEvaluation {
        require(exercise in session.exercises) { "exercise does not belong to session ${session.id.value}" }
        val practiceExercise = practiceExerciseOf(exercise)
        return when (practiceExercise) {
            is RecognitionExercise -> {
                require(answer is UserAnswer.Choice) { "recognition needs a choice answer" }
                answerEvaluator.evaluateChoice(practiceExercise, answer)
            }
            is WrittenExercise -> {
                require(answer is UserAnswer.Text) { "written exercises need a text answer" }
                answerEvaluator.evaluateText(practiceExercise, answer)
            }
        }
    }

    override suspend fun registerAnswer(
        session: LearningSession,
        exercise: Exercise,
        rating: ReviewRating,
        reviewedAt: Long,
        responseTimeMs: Long?,
    ): ReviewResult {
        require(exercise in session.exercises) { "exercise does not belong to session ${session.id.value}" }
        // Se comprueba el turno antes de escribir: si el ejercicio no es el actual,
        // una revisión huérfana quedaría registrada sin avanzar la sesión.
        require(exercise.position == session.currentPosition) {
            "cannot register position ${exercise.position} while the session is at ${session.currentPosition}"
        }
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
            rating = rating,
            learningState = learningState,
            session = advancedSession,
        )
    }

    /**
     * Reconstruye el ejercicio concreto desde la referencia persistida.
     *
     * [ExerciseGenerator] es determinista, así que evaluar y volver a evaluar dan
     * el mismo ejercicio sin necesidad de guardarlo en Room.
     */
    private suspend fun practiceExerciseOf(exercise: Exercise): PracticeExercise {
        val pool = expressionRepository.getAll()
        val expression = requireNotNull(expressionRepository.getById(exercise.expressionId)) {
            "expression ${exercise.expressionId.value} is no longer available"
        }
        return requireNotNull(exerciseGenerator.generate(expression, exercise.reviewType, pool)) {
            "content of ${expression.phrase} cannot support ${exercise.reviewType}"
        }
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
}
