package com.nexvary.aviationnavigator.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedNavigationRepositoryTest {
    private val repository = NavigationFixture.repository()

    @Test
    fun runwaysAreGroupedByAirport() {
        val runways = repository.runwaysForAirport(" test ")
        assertEquals(1, runways.size)
        assertEquals("09", runways.single().normalizedDesignator)
    }

    @Test
    fun navaidLookupAndSearchAreCaseInsensitive() {
        assertEquals("NV1", repository.navaidByIdentifier("nv1")?.normalizedIdentifier)
        assertEquals("NV1", repository.searchNavaids("vor").first().normalizedIdentifier)
    }

    @Test
    fun nearestNavaidsAreDistanceSorted() {
        val results = repository.nearestNavaids(30.0, 31.0, limit = 2)
        assertEquals(2, results.size)
        assertEquals("NV1", results.first().navaid.normalizedIdentifier)
        assertTrue(results[0].distanceNauticalMiles <= results[1].distanceNauticalMiles)
    }

    @Test
    fun waypointLookupSearchAndNearestWork() {
        assertEquals("FIXB", repository.waypointByIdentifier("fixb")?.normalizedIdentifier)
        assertEquals("FIXC", repository.searchWaypoints("fixc").single().normalizedIdentifier)
        val nearest = repository.nearestWaypoints(30.0, 31.0, limit = 1)
        assertEquals("FIXA", nearest.single().waypoint.normalizedIdentifier)
    }

    @Test
    fun airwayLookupAndSearchWork() {
        val airway = repository.airwayByName("j1")
        assertNotNull(airway)
        assertEquals(listOf("FIXA", "FIXB", "FIXC"), airway?.normalizedFixes)
        assertEquals("J1", repository.searchAirways("fixb").single().normalizedName)
    }

    @Test
    fun resolveFixSupportsAirportWaypointAndNavaid() {
        assertNotNull(repository.resolveFix("TEST"))
        assertNotNull(repository.resolveFix("FIXA"))
        assertNotNull(repository.resolveFix("NV1"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nearestWaypointRejectsNegativeDistance() {
        repository.nearestWaypoints(30.0, 31.0, maxDistanceNauticalMiles = -1.0)
    }
}
