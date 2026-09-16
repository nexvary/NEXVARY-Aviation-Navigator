# Architecture

## Product boundary

NEXVARY Aviation Navigator is designed as an Android aviation workspace that combines live traffic awareness with flight planning, navigation data, weather, simulator connectivity and replay. The application must not couple its UI to one ADS-B vendor or one navigation-data supplier.

## Android client

The Android application is organized around four initial workspaces:

1. **Map** — MapLibre-based moving map and live traffic layers.
2. **Plan** — flight-plan, procedure, performance and vertical-profile workspace.
3. **Radar** — range-ring situational display derived from normalized traffic tracks.
4. **Traffic** — searchable/detail-oriented aircraft list.

The visual system uses a near-black base, royal gold primary accent, metallic silver secondary details and restrained electric blue status accents.

## Normalized traffic model

Every live provider must translate its response into `AircraftTrack` before data reaches the UI. The normalized model includes:

- ICAO24 / Mode-S identity
- callsign and registration where available
- aircraft type where available
- latitude / longitude
- barometric and geometric altitude
- ground speed
- track
- vertical rate
- squawk
- on-ground state
- source and last-seen metadata

This boundary allows traffic providers to be added, removed or degraded without rewriting map, radar or traffic-list code.

## Provider layer

`LiveTrafficProvider` is the provider contract. The first adapters are:

- **ADSB.lol** — initial live source used by the mobile MVP.
- **OpenSky Network** — secondary adapter; authenticated production access should be supplied by a backend/gateway rather than shipping OAuth client secrets in the APK.

`TrafficRepository` is responsible for concurrent provider reads and ICAO24 deduplication. Provider quality is evaluated before a single normalized track is exposed upstream.

## Live map layer

MapLibre owns the geographic rendering surface. Live tracks are serialized into a GeoJSON `FeatureCollection` and stored in a `GeoJsonSource`. The source is updated in place when a new traffic snapshot arrives, avoiding map reconstruction. A dedicated layer renders the points above the base style.

Future map work should evolve this into:

- aircraft-shaped symbols rotated by track
- callsign/altitude labels
- selected-aircraft highlighting
- trails
- clustering / density behavior at low zoom
- airport, airway and controlled-airspace overlays
- route and procedure layers

## Aviation Gateway

A server-side gateway is planned between the Android application and sources that require secrets, commercial credentials, aggressive rate management or cross-provider reconciliation.

```text
                    ┌──────────────────────┐
ADSB.lol ──────────>│                      │
OpenSky ───────────>│  Aviation Gateway    │──> Normalized API / stream
Commercial APIs ───>│                      │
Weather ───────────>│ Cache / dedupe / ACL │
                    └──────────┬───────────┘
                               │
                               ▼
                         Android client
```

The gateway should eventually provide caching, rate limiting, credentials isolation, provider health checks, deduplication, historical storage and WebSocket/SSE delivery.

## Navigation data boundary

Live traffic feeds must remain separate from aeronautical navigation data. A navigation repository will later own:

- airports
- runways
- parking/gates where licensed
- VOR / DME / NDB
- fixes and waypoints
- airways
- FIR / CTR / TMA and other airspace
- SID / STAR / approaches where data rights permit
- elevation/terrain metadata

The route engine should operate against the navigation repository, not the live-traffic provider layer.

## Planned route engine

The planner will evolve from a workspace shell into a graph-based routing engine capable of representing:

`Departure → SID → airway/fixes → STAR → approach → destination`

Later layers will add aircraft-performance profiles, fuel/time calculations, TOC/TOD, altitude constraints and a vertical profile.

## Weather and simulation

Weather is a separate repository so METAR, TAF, winds and future radar/cloud products can change vendors independently. Simulator integration will use a local-network bridge for MSFS/X-Plane telemetry rather than mixing simulator state with real ADS-B tracks.

## Persistence and offline use

Local persistence should use Room/SQLite for user plans, cached metadata, aircraft profiles and appropriately licensed navigation datasets. Offline map and nav-data behavior must respect each source's redistribution and caching rights.

## Security and privacy

- Do not embed commercial API secrets in the APK.
- Prefer gateway-issued short-lived access when credentials are required.
- Keep provider parsing isolated from UI code.
- Validate external values before they enter navigation or rendering models.
- Treat downloaded navigation datasets as untrusted input.

## Safety

The platform is informational, planning, simulation and situational-awareness software. It is not certified for real-world primary navigation, ATC separation, collision avoidance or safety-of-life use.
