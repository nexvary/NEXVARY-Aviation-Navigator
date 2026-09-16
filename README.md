# NEXVARY Aviation Navigator

Android aviation navigation and live-aircraft tracking platform combining Little Navmap-style flight planning with real-time ADS-B traffic.

## Vision

The project is designed as a mobile aviation workspace rather than a simple aircraft tracker. It will combine:

- Live ADS-B / MLAT traffic
- IFR/VFR flight planning
- Airports, runways, waypoints, navaids and airways
- SID / STAR / approach awareness
- Airspace overlays
- METAR / TAF / winds
- Aircraft performance and vertical profile
- Radar mode and traffic filtering
- Historical traffic replay
- Simulator connectivity for MSFS / X-Plane
- Offline-capable map/navigation layers where licensing permits

## Current 0.3.0 development line

0.3.0 starts the navigation-data and route-engine layer on top of the verified 0.2.0 application. The first slice adds:

- Provider-independent `NavigationRepository` contract
- Normalized airport model with ICAO/IATA identity, position and elevation
- Case-insensitive airport lookup and search
- Small bundled Egypt airport seed for development and offline architecture verification
- Direct great-circle route engine with nautical-mile distance and initial bearing
- Optional cruise-speed ETA calculation
- Explicit route failures for unknown/same airports and invalid cruise speed
- Unit tests covering airport search, lookup and HECA → HESH route metrics

The bundled seed is intentionally small and informational. Production/global navigation data remains behind the repository boundary so licensed or open datasets can be substituted without coupling the UI to one supplier.

## Verified 0.2.0 application

The verified release includes:

- Jetpack Compose Android application
- Deep-black metallic UI using royal gold, electric blue, neon green, violet and silver accents
- Responsive phone layout with non-overlapping Material navigation icons
- Home dashboard with tracked, airborne and ground traffic statistics
- MapLibre map with live ADS-B aircraft GeoJSON layer
- Live ADSB.lol provider adapter
- OpenSky provider adapter behind a common contract
- Normalized `AircraftTrack` model
- Multi-provider repository with ICAO24 deduplication logic
- Live radar view
- Live traffic list with callsign, altitude and speed
- Functional flight-plan draft form with ICAO and altitude validation
- Dedicated About page with NEXVARY website, Facebook, email, YouTube and X links
- Visible in-app Back control on every secondary page plus Android Back handling
- Arabic, English, Turkish, Spanish, German, Italian, French, Urdu, Persian and Russian UI support
- Explicit RTL layout handling for Arabic, Urdu and Persian
- Dedicated aviation/radar launcher icon
- UI Release Gate and Navigation Integrity Gate for dead-button/disconnected-page regressions
- Android Lint + CodeQL security release gate
- GitHub Actions debug APK and real-emulator screenshot artifacts

## Architecture

The Android client consumes normalized aviation-data models instead of depending directly on one provider. Live traffic and aeronautical navigation data have separate repository boundaries. Initial traffic adapters target ADSB.lol and OpenSky Network; navigation data starts behind `NavigationRepository`. A later gateway service will handle provider aggregation, deduplication, caching and secret-bearing commercial APIs.

```text
ADSB.lol ───────┐
OpenSky ────────┼──> Provider adapters ──> Normalized traffic model ──> Android UI
Future APIs ────┘                         │
                                         ├── Live Map
Navigation DB ──> NavigationRepository ───┼── Flight Planner / Route Engine
Weather ──────────────────────────────────┼── Radar
Simulator bridge ─────────────────────────┴── Navigation
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for design boundaries and [`docs/RELEASE_GATES.md`](docs/RELEASE_GATES.md) for the release verification contract.

## Data-source principles

- ADSB.lol: initial open live-traffic source; its open API/data is licensed separately by its provider.
- OpenSky: secondary provider; programmatic access should use OAuth2 and respect current credit/rate limits.
- Commercial providers such as FlightAware or Plane Finder should be integrated through a backend gateway, not with secrets embedded in the APK.
- Navigation datasets, chart data and procedure data must be reviewed independently for redistribution/use rights.

## Safety scope

NEXVARY Aviation Navigator is an informational, planning, simulation and situational-awareness application. It is **not certified for real-world primary navigation, ATC separation, collision avoidance, or safety-of-life use**.

## Build and verification

The project targets JDK 17, Android Gradle Plugin 9.4.x, Kotlin 2.4.x, Android API 36 and Gradle 9.6.x.

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
gradle :app:lintDebug
```

The emulator release gate additionally runs:

```bash
gradle :app:connectedDebugAndroidTest
```

A debug APK is uploaded only after the build job succeeds. UI gate evidence is captured from the actual APK running on an Android emulator.
