package com.nexvary.aviationnavigator.domain

import com.nexvary.aviationnavigator.data.NavigationRepository
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class DirectRouteSolution(
    val departure: Airport,
    val destination: Airport,
    val distanceNauticalMiles: Double,
    val initialBearingDegrees: Double,
    val estimatedMinutes: Int? = null
)

enum class RouteFailure {
    UNKNOWN_DEPARTURE,
    UNKNOWN_DESTINATION,
    SAME_AIRPORT,
    INVALID_CRUISE_SPEED
}

sealed interface DirectRouteResult {
    data class Success(val solution: DirectRouteSolution) : DirectRouteResult
    data class Failure(val reason: RouteFailure) : DirectRouteResult
}

class RouteEngine(
    private val navigationRepository: NavigationRepository
) {
    fun direct(
        departureIcao: String,
        destinationIcao: String,
        cruiseSpeedKnots: Double? = null
    ): DirectRouteResult {
        val departure = navigationRepository.airportByIcao(departureIcao)
            ?: return DirectRouteResult.Failure(RouteFailure.UNKNOWN_DEPARTURE)
        val destination = navigationRepository.airportByIcao(destinationIcao)
            ?: return DirectRouteResult.Failure(RouteFailure.UNKNOWN_DESTINATION)

        if (departure.normalizedIcao == destination.normalizedIcao) {
            return DirectRouteResult.Failure(RouteFailure.SAME_AIRPORT)
        }
        if (cruiseSpeedKnots != null && (!cruiseSpeedKnots.isFinite() || cruiseSpeedKnots <= 0.0)) {
            return DirectRouteResult.Failure(RouteFailure.INVALID_CRUISE_SPEED)
        }

        val distance = greatCircleDistanceNm(
            departure.latitude,
            departure.longitude,
            destination.latitude,
            destination.longitude
        )
        val bearing = initialBearingDegrees(
            departure.latitude,
            departure.longitude,
            destination.latitude,
            destination.longitude
        )
        val estimatedMinutes = cruiseSpeedKnots?.let { speed ->
            ((distance / speed) * 60.0).toInt().coerceAtLeast(1)
        }

        return DirectRouteResult.Success(
            DirectRouteSolution(
                departure = departure,
                destination = destination,
                distanceNauticalMiles = distance,
                initialBearingDegrees = bearing,
                estimatedMinutes = estimatedMinutes
            )
        )
    }

    private fun greatCircleDistanceNm(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val deltaLat = Math.toRadians(latitude2 - latitude1)
        val deltaLon = Math.toRadians(longitude2 - longitude1)
        val a = sin(deltaLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).let { it * it }
        return 2.0 * EARTH_RADIUS_NM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private fun initialBearingDegrees(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val deltaLon = Math.toRadians(longitude2 - longitude1)
        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    private companion object {
        const val EARTH_RADIUS_NM = 3440.065
    }
}
