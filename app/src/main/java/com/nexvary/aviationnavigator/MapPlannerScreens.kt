package com.nexvary.aviationnavigator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexvary.aviationnavigator.data.DefaultNavigationRepository
import com.nexvary.aviationnavigator.domain.AircraftPerformanceProfile
import com.nexvary.aviationnavigator.domain.AircraftTrack
import com.nexvary.aviationnavigator.domain.FlightPlanDraft
import com.nexvary.aviationnavigator.domain.FlightPlanningResult
import com.nexvary.aviationnavigator.domain.FlightPlanningService
import com.nexvary.aviationnavigator.domain.PreparedFlightPlan
import com.nexvary.aviationnavigator.domain.RoutePlanFailure
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale

private const val PLANNER_LATITUDE = 30.0444
private const val PLANNER_LONGITUDE = 31.2357
private const val PLANNER_RADIUS_NM = 250

private val defaultPlanningProfile = AircraftPerformanceProfile(
    name = "Generic planning jet",
    cruiseSpeedKnots = 430.0,
    fuelBurnKgPerHour = 1_800.0,
    reserveMinutes = 45
)

@Composable
internal fun LiveMapScreen(
    modifier: Modifier,
    tracks: List<AircraftTrack>,
    preparedPlan: PreparedFlightPlan?,
    onBack: () -> Unit,
    onMapViewReady: (MapView) -> Unit
) {
    Box(modifier = modifier.fillMaxSize().testTag("page_MAP")) {
        MapLibreSurface(
            modifier = Modifier.fillMaxSize(),
            tracks = tracks,
            preparedPlan = preparedPlan,
            onMapViewReady = onMapViewReady
        )
        Card(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBlack.copy(alpha = 0.95f)),
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.72f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PageHeader(uiText(UiText.LIVE_AIRSPACE), ElectricBlue, onBack)
                Text(
                    uiText(UiText.RADIUS, PLANNER_RADIUS_NM),
                    modifier = Modifier.fillMaxWidth(),
                    color = MetallicSilver,
                    textAlign = TextAlign.Start
                )
                Text(
                    uiText(UiText.TRACKED_COUNT, tracks.size),
                    modifier = Modifier.fillMaxWidth(),
                    color = Platinum,
                    textAlign = TextAlign.Start
                )
                preparedPlan?.let { plan ->
                    Text(
                        "ROUTE ${plan.route.departure.normalizedIcao} → ${plan.route.destination.normalizedIcao} · ${format0(plan.route.totalDistanceNauticalMiles)} NM",
                        modifier = Modifier.fillMaxWidth().testTag("route_overlay_status"),
                        color = NeonViolet,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                }
                Text(
                    uiText(UiText.LIVE_MAP_CAPTION),
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonGreen,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun MapLibreSurface(
    modifier: Modifier,
    tracks: List<AircraftTrack>,
    preparedPlan: PreparedFlightPlan?,
    onMapViewReady: (MapView) -> Unit
) {
    val context = LocalContext.current
    val latestTracks = rememberUpdatedState(tracks)
    val latestPlan = rememberUpdatedState(preparedPlan)
    val mapHolder = remember { arrayOfNulls<MapLibreMap>(1) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                mapHolder[0] = map
                map.setStyle("https://demotiles.maplibre.org/style.json") { style ->
                    installRouteLayers(style, latestPlan.value)
                    installAircraftLayer(style, latestTracks.value)
                }
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(PLANNER_LATITUDE, PLANNER_LONGITUDE))
                    .zoom(4.8)
                    .build()
            }
            onMapViewReady(this)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = {
            mapHolder[0]?.let { map ->
                updateRouteLayers(map, preparedPlan)
                updateAircraftLayer(map, tracks)
            }
        }
    )
}

@Composable
internal fun FlightPlanScreen(
    modifier: Modifier,
    currentPlan: PreparedFlightPlan?,
    onPlanPrepared: (PreparedFlightPlan?) -> Unit,
    onOpenMap: () -> Unit,
    onBack: () -> Unit
) {
    val service = remember { FlightPlanningService(DefaultNavigationRepository.instance) }
    var departure by rememberSaveable { mutableStateOf("HECA") }
    var destination by rememberSaveable { mutableStateOf("HESH") }
    var altitude by rememberSaveable { mutableStateOf("35000") }
    var route by rememberSaveable { mutableStateOf("DCT") }
    var planningResult by remember { mutableStateOf<FlightPlanningResult?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("page_PLAN")
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PageHeader(uiText(UiText.FLIGHT_PLANNER), NeonViolet, onBack)
        Text(
            uiText(UiText.FLIGHT_PLANNER_INTRO),
            modifier = Modifier.fillMaxWidth(),
            color = MetallicSilver,
            textAlign = TextAlign.Start
        )

        OutlinedTextField(
            value = departure,
            onValueChange = { departure = it.uppercase().filter(Char::isLetter).take(4) },
            label = { Text(uiText(UiText.DEPARTURE_ICAO)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it.uppercase().filter(Char::isLetter).take(4) },
            label = { Text(uiText(UiText.DESTINATION_ICAO)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = altitude,
            onValueChange = { altitude = it.filter(Char::isDigit).take(5) },
            label = { Text(uiText(UiText.CRUISE_ALTITUDE)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = route,
            onValueChange = { route = it.uppercase() },
            label = { Text(uiText(UiText.ROUTE_AIRWAYS)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Button(
            modifier = Modifier.fillMaxWidth().testTag("validate_plan_button"),
            onClick = {
                val draft = FlightPlanDraft(
                    departure = departure,
                    destination = destination,
                    cruiseAltitudeFeet = altitude.toIntOrNull() ?: 0,
                    route = route
                )
                val result = service.prepare(draft, defaultPlanningProfile)
                planningResult = result
                onPlanPrepared((result as? FlightPlanningResult.Success)?.plan)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = RoyalGold,
                contentColor = Color.Black
            )
        ) {
            Text(uiText(UiText.VALIDATE_PLAN), fontWeight = FontWeight.Bold)
        }

        when (val result = planningResult) {
            null -> PlannerIdleCard(currentPlan)
            is FlightPlanningResult.DraftValidationFailure -> PlannerErrorCard(
                result.errors.joinToString("\n") { localizedValidationError(it) }
            )
            is FlightPlanningResult.RouteResolutionFailure -> PlannerErrorCard(
                routeFailureText(result.reason, result.token)
            )
            is FlightPlanningResult.Success -> PlannerSuccessCard(
                plan = result.plan,
                onOpenMap = onOpenMap
            )
        }

        FeatureCard(UiText.PROCEDURES, UiText.PROCEDURES_SUB, NeonViolet)
    }
}

@Composable
private fun PlannerIdleCard(currentPlan: PreparedFlightPlan?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                uiText(UiText.PLAN_STATUS),
                modifier = Modifier.fillMaxWidth(),
                color = RoyalGold,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.height(6.dp))
            Text(
                currentPlan?.let {
                    "${it.route.departure.normalizedIcao} → ${it.route.destination.normalizedIcao} · ${format0(it.route.totalDistanceNauticalMiles)} NM"
                } ?: uiText(UiText.READY_VALIDATE),
                modifier = Modifier.fillMaxWidth(),
                color = Platinum,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun PlannerErrorCard(message: String) {
    AccentCard(AmberGold) {
        Text(
            uiText(UiText.PLAN_STATUS),
            modifier = Modifier.fillMaxWidth(),
            color = AmberGold,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
        Text(
            message,
            modifier = Modifier.fillMaxWidth().testTag("plan_error"),
            color = Platinum,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun PlannerSuccessCard(plan: PreparedFlightPlan, onOpenMap: () -> Unit) {
    AccentCard(
        accent = NeonGreen,
        modifier = Modifier.fillMaxWidth().testTag("plan_metrics")
    ) {
        Text(
            uiText(UiText.PLAN_VALID),
            modifier = Modifier.fillMaxWidth(),
            color = NeonGreen,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
        Text(
            plan.draft.summary,
            modifier = Modifier.fillMaxWidth(),
            color = Platinum,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
        Text(
            "DIST ${format0(plan.route.totalDistanceNauticalMiles)} NM · ETE ${plan.performance.airborneMinutes} MIN",
            modifier = Modifier.fillMaxWidth(),
            color = ElectricBlue,
            textAlign = TextAlign.Start
        )
        Text(
            "GS ${format0(plan.performance.averageGroundSpeedKnots)} KT · TRIP ${format0(plan.performance.tripFuelKg)} KG · RES ${format0(plan.performance.reserveFuelKg)} KG",
            modifier = Modifier.fillMaxWidth(),
            color = MetallicSilver,
            textAlign = TextAlign.Start
        )
        val firstLeg = plan.route.legs.firstOrNull()
        firstLeg?.let {
            Text(
                "BRG ${format0(it.initialBearingDegrees)}° · ${plan.route.legs.size} LEG${if (plan.route.legs.size == 1) "" else "S"}",
                modifier = Modifier.fillMaxWidth(),
                color = NeonViolet,
                textAlign = TextAlign.Start
            )
        }
        val routePreview = plan.route.legs
            .take(6)
            .joinToString("  ›  ") { it.toIdentifier }
        if (routePreview.isNotBlank()) {
            Text(
                "${plan.route.departure.normalizedIcao}  ›  $routePreview",
                modifier = Modifier.fillMaxWidth(),
                color = Platinum,
                textAlign = TextAlign.Start
            )
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().testTag("open_route_map_button"),
            onClick = onOpenMap,
            border = BorderStroke(1.dp, ElectricBlue)
        ) {
            Text(uiText(UiText.LIVE_MAP), color = ElectricBlue, fontWeight = FontWeight.Bold)
        }
    }
}

private fun localizedValidationError(error: String): String = when (error) {
    "Departure must be a 4-letter ICAO code" -> uiText(UiText.ERROR_DEPARTURE_ICAO)
    "Destination must be a 4-letter ICAO code" -> uiText(UiText.ERROR_DESTINATION_ICAO)
    "Departure and destination must be different" -> uiText(UiText.ERROR_SAME_AIRPORT)
    "Cruise altitude must be between 1,000 and 60,000 ft" -> uiText(UiText.ERROR_ALTITUDE)
    else -> error
}

private fun routeFailureText(reason: RoutePlanFailure, token: String?): String {
    val suffix = token?.let { " · $it" }.orEmpty()
    return when (reason) {
        RoutePlanFailure.UNKNOWN_DEPARTURE -> "UNKNOWN DEPARTURE$suffix"
        RoutePlanFailure.UNKNOWN_DESTINATION -> "UNKNOWN DESTINATION$suffix"
        RoutePlanFailure.UNKNOWN_FIX -> "UNKNOWN FIX$suffix"
        RoutePlanFailure.UNKNOWN_AIRWAY -> "UNKNOWN AIRWAY$suffix"
        RoutePlanFailure.INVALID_AIRWAY_ENTRY -> "INVALID AIRWAY ENTRY$suffix"
        RoutePlanFailure.INVALID_AIRWAY_EXIT -> "INVALID AIRWAY EXIT$suffix"
        RoutePlanFailure.EMPTY_ROUTE -> "EMPTY ROUTE"
        RoutePlanFailure.INVALID_CRUISE_SPEED -> "INVALID CRUISE SPEED"
    }
}

private fun format0(value: Double): String = String.format(Locale.US, "%.0f", value)
