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

## Current 0.2.0 application

The current verified development line now includes:

- Jetpack Compose Android application
- Deep-black / royal-gold aviation dashboard
- Home dashboard with tracked, airborne and ground traffic statistics
- MapLibre map with live ADS-B aircraft GeoJSON layer
- Live ADSB.lol provider adapter
- OpenSky provider adapter behind a common contract
- Normalized `AircraftTrack` model
- Multi-provider repository with ICAO24 deduplication logic
- Live radar view
- Live traffic list with callsign, altitude and speed
- Functional flight-plan draft form with ICAO and altitude validation
- Android back behavior that returns internal sections to Home before exit
- Unit tests for aircraft presentation and flight-plan validation
- GitHub Actions build gate and debug APK artifact

## Architecture

The Android client consumes a normalized aviation-data model instead of depending directly on one provider. Initial traffic adapters target ADSB.lol and OpenSky Network. A later gateway service will handle provider aggregation, deduplication, caching and secret-bearing commercial APIs.

```text
ADSB.lol ───────┐
OpenSky ────────┼──> Provider adapters ──> Normalized traffic model ──> Android UI
Future APIs ────┘                         │
                                         ├── Live Map
Navigation DB ────────────────────────────┼── Flight Planner
Weather ──────────────────────────────────┼── Radar
Simulator bridge ─────────────────────────┴── Navigation
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the design boundaries and next platform layers.

## Data-source principles

- ADSB.lol: initial open live-traffic source; its open API/data is licensed separately by its provider.
- OpenSky: secondary provider; programmatic access should use OAuth2 and respect current credit/rate limits.
- Commercial providers such as FlightAware or Plane Finder should be integrated through a backend gateway, not with secrets embedded in the APK.
- Navigation datasets, chart data and procedure data must be reviewed independently for redistribution/use rights.

## Safety scope

NEXVARY Aviation Navigator is an informational, planning, simulation and situational-awareness application. It is **not certified for real-world primary navigation, ATC separation, collision avoidance, or safety-of-life use**.

## Build

The project targets JDK 17, Android Gradle Plugin 9.4.x, Kotlin 2.4.x, Android API 36 and Gradle 9.6.x.

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

GitHub Actions performs both commands and uploads the successful debug APK as `NEXVARY-Aviation-Navigator-debug`.
