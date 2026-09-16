package com.nexvary.aviationnavigator.data

import com.nexvary.aviationnavigator.domain.AircraftTrack
import com.nexvary.aviationnavigator.domain.TrafficQuery
import com.nexvary.aviationnavigator.domain.TrafficSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos

class OpenSkyProvider(
    private val tokenProvider: suspend () -> String? = { null },
    private val baseUrl: String = "https://opensky-network.org/api"
) : LiveTrafficProvider {

    override val id: String = "opensky"
    override val displayName: String = "OpenSky Network"

    override suspend fun fetch(query: TrafficQuery): Result<List<AircraftTrack>> = withContext(Dispatchers.IO) {
        runCatching {
            val latDelta = query.radiusNm / 60.0
            val safeCos = cos(Math.toRadians(query.centerLatitude)).coerceAtLeast(0.15)
            val lonDelta = query.radiusNm / (60.0 * safeCos)

            val endpoint = buildString {
                append(baseUrl.trimEnd('/'))
                append("/states/all")
                append("?lamin=").append((query.centerLatitude - latDelta).coerceAtLeast(-90.0))
                append("&lomin=").append((query.centerLongitude - lonDelta).coerceAtLeast(-180.0))
                append("&lamax=").append((query.centerLatitude + latDelta).coerceAtMost(90.0))
                append("&lomax=").append((query.centerLongitude + lonDelta).coerceAtMost(180.0))
            }

            val token = tokenProvider()
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NEXVARY-Aviation-Navigator/0.1")
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            try {
                check(connection.responseCode in 200..299) {
                    "OpenSky HTTP ${connection.responseCode}"
                }

                val root = connection.inputStream.bufferedReader().use { it.readText() }
                val states = JSONObject(root).optJSONArray("states") ?: return@runCatching emptyList()

                buildList {
                    for (index in 0 until states.length()) {
                        val state = states.optJSONArray(index) ?: continue
                        val lon = state.optDoubleOrNull(5) ?: continue
                        val lat = state.optDoubleOrNull(6) ?: continue
                        val icao24 = state.optStringOrNull(0)?.lowercase() ?: continue

                        add(
                            AircraftTrack(
                                icao24 = icao24,
                                callsign = state.optStringOrNull(1),
                                latitude = lat,
                                longitude = lon,
                                barometricAltitudeMeters = state.optDoubleOrNull(7),
                                onGround = state.optBooleanOrFalse(8),
                                groundSpeedMetersPerSecond = state.optDoubleOrNull(9),
                                trackDegrees = state.optDoubleOrNull(10),
                                verticalRateMetersPerSecond = state.optDoubleOrNull(11),
                                geometricAltitudeMeters = state.optDoubleOrNull(13),
                                squawk = state.optStringOrNull(14),
                                lastSeenEpochSeconds = state.optLongOrNull(4),
                                source = TrafficSource.OPENSKY
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun JSONArray.optStringOrNull(index: Int): String? {
        if (index >= length() || isNull(index)) return null
        return optString(index).trim().takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun JSONArray.optDoubleOrNull(index: Int): Double? {
        if (index >= length() || isNull(index)) return null
        return when (val value = opt(index)) {
            is Number -> value.toDouble().takeIf { it.isFinite() }
            is String -> value.toDoubleOrNull()?.takeIf { it.isFinite() }
            else -> null
        }
    }

    private fun JSONArray.optLongOrNull(index: Int): Long? {
        if (index >= length() || isNull(index)) return null
        return when (val value = opt(index)) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun JSONArray.optBooleanOrFalse(index: Int): Boolean =
        index < length() && !isNull(index) && optBoolean(index, false)
}
