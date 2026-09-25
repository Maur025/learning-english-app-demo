package com.example.learning.english.learning.english.app.data.persistence

import com.example.learning.english.learning.english.app.domain.model.ReviewRating
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
class ReviewEventDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runBlocking {
        database = createInMemoryDatabase()
        database.contentPackDao().upsert(Fixtures.pack)
        database.expressionDao().upsertExpressionRow(Fixtures.expressionEntity())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `keeps every review as history instead of only the last score`() = runBlocking {
        val dao = database.reviewEventDao()

        dao.insert(Fixtures.reviewEventEntity(id = "r1", reviewedAt = 1_000L, rating = ReviewRating.FORGOT))
        dao.insert(Fixtures.reviewEventEntity(id = "r2", reviewedAt = 2_000L, rating = ReviewRating.GOOD))

        val history = dao.observeByExpression(Fixtures.EXPRESSION_ID).first()

        assertEquals(2, history.size)
        assertEquals(listOf("r1", "r2"), history.map { it.id })
    }

    @Test
    fun `history of one expression does not leak other expressions`() = runBlocking {
        val dao = database.reviewEventDao()
        database.expressionDao().upsertExpressionRow(Fixtures.expressionEntity(id = "run-into"))
        dao.insert(Fixtures.reviewEventEntity(id = "r1"))
        dao.insert(Fixtures.reviewEventEntity(id = "r2", expressionId = "run-into"))

        assertEquals(1, dao.countByExpression(Fixtures.EXPRESSION_ID))
        assertEquals(1, dao.observeByExpression(Fixtures.EXPRESSION_ID).first().size)
    }

    @Test
    fun `derives correctness from the stored rating`() = runBlocking {
        val dao = database.reviewEventDao()
        dao.insert(Fixtures.reviewEventEntity(id = "r1", rating = ReviewRating.FORGOT))
        dao.insert(Fixtures.reviewEventEntity(id = "r2", rating = ReviewRating.GOOD))

        val history = dao.observeByExpression(Fixtures.EXPRESSION_ID).first()

        assertTrue(history.first { it.id == "r1" }.rating.isCorrect.not())
        assertTrue(history.first { it.id == "r2" }.rating.isCorrect)
    }
}
