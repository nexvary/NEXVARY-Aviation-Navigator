package com.nexvary.aviationnavigator.domain

import kotlin.math.roundToInt

enum class TrafficSource {
    ADSB_LOL,
    OPENSKY,
    UNKNOWN
}

data class AircraftTrack(
    val icao24: String,
    val callsign: String? = null,
    val registration: String? = null,
    val aircraftType: String? = null,
    val latitude: Double,
    val longitude: Double,
    val barometricAltitudeMeters: Double? = null,
    val geometricAltitudeMeters: Double? = null,
    val groundSpeedMetersPerSecond: Double? = null,
    val trackDegrees: Double? = null,
    val verticalRateMetersPerSecond: Double? = null,
    val squawk: String? = null,
    val onGround: Boolean = false,
    val lastSeenEpochSeconds: Long? = null,
    val source: TrafficSource = TrafficSource.UNKNOWN
) {
    val altitudeFeet: Int?
        get() = (geometricAltitudeMeters ?: barometricAltitudeMeters)
            ?.times(METERS_TO_FEET)
            ?.roundToInt()

    val groundSpeedKnots: Int?
        get() = groundSpeedMetersPerSecond
            ?.times(METERS_PER_SECOND_TO_KNOTS)
            ?.roundToInt()

    val verticalRateFeetPerMinute: Int?
        get() = verticalRateMetersPerSecond
            ?.times(METERS_PER_SECOND_TO_FEET_PER_MINUTE)
            ?.roundToInt()

    val displayIdentity: String
        get() = callsign?.trim()?.takeIf { it.isNotEmpty() }
            ?: registration?.takeIf { it.isNotBlank() }
            ?: icao24.uppercase()

    companion object {
        private const val METERS_TO_FEET = 3.280839895
        private const val METERS_PER_SECOND_TO_KNOTS = 1.943844492
        private const val METERS_PER_SECOND_TO_FEET_PER_MINUTE = 196.850394
    }
}

data class TrafficQuery(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusNm: Int
) {
    init {
        require(centerLatitude in -90.0..90.0)
        require(centerLongitude in -180.0..180.0)
        require(radiusNm in 1..250)
    }
}
