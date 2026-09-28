package com.example.learning.english.learning.english.app.domain.exercise

import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.FIGURE_OUT_ID
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.distractorPool
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.example
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.expression
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.figureOut
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.putOff
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.withoutExamples
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultExerciseGeneratorTest {

    private val generator = DefaultExerciseGenerator()

    // region recognition

    @Test
    fun `recognition offers the primary meaning plus distinct distractors`() {
        val exercise = generator.generate(figureOut, ReviewType.RECOGNITION, distractorPool)
                as RecognitionExercise

        assertEquals(4, exercise.options.size)
        assertEquals(figureOut.phrase, exercise.prompt)
        assertEquals(figureOut.primaryMeaning, exercise.correctOption.text)
        assertEquals(4, exercise.options.map { it.text }.toSet().size)
        assertTrue(exercise.options.any { it.id == exercise.correctOptionId })
    }

    @Test
    fun `recognition is not generated without a distractor`() {
        assertNull(generator.generate(figureOut, ReviewType.RECOGNITION, distractorPool = emptyList()))
    }

    @Test
    fun `recognition ignores candidates repeating the correct meaning`() {
        val duplicateMeaning = expression(
            id = "duplicate",
            phrase = "work out",
            meaning = figureOut.primaryMeaning.uppercase(),
        )

        val exercise = generator.generate(figureOut, ReviewType.RECOGNITION, listOf(duplicateMeaning, putOff))
                as RecognitionExercise

        assertEquals(2, exercise.options.size)
        assertEquals(false, exercise.options.any { it.id == duplicateMeaning.id.value })
    }

    @Test
    fun `recognition is deterministic and does not always place the answer first`() {
        val exercises = distractorPool.map { expression ->
            generator.generate(expression, ReviewType.RECOGNITION, distractorPool) as RecognitionExercise
        }

        val repeated = generator.generate(figureOut, ReviewType.RECOGNITION, distractorPool)
        assertEquals(repeated, generator.generate(figureOut, ReviewType.RECOGNITION, distractorPool))
        assertTrue(exercises.map { it.options.indexOfFirst { option -> option.id == it.correctOptionId } }.toSet().size > 1)
    }

    // endregion

    // region cloze

    @Test
    fun `cloze hides the phrase inside the primary example`() {
        val exercise = generator.generate(figureOut, ReviewType.CLOZE) as ClozeExercise

        assertEquals("I'm trying to ______ what happened last night.", exercise.sentence)
        assertEquals("figure out", exercise.hiddenAnswer)
        assertEquals(listOf("figure out"), exercise.reveal.expectedAnswers)
        assertTrue(exercise.reveal.example!!.english.contains("figure out"))
    }

    @Test
    fun `cloze matches the phrase regardless of case`() {
        val expression = expression(
            id = "figure-out",
            phrase = "figure out",
            meaning = "averiguar",
            examples = listOf(example(english = "We need to Figure Out what happened.")),
        )

        val exercise = generator.generate(expression, ReviewType.CLOZE) as ClozeExercise

        assertEquals("We need to ______ what happened.", exercise.sentence)
        assertEquals("Figure Out", exercise.hiddenAnswer)
        assertEquals(listOf("figure out"), exercise.reveal.expectedAnswers)
    }

    @Test
    fun `cloze falls back to the first word of the phrase when it is not in the example`() {
        val expression = expression(
            id = "figure-out",
            phrase = "figure out",
            meaning = "averiguar",
            examples = listOf(example(english = "It took me a while to get it.")),
        )

        val exercise = generator.generate(expression, ReviewType.CLOZE) as ClozeExercise

        assertEquals("______ out", exercise.sentence)
        assertEquals("figure", exercise.hiddenAnswer)
        assertEquals(listOf("figure out"), exercise.reveal.expectedAnswers)
    }

    @Test
    fun `cloze still works when the example is the phrase itself`() {
        val expression = expression(
            id = "figure-out",
            phrase = "figure out",
            meaning = "averiguar",
            examples = listOf(example(english = "figure out")),
        )

        val exercise = generator.generate(expression, ReviewType.CLOZE) as ClozeExercise

        assertEquals("______ out", exercise.sentence)
        assertEquals(listOf("figure out"), exercise.reveal.expectedAnswers)
    }

    @Test
    fun `cloze is not generated for a single word without a matching example`() {
        val singleWord = ExerciseFixtures.singleWord

        assertNull(generator.generate(singleWord, ReviewType.CLOZE))
    }

    // endregion

    // region guided recall

    @Test
    fun `guided recall builds a first letter hint inside the example`() {
        val exercise = generator.generate(figureOut, ReviewType.GUIDED_RECALL) as GuidedRecallExercise

        assertEquals(figureOut.primaryMeaning, exercise.meaning)
        assertEquals("I'm trying to f----- o-- what happened last night.", exercise.hint)
        assertEquals(listOf(figureOut.phrase), exercise.reveal.expectedAnswers)
        assertEquals("figure + object + out", exercise.reveal.pattern)
    }

    @Test
    fun `guided recall falls back to the phrase skeleton when it is not in the example`() {
        val expression = expression(
            id = "figure-out",
            phrase = "figure out",
            meaning = "averiguar",
            examples = listOf(example(english = "It took me a while to get it.")),
        )

        val exercise = generator.generate(expression, ReviewType.GUIDED_RECALL) as GuidedRecallExercise

        assertEquals("f----- o--", exercise.hint)
    }

    // endregion

    // region translation

    @Test
    fun `translation prompts with the spanish example and expects the english one`() {
        val exercise = generator.generate(figureOut, ReviewType.TRANSLATION) as TranslationExercise

        assertEquals("Estoy intentando averiguar qué pasó anoche.", exercise.prompt)
        assertEquals("I'm trying to figure out what happened last night.", exercise.reveal.expectedAnswer)
    }

    @Test
    fun `translation falls back to the primary meaning when the example has no spanish`() {
        val expression = expression(
            id = "figure-out",
            phrase = "figure out",
            meaning = "averiguar",
            examples = listOf(example(english = "I can't figure it out.")),
        )

        val exercise = generator.generate(expression, ReviewType.TRANSLATION) as TranslationExercise

        assertEquals("averiguar", exercise.prompt)
        assertEquals("I can't figure it out.", exercise.reveal.expectedAnswer)
    }

    @Test
    fun `translation without examples expects the phrase`() {
        val exercise = generator.generate(withoutExamples, ReviewType.TRANSLATION) as TranslationExercise

        assertEquals(withoutExamples.primaryMeaning, exercise.prompt)
        assertEquals(listOf(withoutExamples.phrase), exercise.reveal.expectedAnswers)
    }

    // endregion

    // region production

    @Test
    fun `production describes the situation and reveals natural answers`() {
        val exercise = generator.generate(figureOut, ReviewType.PRODUCTION) as ProductionExercise

        assertEquals(figureOut.explanation, exercise.situation)
        assertEquals("general", exercise.guidance)
        assertEquals(
            listOf("I'm trying to figure out what happened last night.", figureOut.phrase),
            exercise.reveal.expectedAnswers,
        )
    }

    @Test
    fun `production falls back to the meaning when there is no explanation`() {
        val exercise = generator.generate(withoutExamples, ReviewType.PRODUCTION) as ProductionExercise

        assertEquals(withoutExamples.primaryMeaning, exercise.situation)
        assertNull(exercise.guidance)
        assertEquals(listOf(withoutExamples.phrase), exercise.reveal.expectedAnswers)
    }

    // endregion

    @Test
    fun `every review type produces an exercise tagged with its own type and cost`() {
        ReviewType.entries.forEach { reviewType ->
            val exercise = requireNotNull(generator.generate(figureOut, reviewType, distractorPool)) {
                "$reviewType should be generable from bundled content"
            }

            assertEquals(reviewType, exercise.reviewType)
            assertEquals(FIGURE_OUT_ID, exercise.expressionId.value)
            assertNotEquals(0, exercise.estimatedSeconds)
            assertEquals(reviewType.estimatedSeconds, exercise.estimatedSeconds)
        }
    }
}
