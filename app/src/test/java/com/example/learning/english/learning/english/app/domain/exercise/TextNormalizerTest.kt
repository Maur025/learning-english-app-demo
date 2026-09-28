package com.example.learning.english.learning.english.app.domain.exercise

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `lowercases and trims the text`() {
        assertEquals("figure out", TextNormalizer.normalize("  Figure Out  "))
    }

    @Test
    fun `collapses internal and repeated whitespace`() {
        assertEquals("i keep putting off writing", TextNormalizer.normalize("I  keep\tputting off \n writing"))
    }

    @Test
    fun `unifies typographic apostrophes quotes and dashes`() {
        assertEquals("i don't", TextNormalizer.normalize("I don’t"))
        assertEquals("figure out", TextNormalizer.normalize("“figure out”"))
        assertEquals("a - b", TextNormalizer.normalize("a – b"))
    }

    @Test
    fun `removes terminal and surrounding punctuation`() {
        assertEquals("figure out", TextNormalizer.normalize("figure out."))
        assertEquals("figure out", TextNormalizer.normalize("'figure out'!"))
        assertEquals("figure out", TextNormalizer.normalize("(figure out)"))
    }

    @Test
    fun `keeps punctuation inside the sentence`() {
        assertEquals("i can't figure it out, but i'll try", TextNormalizer.normalize("I can't figure it out, but I'll try"))
    }

    @Test
    fun `blank answers normalize to an empty string and never match`() {
        assertEquals("", TextNormalizer.normalize("   "))
        assertEquals("", TextNormalizer.normalize("...  !"))
        assertEquals(false, TextNormalizer.matches("  ", "figure out"))
    }

    @Test
    fun `matches ignores formatting differences but not different words`() {
        assertEquals(true, TextNormalizer.matches("  Figure Out!  ", "figure out"))
        assertEquals(true, TextNormalizer.matches("I don't know", "I don't know"))
        assertEquals(false, TextNormalizer.matches("figure of", "figure out"))
    }
}
