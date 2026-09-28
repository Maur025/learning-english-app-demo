package com.example.learning.english.learning.english.app.domain.engine

import com.example.learning.english.learning.english.app.domain.exercise.DefaultExerciseGenerator
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionPlan
import com.example.learning.english.learning.english.app.domain.model.SessionReason
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySessionPlannerTest {

    private val planner = DailySessionPlanner(DefaultExerciseGenerator())

    private val preferences = UserPreferences(
        onboardingCompleted = true,
        dailyGoalMinutes = 30,
        newExpressionsPerDay = 2,
    )

    @Test
    fun `an empty catalog produces an empty plan`() {
        val plan = planner.plan(emptyList(), emptyList(), preferences, NOW)

        assertTrue(plan.isEmpty)
    }

    @Test
    fun `a fresh learner gets new expressions ordered from easy to hard`() {
        val plan = planner.plan(EngineFixtures.all, emptyList(), preferences.copy(newExpressionsPerDay = 6), NOW)

        assertEquals(
            listOf("deadline", "code-review", "figure-out", "run-into", "ship-it", "put-off"),
            plan.steps.map { it.expressionId.value },
        )
        assertEquals(List(6) { SessionReason.NEW }, plan.steps.map { it.reason })
    }

    @Test
    fun `new expressions are capped by the daily preference`() {
        val plan = planner.plan(EngineFixtures.all, emptyList(), preferences, NOW)

        assertEquals(2, plan.stepsOf(SessionReason.NEW).size)
    }

    @Test
    fun `a new expression costs the introduction price`() {
        val plan = planner.plan(EngineFixtures.all, emptyList(), preferences, NOW)

        assertEquals(
            List(2) { DailySessionPlanner.NEW_EXPRESSION_SECONDS },
            plan.steps.map { it.estimatedSeconds },
        )
    }

    @Test
    fun `no new content is configured means no new content`() {
        val plan = planner.plan(
            EngineFixtures.all,
            emptyList(),
            preferences.copy(newExpressionsPerDay = 0),
            NOW,
        )

        assertTrue(plan.isEmpty)
    }

    @Test
    fun `a backlog of due reviews blocks new content`() {
        val catalog = EngineFixtures.expressions(count = 25)

        val plan = planner.plan(catalog, dueStates(DailySessionPlanner.NEW_BLOCKED_DUE_COUNT), preferences, NOW)

        assertTrue(plan.stepsOf(SessionReason.NEW).isEmpty())
        assertEquals(DailySessionPlanner.NEW_BLOCKED_DUE_COUNT, plan.steps.size)
    }

    @Test
    fun `a moderate backlog halves the new content`() {
        val catalog = EngineFixtures.expressions(count = 25)

        val plan = planner.plan(catalog, dueStates(DailySessionPlanner.NEW_REDUCED_DUE_COUNT), preferences, NOW)

        assertEquals(DailySessionPlanner.NEW_REDUCED_DUE_COUNT, plan.stepsOf(SessionReason.OVERDUE_REVIEW).size)
        assertEquals(1, plan.stepsOf(SessionReason.NEW).size)
    }

    @Test
    fun `due reviews come first, most overdue first`() {
        val states = listOf(
            EngineFixtures.state("run-into", LearningStage.SEEN, nextReviewAt = NOW - 5_000, recognitionScore = 40),
            EngineFixtures.state("figure-out", LearningStage.SEEN, nextReviewAt = NOW - 50_000, recognitionScore = 40),
        )

        val plan = planner.plan(EngineFixtures.all, states, preferences, NOW)

        assertEquals(
            listOf("figure-out", "run-into"),
            plan.steps.filter { it.reason == SessionReason.OVERDUE_REVIEW }.map { it.expressionId.value },
        )
    }

    @Test
    fun `expressions scheduled for the future are not due`() {
        val states = listOf(
            EngineFixtures.state("figure-out", LearningStage.SEEN, nextReviewAt = NOW + 60_000),
        )

        val plan = planner.plan(EngineFixtures.all, states, preferences, NOW)

        assertTrue(plan.steps.none { it.expressionId.value == "figure-out" })
    }

    @Test
    fun `an expression is practiced at most once per session`() {
        val states = listOf(
            EngineFixtures.state("figure-out", LearningStage.RECALLABLE, nextReviewAt = NOW + 1_000, productionScore = 10),
            EngineFixtures.state("run-into", LearningStage.SEEN, nextReviewAt = NOW + 1_000, failedReviewCount = 2),
        )

        val plan = planner.plan(EngineFixtures.all, states, preferences, NOW)

        assertEquals(plan.steps.size, plan.steps.map { it.expressionId }.distinct().size)
    }

    @Test
    fun `the ladder picks the retrieval level that matches the stage`() {
        val states = listOf(
            EngineFixtures.state("figure-out", LearningStage.SEEN, nextReviewAt = NOW - 1_000, recognitionScore = 40),
            EngineFixtures.state("run-into", LearningStage.RECALLABLE, nextReviewAt = NOW - 1_000, productionScore = 40),
            EngineFixtures.state("put-off", LearningStage.PRODUCTIVE, nextReviewAt = NOW - 1_000, productionScore = 80),
        )

        val plan = planner.plan(EngineFixtures.all, states, preferences, NOW)

        assertEquals(ReviewType.CLOZE, plan.typeOf("figure-out"))
        assertEquals(ReviewType.TRANSLATION, plan.typeOf("run-into"))
        assertEquals(ReviewType.PRODUCTION, plan.typeOf("put-off"))
    }

    @Test
    fun `expressions that were forgotten get guided recall`() {
        val states = listOf(
            EngineFixtures.state("figure-out", LearningStage.SEEN, nextReviewAt = NOW + 1_000, failedReviewCount = 1),
        )

        val plan = planner.plan(EngineFixtures.all, states, preferences, NOW)

        assertEquals(SessionReason.WEAK, plan.reasonOf("figure-out"))
        assertEquals(ReviewType.GUIDED_RECALL, plan.typeOf("figure-out"))
    }

    @Test
    fun `a production weak expression is added even when it is not due`() {
        val states = listOf(
            EngineFixtures.state(
                expressionId = "figure-out",
                stage = LearningStage.RECALLABLE,
                nextReviewAt = NOW + 1_000,
                recognitionScore = 80,
                productionScore = 20,
                reviewCount = 6,
            ),
        )

        val plan = planner.plan(EngineFixtures.all, states, preferences, NOW)

        assertEquals(SessionReason.PRODUCTION_RECALL, plan.reasonOf("figure-out"))
        assertEquals(ReviewType.PRODUCTION, plan.typeOf("figure-out"))
    }

    @Test
    fun `a production deficit shifts recognition steps to production`() {
        val states = listOf(
            EngineFixtures.state(
                expressionId = "figure-out",
                stage = LearningStage.RECOGNIZED,
                nextReviewAt = NOW - 1_000,
                recognitionScore = 80,
                productionScore = 10,
                reviewCount = 4,
            ),
        )

        val plan = planner.plan(EngineFixtures.all, states, preferences, NOW)

        assertEquals(ReviewType.TRANSLATION, plan.typeOf("figure-out"))
    }

    @Test
    fun `without a deficit the stage keeps its own level`() {
        val states = listOf(
            EngineFixtures.state(
                expressionId = "figure-out",
                stage = LearningStage.RECOGNIZED,
                nextReviewAt = NOW - 1_000,
                recognitionScore = 80,
                productionScore = 70,
                reviewCount = 4,
            ),
        )

        val plan = planner.plan(EngineFixtures.all, states, preferences, NOW)

        assertEquals(ReviewType.GUIDED_RECALL, plan.typeOf("figure-out"))
    }

    @Test
    fun `only the preferred packs are practiced`() {
        val plan = planner.plan(
            EngineFixtures.all,
            emptyList(),
            preferences.copy(preferredPackIds = setOf(PackId(EngineFixtures.DEVELOPER_PACK))),
            NOW,
        )

        assertEquals(setOf("ship-it", "code-review"), plan.steps.map { it.expressionId.value }.toSet())
    }

    @Test
    fun `a step the content cannot support falls back to another type`() {
        val plan = planner.plan(listOf(EngineFixtures.deadline), emptyList(), preferences, NOW)

        assertEquals(ReviewType.GUIDED_RECALL, plan.typeOf("deadline"))
    }

    @Test
    fun `the session never exceeds the daily duration`() {
        val plan = planner.plan(
            EngineFixtures.expressions(count = 40),
            emptyList(),
            preferences.copy(dailyGoalMinutes = 2, newExpressionsPerDay = 40),
            NOW,
        )

        assertTrue(plan.estimatedSeconds <= 2 * DailySessionPlanner.SECONDS_PER_MINUTE)
        assertEquals(1, plan.steps.size)
    }

    @Test
    fun `the plan has a hard cap on steps`() {
        val catalog = EngineFixtures.expressions(count = 70)
        val states = (1..70).map { index ->
            EngineFixtures.state("bulk-$index", LearningStage.SEEN, nextReviewAt = NOW - 1_000)
        }

        val plan = planner.plan(catalog, states, preferences, NOW)

        assertEquals(DailySessionPlanner.MAX_STEPS, plan.steps.size)
        assertTrue(plan.estimatedSeconds <= preferences.dailyGoalMinutes * DailySessionPlanner.SECONDS_PER_MINUTE)
    }

    @Test
    fun `the same inputs always produce the same plan`() {
        val states = listOf(
            EngineFixtures.state("figure-out", LearningStage.SEEN, nextReviewAt = NOW - 1_000),
        )

        val first = planner.plan(EngineFixtures.all, states, preferences, NOW)
        val second = planner.plan(EngineFixtures.all.shuffled(), states.shuffled(), preferences, NOW)

        assertEquals(first, second)
    }

    private fun dueStates(count: Int): List<LearningState> = (1..count).map { index ->
        EngineFixtures.state(
            expressionId = "bulk-$index",
            stage = LearningStage.SEEN,
            nextReviewAt = NOW - index,
            recognitionScore = 30,
        )
    }
    private fun SessionPlan.typeOf(expressionId: String): ReviewType? =
        steps.singleOrNull { it.expressionId.value == expressionId }?.reviewType

    private fun SessionPlan.reasonOf(expressionId: String): SessionReason? =
        steps.singleOrNull { it.expressionId.value == expressionId }?.reason

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
