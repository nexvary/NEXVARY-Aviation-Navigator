package com.nexvary.aviationnavigator.domain

import com.nexvary.aviationnavigator.data.NavigationFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePlannerTest {
    private val planner = RoutePlanner(NavigationFixture.repository())

    @Test
    fun dctBuildsSingleLegToDestination() {
        val result = planner.resolve("TEST", "TEND", "DCT", 400.0)
        val success = result as RoutePlanResult.Success
        assertEquals(1, success.route.legs.size)
        assertEquals(RouteLegType.DIRECT, success.route.legs.single().type)
        assertEquals("TEND", success.route.legs.single().toIdentifier)
        assertTrue(success.route.totalDistanceNauticalMiles > 0.0)
        assertTrue((success.route.estimatedMinutes ?: 0) > 0)
    }

    @Test
    fun routeViaFixesBuildsDirectSegments() {
        val result = planner.resolve("TEST", "TEND", "FIXA FIXC")
        val success = result as RoutePlanResult.Success
        assertEquals(listOf("FIXA", "FIXC", "TEND"), success.route.legs.map { it.toIdentifier })
        assertTrue(success.route.legs.all { it.type == RouteLegType.DIRECT })
    }

    @Test
    fun airwayExpandsIntermediateFixes() {
        val result = planner.resolve("TEST", "TEND", "FIXA J1 FIXC")
        val success = result as RoutePlanResult.Success
        assertEquals(listOf("FIXA", "FIXB", "FIXC", "TEND"), success.route.legs.map { it.toIdentifier })
        assertEquals(RouteLegType.DIRECT, success.route.legs[0].type)
        assertEquals(RouteLegType.AIRWAY, success.route.legs[1].type)
        assertEquals("J1", success.route.legs[1].airwayName)
        assertEquals(RouteLegType.AIRWAY, success.route.legs[2].type)
    }

    @Test
    fun airwaySupportsReverseTraversal() {
        val result = planner.resolve("TEST", "TEND", "FIXC J1 FIXA")
        val success = result as RoutePlanResult.Success
        assertEquals(listOf("FIXC", "FIXB", "FIXA", "TEND"), success.route.legs.map { it.toIdentifier })
    }

    @Test
    fun unknownFixFailsWithToken() {
        val result = planner.resolve("TEST", "TEND", "MISSING")
        val failure = result as RoutePlanResult.Failure
        assertEquals(RoutePlanFailure.UNKNOWN_FIX, failure.reason)
        assertEquals("MISSING", failure.token)
    }

    @Test
    fun unknownAirwayLikeTokenFailsAsAirway() {
        val result = planner.resolve("TEST", "TEND", "FIXA J99 FIXC")
        val failure = result as RoutePlanResult.Failure
        assertEquals(RoutePlanFailure.UNKNOWN_AIRWAY, failure.reason)
        assertEquals("J99", failure.token)
    }

    @Test
    fun airwayMustStartFromMemberFix() {
        val result = planner.resolve("TEST", "TEND", "NV1 J1 FIXC")
        val failure = result as RoutePlanResult.Failure
        assertEquals(RoutePlanFailure.INVALID_AIRWAY_ENTRY, failure.reason)
    }

    @Test
    fun trailingAirwayFailsWithoutExit() {
        val result = planner.resolve("TEST", "TEND", "FIXA J1")
        val failure = result as RoutePlanResult.Failure
        assertEquals(RoutePlanFailure.INVALID_AIRWAY_EXIT, failure.reason)
    }

    @Test
    fun invalidCruiseSpeedIsRejected() {
        val result = planner.resolve("TEST", "TEND", "DCT", 0.0)
        val failure = result as RoutePlanResult.Failure
        assertEquals(RoutePlanFailure.INVALID_CRUISE_SPEED, failure.reason)
    }
}
