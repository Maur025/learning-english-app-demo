package com.example.learning.english.learning.english.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedPackIdsTest {

    @Test
    fun `uses every installed pack when nothing is selected`() {
        val preferences = UserPreferences()

        assertEquals(INSTALLED, preferences.selectedPackIds(INSTALLED))
    }

    @Test
    fun `keeps only the selected packs`() {
        val preferences = UserPreferences(preferredPackIds = setOf(PackId("developer-english")))

        assertEquals(setOf(PackId("developer-english")), preferences.selectedPackIds(INSTALLED))
    }

    @Test
    fun `ignores selections that are no longer installed`() {
        val preferences = UserPreferences(
            preferredPackIds = setOf(PackId("removed-pack")),
        )

        assertEquals(INSTALLED, preferences.selectedPackIds(INSTALLED))
    }

    @Test
    fun `returns nothing when there is no installed content`() {
        val preferences = UserPreferences(preferredPackIds = setOf(PackId("core-english")))

        assertEquals(emptySet<PackId>(), preferences.selectedPackIds(emptySet()))
    }

    private companion object {
        val INSTALLED = setOf(PackId("core-english"), PackId("developer-english"))
    }
}
