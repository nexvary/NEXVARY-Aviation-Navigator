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

## Data-source principles

- ADSB.lol: initial open live-traffic source; its open API/data is licensed separately by its provider.
- OpenSky: secondary provider; programmatic access should use OAuth2 and respect current credit/rate limits.
- Commercial providers such as FlightAware or Plane Finder should be integrated through a backend gateway, not with secrets embedded in the APK.
- Navigation datasets, chart data and procedure data must be reviewed independently for redistribution/use rights.

## Safety scope

NEXVARY Aviation Navigator is an informational, planning, simulation and situational-awareness application. It is **not certified for real-world primary navigation, ATC separation, collision avoidance, or safety-of-life use**.

## Initial development line

`0.1.x` establishes the Android foundation, normalized aircraft model, provider abstraction, live map shell, flight-planner shell, radar shell and automated build gate.

## Build

The project targets JDK 17, Android Gradle Plugin 9.4.x, Kotlin 2.4.x and Gradle 9.6.x.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```
