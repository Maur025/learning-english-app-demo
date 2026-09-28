package com.example.learning.english.learning.english.app.data.persistence

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LearningStateDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runBlocking {
        database = createInMemoryDatabase()
        database.contentPackDao().upsert(Fixtures.pack)
        database.expressionDao().upsertExpressionRow(Fixtures.expressionEntity())
        database.expressionDao().upsertExpressionRow(Fixtures.expressionEntity(id = "run-into"))
        database.expressionDao().upsertExpressionRow(Fixtures.expressionEntity(id = "fresh"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `stores recognition and production scores separately`() = runBlocking {
        database.learningStateDao().upsert(
            Fixtures.learningStateEntity().copy(recognitionScore = 70, productionScore = 15),
        )

        val stored = database.learningStateDao().getByExpression(Fixtures.EXPRESSION_ID)

        assertEquals(70, stored?.recognitionScore)
        assertEquals(15, stored?.productionScore)
    }

    @Test
    fun `upsert replaces the existing state instead of duplicating it`() = runBlocking {
        val dao = database.learningStateDao()

        dao.upsert(Fixtures.learningStateEntity())
        dao.upsert(Fixtures.learningStateEntity().copy(nextReviewAt = 5_000L))

        assertEquals(1, dao.observeAll().first().size)
        assertEquals(5_000L, dao.getByExpression(Fixtures.EXPRESSION_ID)?.nextReviewAt)
    }

    @Test
    fun `due query returns only scheduled states that are already due`() = runBlocking {
        val dao = database.learningStateDao()
        val now = 10_000L
        dao.upsert(Fixtures.learningStateEntity(nextReviewAt = now - 1))
        dao.upsert(Fixtures.learningStateEntity(expressionId = "run-into", nextReviewAt = now + 1_000))
        dao.upsert(Fixtures.learningStateEntity(expressionId = "fresh", nextReviewAt = null))

        val due = dao.observeDue(now).first()

        assertEquals(listOf(Fixtures.EXPRESSION_ID), due.map { it.expressionId })
        assertEquals(1, dao.countDue(now))
    }

    @Test
    fun `reads a snapshot of every learning state`() = runBlocking {
        val dao = database.learningStateDao()
        dao.upsert(Fixtures.learningStateEntity())
        dao.upsert(Fixtures.learningStateEntity(expressionId = "run-into", nextReviewAt = 20_000L))

        val snapshot = dao.getAll()

        assertEquals(2, snapshot.size)
        assertEquals(setOf(Fixtures.EXPRESSION_ID, "run-into"), snapshot.map { it.expressionId }.toSet())
    }

    @Test
    fun `counts states by stage`() = runBlocking {
        database.learningStateDao().upsert(Fixtures.learningStateEntity())

        val seen = database.learningStateDao()
            .observeCountByStage("SEEN")
            .first()

        assertEquals(1, seen)
        assertTrue(database.learningStateDao().observeCountByStage("NEW").first() == 0)
    }
}
