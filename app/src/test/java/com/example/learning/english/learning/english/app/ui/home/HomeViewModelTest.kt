package com.example.learning.english.learning.english.app.ui.home

import com.example.learning.english.learning.english.app.domain.engine.EngineFixtures
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.ui.FakeContentPackRepository
import com.example.learning.english.learning.english.app.ui.FakeUserPreferencesRepository
import com.example.learning.english.learning.english.app.ui.MainDispatcherRule
import com.example.learning.english.learning.english.app.ui.contentPack
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = 1_000_000L
    private val preferencesRepository = FakeUserPreferencesRepository(
        UserPreferences(dailyGoalMinutes = 45),
    )
    private val contentPackRepository = FakeContentPackRepository(
        listOf(
            contentPack(EngineFixtures.CORE_PACK, "Core English"),
            contentPack(EngineFixtures.DEVELOPER_PACK, "Developer English"),
        ),
    )

    private val session = LearningSession(
        id = SessionId("session-1"),
        startedAt = now,
        exercises = listOf(
            Exercise(
                sessionId = SessionId("session-1"),
                position = 0,
                expressionId = EngineFixtures.figureOut.id,
                reviewType = ReviewType.RECOGNITION,
            ),
        ),
    )

    @Test
    fun `offers nothing to do while no content is installed`() = runTest {
        val viewModel = viewModel(
            data = FakeLearningData(initialExpressions = EngineFixtures.all),
            packs = FakeContentPackRepository(),
        )

        val state = viewModel.uiState.first()

        assertFalse(state.isContentReady)
        assertFalse(state.canStartSession)
    }

    @Test
    fun `counts due and new expressions`() = runTest {
        val data = FakeLearningData(
            initialExpressions = EngineFixtures.all,
            initialStates = listOf(
                EngineFixtures.state("figure-out", LearningStage.RECALLABLE, nextReviewAt = now - 1),
                EngineFixtures.state("run-into", LearningStage.SEEN, nextReviewAt = now + 100_000),
            ),
        )
        val viewModel = viewModel(data)

        val state = viewModel.uiState.first { it.dueCount == 1 }

        assertEquals(1, state.dueCount)
        assertEquals(4, state.newCount)
        assertEquals(45, state.dailyGoalMinutes)
    }

    @Test
    fun `only counts the selected packs`() = runTest {
        preferencesRepository.update { it.copy(preferredPackIds = setOf(PackId(EngineFixtures.CORE_PACK))) }
        val data = FakeLearningData(
            initialExpressions = EngineFixtures.all,
            initialStates = listOf(
                EngineFixtures.state("figure-out", LearningStage.RECALLABLE, nextReviewAt = now - 1),
                EngineFixtures.state("ship-it", LearningStage.RECALLABLE, nextReviewAt = now - 1),
            ),
        )
        val viewModel = viewModel(data)

        val state = viewModel.uiState.first { it.dueCount == 1 }

        assertEquals(1, state.dueCount)
        // De las 4 expresiones del pack solo "figure-out" está revisada.
        assertEquals(3, state.newCount)
    }

    @Test
    fun `reports both skill averages`() = runTest {
        val data = FakeLearningData(
            initialExpressions = EngineFixtures.all,
            initialStates = listOf(
                EngineFixtures.state("figure-out", LearningStage.RECALLABLE, recognitionScore = 80, productionScore = 40),
                EngineFixtures.state("run-into", LearningStage.SEEN, recognitionScore = 60, productionScore = 20),
            ),
        )
        val viewModel = viewModel(data)

        val state = viewModel.uiState.first { it.skillAverages.hasData }

        assertEquals(70, state.skillAverages.recognitionPercent)
        assertEquals(30, state.skillAverages.productionPercent)
    }

    @Test
    fun `has no percentages before the first review`() = runTest {
        val viewModel = viewModel(FakeLearningData(initialExpressions = EngineFixtures.all))

        val state = viewModel.uiState.first { it.newCount == EngineFixtures.all.size }

        assertFalse(state.skillAverages.hasData)
    }

    @Test
    fun `cannot start a session with nothing due and nothing new`() = runTest {
        val data = FakeLearningData(
            initialExpressions = EngineFixtures.all,
            initialStates = EngineFixtures.all.map {
                EngineFixtures.state(it.id.value, LearningStage.SEEN, reviewCount = 1, nextReviewAt = now + 100_000)
            },
        )
        val viewModel = viewModel(data)

        assertFalse(viewModel.uiState.first { it.newCount == 0 }.canStartSession)
    }

    @Test
    fun `starting a session asks the engine and opens it`() = runTest {
        val engine = FakeLearningEngine(session)
        val viewModel = viewModel(FakeLearningData(initialExpressions = EngineFixtures.all), engine)
        viewModel.uiState.first { it.canStartSession }

        viewModel.onStartSession()

        val effect = viewModel.effects.first() as HomeEffect.OpenSession
        assertEquals(SessionId("session-1"), effect.sessionId)
        assertEquals(1, engine.createdSessions)
        assertEquals(45, engine.requestedPreferences?.dailyGoalMinutes)
        assertEquals(now, engine.requestedNow)
    }

    @Test
    fun `does not create two sessions on a double tap`() = runTest {
        // La creación queda a medias: solo así el segundo toque llega mientras la
        // primera sesión todavía está en curso, como pasa en un dispositivo.
        val gate = CompletableDeferred<Unit>()
        val engine = FakeLearningEngine(session, gate)
        val viewModel = viewModel(FakeLearningData(initialExpressions = EngineFixtures.all), engine)
        viewModel.uiState.first { it.canStartSession }

        viewModel.onStartSession()
        viewModel.onStartSession()
        gate.complete(Unit)

        assertEquals(SessionId("session-1"), (viewModel.effects.first() as HomeEffect.OpenSession).sessionId)
        assertEquals(1, engine.createdSessions)
    }

    @Test
    fun `offers to continue the unfinished session`() = runTest {
        val data = FakeLearningData(initialExpressions = EngineFixtures.all)
        data.withSession(session.copy(currentPosition = 0))
        val viewModel = viewModel(data)

        val state = viewModel.uiState.first { it.hasInProgressSession }
        assertEquals(0, state.answeredCount)

        viewModel.onContinueSession()

        assertEquals(SessionId("session-1"), (viewModel.effects.first() as HomeEffect.OpenSession).sessionId)
    }

    @Test
    fun `offers nothing to continue once the session is completed`() = runTest {
        val data = FakeLearningData(initialExpressions = EngineFixtures.all)
        data.withSession(session.copy(completedAt = now, currentPosition = 1))
        val viewModel = viewModel(data)

        val state = viewModel.uiState.first { it.isContentReady }

        assertFalse(state.hasInProgressSession)
        viewModel.onContinueSession()
        assertNull(withTimeoutOrNull(NONE) { viewModel.effects.first() })
    }

    private fun viewModel(
        data: FakeLearningData,
        engine: FakeLearningEngine = FakeLearningEngine(session),
        packs: FakeContentPackRepository = contentPackRepository,
    ) = HomeViewModel(
        userPreferencesRepository = preferencesRepository,
        contentPackRepository = packs,
        learningStateRepository = data.learningStateRepository,
        expressionRepository = data.expressionRepository,
        sessionRepository = data.sessionRepository,
        learningEngine = engine,
        now = { now },
    )

    private companion object {
        /** Margen para comprobar que un efecto no llega, sin esperar de verdad. */
        const val NONE = 100L
    }
}
