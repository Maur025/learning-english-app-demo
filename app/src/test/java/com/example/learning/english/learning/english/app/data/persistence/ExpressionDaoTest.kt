package com.example.learning.english.learning.english.app.data.persistence

import com.example.learning.english.learning.english.app.data.persistence.dao.ExpressionDao
import com.example.learning.english.learning.english.app.data.persistence.entity.TagEntity
import com.example.learning.english.learning.english.app.data.persistence.relation.ExpressionWithRelations
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
class ExpressionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ExpressionDao

    @Before
    fun setUp() = runBlocking {
        database = createInMemoryDatabase()
        dao = database.expressionDao()
        database.contentPackDao().upsert(Fixtures.pack)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `inserts an expression with its relations`() = runBlocking {
        dao.upsertExpressionRow(Fixtures.expressionEntity())
        dao.upsertExamples(listOf(Fixtures.exampleEntity()))
        dao.upsertPatterns(listOf(Fixtures.patternEntity()))

        val stored = dao.getById(Fixtures.EXPRESSION_ID)

        assertEquals("figure out", stored?.expression?.phrase)
        assertEquals(1, stored?.examples?.size)
        assertEquals("figure + object + out", stored?.patterns?.single()?.pattern)
    }

    @Test
    fun `reading a missing expression returns null`() = runBlocking {
        assertNull(dao.getById("missing"))
    }

    @Test
    fun `upserting the same expression twice does not duplicate children`() = runBlocking {
        val relation = ExpressionWithRelations(
            expression = Fixtures.expressionEntity(),
            examples = listOf(Fixtures.exampleEntity()),
            patterns = listOf(Fixtures.patternEntity()),
            tags = listOf(TagEntity("phrasal-verb", "phrasal-verb")),
        )

        dao.upsertExpression(relation)
        dao.upsertExpression(relation)

        val stored = dao.getById(Fixtures.EXPRESSION_ID)
        assertEquals(1, stored?.examples?.size)
        assertEquals(1, stored?.patterns?.size)
    }

    @Test
    fun `re-importing without examples removes the ones no longer present`() = runBlocking {
        dao.upsertExpressionRow(Fixtures.expressionEntity())
        dao.upsertExamples(
            listOf(
                Fixtures.exampleEntity(),
                Fixtures.exampleEntity().copy(id = "figure-out-example-2", english = "Another one."),
            ),
        )
        assertEquals(2, dao.getById(Fixtures.EXPRESSION_ID)?.examples?.size)

        dao.upsertExpression(
            ExpressionWithRelations(
                expression = Fixtures.expressionEntity(),
                examples = listOf(Fixtures.exampleEntity()),
                patterns = emptyList(),
                tags = emptyList(),
            ),
        )

        assertEquals(1, dao.getById(Fixtures.EXPRESSION_ID)?.examples?.size)
    }

    @Test
    fun `re-importing without patterns removes the ones no longer present`() = runBlocking {
        dao.upsertExpressionRow(Fixtures.expressionEntity())
        dao.upsertPatterns(listOf(Fixtures.patternEntity()))

        dao.upsertExpression(
            ExpressionWithRelations(
                expression = Fixtures.expressionEntity(),
                examples = emptyList(),
                patterns = emptyList(),
                tags = emptyList(),
            ),
        )

        assertTrue(dao.getById(Fixtures.EXPRESSION_ID)?.patterns.orEmpty().isEmpty())
    }

    @Test
    fun `deleting an expression cascades to state and examples`() = runBlocking {
        dao.upsertExpressionRow(Fixtures.expressionEntity())
        dao.upsertExamples(listOf(Fixtures.exampleEntity()))
        database.learningStateDao().upsert(Fixtures.learningStateEntity())

        dao.deleteById(Fixtures.EXPRESSION_ID)

        assertNull(database.learningStateDao().getByExpression(Fixtures.EXPRESSION_ID))
        assertTrue(dao.getById(Fixtures.EXPRESSION_ID)?.examples.orEmpty().isEmpty())
    }

    @Test
    fun `observes expressions filtered by pack`() = runBlocking {
        database.contentPackDao().upsert(Fixtures.pack.copy(id = "developer-english", name = "Developer English"))
        dao.upsertExpressionRow(Fixtures.expressionEntity())
        dao.upsertExpressionRow(Fixtures.expressionEntity(id = "run-into", packId = "developer-english"))

        val corePack = dao.observeByPack(Fixtures.PACK_ID).first()

        assertEquals(1, corePack.size)
        assertEquals("figure out", corePack.single().expression.phrase)
    }
}
