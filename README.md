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

0.3.0 builds the navigation-data and route-engine layer on top of the verified 0.2.0 application. The current slice includes:

- Provider-independent `NavigationRepository` contract
- Normalized airport model with ICAO/IATA identity, position and elevation
- Runway model with airport association, dimensions, surface and heading metadata
- Navaid model with VOR/VOR-DME/DME/NDB types, frequency and normalized identifiers
- Waypoint/fix model and airway model with ordered fix membership and altitude bounds
- Case-insensitive search and lookup for airports, navaids, waypoints and airways
- Distance-sorted nearest-airport, nearest-navaid and nearest-waypoint queries
- Shared great-circle distance and initial-bearing geometry
- Generic fix resolution across airports, navaids and waypoints
- Direct route engine with nautical-mile distance, bearing and optional cruise-speed ETA
- Airway-aware `RoutePlanner` capable of forward and reverse airway traversal and intermediate-fix expansion
- Structured route failures for unknown fixes/airways, invalid airway entry/exit and invalid speed
- Cumulative route-progress metrics with leg, flown and remaining distance
- Route GeoJSON serialization for future MapLibre route overlay integration
- Aircraft performance profiles with trip-time, trip-fuel and reserve-fuel estimates
- `FlightPlanningService` joining draft validation, route resolution, performance estimates, progress metrics and GeoJSON output
- Deterministic navigation fixtures and unit tests covering the repository, route expansion, failures, performance and GeoJSON
- Small bundled Egypt airport seed retained only for development/offline architecture verification

The bundled airport seed is intentionally small and informational. Production/global navigation data remains behind the repository boundary so licensed or open datasets can be substituted without coupling the UI to one supplier. Operational runway, navaid, airway, chart and procedure datasets must come from a reviewed source with appropriate usage and redistribution rights.

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

The Android client consumes normalized aviation-data models instead of depending directly on one provider. Live traffic and aeronautical navigation data have separate repository boundaries. Initial traffic adapters target ADSB.lol and OpenSky Network; navigation data lives behind `NavigationRepository`. A later gateway service will handle provider aggregation, deduplication, caching and secret-bearing commercial APIs.

```text
ADSB.lol ───────┐
OpenSky ────────┼──> Provider adapters ──> Normalized traffic model ──> Android UI
Future APIs ────┘                         │
                                         ├── Live Map
Navigation DB ──> NavigationRepository ───┼── Flight Planner / Route Engine
                                         │        │
                                         │        ├── Route metrics / ETA
                                         │        ├── Fuel + reserve estimate
                                         │        └── Route GeoJSON
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
