package com.nexvary.aviationnavigator.data

import com.nexvary.aviationnavigator.domain.Airport
import com.nexvary.aviationnavigator.domain.AirportDistance
import com.nexvary.aviationnavigator.domain.AviationGeo
import com.nexvary.aviationnavigator.domain.GeoPoint
import com.nexvary.aviationnavigator.domain.geoPoint

interface NavigationRepository {
    fun airports(): List<Airport>
    fun airportByIcao(icao: String): Airport?
    fun searchAirports(query: String, limit: Int = 20): List<Airport>
    fun nearestAirports(
        latitude: Double,
        longitude: Double,
        limit: Int = 10,
        maxDistanceNauticalMiles: Double? = null
    ): List<AirportDistance>
}

class InMemoryNavigationRepository(
    airports: List<Airport>
) : NavigationRepository {
    private val airportList = airports
        .distinctBy { it.normalizedIcao }
        .sortedBy { it.normalizedIcao }

    private val byIcao = airportList.associateBy { it.normalizedIcao }

    override fun airports(): List<Airport> = airportList

    override fun airportByIcao(icao: String): Airport? =
        byIcao[icao.trim().uppercase()]

    override fun searchAirports(query: String, limit: Int): List<Airport> {
        require(limit > 0) { "limit must be positive" }
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return airportList.take(limit)

        return airportList
            .asSequence()
            .filter { airport ->
                airport.normalizedIcao.lowercase().contains(needle) ||
                    airport.iata?.lowercase()?.contains(needle) == true ||
                    airport.name.lowercase().contains(needle) ||
                    airport.municipality.lowercase().contains(needle) ||
                    airport.countryCode.lowercase().contains(needle)
            }
            .sortedWith(
                compareBy<Airport> { airport ->
                    when {
                        airport.normalizedIcao.equals(query.trim(), ignoreCase = true) -> 0
                        airport.iata?.equals(query.trim(), ignoreCase = true) == true -> 1
                        else -> 2
                    }
                }.thenBy { it.normalizedIcao }
            )
            .take(limit)
            .toList()
    }

    override fun nearestAirports(
        latitude: Double,
        longitude: Double,
        limit: Int,
        maxDistanceNauticalMiles: Double?
    ): List<AirportDistance> {
        require(limit > 0) { "limit must be positive" }
        if (maxDistanceNauticalMiles != null) {
            require(maxDistanceNauticalMiles.isFinite() && maxDistanceNauticalMiles >= 0.0) {
                "maxDistanceNauticalMiles must be finite and non-negative"
            }
        }

        val origin = GeoPoint(latitude, longitude)
        return airportList
            .asSequence()
            .map { airport ->
                val point = airport.geoPoint()
                AirportDistance(
                    airport = airport,
                    distanceNauticalMiles = AviationGeo.greatCircleDistanceNm(origin, point),
                    initialBearingDegrees = AviationGeo.initialBearingDegrees(origin, point)
                )
            }
            .filter { result ->
                maxDistanceNauticalMiles == null || result.distanceNauticalMiles <= maxDistanceNauticalMiles
            }
            .sortedWith(compareBy<AirportDistance> { it.distanceNauticalMiles }.thenBy { it.airport.normalizedIcao })
            .take(limit)
            .toList()
    }
}
