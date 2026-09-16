package com.nexvary.aviationnavigator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightPlanDraftTest {
    @Test
    fun normalizesAndBuildsSummary() {
        val plan = FlightPlanDraft(
            departure = " heca ",
            destination = "hesh",
            cruiseAltitudeFeet = 35_000,
            route = " dct "
        )

        assertTrue(plan.validate().isEmpty())
        assertEquals("HECA → HESH · DCT · FL350", plan.summary)
    }

    @Test
    fun rejectsBadIcaoAndAltitude() {
        val errors = FlightPlanDraft(
            departure = "CAI",
            destination = "CAI",
            cruiseAltitudeFeet = 70_000,
            route = ""
        ).validate()

        assertEquals(3, errors.size)
    }

    @Test
    fun rejectsSameValidAirport() {
        val errors = FlightPlanDraft(
            departure = "HECA",
            destination = "HECA",
            cruiseAltitudeFeet = 30_000
        ).validate()

        assertEquals(listOf("Departure and destination must be different"), errors)
    }
}
