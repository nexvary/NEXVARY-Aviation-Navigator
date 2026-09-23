package com.nexvary.aviationnavigator

import android.graphics.Color
import com.nexvary.aviationnavigator.domain.AircraftTrack
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val AIRCRAFT_SOURCE_ID = "live-aircraft-source"
private const val AIRCRAFT_LAYER_ID = "live-aircraft-layer"
private const val AIRCRAFT_LABEL_LAYER_ID = "live-aircraft-label-layer"

internal fun installAircraftLayer(style: Style, tracks: List<AircraftTrack>) {
    if (style.getSource(AIRCRAFT_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(AIRCRAFT_SOURCE_ID, aircraftGeoJson(tracks)))
    }

    if (style.getLayer(AIRCRAFT_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(AIRCRAFT_LAYER_ID, AIRCRAFT_SOURCE_ID).withProperties(
                PropertyFactory.circleColor(Color.parseColor("#D4AF37")),
                PropertyFactory.circleRadius(5.5f),
                PropertyFactory.circleStrokeColor(Color.parseColor("#111317")),
                PropertyFactory.circleStrokeWidth(1.5f),
                PropertyFactory.circleOpacity(0.94f)
            )
        )
    }
    if (style.getLayer(AIRCRAFT_LABEL_LAYER_ID) == null) {
        style.addLayer(
            SymbolLayer(AIRCRAFT_LABEL_LAYER_ID, AIRCRAFT_SOURCE_ID).withProperties(
                PropertyFactory.textField("{identity}  ✈"),
                PropertyFactory.textSize(11f),
                PropertyFactory.textColor(Color.parseColor("#F4F7FB")),
                PropertyFactory.textHaloColor(Color.parseColor("#07111D")),
                PropertyFactory.textHaloWidth(1.5f),
                PropertyFactory.textOffset(arrayOf(0f, 1.4f)),
                PropertyFactory.textAllowOverlap(false)
            )
        )
    }
}

internal fun updateAircraftLayer(map: MapLibreMap, tracks: List<AircraftTrack>) {
    val style = map.style ?: return
    val source = style.getSourceAs<GeoJsonSource>(AIRCRAFT_SOURCE_ID) ?: return
    source.setGeoJson(aircraftGeoJson(tracks))
}

private fun aircraftGeoJson(tracks: List<AircraftTrack>): String {
    val features = JSONArray()

    tracks.forEach { track ->
        val properties = JSONObject()
            .put("icao24", track.icao24)
            .put("identity", track.displayIdentity)
            .put("altitude_ft", track.altitudeFeet ?: JSONObject.NULL)
            .put("speed_kt", track.groundSpeedKnots ?: JSONObject.NULL)
            .put("ground", track.onGround)
            .put("track", track.trackDegrees ?: 0.0)

        val coordinates = JSONArray()
            .put(track.longitude)
            .put(track.latitude)

        val geometry = JSONObject()
            .put("type", "Point")
            .put("coordinates", coordinates)

        features.put(
            JSONObject()
                .put("type", "Feature")
                .put("properties", properties)
                .put("geometry", geometry)
        )
    }

    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)
        .toString()
}
