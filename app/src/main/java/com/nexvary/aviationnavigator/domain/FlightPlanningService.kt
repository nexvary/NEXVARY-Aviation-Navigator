package com.nexvary.aviationnavigator.domain

import com.nexvary.aviationnavigator.data.NavigationRepository

data class PreparedFlightPlan(
    val draft: FlightPlanDraft,
    val route: ResolvedRoute,
    val performance: FlightPerformanceEstimate,
    val progress: List<RouteProgressPoint>,
    val routeGeoJson: String
)

sealed interface FlightPlanningResult {
    data class Success(val plan: PreparedFlightPlan) : FlightPlanningResult
    data class DraftValidationFailure(val errors: List<String>) : FlightPlanningResult
    data class RouteResolutionFailure(
        val reason: RoutePlanFailure,
        val token: String? = null
    ) : FlightPlanningResult
}

class FlightPlanningService(
    navigationRepository: NavigationRepository
) {
    private val routePlanner = RoutePlanner(navigationRepository)

    fun prepare(
        draft: FlightPlanDraft,
        profile: AircraftPerformanceProfile
    ): FlightPlanningResult {
        val normalized = draft.normalized()
        val errors = normalized.validate()
        if (errors.isNotEmpty()) {
            return FlightPlanningResult.DraftValidationFailure(errors)
        }

        return when (
            val routeResult = routePlanner.resolve(
                departureIcao = normalized.departure,
                destinationIcao = normalized.destination,
                routeText = normalized.route,
                cruiseSpeedKnots = profile.cruiseSpeedKnots
            )
        ) {
            is RoutePlanResult.Failure -> FlightPlanningResult.RouteResolutionFailure(
                reason = routeResult.reason,
                token = routeResult.token
            )
            is RoutePlanResult.Success -> {
                val route = routeResult.route
                FlightPlanningResult.Success(
                    PreparedFlightPlan(
                        draft = normalized,
                        route = route,
                        performance = FlightEstimateEngine.estimate(route, profile),
                        progress = RouteMetrics.progress(route),
                        routeGeoJson = RouteGeoJson.featureCollection(route)
                    )
                )
            }
        }
    }
}
