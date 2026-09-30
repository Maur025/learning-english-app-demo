package com.example.learning.english.learning.english.app.data.repository

import com.example.learning.english.learning.english.app.data.persistence.AppDatabase
import com.example.learning.english.learning.english.app.data.persistence.Fixtures
import com.example.learning.english.learning.english.app.data.persistence.createInMemoryDatabase
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.SkillAverages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Agregados que alimentan los contadores de Home (README §43.2).
 *
 * El caso interesante no es el SQL sino los bordes: sin packs seleccionados no
 * hay nada que contar, y sin revisiones no hay porcentaje que inventar.
 */
@RunWith(RobolectricTestRunner::class)
class RoomLearningStateRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomLearningStateRepository

    @Before
    fun setUp() = runBlocking {
        database = createInMemoryDatabase()
        repository = RoomLearningStateRepository(database.learningStateDao())
        database.contentPackDao().upsert(Fixtures.pack)
        database.expressionDao().upsertExpressionRow(Fixtures.expressionEntity())
        database.expressionDao().upsertExpressionRow(
            Fixtures.expressionEntity(id = "run-into", phrase = "run into"),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `counts states due at the given moment`() = runBlocking {
        val now = 10_000L
        repository.upsert(Fixtures.domainLearningState(nextReviewAt = now - 1))
        repository.upsert(
            Fixtures.domainLearningState(expressionId = "run-into").copy(nextReviewAt = now + 1),
        )

        assertEquals(1, repository.observeDueCount(now, setOf(PackId(Fixtures.PACK_ID))).first())
    }

    @Test
    fun `counts nothing when no pack is selected`() = runBlocking {
        repository.upsert(Fixtures.domainLearningState(nextReviewAt = 1_000L))

        assertEquals(0, repository.observeDueCount(10_000L, emptySet()).first())
    }

    @Test
    fun `rounds the average scores into percentages`() = runBlocking {
        repository.upsert(
            Fixtures.domainLearningState().copy(recognitionScore = 80, productionScore = 33, reviewCount = 2),
        )

        val averages = repository.observeSkillAverages(setOf(PackId(Fixtures.PACK_ID))).first()

        assertEquals(80, averages.recognitionPercent)
        assertEquals(33, averages.productionPercent)
        assertEquals(true, averages.hasData)
    }

    @Test
    fun `reports no averages before the first review`() = runBlocking {
        repository.upsert(Fixtures.domainLearningState())

        val averages = repository.observeSkillAverages(setOf(PackId(Fixtures.PACK_ID))).first()

        assertEquals(SkillAverages.EMPTY, averages)
        assertEquals(false, averages.hasData)
    }

    @Test
    fun `reports no averages when no pack is selected`() = runBlocking {
        repository.upsert(Fixtures.domainLearningState().copy(reviewCount = 2))

        assertEquals(SkillAverages.EMPTY, repository.observeSkillAverages(emptySet()).first())
    }

    @Test
    fun `keeps scores inside the domain range`() = runBlocking {
        repository.upsert(
            Fixtures.domainLearningState().copy(
                recognitionScore = LearningState.SCORE_MAX,
                productionScore = LearningState.SCORE_MIN,
                reviewCount = 1,
            ),
        )

        val averages = repository.observeSkillAverages(setOf(PackId(Fixtures.PACK_ID))).first()

        assertEquals(100, averages.recognitionPercent)
        assertEquals(0, averages.productionPercent)
    }
}
