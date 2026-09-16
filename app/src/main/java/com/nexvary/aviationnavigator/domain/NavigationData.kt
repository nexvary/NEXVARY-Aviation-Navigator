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
    val frequency: String? = null
)
