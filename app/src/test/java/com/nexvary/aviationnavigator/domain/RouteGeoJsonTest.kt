package com.nexvary.aviationnavigator.domain

import com.nexvary.aviationnavigator.data.NavigationFixture
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGeoJsonTest {
    @Test
    fun serializedRouteContainsRouteAndLegFeatures() {
        val result = RoutePlanner(NavigationFixture.repository())
            .resolve("TEST", "TEND", "FIXA J1 FIXC") as RoutePlanResult.Success

        val json = RouteGeoJson.featureCollection(result.route)

        assertTrue(json.startsWith("{\"type\":\"FeatureCollection\""))
        assertTrue(json.contains("\"departure\":\"TEST\""))
        assertTrue(json.contains("\"destination\":\"TEND\""))
        assertTrue(json.contains("\"airway\":\"J1\""))
        assertTrue(json.contains("\"to\":\"FIXB\""))
        assertFalse(json.contains("NaN"))
        assertFalse(json.contains("Infinity"))
    }
}
