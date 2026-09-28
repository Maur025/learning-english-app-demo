package com.example.learning.english.learning.english.app.data.persistence

import com.example.learning.english.learning.english.app.data.repository.RoomSessionRepository
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionPersistenceTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomSessionRepository

    private val sessionId = SessionId("session-1")

    @Before
    fun setUp() = runBlocking {
        database = createInMemoryDatabase()
        repository = RoomSessionRepository(database.learningSessionDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun session(
        exercises: List<Exercise>,
        completedAt: Long? = 5_000L,
        currentPosition: Int = exercises.size,
    ) = LearningSession(
        id = sessionId,
        startedAt = 1_000L,
        completedAt = completedAt,
        currentPosition = currentPosition,
        exercises = exercises,
    )

    private fun exercise(position: Int, expressionId: String) = Exercise(
        sessionId = sessionId,
        position = position,
        expressionId = ExpressionId(expressionId),
        reviewType = ReviewType.RECOGNITION,
    )

    @Test
    fun `saves and restores a session with its exercises`() = runBlocking {
        repository.save(session(listOf(exercise(0, "figure-out"), exercise(1, "run-into"))))

        val restored = repository.getById(sessionId)

        assertEquals(5_000L, restored?.completedAt)
        assertEquals(2, restored?.exercises?.size)
        assertEquals(listOf(0, 1), restored?.exercises?.map { it.position })
        assertTrue(restored?.isCompleted == true)
    }

    @Test
    fun `the progress of an interrupted session survives a restart`() = runBlocking {
        val exercises = listOf(exercise(0, "figure-out"), exercise(1, "run-into"))

        repository.save(session(exercises, completedAt = null, currentPosition = 0))
        repository.save(session(exercises, completedAt = null, currentPosition = 1))

        val restored = repository.getById(sessionId)

        assertEquals(1, restored?.currentPosition)
        assertEquals("run-into", restored?.nextExercise?.expressionId?.value)
        assertTrue(restored?.isCompleted == false)
    }

    @Test
    fun `saving again replaces the previous exercise list`() = runBlocking {
        repository.save(session(listOf(exercise(0, "figure-out"), exercise(1, "run-into"))))
        repository.save(session(listOf(exercise(0, "figure-out"))))

        assertEquals(1, repository.getById(sessionId)?.exercises?.size)
    }

    @Test
    fun `saving a session without exercises is valid`() = runBlocking {
        repository.save(session(exercises = emptyList(), completedAt = null))

        val restored = repository.getById(sessionId)

        assertEquals(0, restored?.exercises?.size)
        assertTrue(restored?.isCompleted == false)
    }

    @Test
    fun `returns null for an unknown session`() = runBlocking {
        assertNull(repository.getById(SessionId("missing")))
    }

    @Test
    fun `observes recent sessions most recent first and respects the limit`() = runBlocking {
        repository.save(LearningSession(SessionId("old"), startedAt = 1_000L))
        repository.save(LearningSession(SessionId("new"), startedAt = 3_000L))
        repository.save(LearningSession(SessionId("mid"), startedAt = 2_000L))

        val recent = repository.observeRecent(limit = 2).first()

        assertEquals(listOf("new", "mid"), recent.map { it.id.value })
    }
}
