package com.example.learning.english.learning.english.app.domain.engine

import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningStateProgressorTest {

    private val progressor = LearningStateProgressor()

    @Test
    fun `a recognition review only moves the recognition score`() {
        val state = newState().reviewed(ReviewType.CLOZE, ReviewRating.GOOD)

        assertEquals(30, state.recognitionScore)
        assertEquals(0, state.productionScore)
    }

    @Test
    fun `a production review only moves the production score`() {
        val state = newState().reviewed(ReviewType.TRANSLATION, ReviewRating.GOOD)

        assertEquals(0, state.recognitionScore)
        assertEquals(30, state.productionScore)
    }

    @Test
    fun `the first successful review moves the expression out of NEW`() {
        val state = newState().reviewed(ReviewType.RECOGNITION, ReviewRating.GOOD)

        assertEquals(LearningStage.SEEN, state.stage)
        assertEquals(1, state.reviewCount)
    }

    @Test
    fun `sustained correct recognition reviews walk the stage ladder`() {
        val stages = stagesAfter(RECOGNITION_TIMES, ReviewType.RECOGNITION, ReviewRating.GOOD)

        assertEquals(LearningStage.SEEN, stages[0])
        assertEquals(LearningStage.RECOGNIZED, stages[2])
        assertEquals(LearningStage.RECALLABLE, stages[4])
        assertTrue(LearningStage.PRODUCTIVE !in stages)
    }

    @Test
    fun `an expression is never mastered on recognition alone`() {
        val state = reviewTimes(RECOGNITION_TIMES, ReviewType.RECOGNITION, ReviewRating.EASY)

        assertTrue(state.recognitionScore >= 99)
        assertEquals(0, state.productionScore)
        assertEquals(LearningStage.RECALLABLE, state.stage)
    }

    @Test
    fun `sustained reviews on both axes reach mastery`() {
        val state = (1..RECOGNITION_TIMES).fold(newState()) { state, index ->
            val reviewed = if (index == 1) state else state.reviewed(ReviewType.RECOGNITION, ReviewRating.GOOD)
            reviewed.reviewed(ReviewType.PRODUCTION, ReviewRating.GOOD)
        }

        assertTrue(state.recognitionScore >= LearningStateProgressor.MASTERED_SCORE)
        assertTrue(state.productionScore >= LearningStateProgressor.MASTERED_SCORE)
        assertEquals(LearningStage.MASTERED, state.stage)
    }

    @Test
    fun `a forgotten review lowers the score and demotes the stage`() {
        val solid = reviewTimes(RECOGNITION_TIMES, ReviewType.RECOGNITION, ReviewRating.GOOD)

        val forgotten = progressor.apply(solid, ReviewType.RECOGNITION, ReviewRating.FORGOT)

        assertTrue(forgotten.recognitionScore < solid.recognitionScore)
        assertTrue(forgotten.stage < solid.stage)
    }

    @Test
    fun `a hard review penalizes less than a forgotten one`() {
        val base = newState().reviewed(ReviewType.CLOZE, ReviewRating.GOOD)

        val hard = progressor.apply(base, ReviewType.CLOZE, ReviewRating.HARD)
        val forgot = progressor.apply(base, ReviewType.CLOZE, ReviewRating.FORGOT)

        assertTrue(forgot.recognitionScore < hard.recognitionScore)
    }

    @Test
    fun `scores never leave the 0 to 100 range`() {
        val perfect = reviewTimes(RECOGNITION_TIMES, ReviewType.PRODUCTION, ReviewRating.EASY)
        val failing = (1..RECOGNITION_TIMES).fold(newState()) { state, index ->
            val reviewed = if (index == 1) state else state.reviewed(ReviewType.PRODUCTION, ReviewRating.EASY)
            progressor.apply(reviewed, ReviewType.PRODUCTION, ReviewRating.FORGOT)
        }

        assertTrue(perfect.productionScore >= 99)
        assertTrue(failing.productionScore in LearningState.SCORE_RANGE)
    }

    private fun newState(): LearningState = LearningState.new(EXPRESSION_ID)

    private fun reviewTimes(
        times: Int,
        reviewType: ReviewType,
        rating: ReviewRating,
    ): LearningState = (1..times).fold(newState()) { state, _ -> state.reviewed(reviewType, rating) }

    /** Etapa resultante **después** de cada revisión, en orden. */
    private fun stagesAfter(
        times: Int,
        reviewType: ReviewType,
        rating: ReviewRating,
    ): List<LearningStage> {
        val stages = mutableListOf<LearningStage>()
        var current = newState()
        repeat(times) {
            current = current.reviewed(reviewType, rating)
            stages += current.stage
        }
        return stages
    }

    /**
     * El scheduler es quien incrementa `reviewCount`; aquí se simula esa revisión
     * ya contabilizada para probar la progresión por sí sola.
     */
    private fun LearningState.reviewed(reviewType: ReviewType, rating: ReviewRating): LearningState =
        progressor.apply(copy(reviewCount = reviewCount + 1), reviewType, rating)

    private companion object {
        val EXPRESSION_ID = ExpressionId("figure-out")
        const val RECOGNITION_TIMES = 12
    }
}
