package com.example.learning.english.learning.english.app.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentPackParserTest {

    private val parser = ContentPackParser()

    @Test
    fun `reads pack metadata and expressions`() {
        val result = parser.parse(ContentPackFixtures.validPack)

        assertTrue(result is ContentParseResult.Parsed)
        val file = (result as ContentParseResult.Parsed).file
        assertEquals(1, file.schemaVersion)
        assertEquals("core-english", file.pack.id)
        assertEquals(1, file.pack.version)
        assertEquals("en", file.pack.language)
        assertEquals("figure out", file.expressions.single().phrase)
    }

    @Test
    fun `reports malformed json instead of throwing`() {
        val result = parser.parse("{ this is not json")

        assertTrue(result is ContentParseResult.Malformed)
        val issue = (result as ContentParseResult.Malformed).issue
        assertEquals(ContentIssueReason.MALFORMED_JSON, issue.reason)
    }

    @Test
    fun `reports a missing required field`() {
        val withoutMeaning = ContentPackFixtures.validPack.replace("\"meaning\": \"averiguar, resolver\",", "")

        val result = parser.parse(withoutMeaning)

        assertTrue(result is ContentParseResult.Malformed)
        assertEquals(ContentIssueReason.MALFORMED_JSON, (result as ContentParseResult.Malformed).issue.reason)
    }

    @Test
    fun `ignores unknown fields so new optional data does not break older builds`() {
        val withExtraField = ContentPackFixtures.validPack.replace(
            "\"schemaVersion\": 1,",
            "\"schemaVersion\": 1, \"author\": \"equipo\",",
        )

        val result = parser.parse(withExtraField)

        assertTrue(result is ContentParseResult.Parsed)
    }

    @Test
    fun `expressions default to an empty list when the field is absent`() {
        val withoutExpressions = """
            {
              "schemaVersion": 1,
              "pack": { "id": "core-english", "name": "Core English" }
            }
        """.trimIndent()

        val result = parser.parse(withoutExpressions)

        assertTrue(result is ContentParseResult.Parsed)
        assertTrue((result as ContentParseResult.Parsed).file.expressions.isEmpty())
    }
}
