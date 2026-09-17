package com.nexvary.aviationnavigator.domain

import kotlin.math.roundToInt

data class AircraftPerformanceProfile(
    val name: String,
    val cruiseSpeedKnots: Double,
    val fuelBurnKgPerHour: Double,
    val reserveMinutes: Int = 45
) {
    init {
        require(name.isNotBlank()) { "profile name must not be blank" }
        require(cruiseSpeedKnots.isFinite() && cruiseSpeedKnots > 0.0) { "cruise speed must be positive" }
        require(fuelBurnKgPerHour.isFinite() && fuelBurnKgPerHour >= 0.0) { "fuel burn must be non-negative" }
        require(reserveMinutes >= 0) { "reserve minutes must be non-negative" }
    }
}

data class FlightPerformanceEstimate(
    val airborneMinutes: Int,
    val tripFuelKg: Double,
    val reserveFuelKg: Double,
    val totalFuelKg: Double,
    val averageGroundSpeedKnots: Double
)

object FlightEstimateEngine {
    fun estimate(route: ResolvedRoute, profile: AircraftPerformanceProfile): FlightPerformanceEstimate {
        val airborneHours = route.totalDistanceNauticalMiles / profile.cruiseSpeedKnots
        val airborneMinutes = (airborneHours * 60.0).roundToInt().coerceAtLeast(1)
        val tripFuel = airborneHours * profile.fuelBurnKgPerHour
        val reserveFuel = (profile.reserveMinutes / 60.0) * profile.fuelBurnKgPerHour
        return FlightPerformanceEstimate(
            airborneMinutes = airborneMinutes,
            tripFuelKg = tripFuel,
            reserveFuelKg = reserveFuel,
            totalFuelKg = tripFuel + reserveFuel,
            averageGroundSpeedKnots = profile.cruiseSpeedKnots
        )
    }
}
