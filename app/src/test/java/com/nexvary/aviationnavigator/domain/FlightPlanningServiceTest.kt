package com.nexvary.aviationnavigator.domain

import com.nexvary.aviationnavigator.data.NavigationFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightPlanningServiceTest {
    private val profile = AircraftPerformanceProfile(
        name = "Test Jet",
        cruiseSpeedKnots = 420.0,
        fuelBurnKgPerHour = 2_400.0,
        reserveMinutes = 45
    )

    @Test
    fun prepareReturnsResolvedRoutePerformanceProgressAndGeoJson() {
        val service = FlightPlanningService(NavigationFixture.repository())
        val result = service.prepare(
            FlightPlanDraft(
                departure = "test",
                destination = "tend",
                cruiseAltitudeFeet = 35_000,
                route = "FIXA J1 FIXC"
            ),
            profile
        )

        val success = result as FlightPlanningResult.Success
        assertEquals("TEST", success.plan.draft.departure)
        assertEquals("TEND", success.plan.draft.destination)
        assertEquals(4, success.plan.route.legs.size)
        assertEquals(success.plan.route.legs.size + 1, success.plan.progress.size)
        assertEquals(0.0, success.plan.progress.last().remainingDistanceNauticalMiles, 0.001)
        assertTrue(success.plan.performance.airborneMinutes > 0)
        assertTrue(success.plan.performance.totalFuelKg > success.plan.performance.tripFuelKg)
        assertTrue(success.plan.routeGeoJson.contains("FeatureCollection"))
    }

    @Test
    fun invalidDraftStopsBeforeRouteResolution() {
        val service = FlightPlanningService(NavigationFixture.repository())
        val result = service.prepare(
            FlightPlanDraft("BAD", "TEND", 500, "DCT"),
            profile
        )
        val failure = result as FlightPlanningResult.DraftValidationFailure
        assertTrue(failure.errors.isNotEmpty())
    }

    @Test
    fun unresolvedRouteIsReturnedAsStructuredFailure() {
        val service = FlightPlanningService(NavigationFixture.repository())
        val result = service.prepare(
            FlightPlanDraft("TEST", "TEND", 35_000, "MISSING"),
            profile
        )
        val failure = result as FlightPlanningResult.RouteResolutionFailure
        assertEquals(RoutePlanFailure.UNKNOWN_FIX, failure.reason)
        assertEquals("MISSING", failure.token)
    }

    @Test
    fun performanceEstimateAddsReserveFuel() {
        val route = (RoutePlanner(NavigationFixture.repository())
            .resolve("TEST", "TEND", "DCT") as RoutePlanResult.Success).route
        val estimate = FlightEstimateEngine.estimate(route, profile)
        assertTrue(estimate.tripFuelKg > 0.0)
        assertTrue(estimate.reserveFuelKg > 0.0)
        assertEquals(estimate.tripFuelKg + estimate.reserveFuelKg, estimate.totalFuelKg, 0.0001)
    }
}
