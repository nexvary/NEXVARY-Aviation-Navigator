package com.nexvary.aviationnavigator

import com.nexvary.aviationnavigator.data.DefaultNavigationRepository
import com.nexvary.aviationnavigator.domain.RoutePlanResult
import com.nexvary.aviationnavigator.domain.RoutePlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationReleaseContractTest {
    @Test
    fun bundledDefaultKeepsOperationalNavDatasetsEmptyUntilReviewedSourceIsConnected() {
        val repository = DefaultNavigationRepository.instance
        assertTrue(repository.runways().isEmpty())
        assertTrue(repository.navaids().isEmpty())
        assertTrue(repository.waypoints().isEmpty())
        assertTrue(repository.airways().isEmpty())
        assertTrue(repository.airports().isNotEmpty())
    }

    @Test
    fun bundledAirportSeedCanProduceDirectPlanningGeometry() {
        val result = RoutePlanner(DefaultNavigationRepository.instance)
            .resolve("HECA", "HESH", "DCT", 430.0)
        val success = result as RoutePlanResult.Success
        assertEquals("HECA", success.route.departure.normalizedIcao)
        assertEquals("HESH", success.route.destination.normalizedIcao)
        assertEquals(1, success.route.legs.size)
        assertTrue(success.route.totalDistanceNauticalMiles > 0.0)
    }
}
