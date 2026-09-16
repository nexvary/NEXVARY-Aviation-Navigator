package com.nexvary.aviationnavigator.data

import com.nexvary.aviationnavigator.domain.AircraftTrack
import com.nexvary.aviationnavigator.domain.TrafficQuery
import com.nexvary.aviationnavigator.domain.TrafficSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TrafficRepository(
    private val providers: List<LiveTrafficProvider>
) {
    init {
        require(providers.isNotEmpty())
    }

    suspend fun fetchMerged(query: TrafficQuery): Result<List<AircraftTrack>> = coroutineScope {
        val results = providers
            .map { provider -> async { provider.id to provider.fetch(query) } }
            .map { it.await() }

        val successfulTracks = results.flatMap { (_, result) -> result.getOrDefault(emptyList()) }
        val failures = results.mapNotNull { (id, result) -> result.exceptionOrNull()?.let { id to it } }

        if (successfulTracks.isEmpty() && failures.isNotEmpty()) {
            return@coroutineScope Result.failure(
                IllegalStateException(
                    failures.joinToString(prefix = "All traffic providers failed: ") { (id, error) ->
                        "$id=${error.message ?: error::class.java.simpleName}"
                    }
                )
            )
        }

        Result.success(
            successfulTracks
                .groupBy { it.icao24.lowercase() }
                .values
                .map { candidates -> candidates.maxBy(::qualityScore) }
                .sortedWith(
                    compareByDescending<AircraftTrack> { it.altitudeFeet ?: Int.MIN_VALUE }
                        .thenBy { it.displayIdentity }
                )
        )
    }

    private fun qualityScore(track: AircraftTrack): Int {
        var score = when (track.source) {
            TrafficSource.ADSB_LOL -> 100
            TrafficSource.OPENSKY -> 90
            TrafficSource.UNKNOWN -> 0
        }

        if (!track.callsign.isNullOrBlank()) score += 8
        if (!track.registration.isNullOrBlank()) score += 6
        if (!track.aircraftType.isNullOrBlank()) score += 5
        if (track.geometricAltitudeMeters != null) score += 4
        if (track.barometricAltitudeMeters != null) score += 3
        if (track.groundSpeedMetersPerSecond != null) score += 3
        if (track.trackDegrees != null) score += 3
        if (track.verticalRateMetersPerSecond != null) score += 2
        if (!track.squawk.isNullOrBlank()) score += 1

        return score
    }
}
