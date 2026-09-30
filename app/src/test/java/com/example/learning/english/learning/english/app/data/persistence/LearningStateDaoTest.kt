package com.example.learning.english.learning.english.app.data.persistence

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

    @Test
    fun `due count only includes the selected packs`() = runBlocking {
        val dao = database.learningStateDao()
        val now = 10_000L
        database.contentPackDao().upsert(
            Fixtures.pack.copy(id = OTHER_PACK_ID, name = "Developer English"),
        )
        database.expressionDao().upsertExpressionRow(
            Fixtures.expressionEntity(id = "ship-it", packId = OTHER_PACK_ID, phrase = "ship it"),
        )
        dao.upsert(Fixtures.learningStateEntity(nextReviewAt = now - 1))
        dao.upsert(Fixtures.learningStateEntity(expressionId = "run-into", nextReviewAt = now - 1))
        dao.upsert(Fixtures.learningStateEntity(expressionId = "ship-it", nextReviewAt = now - 1))

        assertEquals(2, dao.observeDueCount(now, listOf(Fixtures.PACK_ID)).first())
        assertEquals(1, dao.observeDueCount(now, listOf(OTHER_PACK_ID)).first())
        assertEquals(3, dao.observeDueCount(now, listOf(Fixtures.PACK_ID, OTHER_PACK_ID)).first())
    }

    @Test
    fun `due count ignores states that are not scheduled`() = runBlocking {
        val dao = database.learningStateDao()
        dao.upsert(Fixtures.learningStateEntity(nextReviewAt = null))
        dao.upsert(Fixtures.learningStateEntity(expressionId = "run-into", nextReviewAt = 20_000L))

        assertEquals(0, dao.observeDueCount(10_000L, listOf(Fixtures.PACK_ID)).first())
    }

    @Test
    fun `skill averages only count reviewed expressions`() = runBlocking {
        val dao = database.learningStateDao()
        dao.upsert(
            Fixtures.learningStateEntity().copy(
                recognitionScore = 80,
                productionScore = 40,
                reviewCount = 2,
            ),
        )
        dao.upsert(
            Fixtures.learningStateEntity(expressionId = "run-into").copy(
                recognitionScore = 60,
                productionScore = 20,
                reviewCount = 1,
            ),
        )
        // Sin revisiones: no debe rebajar la media.
        dao.upsert(Fixtures.learningStateEntity(expressionId = "fresh"))

        val averages = dao.observeSkillAverages(listOf(Fixtures.PACK_ID)).first()

        assertEquals(70.0, averages.avgRecognition!!, 0.01)
        assertEquals(30.0, averages.avgProduction!!, 0.01)
    }

    @Test
    fun `skill averages are null when nothing has been reviewed`() = runBlocking {
        database.learningStateDao().upsert(Fixtures.learningStateEntity())

        val averages = database.learningStateDao().observeSkillAverages(listOf(Fixtures.PACK_ID)).first()

        assertNull(averages.avgRecognition)
        assertNull(averages.avgProduction)
    }

    @Test
    fun `skill averages only count the selected packs`() = runBlocking {
        val dao = database.learningStateDao()
        database.contentPackDao().upsert(
            Fixtures.pack.copy(id = OTHER_PACK_ID, name = "Developer English"),
        )
        database.expressionDao().upsertExpressionRow(
            Fixtures.expressionEntity(id = "ship-it", packId = OTHER_PACK_ID, phrase = "ship it"),
        )
        dao.upsert(Fixtures.learningStateEntity().copy(recognitionScore = 90, reviewCount = 1))
        dao.upsert(
            Fixtures.learningStateEntity(expressionId = "ship-it").copy(
                recognitionScore = 10,
                reviewCount = 1,
            ),
        )

        assertEquals(90.0, dao.observeSkillAverages(listOf(Fixtures.PACK_ID)).first().avgRecognition!!, 0.01)
        assertEquals(10.0, dao.observeSkillAverages(listOf(OTHER_PACK_ID)).first().avgRecognition!!, 0.01)
    }

    private companion object {
        const val OTHER_PACK_ID = "developer-english"
    }
}
