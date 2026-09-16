package com.nexvary.aviationnavigator.data

import com.nexvary.aviationnavigator.domain.Airport
import com.nexvary.aviationnavigator.domain.AirportDistance
import com.nexvary.aviationnavigator.domain.Airway
import com.nexvary.aviationnavigator.domain.AviationGeo
import com.nexvary.aviationnavigator.domain.GeoPoint
import com.nexvary.aviationnavigator.domain.Navaid
import com.nexvary.aviationnavigator.domain.NavaidDistance
import com.nexvary.aviationnavigator.domain.Runway
import com.nexvary.aviationnavigator.domain.Waypoint
import com.nexvary.aviationnavigator.domain.WaypointDistance
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

    fun runways(): List<Runway>
    fun runwaysForAirport(icao: String): List<Runway>

    fun navaids(): List<Navaid>
    fun navaidByIdentifier(identifier: String): Navaid?
    fun searchNavaids(query: String, limit: Int = 20): List<Navaid>
    fun nearestNavaids(
        latitude: Double,
        longitude: Double,
        limit: Int = 10,
        maxDistanceNauticalMiles: Double? = null
    ): List<NavaidDistance>

    fun waypoints(): List<Waypoint>
    fun waypointByIdentifier(identifier: String): Waypoint?
    fun searchWaypoints(query: String, limit: Int = 20): List<Waypoint>
    fun nearestWaypoints(
        latitude: Double,
        longitude: Double,
        limit: Int = 10,
        maxDistanceNauticalMiles: Double? = null
    ): List<WaypointDistance>

    fun airways(): List<Airway>
    fun airwayByName(name: String): Airway?
    fun searchAirways(query: String, limit: Int = 20): List<Airway>

    fun resolveFix(identifier: String): GeoPoint?
}

class InMemoryNavigationRepository(
    airports: List<Airport>,
    runways: List<Runway> = emptyList(),
    navaids: List<Navaid> = emptyList(),
    waypoints: List<Waypoint> = emptyList(),
    airways: List<Airway> = emptyList()
) : NavigationRepository {
    private val airportList = airports
        .distinctBy { it.normalizedIcao }
        .sortedBy { it.normalizedIcao }
    private val runwayList = runways
        .distinctBy { "${it.normalizedAirportIcao}:${it.normalizedDesignator}" }
        .sortedWith(compareBy<Runway> { it.normalizedAirportIcao }.thenBy { it.normalizedDesignator })
    private val navaidList = navaids
        .distinctBy { it.normalizedIdentifier }
        .sortedBy { it.normalizedIdentifier }
    private val waypointList = waypoints
        .distinctBy { it.normalizedIdentifier }
        .sortedBy { it.normalizedIdentifier }
    private val airwayList = airways
        .distinctBy { it.normalizedName }
        .sortedBy { it.normalizedName }

    private val byIcao = airportList.associateBy { it.normalizedIcao }
    private val navaidById = navaidList.associateBy { it.normalizedIdentifier }
    private val waypointById = waypointList.associateBy { it.normalizedIdentifier }
    private val airwayById = airwayList.associateBy { it.normalizedName }

    override fun airports(): List<Airport> = airportList

    override fun airportByIcao(icao: String): Airport? =
        byIcao[icao.trim().uppercase()]

    override fun searchAirports(query: String, limit: Int): List<Airport> {
        requirePositiveLimit(limit)
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
        requirePositiveLimit(limit)
        validateMaxDistance(maxDistanceNauticalMiles)
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
            .filter { withinDistance(it.distanceNauticalMiles, maxDistanceNauticalMiles) }
            .sortedWith(compareBy<AirportDistance> { it.distanceNauticalMiles }.thenBy { it.airport.normalizedIcao })
            .take(limit)
            .toList()
    }

    override fun runways(): List<Runway> = runwayList

    override fun runwaysForAirport(icao: String): List<Runway> {
        val normalized = icao.trim().uppercase()
        return runwayList.filter { it.normalizedAirportIcao == normalized }
    }

    override fun navaids(): List<Navaid> = navaidList

    override fun navaidByIdentifier(identifier: String): Navaid? =
        navaidById[identifier.trim().uppercase()]

    override fun searchNavaids(query: String, limit: Int): List<Navaid> {
        requirePositiveLimit(limit)
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return navaidList.take(limit)
        return navaidList
            .asSequence()
            .filter {
                it.normalizedIdentifier.lowercase().contains(needle) ||
                    it.name.lowercase().contains(needle) ||
                    it.type.name.lowercase().contains(needle) ||
                    it.frequency?.lowercase()?.contains(needle) == true
            }
            .sortedWith(compareBy<Navaid> { if (it.normalizedIdentifier.equals(query.trim(), true)) 0 else 1 }.thenBy { it.normalizedIdentifier })
            .take(limit)
            .toList()
    }

    override fun nearestNavaids(
        latitude: Double,
        longitude: Double,
        limit: Int,
        maxDistanceNauticalMiles: Double?
    ): List<NavaidDistance> {
        requirePositiveLimit(limit)
        validateMaxDistance(maxDistanceNauticalMiles)
        val origin = GeoPoint(latitude, longitude)
        return navaidList
            .asSequence()
            .map { navaid ->
                val point = navaid.geoPoint()
                NavaidDistance(
                    navaid = navaid,
                    distanceNauticalMiles = AviationGeo.greatCircleDistanceNm(origin, point),
                    initialBearingDegrees = AviationGeo.initialBearingDegrees(origin, point)
                )
            }
            .filter { withinDistance(it.distanceNauticalMiles, maxDistanceNauticalMiles) }
            .sortedWith(compareBy<NavaidDistance> { it.distanceNauticalMiles }.thenBy { it.navaid.normalizedIdentifier })
            .take(limit)
            .toList()
    }

    override fun waypoints(): List<Waypoint> = waypointList

    override fun waypointByIdentifier(identifier: String): Waypoint? =
        waypointById[identifier.trim().uppercase()]

    override fun searchWaypoints(query: String, limit: Int): List<Waypoint> {
        requirePositiveLimit(limit)
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return waypointList.take(limit)
        return waypointList
            .asSequence()
            .filter {
                it.normalizedIdentifier.lowercase().contains(needle) ||
                    it.name?.lowercase()?.contains(needle) == true ||
                    it.countryCode?.lowercase()?.contains(needle) == true
            }
            .sortedWith(compareBy<Waypoint> { if (it.normalizedIdentifier.equals(query.trim(), true)) 0 else 1 }.thenBy { it.normalizedIdentifier })
            .take(limit)
            .toList()
    }

    override fun nearestWaypoints(
        latitude: Double,
        longitude: Double,
        limit: Int,
        maxDistanceNauticalMiles: Double?
    ): List<WaypointDistance> {
        requirePositiveLimit(limit)
        validateMaxDistance(maxDistanceNauticalMiles)
        val origin = GeoPoint(latitude, longitude)
        return waypointList
            .asSequence()
            .map { waypoint ->
                val point = waypoint.geoPoint()
                WaypointDistance(
                    waypoint = waypoint,
                    distanceNauticalMiles = AviationGeo.greatCircleDistanceNm(origin, point),
                    initialBearingDegrees = AviationGeo.initialBearingDegrees(origin, point)
                )
            }
            .filter { withinDistance(it.distanceNauticalMiles, maxDistanceNauticalMiles) }
            .sortedWith(compareBy<WaypointDistance> { it.distanceNauticalMiles }.thenBy { it.waypoint.normalizedIdentifier })
            .take(limit)
            .toList()
    }

    override fun airways(): List<Airway> = airwayList

    override fun airwayByName(name: String): Airway? = airwayById[name.trim().uppercase()]

    override fun searchAirways(query: String, limit: Int): List<Airway> {
        requirePositiveLimit(limit)
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return airwayList.take(limit)
        return airwayList
            .asSequence()
            .filter { airway ->
                airway.normalizedName.lowercase().contains(needle) ||
                    airway.normalizedFixes.any { it.lowercase().contains(needle) }
            }
            .sortedWith(compareBy<Airway> { if (it.normalizedName.equals(query.trim(), true)) 0 else 1 }.thenBy { it.normalizedName })
            .take(limit)
            .toList()
    }

    override fun resolveFix(identifier: String): GeoPoint? {
        val normalized = identifier.trim().uppercase()
        return waypointById[normalized]?.geoPoint()
            ?: navaidById[normalized]?.geoPoint()
            ?: byIcao[normalized]?.geoPoint()
    }

    private fun requirePositiveLimit(limit: Int) {
        require(limit > 0) { "limit must be positive" }
    }

    private fun validateMaxDistance(value: Double?) {
        if (value != null) {
            require(value.isFinite() && value >= 0.0) {
                "maxDistanceNauticalMiles must be finite and non-negative"
            }
        }
    }

    private fun withinDistance(distance: Double, maxDistance: Double?): Boolean =
        maxDistance == null || distance <= maxDistance
}
