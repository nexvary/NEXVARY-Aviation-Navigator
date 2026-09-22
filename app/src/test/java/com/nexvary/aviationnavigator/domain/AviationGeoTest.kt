package com.nexvary.aviationnavigator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AviationGeoTest {

    @Test
    fun samePointHasZeroDistanceAndBearing() {
        val point = GeoPoint(30.0444, 31.2357)
        assertEquals(0.0, AviationGeo.greatCircleDistanceNm(point, point), 0.000001)
        assertEquals(0.0, AviationGeo.initialBearingDegrees(point, point), 0.000001)
    }

    @Test
    fun cairoToSharmMetricsAreStable() {
        val cairo = GeoPoint(30.121944, 31.405556)
        val sharm = GeoPoint(27.977286, 34.394950)

        val distance = AviationGeo.greatCircleDistanceNm(cairo, sharm)
        val bearing = AviationGeo.initialBearingDegrees(cairo, sharm)

        assertTrue(distance in 200.0..206.0)
        assertTrue(bearing in 126.0..131.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidLongitudeIsRejected() {
        GeoPoint(30.0, 181.0)
    }
}
