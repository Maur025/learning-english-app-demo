package com.example.learning.english.learning.english.app.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentPackMapperTest {

    @Test
    fun `maps pack metadata`() {
        val pack = ContentPackMapper.toParsedContentPack(file(), timestamp = 1_000L)

        assertEquals("core-english", pack.metadata.id.value)
        assertEquals("Core English", pack.metadata.name)
        assertEquals(1, pack.metadata.version)
        assertEquals("en", pack.metadata.language)
        assertEquals("Expresiones del inglés general", pack.metadata.description)
    }

    @Test
    fun `namespaces expression ids with the pack`() {
        val pack = ContentPackMapper.toParsedContentPack(file(), timestamp = 1_000L)

        assertEquals("core-english/figure-out", pack.expressions.single().id.value)
    }

    @Test
    fun `derives stable ids for examples and patterns`() {
        val pack = ContentPackMapper.toParsedContentPack(file(), timestamp = 1_000L)

        val expression = pack.expressions.single()
        assertEquals("core-english/figure-out/examples/1", expression.examples.single().id.value)
        assertEquals("core-english/figure-out/patterns/1", expression.patterns.single().id.value)
    }

    @Test
    fun `applies the same timestamp to every imported row`() {
        val pack = ContentPackMapper.toParsedContentPack(file(), timestamp = 4_242L)

        val expression = pack.expressions.single()
        assertEquals(4_242L, expression.createdAt)
        assertEquals(4_242L, expression.updatedAt)
    }

    @Test
    fun `the first example becomes primary when the file marks none`() {
        val pack = ContentPackMapper.toParsedContentPack(file(), timestamp = 1_000L)

        assertTrue(pack.expressions.single().examples.single().isPrimary)
    }

    @Test
    fun `an example marked as primary keeps that role`() {
        val file = ContentPackFile(
            schemaVersion = 1,
            pack = PackMetadataFile(id = "core-english", name = "Core English"),
            expressions = listOf(
                ExpressionFile(
                    id = "figure-out",
                    phrase = "figure out",
                    meaning = "averiguar",
                    difficulty = "MEDIUM",
                    examples = listOf(
                        ExampleFile(english = "First example."),
                        ExampleFile(english = "Second example.", isPrimary = true),
                    ),
                ),
            ),
        )

        val examples = ContentPackMapper.toParsedContentPack(file, timestamp = 1_000L)
            .expressions
            .single()
            .examples

        assertEquals(listOf(false, true), examples.map { it.isPrimary })
        assertEquals("core-english/figure-out/examples/2", examples.last().id.value)
    }

    @Test
    fun `tags keep their name as identifier`() {
        val pack = ContentPackMapper.toParsedContentPack(file(), timestamp = 1_000L)

        val tags = pack.expressions.single().tags
        assertEquals(setOf("phrasal-verb", "problem-solving"), tags.map { it.id.value }.toSet())
        assertEquals(setOf("phrasal-verb", "problem-solving"), tags.map { it.name }.toSet())
    }

    private fun file(): ContentPackFile {
        val parsed = ContentPackParser().parse(ContentPackFixtures.validPack)
        check(parsed is ContentParseResult.Parsed) { "el fixture debe ser válido" }
        assertEquals(emptyList<ContentIssue>(), ContentPackValidator.validate((parsed as ContentParseResult.Parsed).file))
        return parsed.file
    }
}
