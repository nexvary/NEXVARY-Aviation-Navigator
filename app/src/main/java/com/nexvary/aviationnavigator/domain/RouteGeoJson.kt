package com.nexvary.aviationnavigator.domain

object RouteGeoJson {
    fun featureCollection(route: ResolvedRoute): String {
        val coordinates = buildList {
            add(route.departure.geoPoint())
            route.legs.forEach { add(it.to) }
        }

        val lineCoordinates = coordinates.joinToString(",") { point ->
            "[${format(point.longitude)},${format(point.latitude)}]"
        }

        val legFeatures = route.legs.mapIndexed { index, leg ->
            """{"type":"Feature","properties":{"kind":"leg","index":$index,"from":"${escape(leg.fromIdentifier)}","to":"${escape(leg.toIdentifier)}","legType":"${leg.type.name}","airway":${leg.airwayName?.let { "\"${escape(it)}\"" } ?: "null"},"distanceNm":${format(leg.distanceNauticalMiles)},"bearing":${format(leg.initialBearingDegrees)}},"geometry":{"type":"LineString","coordinates":[[${format(leg.from.longitude)},${format(leg.from.latitude)}],[${format(leg.to.longitude)},${format(leg.to.latitude)}]]}}"""
        }

        val routeFeature = """{"type":"Feature","properties":{"kind":"route","departure":"${escape(route.departure.normalizedIcao)}","destination":"${escape(route.destination.normalizedIcao)}","distanceNm":${format(route.totalDistanceNauticalMiles)}},"geometry":{"type":"LineString","coordinates":[$lineCoordinates]}}"""

        return """{"type":"FeatureCollection","features":[${(listOf(routeFeature) + legFeatures).joinToString(",")}]}"""
    }

    private fun format(value: Double): String = String.format(java.util.Locale.US, "%.6f", value)

    private fun escape(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}
