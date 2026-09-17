package com.nexvary.aviationnavigator.domain

import com.nexvary.aviationnavigator.data.NavigationRepository
import kotlin.math.roundToInt

enum class RouteLegType {
    DIRECT,
    AIRWAY
}

data class RouteLeg(
    val fromIdentifier: String,
    val toIdentifier: String,
    val from: GeoPoint,
    val to: GeoPoint,
    val type: RouteLegType,
    val airwayName: String? = null,
    val distanceNauticalMiles: Double,
    val initialBearingDegrees: Double
)

data class ResolvedRoute(
    val departure: Airport,
    val destination: Airport,
    val legs: List<RouteLeg>,
    val totalDistanceNauticalMiles: Double,
    val estimatedMinutes: Int? = null,
    val routeTokens: List<String>
)

enum class RoutePlanFailure {
    UNKNOWN_DEPARTURE,
    UNKNOWN_DESTINATION,
    UNKNOWN_FIX,
    UNKNOWN_AIRWAY,
    INVALID_AIRWAY_ENTRY,
    INVALID_AIRWAY_EXIT,
    EMPTY_ROUTE,
    INVALID_CRUISE_SPEED
}

sealed interface RoutePlanResult {
    data class Success(val route: ResolvedRoute) : RoutePlanResult
    data class Failure(
        val reason: RoutePlanFailure,
        val token: String? = null
    ) : RoutePlanResult
}

object RouteSyntax {
    fun tokenize(route: String): List<String> = route
        .trim()
        .uppercase()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
}

class RoutePlanner(
    private val navigationRepository: NavigationRepository
) {
    fun resolve(
        departureIcao: String,
        destinationIcao: String,
        routeText: String,
        cruiseSpeedKnots: Double? = null
    ): RoutePlanResult {
        val departure = navigationRepository.airportByIcao(departureIcao)
            ?: return RoutePlanResult.Failure(RoutePlanFailure.UNKNOWN_DEPARTURE, departureIcao.trim().uppercase())
        val destination = navigationRepository.airportByIcao(destinationIcao)
            ?: return RoutePlanResult.Failure(RoutePlanFailure.UNKNOWN_DESTINATION, destinationIcao.trim().uppercase())

        if (cruiseSpeedKnots != null && (!cruiseSpeedKnots.isFinite() || cruiseSpeedKnots <= 0.0)) {
            return RoutePlanResult.Failure(RoutePlanFailure.INVALID_CRUISE_SPEED)
        }

        val tokens = RouteSyntax.tokenize(routeText.ifBlank { "DCT" })
        if (tokens.isEmpty()) {
            return RoutePlanResult.Failure(RoutePlanFailure.EMPTY_ROUTE)
        }

        val legs = mutableListOf<RouteLeg>()
        var currentIdentifier = departure.normalizedIcao
        var currentPoint = departure.geoPoint()
        var pendingAirway: Airway? = null

        fun appendLeg(
            toIdentifier: String,
            toPoint: GeoPoint,
            type: RouteLegType,
            airwayName: String? = null
        ) {
            val distance = AviationGeo.greatCircleDistanceNm(currentPoint, toPoint)
            legs += RouteLeg(
                fromIdentifier = currentIdentifier,
                toIdentifier = toIdentifier,
                from = currentPoint,
                to = toPoint,
                type = type,
                airwayName = airwayName,
                distanceNauticalMiles = distance,
                initialBearingDegrees = AviationGeo.initialBearingDegrees(currentPoint, toPoint)
            )
            currentIdentifier = toIdentifier
            currentPoint = toPoint
        }

        for (token in tokens) {
            if (token == "DCT") {
                pendingAirway = null
                continue
            }

            val airway = navigationRepository.airwayByName(token)
            if (airway != null) {
                pendingAirway = airway
                continue
            }

            val fixPoint = navigationRepository.resolveFix(token)
            if (fixPoint == null) {
                return if (looksLikeAirway(token)) {
                    RoutePlanResult.Failure(RoutePlanFailure.UNKNOWN_AIRWAY, token)
                } else {
                    RoutePlanResult.Failure(RoutePlanFailure.UNKNOWN_FIX, token)
                }
            }

            val activeAirway = pendingAirway
            if (activeAirway == null) {
                if (token != currentIdentifier) {
                    appendLeg(token, fixPoint, RouteLegType.DIRECT)
                }
                continue
            }

            val entryIndex = activeAirway.normalizedFixes.indexOf(currentIdentifier)
            if (entryIndex < 0) {
                return RoutePlanResult.Failure(
                    RoutePlanFailure.INVALID_AIRWAY_ENTRY,
                    "${activeAirway.normalizedName}:$currentIdentifier"
                )
            }
            val exitIndex = activeAirway.normalizedFixes.indexOf(token)
            if (exitIndex < 0 || exitIndex == entryIndex) {
                return RoutePlanResult.Failure(
                    RoutePlanFailure.INVALID_AIRWAY_EXIT,
                    "${activeAirway.normalizedName}:$token"
                )
            }

            val step = if (exitIndex > entryIndex) 1 else -1
            var index = entryIndex + step
            while (true) {
                val airwayFix = activeAirway.normalizedFixes[index]
                val airwayPoint = navigationRepository.resolveFix(airwayFix)
                    ?: return RoutePlanResult.Failure(RoutePlanFailure.UNKNOWN_FIX, airwayFix)
                appendLeg(
                    toIdentifier = airwayFix,
                    toPoint = airwayPoint,
                    type = RouteLegType.AIRWAY,
                    airwayName = activeAirway.normalizedName
                )
                if (index == exitIndex) break
                index += step
            }
            pendingAirway = null
        }

        if (pendingAirway != null) {
            return RoutePlanResult.Failure(RoutePlanFailure.INVALID_AIRWAY_EXIT, pendingAirway.normalizedName)
        }

        if (currentIdentifier != destination.normalizedIcao) {
            appendLeg(destination.normalizedIcao, destination.geoPoint(), RouteLegType.DIRECT)
        }

        val totalDistance = legs.sumOf { it.distanceNauticalMiles }
        val estimatedMinutes = cruiseSpeedKnots?.let { speed ->
            ((totalDistance / speed) * 60.0).roundToInt().coerceAtLeast(1)
        }

        return RoutePlanResult.Success(
            ResolvedRoute(
                departure = departure,
                destination = destination,
                legs = legs,
                totalDistanceNauticalMiles = totalDistance,
                estimatedMinutes = estimatedMinutes,
                routeTokens = tokens
            )
        )
    }

    private fun looksLikeAirway(token: String): Boolean =
        token.length in 2..6 && token.any(Char::isDigit) && token.any(Char::isLetter)
}
