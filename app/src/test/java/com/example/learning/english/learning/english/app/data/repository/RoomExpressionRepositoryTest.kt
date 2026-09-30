package com.example.learning.english.learning.english.app.data.repository

import com.example.learning.english.learning.english.app.data.persistence.AppDatabase
import com.example.learning.english.learning.english.app.data.persistence.Fixtures
import com.example.learning.english.learning.english.app.data.persistence.createInMemoryDatabase
import com.example.learning.english.learning.english.app.domain.model.Example
import com.example.learning.english.learning.english.app.domain.model.ExampleId
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.ReviewId
import com.example.learning.english.learning.english.app.domain.model.SessionId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * El punto crítico de esta fase: Room como fuente de verdad y una importación
 * idempotente (README §29, §59).
 */
@RunWith(RobolectricTestRunner::class)
class RoomExpressionRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomExpressionRepository

    @Before
    fun setUp() = runBlocking {
        database = createInMemoryDatabase()
        repository = RoomExpressionRepository(database)
        database.contentPackDao().upsert(Fixtures.pack)
        database.contentPackDao().upsert(Fixtures.pack.copy(id = "developer-english", name = "Developer English"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `round trips an expression through Room`() = runBlocking {
        val expression = Fixtures.domainExpression()

        repository.upsertAll(listOf(expression))

        assertEquals(expression, repository.getById(expression.id))
    }

    @Test
    fun `importing the same content twice keeps a single copy`() = runBlocking {
        val content = listOf(
            Fixtures.domainExpression(tags = setOf(Fixtures.tag("phrasal-verb"))),
            Fixtures.domainExpression(
                id = "run-into",
                packId = "developer-english",
                phrase = "run into",
                tags = setOf(Fixtures.tag("idiom")),
            ),
        )

        repository.upsertAll(content)
        repository.upsertAll(content)

        val stored = repository.observeAll().first()
        assertEquals(2, stored.size)
        assertEquals(2, database.expressionDao().count())
        assertEquals(2, stored.sumOf { it.examples.size })
        assertEquals(2, database.tagDao().count())
        assertEquals(
            listOf("idiom", "phrasal-verb"),
            stored.map { it.tags.single().id.value }.sorted(),
        )
    }

    @Test
    fun `a tag shared by two expressions is stored once`() = runBlocking {
        repository.upsertAll(listOf(Fixtures.domainExpression()))
        repository.upsertAll(listOf(Fixtures.domainExpression(id = "run-into", phrase = "run into")))

        val tagDao = database.tagDao()
        assertEquals(1, tagDao.count())
        assertEquals(1, tagDao.getCrossRefsOf(Fixtures.EXPRESSION_ID).size)
        assertEquals(1, tagDao.getCrossRefsOf("run-into").size)
    }

    @Test
    fun `updating an expression replaces its examples and patterns`() = runBlocking {
        repository.upsertAll(listOf(Fixtures.domainExpression()))
        val updated = Fixtures.domainExpression(
            examples = listOf(
                Fixtures.domainExample().copy(id = ExampleId("figure-out-example-9"), english = "New example."),
            ),
        )

        repository.upsertAll(listOf(updated))

        val stored = repository.getById(ExpressionId(Fixtures.EXPRESSION_ID))
        assertEquals(1, stored?.examples?.size)
        assertEquals("New example.", stored?.examples?.single()?.english)
    }

    @Test
    fun `re-importing an expression without tags drops its old tag relations`() = runBlocking {
        repository.upsertAll(listOf(Fixtures.domainExpression(tags = setOf(Fixtures.tag("phrasal-verb")))))
        assertEquals(1, database.tagDao().getCrossRefsOf(Fixtures.EXPRESSION_ID).size)

        repository.upsertAll(listOf(Fixtures.domainExpression(tags = emptySet())))

        assertTrue(database.tagDao().getCrossRefsOf(Fixtures.EXPRESSION_ID).isEmpty())
        assertTrue(repository.getById(ExpressionId(Fixtures.EXPRESSION_ID))?.tags.orEmpty().isEmpty())
    }

    @Test
    fun `re-importing an expression without examples drops its old examples`() = runBlocking {
        repository.upsertAll(listOf(Fixtures.domainExpression()))

        repository.upsertAll(listOf(Fixtures.domainExpression(examples = emptyList())))

        assertTrue(repository.getById(ExpressionId(Fixtures.EXPRESSION_ID))?.examples.orEmpty().isEmpty())
    }

    @Test
    fun `fails loudly when the pack does not exist`() = runBlocking {
        val orphan = Fixtures.domainExpression(packId = "missing-pack")

        val failure = runCatching { repository.upsertAll(listOf(orphan)) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(failure?.message.orEmpty().contains("missing-pack"))
        assertEquals(0, database.expressionDao().count())
    }

    @Test
    fun `observes expressions by pack`() = runBlocking {
        repository.upsertAll(
            listOf(
                Fixtures.domainExpression(),
                Fixtures.domainExpression(id = "run-into", packId = "developer-english", phrase = "run into"),
            ),
        )

        assertEquals(1, repository.observeByPack(PackId("developer-english")).first().size)
        assertEquals(1, repository.observeByPack(PackId(Fixtures.PACK_ID)).first().size)
    }

    @Test
    fun `an empty import is a no-op`() = runBlocking {
        repository.upsertAll(emptyList())

        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test
    fun `counts expressions that were never reviewed`() = runBlocking {
        repository.upsertAll(
            listOf(
                Fixtures.domainExpression(),
                Fixtures.domainExpression(id = "run-into", packId = "developer-english", phrase = "run into"),
            ),
        )
        val stateRepository = RoomLearningStateRepository(database.learningStateDao())
        stateRepository.upsert(Fixtures.domainLearningState())
        stateRepository.upsert(
            Fixtures.domainLearningState(expressionId = "run-into").copy(reviewCount = 3),
        )

        assertEquals(1, repository.observeNewCount(setOf(PackId(Fixtures.PACK_ID))).first())
        assertEquals(0, repository.observeNewCount(setOf(PackId("developer-english"))).first())
    }

    @Test
    fun `counts nothing when no pack is selected`() = runBlocking {
        repository.upsertAll(listOf(Fixtures.domainExpression()))

        assertEquals(0, repository.observeNewCount(emptySet()).first())
    }

    @Test
    fun `reads a snapshot of the whole catalog with its relations`() = runBlocking {
        repository.upsertAll(
            listOf(
                Fixtures.domainExpression(tags = setOf(Fixtures.tag("phrasal-verb"))),
                Fixtures.domainExpression(id = "run-into", packId = "developer-english", phrase = "run into"),
            ),
        )

        val snapshot = repository.getAll()

        assertEquals(2, snapshot.size)
        assertEquals(1, snapshot.first { it.id.value == Fixtures.EXPRESSION_ID }.examples.size)
        assertEquals(
            "phrasal-verb",
            snapshot.first { it.id.value == Fixtures.EXPRESSION_ID }.tags.single().id.value,
        )
    }

    @Test
    fun `an empty catalog is an empty snapshot`() = runBlocking {
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `primary examples come first when reading content back`() = runBlocking {
        val primary = Fixtures.domainExample()
        val secondary = Example(
            id = ExampleId("figure-out-example-2"),
            expressionId = ExpressionId(Fixtures.EXPRESSION_ID),
            english = "Another example.",
            isPrimary = false,
        )
        repository.upsertAll(listOf(Fixtures.domainExpression(examples = listOf(primary, secondary))))

        val stored = repository.getById(ExpressionId(Fixtures.EXPRESSION_ID))

        assertEquals(listOf(true, false), stored?.examples?.map { it.isPrimary })
    }

    @Test
    fun `review history can be read back one session at a time`() = runBlocking {
        repository.upsertAll(listOf(Fixtures.domainExpression()))
        val reviews = RoomReviewRepository(database.reviewEventDao())
        reviews.record(Fixtures.domainReviewEvent(id = "review-1", reviewedAt = 3_000L, sessionId = SESSION_ID))
        reviews.record(Fixtures.domainReviewEvent(id = "review-2", reviewedAt = 1_000L, sessionId = SESSION_ID))
        reviews.record(Fixtures.domainReviewEvent(id = "review-3", reviewedAt = 2_000L, sessionId = "other"))

        val mine = reviews.getBySession(SessionId(SESSION_ID))

        assertEquals(listOf(ReviewId("review-2"), ReviewId("review-1")), mine.map { it.id })
        assertTrue(mine.all { it.sessionId == SessionId(SESSION_ID) })
    }

    @Test
    fun `a session without reviews reads back empty`() = runBlocking {
        repository.upsertAll(listOf(Fixtures.domainExpression()))
        val reviews = RoomReviewRepository(database.reviewEventDao())
        reviews.record(Fixtures.domainReviewEvent(sessionId = "other"))

        assertTrue(reviews.getBySession(SessionId(SESSION_ID)).isEmpty())
    }

    @Test
    fun `learning state and review history are stored through their own repositories`() = runBlocking {
        val learningStates = RoomLearningStateRepository(database.learningStateDao())
        val reviews = RoomReviewRepository(database.reviewEventDao())
        repository.upsertAll(listOf(Fixtures.domainExpression()))

        learningStates.upsert(Fixtures.domainLearningState(nextReviewAt = 10_000L))
        reviews.record(Fixtures.domainReviewEvent())

        val state = learningStates.getByExpression(ExpressionId(Fixtures.EXPRESSION_ID))
        val history: List<ReviewEvent> = reviews.observeHistory(ExpressionId(Fixtures.EXPRESSION_ID)).first()
        assertEquals(10, state?.recognitionScore)
        assertEquals(LearningState.new(ExpressionId(Fixtures.EXPRESSION_ID)).productionScore, state?.productionScore)
        assertEquals(1, history.size)
        assertTrue(history.single().isCorrect)
    }

    private companion object {
        const val SESSION_ID = "session-1"
    }
}
