package com.example.learning.english.learning.english.app.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentPackValidatorTest {

    private val parser = ContentPackParser()

    @Test
    fun `a valid pack has no issues`() {
        assertEquals(emptyList<ContentIssue>(), issuesOf(ContentPackFixtures.validPack))
    }

    @Test
    fun `an unsupported schema version stops the review`() {
        val issues = issuesOf(ContentPackFixtures.validPackWith("\"schemaVersion\": 2", "\"schemaVersion\": 1"))

        assertEquals(listOf(ContentIssueReason.UNSUPPORTED_SCHEMA_VERSION), issues.map { it.reason })
        assertEquals("schemaVersion", issues.single().path)
    }

    @Test
    fun `an id that is not kebab-case is rejected`() {
        val issues = issuesOf(ContentPackFixtures.validPackWith("\"Core_English!\"", "\"$PACK_ID\""))

        assertEquals(listOf(ContentIssueReason.INVALID_IDENTIFIER), issues.map { it.reason })
        assertEquals("pack.id", issues.single().path)
    }

    @Test
    fun `a pack revision below one is rejected`() {
        val issues = issuesOf(ContentPackFixtures.validPackWith("\"version\": 0,", "\"version\": 1,"))

        assertEquals(listOf(ContentIssueReason.INVALID_VERSION), issues.map { it.reason })
    }

    @Test
    fun `an empty pack is rejected`() {
        val emptyPack = """
            {
              "schemaVersion": 1,
              "pack": { "id": "core-english", "name": "Core English" }
            }
        """.trimIndent()

        val issues = issuesOf(emptyPack)

        assertTrue(issues.any { it.reason == ContentIssueReason.EMPTY_PACK })
    }

    @Test
    fun `repeated expression ids are rejected once`() {
        val duplicated = """
            {
              "schemaVersion": 1,
              "pack": { "id": "core-english", "name": "Core English" },
              "expressions": [
                ${expressionJson(id = "figure-out")},
                ${expressionJson(id = "figure-out", phrase = "sort out")}
              ]
            }
        """.trimIndent()

        val issues = issuesOf(duplicated)

        assertEquals(listOf(ContentIssueReason.DUPLICATE_EXPRESSION_ID), issues.map { it.reason })
    }

    @Test
    fun `an unknown difficulty is reported with the accepted values`() {
        val issues = issuesOf(
            ContentPackFixtures.validPackWith("\"difficulty\": \"IMPOSSIBLE\"", "\"difficulty\": \"MEDIUM\""),
        )

        assertEquals(listOf(ContentIssueReason.UNKNOWN_DIFFICULTY), issues.map { it.reason })
        assertTrue(issues.single().detail.contains("MEDIUM"))
        assertEquals("expressions[0].difficulty", issues.single().path)
    }

    @Test
    fun `an unknown level is reported`() {
        val issues = issuesOf(ContentPackFixtures.validPackWith("\"level\": \"B3\"", "\"level\": \"B1\""))

        assertEquals(listOf(ContentIssueReason.UNKNOWN_LEVEL), issues.map { it.reason })
    }

    @Test
    fun `a blank phrase is reported`() {
        val issues = issuesOf(ContentPackFixtures.validPackWith("\"expression\": \" \"", "\"expression\": \"figure out\""))

        assertEquals(listOf(ContentIssueReason.BLANK_FIELD), issues.map { it.reason })
        assertEquals("expressions[0].expression", issues.single().path)
    }

    @Test
    fun `an expression without examples is rejected`() {
        val withoutExamples = """
            {
              "schemaVersion": 1,
              "pack": { "id": "core-english", "name": "Core English" },
              "expressions": [
                {
                  "id": "figure-out",
                  "expression": "figure out",
                  "meaning": "averiguar",
                  "difficulty": "MEDIUM"
                }
              ]
            }
        """.trimIndent()

        val issues = issuesOf(withoutExamples)

        assertTrue(issues.any { it.reason == ContentIssueReason.NO_EXAMPLES })
    }

    @Test
    fun `two primary examples are rejected`() {
        val twoPrimary = """
            {
              "schemaVersion": 1,
              "pack": { "id": "core-english", "name": "Core English" },
              "expressions": [
                {
                  "id": "figure-out",
                  "expression": "figure out",
                  "meaning": "averiguar",
                  "difficulty": "MEDIUM",
                  "examples": [
                    { "english": "First example.", "isPrimary": true },
                    { "english": "Second example.", "isPrimary": true }
                  ]
                }
              ]
            }
        """.trimIndent()

        val issues = issuesOf(twoPrimary)

        assertEquals(listOf(ContentIssueReason.MULTIPLE_PRIMARY_EXAMPLES), issues.map { it.reason })
    }

    @Test
    fun `an example with an unknown difficulty is reported`() {
        val brokenExample = """
            {
              "schemaVersion": 1,
              "pack": { "id": "core-english", "name": "Core English" },
              "expressions": [
                {
                  "id": "figure-out",
                  "expression": "figure out",
                  "meaning": "averiguar",
                  "difficulty": "MEDIUM",
                  "examples": [
                    { "english": "I'm trying to figure out what happened.", "difficulty": "IMPOSSIBLE" }
                  ]
                }
              ]
            }
        """.trimIndent()

        val issues = issuesOf(brokenExample)

        assertEquals(listOf(ContentIssueReason.UNKNOWN_DIFFICULTY), issues.map { it.reason })
        assertEquals("expressions[0].examples[0].difficulty", issues.single().path)
    }

    @Test
    fun `repeated tags inside one expression are rejected`() {
        val repeatedTags = ContentPackFixtures.validPack.replace(
            "[\"phrasal-verb\", \"problem-solving\"]",
            "[\"phrasal-verb\", \"phrasal-verb\"]",
        )

        val issues = issuesOf(repeatedTags)

        assertEquals(listOf(ContentIssueReason.DUPLICATE_TAG), issues.map { it.reason })
    }

    @Test
    fun `repeated patterns are rejected`() {
        val repeatedPatterns = ContentPackFixtures.validPack.replace(
            "[\"figure + object + out\"]",
            "[\"figure + object + out\", \"figure + object + out\"]",
        )

        val issues = issuesOf(repeatedPatterns)

        assertEquals(listOf(ContentIssueReason.DUPLICATE_PATTERN), issues.map { it.reason })
    }

    @Test
    fun `every problem of a pack is reported at once`() {
        val broken = """
            {
              "schemaVersion": 1,
              "pack": { "id": "core-english", "name": "" },
              "expressions": [
                ${expressionJson(id = "figure-out", difficulty = "IMPOSSIBLE")},
                ${expressionJson(id = "figure-out", phrase = "sort out", difficulty = "IMPOSSIBLE")}
              ]
            }
        """.trimIndent()

        val issues = issuesOf(broken)

        assertTrue(issues.any { it.reason == ContentIssueReason.BLANK_FIELD && it.path == "pack.name" })
        assertTrue(issues.any { it.reason == ContentIssueReason.DUPLICATE_EXPRESSION_ID })
        assertEquals(2, issues.count { it.reason == ContentIssueReason.UNKNOWN_DIFFICULTY })
    }

    private fun issuesOf(rawJson: String): List<ContentIssue> {
        val parsed = parser.parse(rawJson)
        check(parsed is ContentParseResult.Parsed) { "el fixture debe ser JSON válido: $parsed" }
        return ContentPackValidator.validate((parsed as ContentParseResult.Parsed).file)
    }

    private fun expressionJson(
        id: String,
        phrase: String = "figure out",
        difficulty: String = "MEDIUM",
    ) = """
        {
          "id": "$id",
          "expression": "$phrase",
          "meaning": "averiguar",
          "difficulty": "$difficulty",
          "examples": [{ "english": "I'm trying to figure out what happened." }]
        }
    """.trimIndent()

    private companion object {
        const val PACK_ID = "core-english"
    }
}
