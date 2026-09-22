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

The route stack now exposes `RouteGeoJson.featureCollection(...)`, giving the UI a provider-independent line/leg representation that can be connected to a dedicated MapLibre source without coupling route computation to rendering.

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

Live traffic feeds remain separate from aeronautical navigation data. `NavigationRepository` now owns normalized access to:

- airports
- runways
- VOR / VOR-DME / DME / NDB navaids
- fixes and waypoints
- airways with ordered fix membership and optional altitude bounds

The repository provides case-insensitive lookup/search, nearest-object queries using shared geodesic calculations, runway grouping by airport and generic fix resolution across airport/navaid/waypoint identities.

Parking/gates, FIR/CTR/TMA, SID/STAR/approach data and terrain/elevation datasets remain future repository extensions where data rights permit.

## Route engine

The route layer is split into four responsibilities:

1. `RouteSyntax` normalizes free-form route text into route tokens.
2. `RoutePlanner` resolves direct fixes and airway segments against `NavigationRepository`.
3. `RouteMetrics` produces cumulative and remaining distance at every route point.
4. `RouteGeoJson` converts a resolved route to rendering-ready GeoJSON.

The airway resolver supports forward and reverse traversal through ordered airway fixes and expands intermediate fixes. Failures are structured as typed reasons rather than opaque strings, including unknown fixes/airways and invalid airway entry/exit.

Current route shape:

`Departure → direct fix → airway fixes → direct fix → Destination`

The next structural evolution is:

`Departure → SID → airway/fixes → STAR → approach → destination`

## Flight planning and performance

`FlightPlanningService` now composes draft validation, route resolution, route metrics, GeoJSON and a first aircraft-performance estimate. `AircraftPerformanceProfile` supplies cruise speed, hourly fuel burn and reserve minutes. `FlightEstimateEngine` returns airborne time, trip fuel, reserve fuel and total fuel.

This is deliberately a simple planning estimate, not a certified flight-management or dispatch calculation. Later performance work can add climb/descent profiles, winds, altitude constraints, TOC/TOD and vertical-profile rendering without changing the navigation repository boundary.

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
- Keep operational navigation datasets replaceable behind `NavigationRepository` rather than baking supplier-specific assumptions into the UI.

## Safety

The platform is informational, planning, simulation and situational-awareness software. It is not certified for real-world primary navigation, ATC separation, collision avoidance or safety-of-life use.
