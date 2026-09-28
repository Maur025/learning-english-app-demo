package com.example.learning.english.learning.english.app.domain.scheduler

import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2ReviewSchedulerTest {

    private val scheduler = Sm2ReviewScheduler()
    private val reviewedAt = 1_000_000L

    @Test
    fun `first GOOD schedules two days ahead and updates counters`() {
        val state = LearningState.new(EXPRESSION_ID)

        val scheduled = scheduler.schedule(state, ReviewRating.GOOD, reviewedAt)

        assertEquals(2, scheduled.currentIntervalDays)
        assertEquals(reviewedAt + 2 * Sm2ReviewScheduler.MILLIS_PER_DAY, scheduled.nextReviewAt)
        assertEquals(reviewedAt, scheduled.lastReviewedAt)
        assertEquals(reviewedAt, scheduled.updatedAt)
        assertEquals(1, scheduled.reviewCount)
        assertEquals(1, scheduled.successfulReviewCount)
        assertEquals(0, scheduled.failedReviewCount)
        assertEquals(LearningState.DEFAULT_EASE_FACTOR, scheduled.easeFactor, 0.0)
    }

    @Test
    fun `first review intervals grow with harder to easier ratings`() {
        val hard = scheduler.schedule(LearningState.new(EXPRESSION_ID), ReviewRating.HARD, reviewedAt)
        val good = scheduler.schedule(LearningState.new(EXPRESSION_ID), ReviewRating.GOOD, reviewedAt)
        val easy = scheduler.schedule(LearningState.new(EXPRESSION_ID), ReviewRating.EASY, reviewedAt)

        assertEquals(1, hard.currentIntervalDays)
        assertEquals(2, good.currentIntervalDays)
        assertEquals(4, easy.currentIntervalDays)
        assertTrue(hard.currentIntervalDays < good.currentIntervalDays)
        assertTrue(good.currentIntervalDays < easy.currentIntervalDays)
    }

    @Test
    fun `FORGOT on first review leaves the expression due immediately`() {
        val scheduled = scheduler.schedule(LearningState.new(EXPRESSION_ID), ReviewRating.FORGOT, reviewedAt)

        assertEquals(0, scheduled.currentIntervalDays)
        assertEquals(reviewedAt, scheduled.nextReviewAt)
        assertEquals(1, scheduled.reviewCount)
        assertEquals(0, scheduled.successfulReviewCount)
        assertEquals(1, scheduled.failedReviewCount)
        assertEquals(LearningState.DEFAULT_EASE_FACTOR + Sm2ReviewScheduler.EASE_DELTA_FORGOT, scheduled.easeFactor, 0.0)
    }

    @Test
    fun `good reviews lengthen the interval progressively`() {
        var state = scheduler.schedule(LearningState.new(EXPRESSION_ID), ReviewRating.GOOD, reviewedAt)
        assertEquals(2, state.currentIntervalDays)

        state = scheduler.schedule(state, ReviewRating.GOOD, reviewedAt + 1)
        assertEquals(5, state.currentIntervalDays)

        state = scheduler.schedule(state, ReviewRating.GOOD, reviewedAt + 2)
        assertEquals(13, state.currentIntervalDays)

        state = scheduler.schedule(state, ReviewRating.GOOD, reviewedAt + 3)
        assertEquals(33, state.currentIntervalDays)
    }

    @Test
    fun `FORGOT after success resets the interval and the next success restarts small`() {
        var state = scheduler.schedule(LearningState.new(EXPRESSION_ID), ReviewRating.GOOD, reviewedAt)
        assertEquals(2, state.currentIntervalDays)

        state = scheduler.schedule(state, ReviewRating.FORGOT, reviewedAt + 1)
        assertEquals(0, state.currentIntervalDays)
        assertEquals(reviewedAt + 1, state.nextReviewAt)
        assertEquals(1, state.successfulReviewCount)
        assertEquals(1, state.failedReviewCount)

        state = scheduler.schedule(state, ReviewRating.GOOD, reviewedAt + 2)
        assertEquals(1, state.currentIntervalDays)
    }

    @Test
    fun `repeated FORGOT never drops the ease factor below the floor`() {
        var state = LearningState.new(EXPRESSION_ID)

        repeat(7) { state = scheduler.schedule(state, ReviewRating.FORGOT, reviewedAt + it) }

        assertEquals(Sm2ReviewScheduler.MIN_EASE_FACTOR, state.easeFactor, 0.0)
    }

    @Test
    fun `ease factor reacts to each rating`() {
        val base = stateReviewedOnce(ReviewRating.GOOD)

        assertEquals(LearningState.DEFAULT_EASE_FACTOR, base.easeFactor, 0.0)
        assertEquals(
            LearningState.DEFAULT_EASE_FACTOR + Sm2ReviewScheduler.EASE_DELTA_HARD,
            scheduler.schedule(base, ReviewRating.HARD, reviewedAt).easeFactor,
            0.0,
        )
        assertEquals(LearningState.DEFAULT_EASE_FACTOR, scheduler.schedule(base, ReviewRating.GOOD, reviewedAt).easeFactor, 0.0)
        assertEquals(
            LearningState.DEFAULT_EASE_FACTOR + Sm2ReviewScheduler.EASE_DELTA_EASY,
            scheduler.schedule(base, ReviewRating.EASY, reviewedAt).easeFactor,
            0.0,
        )
    }

    @Test
    fun `interval never exceeds the maximum`() {
        val longInterval = stateReviewedOnce(ReviewRating.GOOD).copy(currentIntervalDays = 200)

        val good = scheduler.schedule(longInterval, ReviewRating.GOOD, reviewedAt)
        val easy = scheduler.schedule(longInterval, ReviewRating.EASY, reviewedAt)

        assertEquals(Sm2ReviewScheduler.MAX_INTERVAL_DAYS, good.currentIntervalDays)
        assertEquals(Sm2ReviewScheduler.MAX_INTERVAL_DAYS, easy.currentIntervalDays)
    }

    @Test
    fun `stage and scores are not touched by the scheduler`() {
        val state = LearningState.new(EXPRESSION_ID).copy(
            stage = LearningStage.RECOGNIZED,
            recognitionScore = 80,
            productionScore = 40,
        )

        val scheduled = scheduler.schedule(state, ReviewRating.EASY, reviewedAt)

        assertEquals(LearningStage.RECOGNIZED, scheduled.stage)
        assertEquals(80, scheduled.recognitionScore)
        assertEquals(40, scheduled.productionScore)
    }

    @Test
    fun `scheduling is deterministic`() {
        val input = stateReviewedOnce(ReviewRating.EASY)

        val first = scheduler.schedule(input, ReviewRating.GOOD, reviewedAt)
        val second = scheduler.schedule(input, ReviewRating.GOOD, reviewedAt)

        assertEquals(first, second)
    }

    private fun stateReviewedOnce(rating: ReviewRating): LearningState =
        scheduler.schedule(LearningState.new(EXPRESSION_ID), rating, reviewedAt)

    private companion object {
        val EXPRESSION_ID = ExpressionId("figure-out")
    }
}