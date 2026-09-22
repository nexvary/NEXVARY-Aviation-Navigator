package com.nexvary.aviationnavigator.domain

import com.nexvary.aviationnavigator.data.NavigationRepository

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

        val departurePoint = departure.geoPoint()
        val destinationPoint = destination.geoPoint()
        val distance = AviationGeo.greatCircleDistanceNm(departurePoint, destinationPoint)
        val bearing = AviationGeo.initialBearingDegrees(departurePoint, destinationPoint)
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
}
