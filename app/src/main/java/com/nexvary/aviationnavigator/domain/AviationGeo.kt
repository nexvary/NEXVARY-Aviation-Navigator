package com.nexvary.aviationnavigator.domain

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "latitude must be between -90 and 90" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "longitude must be between -180 and 180" }
    }
}

data class AirportDistance(
    val airport: Airport,
    val distanceNauticalMiles: Double,
    val initialBearingDegrees: Double
)

data class NavaidDistance(
    val navaid: Navaid,
    val distanceNauticalMiles: Double,
    val initialBearingDegrees: Double
)

data class WaypointDistance(
    val waypoint: Waypoint,
    val distanceNauticalMiles: Double,
    val initialBearingDegrees: Double
)

object AviationGeo {
    fun greatCircleDistanceNm(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(deltaLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).let { it * it }
        return 2.0 * EARTH_RADIUS_NM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun initialBearingDegrees(from: GeoPoint, to: GeoPoint): Double {
        if (from == to) return 0.0
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    private const val EARTH_RADIUS_NM = 3440.065
}

fun Airport.geoPoint(): GeoPoint = GeoPoint(latitude, longitude)
fun Navaid.geoPoint(): GeoPoint = GeoPoint(latitude, longitude)
fun Waypoint.geoPoint(): GeoPoint = GeoPoint(latitude, longitude)
