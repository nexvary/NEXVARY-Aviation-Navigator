package com.nexvary.aviationnavigator.domain

data class RouteProgressPoint(
    val identifier: String,
    val legDistanceNauticalMiles: Double,
    val cumulativeDistanceNauticalMiles: Double,
    val remainingDistanceNauticalMiles: Double,
    val inboundBearingDegrees: Double?
)

object RouteMetrics {
    fun progress(route: ResolvedRoute): List<RouteProgressPoint> {
        val total = route.totalDistanceNauticalMiles
        var cumulative = 0.0
        val points = mutableListOf(
            RouteProgressPoint(
                identifier = route.departure.normalizedIcao,
                legDistanceNauticalMiles = 0.0,
                cumulativeDistanceNauticalMiles = 0.0,
                remainingDistanceNauticalMiles = total,
                inboundBearingDegrees = null
            )
        )

        route.legs.forEach { leg ->
            cumulative += leg.distanceNauticalMiles
            points += RouteProgressPoint(
                identifier = leg.toIdentifier,
                legDistanceNauticalMiles = leg.distanceNauticalMiles,
                cumulativeDistanceNauticalMiles = cumulative,
                remainingDistanceNauticalMiles = (total - cumulative).coerceAtLeast(0.0),
                inboundBearingDegrees = leg.initialBearingDegrees
            )
        }
        return points
    }
}
