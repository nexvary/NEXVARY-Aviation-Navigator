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

    @Test(expected = IllegalArgumentException::class)
    fun searchRejectsNonPositiveLimit() {
        repository.searchAirports("cairo", 0)
    }
}
