package com.example.learning.english.learning.english.app.data.repository

import com.example.learning.english.learning.english.app.data.persistence.AppDatabase
import com.example.learning.english.learning.english.app.data.persistence.Fixtures
import com.example.learning.english.learning.english.app.data.persistence.createInMemoryDatabase
import com.example.learning.english.learning.english.app.data.persistence.entity.ContentPackEntity
import com.example.learning.english.learning.english.app.domain.model.PackId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomContentPackRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomContentPackRepository

    @Before
    fun setUp() = runBlocking {
        database = createInMemoryDatabase()
        repository = RoomContentPackRepository(database.contentPackDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `is empty before any pack is installed`() = runBlocking {
        assertEquals(emptyList<Any>(), repository.observeAll().first())
    }

    @Test
    fun `exposes the pack metadata with its expression count`() = runBlocking {
        database.contentPackDao().upsert(Fixtures.pack)
        database.expressionDao().upsertExpressionRow(Fixtures.expressionEntity())
        database.expressionDao().upsertExpressionRow(
            Fixtures.expressionEntity(id = "look-up", phrase = "look up"),
        )

        val pack = repository.observeAll().first().single()

        assertEquals(PackId(Fixtures.PACK_ID), pack.id)
        assertEquals("Core English", pack.name)
        assertEquals("Expresiones de base", pack.description)
        assertEquals(2, pack.expressionCount)
        assertEquals(true, pack.bundled)
    }

    @Test
    fun `keeps a pack with no expressions`() = runBlocking {
        database.contentPackDao().upsert(Fixtures.pack)

        val pack = repository.observeAll().first().single()

        assertEquals(0, pack.expressionCount)
    }

    @Test
    fun `orders packs by name and counts only their own expressions`() = runBlocking {
        val expressions = database.expressionDao()
        database.contentPackDao().upsertAll(listOf(Fixtures.pack, developerPack()))
        expressions.upsertExpressionRow(Fixtures.expressionEntity())
        expressions.upsertExpressionRow(Fixtures.expressionEntity(id = "ship-it", packId = DEVELOPER_PACK_ID))

        val packs = repository.observeAll().first()

        assertEquals(listOf("Core English", "Developer English"), packs.map { it.name })
        assertEquals(listOf(1, 1), packs.map { it.expressionCount })
    }

    @Test
    fun `reacts to a pack installed after the first emission`() = runBlocking {
        val flow = repository.observeAll()
        assertEquals(emptyList<Any>(), flow.first())

        database.contentPackDao().upsert(Fixtures.pack)

        assertEquals(1, flow.first().size)
    }

    private fun developerPack() = ContentPackEntity(
        id = DEVELOPER_PACK_ID,
        name = "Developer English",
        description = "Expresiones técnicas",
        version = 1,
        language = "en",
        source = null,
        installedAt = 1_000L,
        updatedAt = 1_000L,
        bundled = true,
    )

    private companion object {
        const val DEVELOPER_PACK_ID = "developer-english"
    }
}
