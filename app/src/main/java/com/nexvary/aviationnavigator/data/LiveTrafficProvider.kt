package com.nexvary.aviationnavigator.data

import com.nexvary.aviationnavigator.domain.AircraftTrack
import com.nexvary.aviationnavigator.domain.TrafficQuery

interface LiveTrafficProvider {
    val id: String
    val displayName: String

    suspend fun fetch(query: TrafficQuery): Result<List<AircraftTrack>>
}
