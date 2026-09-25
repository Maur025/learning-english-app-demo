package com.example.learning.english.learning.english.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionTest {

    @Test
    fun `expression keeps examples, patterns and tags together`() {
        val expressionId = ExpressionId("figure-out")
        val expression = Expression(
            id = expressionId,
            phrase = "figure out",
            primaryMeaning = "averiguar",
            packId = PackId("core-english"),
            difficulty = Difficulty.MEDIUM,
            examples = listOf(
                Example(
                    id = ExampleId("figure-out-1"),
                    expressionId = expressionId,
                    english = "We need to figure out what happened.",
                    isPrimary = true,
                ),
            ),
            patterns = listOf(
                ExpressionPattern(
                    id = PatternId("figure-out-pattern-1"),
                    expressionId = expressionId,
                    pattern = "figure + object + out",
                ),
            ),
            tags = setOf(Tag(TagId("phrasal-verb"), "phrasal-verb")),
        )

        assertEquals(1, expression.examples.size)
        assertEquals(1, expression.patterns.size)
        assertEquals(setOf("phrasal-verb"), expression.tags.map { it.name }.toSet())
    }

    @Test
    fun `optional fields fall back to sensible defaults`() {
        val expression = Expression(
            id = ExpressionId("figure-out"),
            phrase = "figure out",
            primaryMeaning = "averiguar",
            packId = PackId("core-english"),
            difficulty = Difficulty.MEDIUM,
        )

        assertEquals(null, expression.level)
        assertEquals(null, expression.explanation)
        assertTrue(expression.examples.isEmpty())
    }

    @Test
    fun `blank phrase or meaning is rejected`() {
        assertIllegalArgument {
            Expression(
                id = ExpressionId("figure-out"),
                phrase = " ",
                primaryMeaning = "averiguar",
                packId = PackId("core-english"),
                difficulty = Difficulty.MEDIUM,
            )
        }
        assertIllegalArgument {
            Expression(
                id = ExpressionId("figure-out"),
                phrase = "figure out",
                primaryMeaning = "",
                packId = PackId("core-english"),
                difficulty = Difficulty.MEDIUM,
            )
        }
    }

    @Test
    fun `ids compare by value`() {
        assertEquals(ExpressionId("figure-out"), ExpressionId("figure-out"))
        assertEquals("figure-out", ExpressionId("figure-out").value)
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        val result = runCatching(block)
        assertTrue("expected IllegalArgumentException", result.exceptionOrNull() is IllegalArgumentException)
    }
}
