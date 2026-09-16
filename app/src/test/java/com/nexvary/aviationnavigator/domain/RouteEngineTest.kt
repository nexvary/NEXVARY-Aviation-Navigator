package com.nexvary.aviationnavigator.domain

import com.nexvary.aviationnavigator.data.EgyptNavigationSeed
import com.nexvary.aviationnavigator.data.InMemoryNavigationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteEngineTest {

    private val engine = RouteEngine(InMemoryNavigationRepository(EgyptNavigationSeed.airports))

    @Test
    fun directRouteResolvesAirportsAndComputesGreatCircleMetrics() {
        val result = engine.direct("HECA", "HESH", cruiseSpeedKnots = 450.0)
        assertTrue(result is DirectRouteResult.Success)

        val solution = (result as DirectRouteResult.Success).solution
        assertEquals("HECA", solution.departure.normalizedIcao)
        assertEquals("HESH", solution.destination.normalizedIcao)
        assertTrue(solution.distanceNauticalMiles in 200.0..206.0)
        assertTrue(solution.initialBearingDegrees in 126.0..131.0)
        assertTrue(solution.estimatedMinutes in 26..28)
    }

    @Test
    fun directRouteRejectsUnknownAndDuplicateAirports() {
        assertEquals(
            RouteFailure.UNKNOWN_DEPARTURE,
            (engine.direct("ZZZZ", "HESH") as DirectRouteResult.Failure).reason
        )
        assertEquals(
            RouteFailure.UNKNOWN_DESTINATION,
            (engine.direct("HECA", "ZZZZ") as DirectRouteResult.Failure).reason
        )
        assertEquals(
            RouteFailure.SAME_AIRPORT,
            (engine.direct("HECA", "heca") as DirectRouteResult.Failure).reason
        )
    }

    @Test
    fun directRouteRejectsInvalidCruiseSpeed() {
        assertEquals(
            RouteFailure.INVALID_CRUISE_SPEED,
            (engine.direct("HECA", "HESH", 0.0) as DirectRouteResult.Failure).reason
        )
    }
}
