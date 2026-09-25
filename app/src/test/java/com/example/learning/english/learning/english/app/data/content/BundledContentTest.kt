package com.example.learning.english.learning.english.app.data.content

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guarda contra el contenido real que viaja en el APK: si alguien rompe un
 * pack, falla la compilación de las pruebas y no el primer arranque del usuario.
 */
@RunWith(RobolectricTestRunner::class)
class BundledContentTest {

    private val parser = ContentPackParser()
    private val source = AssetContentSource(ApplicationProvider.getApplicationContext())

    @Test
    fun `every bundled pack is valid`() {
        BundledContent.FILES.forEach { fileName ->
            val file = parse(fileName)

            assertEquals(
                "el pack $fileName tiene incidencias",
                emptyList<ContentIssue>(),
                ContentPackValidator.validate(file),
            )
        }
    }

    @Test
    fun `the app ships between 50 and 100 expressions`() {
        val expressionCount = BundledContent.FILES.sumOf { parse(it).expressions.size }

        assertTrue(
            "se esperaban entre 50 y 100 expresiones y hay $expressionCount",
            expressionCount in 50..100,
        )
    }

    @Test
    fun `expression ids do not collide across packs`() {
        val ids = BundledContent.FILES.flatMap { parse(it).expressions }.map { it.id }

        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `expression ids are stable keys in the database`() {
        val packIds = BundledContent.FILES.map { parse(it).pack.id }

        assertEquals(packIds.size, packIds.distinct().size)
        assertEquals(BundledContent.FILES.size, packIds.size)
    }

    private fun parse(fileName: String): ContentPackFile {
        val result = parser.parse(source.readText(fileName))
        check(result is ContentParseResult.Parsed) { "$fileName no se pudo leer: $result" }
        return (result as ContentParseResult.Parsed).file
    }
}
