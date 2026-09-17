package com.nexvary.aviationnavigator.data

import com.nexvary.aviationnavigator.domain.Airport
import com.nexvary.aviationnavigator.domain.Airway
import com.nexvary.aviationnavigator.domain.Navaid
import com.nexvary.aviationnavigator.domain.NavaidType
import com.nexvary.aviationnavigator.domain.Runway
import com.nexvary.aviationnavigator.domain.Waypoint

object NavigationFixture {
    val airports = listOf(
        Airport("TEST", "TST", "Test Departure", "Alpha", "ZZ", 30.0, 31.0, 100),
        Airport("TEND", "END", "Test Destination", "Bravo", "ZZ", 31.0, 35.0, 200)
    )

    val runways = listOf(
        Runway("TEST", "09", "27", 10_000, 150, "ASPHALT", 90.0),
        Runway("TEND", "18", "36", 8_000, 140, "ASPHALT", 180.0)
    )

    val navaids = listOf(
        Navaid("NV1", "Test VOR", NavaidType.VOR_DME, 30.2, 31.5, "113.00", "ZZ"),
        Navaid("NV2", "Test NDB", NavaidType.NDB, 30.8, 34.5, "350", "ZZ")
    )

    val waypoints = listOf(
        Waypoint("FIXA", 30.3, 32.0, "ZZ"),
        Waypoint("FIXB", 30.5, 33.0, "ZZ"),
        Waypoint("FIXC", 30.7, 34.0, "ZZ")
    )

    val airways = listOf(
        Airway("J1", listOf("FIXA", "FIXB", "FIXC"), lowerAltitudeFeet = 5_000, upperAltitudeFeet = 45_000)
    )

    fun repository(): NavigationRepository = InMemoryNavigationRepository(
        airports = airports,
        runways = runways,
        navaids = navaids,
        waypoints = waypoints,
        airways = airways
    )
}
