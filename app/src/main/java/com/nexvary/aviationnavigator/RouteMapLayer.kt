package com.nexvary.aviationnavigator

import android.graphics.Color
import com.nexvary.aviationnavigator.domain.PreparedFlightPlan
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val ROUTE_SOURCE_ID = "planned-route-source"
private const val ROUTE_CASING_LAYER_ID = "planned-route-casing"
private const val ROUTE_LAYER_ID = "planned-route-line"
private const val AIRWAY_SOURCE_ID = "planned-airway-source"
private const val AIRWAY_LAYER_ID = "planned-airway-line"
private const val FIX_SOURCE_ID = "planned-route-fix-source"
private const val FIX_LAYER_ID = "planned-route-fix-layer"

private const val EMPTY_FEATURE_COLLECTION = "{\"type\":\"FeatureCollection\",\"features\":[]}"

internal fun installRouteLayers(style: Style, plan: PreparedFlightPlan?) {
    val route = routeLineGeoJson(plan)
    val airways = airwayLineGeoJson(plan)
    val fixes = routeFixGeoJson(plan)

    if (style.getSource(ROUTE_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, route))
    }
    if (style.getSource(AIRWAY_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(AIRWAY_SOURCE_ID, airways))
    }
    if (style.getSource(FIX_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(FIX_SOURCE_ID, fixes))
    }

    if (style.getLayer(ROUTE_CASING_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(ROUTE_CASING_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(Color.parseColor("#D6A84B")),
                PropertyFactory.lineWidth(7.0f),
                PropertyFactory.lineOpacity(0.62f)
            )
        )
    }
    if (style.getLayer(ROUTE_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(Color.parseColor("#1EA8FF")),
                PropertyFactory.lineWidth(4.0f),
                PropertyFactory.lineOpacity(0.96f)
            )
        )
    }
    if (style.getLayer(AIRWAY_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(AIRWAY_LAYER_ID, AIRWAY_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(Color.parseColor("#8B5CF6")),
                PropertyFactory.lineWidth(4.8f),
                PropertyFactory.lineOpacity(0.96f)
            )
        )
    }
    if (style.getLayer(FIX_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(FIX_LAYER_ID, FIX_SOURCE_ID).withProperties(
                PropertyFactory.circleColor(Color.parseColor("#F4F7FB")),
                PropertyFactory.circleRadius(4.5f),
                PropertyFactory.circleStrokeColor(Color.parseColor("#D6A84B")),
                PropertyFactory.circleStrokeWidth(1.8f),
                PropertyFactory.circleOpacity(0.98f)
            )
        )
    }
}

internal fun updateRouteLayers(map: MapLibreMap, plan: PreparedFlightPlan?) {
    val style = map.style ?: return
    style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)?.setGeoJson(routeLineGeoJson(plan))
    style.getSourceAs<GeoJsonSource>(AIRWAY_SOURCE_ID)?.setGeoJson(airwayLineGeoJson(plan))
    style.getSourceAs<GeoJsonSource>(FIX_SOURCE_ID)?.setGeoJson(routeFixGeoJson(plan))
}

private fun routeLineGeoJson(plan: PreparedFlightPlan?): String {
    if (plan == null) return EMPTY_FEATURE_COLLECTION
    val points = buildList {
        add(plan.route.departure.longitude to plan.route.departure.latitude)
        plan.route.legs.forEach { leg -> add(leg.to.longitude to leg.to.latitude) }
    }
    if (points.size < 2) return EMPTY_FEATURE_COLLECTION

    val coordinates = JSONArray()
    points.forEach { (longitude, latitude) ->
        coordinates.put(JSONArray().put(longitude).put(latitude))
    }
    return featureCollection(
        JSONObject()
            .put("type", "Feature")
            .put("properties", JSONObject().put("kind", "route"))
            .put(
                "geometry",
                JSONObject().put("type", "LineString").put("coordinates", coordinates)
            )
    )
}

private fun airwayLineGeoJson(plan: PreparedFlightPlan?): String {
    if (plan == null) return EMPTY_FEATURE_COLLECTION
    val features = JSONArray()
    plan.route.legs.filter { it.airwayName != null }.forEach { leg ->
        val coordinates = JSONArray()
            .put(JSONArray().put(leg.from.longitude).put(leg.from.latitude))
            .put(JSONArray().put(leg.to.longitude).put(leg.to.latitude))
        features.put(
            JSONObject()
                .put("type", "Feature")
                .put("properties", JSONObject().put("airway", leg.airwayName))
                .put(
                    "geometry",
                    JSONObject().put("type", "LineString").put("coordinates", coordinates)
                )
        )
    }
    return JSONObject().put("type", "FeatureCollection").put("features", features).toString()
}

private fun routeFixGeoJson(plan: PreparedFlightPlan?): String {
    if (plan == null) return EMPTY_FEATURE_COLLECTION
    val features = JSONArray()

    fun addFix(identifier: String, longitude: Double, latitude: Double, kind: String) {
        features.put(
            JSONObject()
                .put("type", "Feature")
                .put(
                    "properties",
                    JSONObject().put("identifier", identifier).put("kind", kind)
                )
                .put(
                    "geometry",
                    JSONObject()
                        .put("type", "Point")
                        .put("coordinates", JSONArray().put(longitude).put(latitude))
                )
        )
    }

    addFix(
        plan.route.departure.normalizedIcao,
        plan.route.departure.longitude,
        plan.route.departure.latitude,
        "departure"
    )
    plan.route.legs.forEachIndexed { index, leg ->
        val kind = if (index == plan.route.legs.lastIndex) "destination" else "fix"
        addFix(leg.toIdentifier, leg.to.longitude, leg.to.latitude, kind)
    }

    return JSONObject().put("type", "FeatureCollection").put("features", features).toString()
}

private fun featureCollection(feature: JSONObject): String =
    JSONObject()
        .put("type", "FeatureCollection")
        .put("features", JSONArray().put(feature))
        .toString()
