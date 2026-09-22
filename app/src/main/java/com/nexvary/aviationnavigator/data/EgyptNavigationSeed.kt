package com.nexvary.aviationnavigator.data

import com.nexvary.aviationnavigator.domain.Airport

/**
 * Small bundled seed used to exercise the navigation-data boundary before a
 * licensed/global dataset is connected. Coordinates are informational only.
 */
object EgyptNavigationSeed {
    val airports: List<Airport> = listOf(
        Airport(
            icao = "HECA",
            iata = "CAI",
            name = "Cairo International Airport",
            municipality = "Cairo",
            countryCode = "EG",
            latitude = 30.121944,
            longitude = 31.405556,
            elevationFeet = 382
        ),
        Airport(
            icao = "HEBA",
            iata = "HBE",
            name = "Borg El Arab International Airport",
            municipality = "Alexandria",
            countryCode = "EG",
            latitude = 30.917669,
            longitude = 29.696437,
            elevationFeet = 177
        ),
        Airport(
            icao = "HEGN",
            iata = "HRG",
            name = "Hurghada International Airport",
            municipality = "Hurghada",
            countryCode = "EG",
            latitude = 27.178317,
            longitude = 33.799436,
            elevationFeet = 52
        ),
        Airport(
            icao = "HELX",
            iata = "LXR",
            name = "Luxor International Airport",
            municipality = "Luxor",
            countryCode = "EG",
            latitude = 25.671028,
            longitude = 32.706583,
            elevationFeet = 294
        ),
        Airport(
            icao = "HESH",
            iata = "SSH",
            name = "Sharm El Sheikh International Airport",
            municipality = "Sharm El Sheikh",
            countryCode = "EG",
            latitude = 27.977286,
            longitude = 34.394950,
            elevationFeet = 143
        ),
        Airport(
            icao = "HESN",
            iata = "ASW",
            name = "Aswan International Airport",
            municipality = "Aswan",
            countryCode = "EG",
            latitude = 23.964356,
            longitude = 32.819975,
            elevationFeet = 662
        )
    )
}

object DefaultNavigationRepository {
    val instance: NavigationRepository by lazy {
        InMemoryNavigationRepository(EgyptNavigationSeed.airports)
    }
}
