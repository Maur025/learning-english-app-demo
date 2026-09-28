package com.example.learning.english.learning.english.app.domain.exercise

import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.distractorPool
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.figureOut
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseFixtures.withoutExamples
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.UserAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerEvaluatorTest {

    private val generator = DefaultExerciseGenerator()
    private val evaluator = AnswerEvaluator()

    // region recognition

    @Test
    fun `recognition is correct when the right option is chosen`() {
        val exercise = generator.generate(figureOut, ReviewType.RECOGNITION, distractorPool) as RecognitionExercise

        val evaluation = evaluator.evaluateChoice(exercise, UserAnswer.Choice(exercise.correctOptionId))

        assertEquals(MatchKind.EXACT, evaluation.match)
        assertTrue(evaluation.isCorrect)
        assertEquals(listOf(figureOut.primaryMeaning), evaluation.expectedAnswers)
        assertNull(evaluation.normalizedAnswer)
    }

    @Test
    fun `recognition is incorrect when a distractor is chosen`() {
        val exercise = generator.generate(figureOut, ReviewType.RECOGNITION, distractorPool) as RecognitionExercise
        val wrongOption = exercise.options.first { it.id != exercise.correctOptionId }

        val evaluation = evaluator.evaluateChoice(exercise, UserAnswer.Choice(wrongOption.id))

        assertEquals(MatchKind.INCORRECT, evaluation.match)
        assertEquals(false, evaluation.isCorrect)
        assertEquals(figureOut.primaryMeaning, evaluation.expectedAnswer)
    }

    // endregion

    // region written exercises

    @Test
    fun `an identical sentence is an exact match`() {
        val exercise = generator.generate(figureOut, ReviewType.TRANSLATION) as TranslationExercise

        val evaluation = evaluator.evaluateText(exercise, UserAnswer.Text(exercise.reveal.expectedAnswer)).graded()

        assertEquals(MatchKind.EXACT, evaluation.match)
        assertEquals("i'm trying to figure out what happened last night", evaluation.normalizedAnswer)
    }

    @Test
    fun `differences in case apostrophe and terminal punctuation are accepted`() {
        val exercise = generator.generate(figureOut, ReviewType.CLOZE) as ClozeExercise

        val evaluation = evaluator.evaluateText(exercise, UserAnswer.Text("  Figure Out!  ")).graded()

        assertEquals(MatchKind.NORMALIZED, evaluation.match)
        assertTrue(evaluation.isCorrect)
    }

    @Test
    fun `a different but similar sentence is rejected`() {
        val exercise = generator.generate(figureOut, ReviewType.CLOZE) as ClozeExercise

        val evaluation = evaluator.evaluateText(exercise, UserAnswer.Text("figure it out")).graded()

        assertEquals(MatchKind.INCORRECT, evaluation.match)
        assertEquals(false, evaluation.isCorrect)
    }

    @Test
    fun `a blank answer is never correct`() {
        val exercise = generator.generate(figureOut, ReviewType.GUIDED_RECALL) as GuidedRecallExercise

        val evaluation = evaluator.evaluateText(exercise, UserAnswer.Text("   ")).graded()

        assertEquals(MatchKind.INCORRECT, evaluation.match)
        assertEquals("", evaluation.normalizedAnswer)
    }

    @Test
    fun `the surface form shown in the example is also accepted`() {
        val exercise = generator.generate(figureOut, ReviewType.CLOZE) as ClozeExercise

        val evaluation = evaluator.evaluateText(exercise, UserAnswer.Text("Figure Out")).graded()

        assertEquals(MatchKind.NORMALIZED, evaluation.match)
    }

    @Test
    fun `production is never graded automatically`() {
        val exercise = generator.generate(figureOut, ReviewType.PRODUCTION) as ProductionExercise

        val evaluation = evaluator.evaluateText(exercise, UserAnswer.Text("I'm trying to find out why it failed."))

        val selfAssessed = evaluation as AnswerEvaluation.SelfAssessed
        assertEquals(exercise.reveal.expectedAnswers, selfAssessed.expectedAnswers)
        assertEquals("i'm trying to find out why it failed", selfAssessed.normalizedAnswer)
        assertEquals(exercise.reveal.expectedAnswer, selfAssessed.expectedAnswer)
    }

    // endregion

    @Test
    fun `the expected answer is revealed even when the answer is wrong`() {
        val exercise = generator.generate(figureOut, ReviewType.TRANSLATION) as TranslationExercise

        val evaluation = evaluator.evaluateText(exercise, UserAnswer.Text("I'm trying to find out what happened."))

        assertEquals(MatchKind.INCORRECT, evaluation.graded().match)
        assertEquals(exercise.reveal.expectedAnswers, evaluation.expectedAnswers)
    }

    @Test
    fun `exercises without examples are still evaluated against the phrase`() {
        val exercise = generator.generate(withoutExamples, ReviewType.TRANSLATION) as TranslationExercise

        val evaluation = evaluator.evaluateText(exercise, UserAnswer.Text("be used to."))

        assertEquals(MatchKind.NORMALIZED, evaluation.graded().match)
    }

    /** Todos los tipos escritos salvo production se califican automáticamente (§35). */
    private fun AnswerEvaluation.graded(): AnswerEvaluation.Graded = this as AnswerEvaluation.Graded
}
