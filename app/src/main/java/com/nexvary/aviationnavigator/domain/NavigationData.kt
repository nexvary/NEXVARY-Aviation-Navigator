package com.nexvary.aviationnavigator.domain

data class Airport(
    val icao: String,
    val iata: String? = null,
    val name: String,
    val municipality: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val elevationFeet: Int? = null
) {
    val normalizedIcao: String = icao.trim().uppercase()
}

data class Runway(
    val airportIcao: String,
    val designator: String,
    val reciprocalDesignator: String? = null,
    val lengthFeet: Int,
    val widthFeet: Int? = null,
    val surface: String? = null,
    val headingDegrees: Double? = null
) {
    val normalizedAirportIcao: String = airportIcao.trim().uppercase()
    val normalizedDesignator: String = designator.trim().uppercase()

    init {
        require(lengthFeet > 0) { "runway length must be positive" }
        require(widthFeet == null || widthFeet > 0) { "runway width must be positive when supplied" }
        require(headingDegrees == null || (headingDegrees.isFinite() && headingDegrees in 0.0..360.0)) {
            "runway heading must be between 0 and 360 degrees"
        }
    }
}

enum class NavaidType {
    VOR,
    VOR_DME,
    DME,
    NDB
}

data class Navaid(
    val identifier: String,
    val name: String,
    val type: NavaidType,
    val latitude: Double,
    val longitude: Double,
    val frequency: String? = null,
    val countryCode: String? = null,
    val elevationFeet: Int? = null
) {
    val normalizedIdentifier: String = identifier.trim().uppercase()
}

enum class WaypointType {
    FIX,
    REPORTING_POINT
}

data class Waypoint(
    val identifier: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String? = null,
    val name: String? = null,
    val type: WaypointType = WaypointType.FIX
) {
    val normalizedIdentifier: String = identifier.trim().uppercase()
}

data class Airway(
    val name: String,
    val fixes: List<String>,
    val lowerAltitudeFeet: Int? = null,
    val upperAltitudeFeet: Int? = null
) {
    val normalizedName: String = name.trim().uppercase()
    val normalizedFixes: List<String> = fixes.map { it.trim().uppercase() }

    init {
        require(normalizedName.isNotBlank()) { "airway name must not be blank" }
        require(normalizedFixes.size >= 2) { "airway must contain at least two fixes" }
        require(normalizedFixes.none { it.isBlank() }) { "airway fixes must not be blank" }
        require(lowerAltitudeFeet == null || lowerAltitudeFeet >= 0) { "lower altitude must be non-negative" }
        require(upperAltitudeFeet == null || upperAltitudeFeet >= 0) { "upper altitude must be non-negative" }
        require(
            lowerAltitudeFeet == null || upperAltitudeFeet == null || lowerAltitudeFeet <= upperAltitudeFeet
        ) { "lower altitude must not exceed upper altitude" }
    }
}
