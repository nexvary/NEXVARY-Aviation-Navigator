# NEXVARY Aviation Navigator

Active development line: **0.4.0**

NEXVARY Aviation Navigator is an Android aviation situational-awareness and flight-planning application built with Kotlin, Jetpack Compose and MapLibre.

## Current capabilities

- Live traffic integration with ADSB.lol and OpenSky
- Provider merging and quality-based de-duplication
- Persistent traffic snapshot fallback for temporary network/provider outages
- Live map and aircraft overlay
- Flight planning with airports, runways, navaids, waypoints and airways
- Great-circle distance, bearing, route legs, ETE and fuel estimates
- MapLibre route overlay
- Radar and traffic views
- Arabic/English/Turkish/Spanish/German/Italian/French/Urdu/Persian/Russian UI support
- RTL support for Arabic, Persian and Urdu
- NEXVARY corporate visual system: deep navy/black, metallic silver and electric/cobalt blue
- Navigation Integrity Gate, UI Release Gate, Android Lint, unit tests and CodeQL security analysis

## Development status

The `dev/navigation-data-0.3.0` branch now carries the 0.4.0 completion jump. It is not treated as final until Android CI and the Heavy Release Gate pass on the exact current HEAD.

The aviation navigation seed data and calculated planning output are for development and situational-awareness use. Operational flight planning must use current authoritative aeronautical data and procedures.
