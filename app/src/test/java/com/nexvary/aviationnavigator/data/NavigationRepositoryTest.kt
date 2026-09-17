package com.nexvary.aviationnavigator.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRepositoryTest {

    private val repository = InMemoryNavigationRepository(EgyptNavigationSeed.airports)

    @Test
    fun airportLookupIsCaseInsensitive() {
        assertEquals("Cairo International Airport", repository.airportByIcao("heca")?.name)
        assertEquals("HESH", repository.airportByIcao(" HESH ")?.normalizedIcao)
        assertNull(repository.airportByIcao("ZZZZ"))
    }

    @Test
    fun airportSearchMatchesIcaoIataNameAndMunicipality() {
        assertEquals("HECA", repository.searchAirports("cai").first().normalizedIcao)
        assertEquals("HESH", repository.searchAirports("SSH").first().normalizedIcao)
        assertTrue(repository.searchAirports("hurghada").any { it.normalizedIcao == "HEGN" })
        assertTrue(repository.searchAirports("alexandria").any { it.normalizedIcao == "HEBA" })
    }

    @Test
    fun nearestAirportsAreDistanceSorted() {
        val results = repository.nearestAirports(
            latitude = 30.0444,
            longitude = 31.2357,
            limit = 3
        )

        assertEquals(3, results.size)
        assertEquals("HECA", results.first().airport.normalizedIcao)
        assertTrue(results.first().distanceNauticalMiles < 10.0)
        assertTrue(results.zipWithNext().all { (a, b) -> a.distanceNauticalMiles <= b.distanceNauticalMiles })
    }

    @Test
    fun nearestAirportsRespectMaximumDistance() {
        val results = repository.nearestAirports(
            latitude = 30.0444,
            longitude = 31.2357,
            limit = 10,
            maxDistanceNauticalMiles = 20.0
        )

        assertEquals(listOf("HECA"), results.map { it.airport.normalizedIcao })
    }

    @Test(expected = IllegalArgumentException::class)
    fun searchRejectsNonPositiveLimit() {
        repository.searchAirports("cairo", 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nearestSearchRejectsInvalidLatitude() {
        repository.nearestAirports(latitude = 91.0, longitude = 31.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nearestSearchRejectsNegativeRadius() {
        repository.nearestAirports(
            latitude = 30.0,
            longitude = 31.0,
            maxDistanceNauticalMiles = -1.0
        )
    }
}
