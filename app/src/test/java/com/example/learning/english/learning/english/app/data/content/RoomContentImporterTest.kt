package com.example.learning.english.learning.english.app.data.content

import com.example.learning.english.learning.english.app.data.persistence.AppDatabase
import com.example.learning.english.learning.english.app.data.persistence.Fixtures
import com.example.learning.english.learning.english.app.data.mapper.toDomain
import com.example.learning.english.learning.english.app.data.persistence.createInMemoryDatabase
import com.example.learning.english.learning.english.app.domain.model.PackId
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
class RoomContentImporterTest {

    private lateinit var database: AppDatabase
    private lateinit var importer: RoomContentImporter

    @Before
    fun setUp() {
        database = createInMemoryDatabase()
        importer = RoomContentImporter(database, clock = { 1_000L })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `imports the pack with its expressions, examples, patterns and tags`() = runBlocking {
        val result = importer.import(pack())

        assertEquals(PackImportResult.Imported(PackId(PACK_ID), 1), result)
        val stored = database.expressionDao().getById("$PACK_ID/figure-out")
        assertEquals("figure out", stored?.expression?.phrase)
        assertEquals(2, stored?.examples?.size)
        assertEquals(1, stored?.patterns?.size)
        assertEquals(setOf("phrasal-verb"), stored?.tags?.map { it.id }?.toSet())
    }

    @Test
    fun `importing the same revision twice skips and never duplicates`() = runBlocking {
        importer.import(pack())
        val second = importer.import(pack())

        assertEquals(PackImportResult.Skipped(PackId(PACK_ID), installedVersion = 1), second)
        assertEquals(1, database.expressionDao().count())
        assertEquals(1, database.tagDao().count())
    }

    @Test
    fun `an older revision never overwrites the installed one`() = runBlocking {
        importer.import(pack(version = 3))
        val result = importer.import(pack(version = 1))

        assertEquals(PackImportResult.Skipped(PackId(PACK_ID), installedVersion = 3), result)
    }

    @Test
    fun `a newer revision converges to the new content`() = runBlocking {
        importer.import(pack())
        val result = importer.import(pack(version = 2, withoutSecondExample = true))

        assertEquals(PackImportResult.Imported(PackId(PACK_ID), 1), result)
        val stored = database.expressionDao().getById("$PACK_ID/figure-out")
        assertEquals(1, stored?.examples?.size)
        assertEquals(2, database.contentPackDao().getByIds(listOf(PACK_ID)).single().version)
    }

    @Test
    fun `the installation date survives an update`() = runBlocking {
        importer.import(pack())
        importer.import(pack(version = 2))

        val stored = database.contentPackDao().getByIds(listOf(PACK_ID)).single()
        assertEquals(1_000L, stored.installedAt)
    }

    @Test
    fun `learning progress is not erased by a reimport`() = runBlocking {
        importer.import(pack())
        database.learningStateDao().upsert(Fixtures.learningStateEntity(expressionId = "$PACK_ID/figure-out"))

        importer.import(pack(version = 2))

        val state = database.learningStateDao().getByExpression("$PACK_ID/figure-out")
        assertEquals(10, state?.recognitionScore)
    }

    @Test
    fun `two packs can use the same local expression id`() = runBlocking {
        importer.import(pack())
        importer.import(pack(packId = "developer-english"))

        assertEquals(2, database.expressionDao().count())
        val core = database.expressionDao().observeByPack(PACK_ID).first()
        val developer = database.expressionDao().observeByPack("developer-english").first()
        assertEquals(PackId(PACK_ID), core.single().toDomain().packId)
        assertEquals(PackId("developer-english"), developer.single().toDomain().packId)
    }

    @Test
    fun `the pack is marked as bundled`() = runBlocking {
        importer.import(pack())

        val stored = database.contentPackDao().getByIds(listOf(PACK_ID)).single()
        assertTrue(stored.bundled)
    }

    private fun pack(
        packId: String = PACK_ID,
        version: Int = 1,
        withoutSecondExample: Boolean = false,
    ): ParsedContentPack {
        val examples = buildList {
            add(
                ExampleFile(
                    english = "I'm trying to figure out what happened.",
                    spanish = "Estoy intentando averiguar qué pasó.",
                    isPrimary = true,
                ),
            )
            if (!withoutSecondExample) {
                add(ExampleFile(english = "We need to figure out why it failed."))
            }
        }
        val file = ContentPackFile(
            schemaVersion = 1,
            pack = PackMetadataFile(id = packId, name = "Core English", version = version),
            expressions = listOf(
                ExpressionFile(
                    id = "figure-out",
                    phrase = "figure out",
                    meaning = "averiguar, resolver",
                    difficulty = "MEDIUM",
                    level = "B1",
                    patterns = listOf("figure + object + out"),
                    examples = examples,
                    tags = listOf("phrasal-verb"),
                ),
            ),
        )
        return ContentPackMapper.toParsedContentPack(file, timestamp = 1_000L)
    }

    private companion object {
        const val PACK_ID = "core-english"
    }
}
