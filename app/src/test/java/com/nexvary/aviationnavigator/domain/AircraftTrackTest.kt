package com.nexvary.aviationnavigator.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AircraftTrackTest {
    @Test
    fun convertsAviationUnitsForPresentation() {
        val track = AircraftTrack(
            icao24 = "ABC123",
            callsign = "MSR742 ",
            latitude = 30.0,
            longitude = 31.0,
            geometricAltitudeMeters = 10_000.0,
            groundSpeedMetersPerSecond = 230.0,
            verticalRateMetersPerSecond = -5.0
        )

        assertEquals("MSR742", track.displayIdentity)
        assertEquals(32_808, track.altitudeFeet)
        assertEquals(447, track.groundSpeedKnots)
        assertEquals(-984, track.verticalRateFeetPerMinute)
    }

    @Test
    fun fallsBackToIcaoIdentity() {
        val track = AircraftTrack(
            icao24 = "ab12cd",
            latitude = 0.0,
            longitude = 0.0
        )

        assertEquals("AB12CD", track.displayIdentity)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedTrafficRadius() {
        TrafficQuery(
            centerLatitude = 30.0,
            centerLongitude = 31.0,
            radiusNm = 500
        )
    }
}
