package com.nexvary.aviationnavigator.data

import com.nexvary.aviationnavigator.domain.AircraftTrack
import com.nexvary.aviationnavigator.domain.TrafficQuery
import com.nexvary.aviationnavigator.domain.TrafficSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AdsbLolProvider(
    private val baseUrl: String = "https://api.adsb.lol"
) : LiveTrafficProvider {

    override val id: String = "adsb_lol"
    override val displayName: String = "ADSB.lol"

    override suspend fun fetch(query: TrafficQuery): Result<List<AircraftTrack>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = buildString {
                append(baseUrl.trimEnd('/'))
                append("/v2/lat/")
                append(query.centerLatitude)
                append("/lon/")
                append(query.centerLongitude)
                append("/dist/")
                append(query.radiusNm)
            }

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NEXVARY-Aviation-Navigator/0.3")
            }

            try {
                check(connection.responseCode in 200..299) {
                    "ADSB.lol HTTP ${connection.responseCode}"
                }

                val root = connection.inputStream.bufferedReader().use { it.readText() }
                val aircraft = JSONObject(root).optJSONArray("ac") ?: return@runCatching emptyList()

                buildList {
                    for (index in 0 until aircraft.length()) {
                        val item = aircraft.optJSONObject(index) ?: continue
                        val lat = item.optFiniteDouble("lat") ?: continue
                        val lon = item.optFiniteDouble("lon") ?: continue
                        val hex = item.optString("hex").trim().removePrefix("~")
                        if (hex.isBlank()) continue

                        add(
                            AircraftTrack(
                                icao24 = hex.lowercase(),
                                callsign = item.optNullableString("flight"),
                                registration = item.optNullableString("r"),
                                aircraftType = item.optNullableString("t"),
                                latitude = lat,
                                longitude = lon,
                                barometricAltitudeMeters = item.optAltitudeFeet("alt_baro")?.times(FEET_TO_METERS),
                                geometricAltitudeMeters = item.optAltitudeFeet("alt_geom")?.times(FEET_TO_METERS),
                                groundSpeedMetersPerSecond = item.optFiniteDouble("gs")?.times(KNOTS_TO_METERS_PER_SECOND),
                                trackDegrees = item.optFiniteDouble("track"),
                                verticalRateMetersPerSecond = item.optFiniteDouble("baro_rate")?.times(FEET_PER_MINUTE_TO_METERS_PER_SECOND),
                                squawk = item.optNullableString("squawk"),
                                onGround = item.optString("alt_baro").equals("ground", ignoreCase = true),
                                lastSeenEpochSeconds = null,
                                source = TrafficSource.ADSB_LOL
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }

    private fun JSONObject.optFiniteDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toDouble().takeIf { it.isFinite() }
            is String -> value.toDoubleOrNull()?.takeIf { it.isFinite() }
            else -> null
        }
    }

    private fun JSONObject.optAltitudeFeet(key: String): Double? =
        optFiniteDouble(key)

    companion object {
        private const val FEET_TO_METERS = 0.3048
        private const val KNOTS_TO_METERS_PER_SECOND = 0.514444444
        private const val FEET_PER_MINUTE_TO_METERS_PER_SECOND = 0.00508
    }
}
