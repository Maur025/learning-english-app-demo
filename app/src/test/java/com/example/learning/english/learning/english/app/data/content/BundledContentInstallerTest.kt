package com.example.learning.english.learning.english.app.data.content

import com.example.learning.english.learning.english.app.data.mapper.toDomain
import com.example.learning.english.learning.english.app.data.persistence.createInMemoryDatabase
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundledContentInstallerTest {

    private val database = createInMemoryDatabase()
    private val files = mutableMapOf<String, String>()

    private val source = object : ContentSource {
        override fun readText(fileName: String): String =
            files[fileName] ?: throw IOException("no existe el archivo $fileName")
    }

    private val installer = BundledContentInstaller(
        contentSource = source,
        parser = ContentPackParser(),
        importer = RoomContentImporter(database, clock = { 1_000L }),
        files = listOf("core-english.json", "developer-english.json"),
        dispatcher = Dispatchers.Unconfined,
        clock = { 1_000L },
    )

    @Before
    fun setUp() {
        files["core-english.json"] = ContentPackFixtures.validPack
        files["developer-english.json"] = ContentPackFixtures.validPackWith("\"developer-english\"", "\"core-english\"")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `installs every bundled pack`() = runBlocking {
        val report = installer.install()

        assertFalse(report.hasFailures)
        assertEquals(2, report.imported.size)
        assertEquals(2, report.importedExpressionCount)
        assertEquals(2, database.expressionDao().count())
    }

    @Test
    fun `a second run is idempotent`() = runBlocking {
        installer.install()
        val report = installer.install()

        assertEquals(emptyList<PackImportResult.Imported>(), report.imported)
        assertEquals(2, report.skipped.size)
        assertEquals(2, database.expressionDao().count())
    }

    @Test
    fun `an invalid pack is reported and does not stop the others`() = runBlocking {
        files["core-english.json"] = ContentPackFixtures.validPack.replace("\"difficulty\": \"MEDIUM\"", "\"difficulty\": \"IMPOSSIBLE\"")

        val report = installer.install()

        assertTrue(report.hasFailures)
        val failure = report.failures.single()
        assertEquals("core-english.json", failure.fileName)
        assertEquals(ContentIssueReason.UNKNOWN_DIFFICULTY, failure.issues.single().reason)
        assertEquals(listOf("developer-english"), report.imported.map { it.packId.value })
    }

    @Test
    fun `a malformed file is reported without throwing`() = runBlocking {
        files["core-english.json"] = "{ nope"

        val report = installer.install()

        val failure = report.failures.single()
        assertEquals(ContentIssueReason.MALFORMED_JSON, failure.issues.single().reason)
        assertEquals(1, database.expressionDao().count())
    }

    @Test
    fun `an unsupported schema version is not imported`() = runBlocking {
        files["core-english.json"] = ContentPackFixtures.validPackWith("\"schemaVersion\": 99", "\"schemaVersion\": 1")

        val report = installer.install()

        assertEquals(
            ContentIssueReason.UNSUPPORTED_SCHEMA_VERSION,
            report.failures.single().issues.single().reason,
        )
        assertEquals(1, database.contentPackDao().count())
    }

    @Test
    fun `a missing file is reported without throwing`() = runBlocking {
        files.remove("core-english.json")

        val report = installer.install()

        val failure = report.failures.single()
        assertEquals("core-english.json", failure.fileName)
        assertEquals(ContentIssueReason.UNREADABLE_FILE, failure.issues.single().reason)
        assertEquals(1, database.expressionDao().count())
    }

    @Test
    fun `imported content is readable as domain expressions`() = runBlocking {
        installer.install()

        val expression = database.expressionDao().observeById("core-english/figure-out").first()?.toDomain()

        assertEquals("figure out", expression?.phrase)
        assertEquals("averiguar, resolver", expression?.primaryMeaning)
        assertTrue(expression?.examples?.single()?.isPrimary == true)
    }
}
