package com.nexvary.aviationnavigator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexvary.aviationnavigator.data.AdsbLolProvider
import com.nexvary.aviationnavigator.domain.AircraftTrack
import com.nexvary.aviationnavigator.domain.FlightPlanDraft
import com.nexvary.aviationnavigator.domain.TrafficQuery
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlin.math.cos

private const val DEFAULT_LATITUDE = 30.0444
private const val DEFAULT_LONGITUDE = 31.2357
private const val DEFAULT_RADIUS_NM = 250

class MainActivity : ComponentActivity() {
    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapLibre.getInstance(this)
        setContent {
            NexvaryAviationTheme {
                LocalizedLayout {
                    AviationNavigatorApp(onMapViewReady = { mapView = it })
                }
            }
        }
    }

    override fun onStart() { super.onStart(); mapView?.onStart() }
    override fun onResume() { super.onResume(); mapView?.onResume() }
    override fun onPause() { mapView?.onPause(); super.onPause() }
    override fun onStop() { mapView?.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView?.onSaveInstanceState(outState) }
    override fun onDestroy() { mapView?.onDestroy(); mapView = null; super.onDestroy() }
}

private enum class AppSection(val text: UiText, val icon: ImageVector) {
    HOME(UiText.HOME, Icons.Outlined.Home),
    MAP(UiText.MAP, Icons.Outlined.Map),
    PLAN(UiText.PLAN, Icons.Outlined.Flight),
    RADAR(UiText.RADAR, Icons.Outlined.MyLocation),
    TRAFFIC(UiText.TRAFFIC, Icons.Outlined.List),
    ABOUT(UiText.ABOUT, Icons.Outlined.Info)
}

private sealed interface TrafficStatus {
    data object Connecting : TrafficStatus
    data class Loaded(val count: Int) : TrafficStatus
    data class Failed(val reason: String) : TrafficStatus
}

@Composable
private fun AviationNavigatorApp(onMapViewReady: (MapView) -> Unit) {
    val provider = remember { AdsbLolProvider() }
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf(AppSection.HOME) }
    var tracks by remember { mutableStateOf<List<AircraftTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var trafficStatus by remember { mutableStateOf<TrafficStatus>(TrafficStatus.Connecting) }

    BackHandler(enabled = selected != AppSection.HOME) {
        selected = AppSection.HOME
    }

    fun refresh() {
        if (loading) return
        scope.launch {
            loading = true
            trafficStatus = TrafficStatus.Connecting
            provider.fetch(
                TrafficQuery(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_RADIUS_NM)
            ).onSuccess {
                tracks = it
                trafficStatus = TrafficStatus.Loaded(it.size)
            }.onFailure {
                trafficStatus = TrafficStatus.Failed(it.message ?: "unknown")
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val statusText = when (val state = trafficStatus) {
        TrafficStatus.Connecting -> uiText(UiText.STATUS_CONNECTING)
        is TrafficStatus.Loaded -> uiText(UiText.STATUS_AIRCRAFT, state.count)
        is TrafficStatus.Failed -> uiText(UiText.STATUS_UNAVAILABLE, state.reason)
    }

    Scaffold(
        modifier = Modifier.testTag("app_root"),
        containerColor = AppBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = PanelBlack, tonalElevation = 0.dp) {
                AppSection.entries.forEach { section ->
                    val accent = sectionAccent(section)
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav_${section.name}"),
                        selected = selected == section,
                        onClick = { selected = section },
                        icon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = uiText(section.text),
                                tint = if (selected == section) accent else MetallicSilver,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                uiText(section.text),
                                color = if (selected == section) Platinum else MetallicSilver,
                                maxLines = 1
                            )
                        },
                        alwaysShowLabel = selected == section
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBlack)
        ) {
            StatusHeader(statusText, loading, ::refresh)
            when (selected) {
                AppSection.HOME -> DashboardScreen(Modifier.weight(1f), tracks) { selected = it }
                AppSection.MAP -> LiveMapScreen(Modifier.weight(1f), tracks, { selected = AppSection.HOME }, onMapViewReady)
                AppSection.PLAN -> FlightPlanScreen(Modifier.weight(1f)) { selected = AppSection.HOME }
                AppSection.RADAR -> RadarScreen(Modifier.weight(1f), tracks) { selected = AppSection.HOME }
                AppSection.TRAFFIC -> TrafficScreen(Modifier.weight(1f), tracks) { selected = AppSection.HOME }
                AppSection.ABOUT -> AboutScreen(Modifier.weight(1f)) { selected = AppSection.HOME }
            }
        }
    }
}

@Composable
private fun StatusHeader(status: String, loading: Boolean, onRefresh: () -> Unit) {
    Surface(
        color = PanelBlack,
        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "NEXVARY AVIATION NAVIGATOR",
                    modifier = Modifier.fillMaxWidth(),
                    color = RoyalGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Text(
                    status,
                    modifier = Modifier.fillMaxWidth(),
                    color = Platinum,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start
                )
            }
            Spacer(Modifier.width(8.dp))
            if (loading) {
                CircularProgressIndicator(
                    progress = { 0.72f },
                    modifier = Modifier.size(30.dp),
                    color = NeonGreen,
                    trackColor = Gunmetal,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    modifier = Modifier.testTag("refresh_button"),
                    onClick = onRefresh
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = uiText(UiText.REFRESH),
                        tint = ElectricBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    modifier: Modifier,
    tracks: List<AircraftTrack>,
    navigate: (AppSection) -> Unit
) {
    val airborne = tracks.count { !it.onGround }
    val ground = tracks.count { it.onGround }
    val highest = tracks.maxByOrNull { it.altitudeFeet ?: Int.MIN_VALUE }
    val fastest = tracks.maxByOrNull { it.groundSpeedKnots ?: Int.MIN_VALUE }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("page_HOME")
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            uiText(UiText.WORKSPACE),
            modifier = Modifier.fillMaxWidth(),
            color = Platinum,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
        Text(
            uiText(UiText.WORKSPACE_SUBTITLE),
            modifier = Modifier.fillMaxWidth(),
            color = MetallicSilver,
            textAlign = TextAlign.Start
        )

        ResponsiveStats(tracks.size, airborne, ground)

        if (highest != null || fastest != null) {
            AccentCard(accent = NeonViolet) {
                Text(
                    uiText(UiText.LIVE_HIGHLIGHTS),
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonViolet,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                highest?.let {
                    Text(
                        uiText(UiText.HIGHEST, it.displayIdentity, it.altitudeFeet ?: 0),
                        modifier = Modifier.fillMaxWidth(),
                        color = Platinum,
                        textAlign = TextAlign.Start
                    )
                }
                fastest?.let {
                    Text(
                        uiText(UiText.FASTEST, it.displayIdentity, it.groundSpeedKnots ?: 0),
                        modifier = Modifier.fillMaxWidth(),
                        color = Platinum,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Text(
            uiText(UiText.QUICK_ACCESS),
            modifier = Modifier.fillMaxWidth(),
            color = RoyalGold,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
        QuickAction(AppSection.MAP, UiText.LIVE_MAP, UiText.LIVE_MAP_SUB, "quick_MAP") { navigate(AppSection.MAP) }
        QuickAction(AppSection.PLAN, UiText.FLIGHT_PLAN, UiText.FLIGHT_PLAN_SUB, "quick_PLAN") { navigate(AppSection.PLAN) }
        QuickAction(AppSection.RADAR, UiText.RADAR, UiText.RADAR_SUB, "quick_RADAR") { navigate(AppSection.RADAR) }
        QuickAction(AppSection.TRAFFIC, UiText.TRAFFIC, UiText.TRAFFIC_SUB, "quick_TRAFFIC") { navigate(AppSection.TRAFFIC) }
        QuickAction(AppSection.ABOUT, UiText.ABOUT, UiText.ABOUT_SUB, "quick_ABOUT") { navigate(AppSection.ABOUT) }
    }
}

@Composable
private fun ResponsiveStats(tracked: Int, airborne: Int, ground: Int) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 420.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(UiText.TRACKED, tracked.toString(), ElectricBlue, Modifier.fillMaxWidth())
                StatCard(UiText.AIRBORNE, airborne.toString(), NeonGreen, Modifier.fillMaxWidth())
                StatCard(UiText.GROUND, ground.toString(), NeonViolet, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(UiText.TRACKED, tracked.toString(), ElectricBlue, Modifier.weight(1f))
                StatCard(UiText.AIRBORNE, airborne.toString(), NeonGreen, Modifier.weight(1f))
                StatCard(UiText.GROUND, ground.toString(), NeonViolet, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(label: UiText, value: String, accent: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.62f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                uiText(label),
                modifier = Modifier.fillMaxWidth(),
                color = MetallicSilver,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Start
            )
            Text(
                value,
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun QuickAction(
    section: AppSection,
    title: UiText,
    subtitle: UiText,
    tag: String,
    onClick: () -> Unit
) {
    val accent = sectionAccent(section)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                section.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    uiText(title),
                    modifier = Modifier.fillMaxWidth(),
                    color = Platinum,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Text(
                    uiText(subtitle),
                    modifier = Modifier.fillMaxWidth(),
                    color = MetallicSilver,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start
                )
            }
            Text(uiText(UiText.OPEN), color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PageHeader(title: String, accent: Color, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.testTag("back_button"),
            onClick = onBack
        ) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = uiText(UiText.BACK),
                tint = accent
            )
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = accent,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun LiveMapScreen(
    modifier: Modifier,
    tracks: List<AircraftTrack>,
    onBack: () -> Unit,
    onMapViewReady: (MapView) -> Unit
) {
    Box(modifier = modifier.fillMaxSize().testTag("page_MAP")) {
        MapLibreSurface(Modifier.fillMaxSize(), tracks, onMapViewReady)
        Card(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBlack.copy(alpha = 0.95f)),
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.72f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(10.dp)) {
                PageHeader(uiText(UiText.LIVE_AIRSPACE), ElectricBlue, onBack)
                Text(
                    uiText(UiText.RADIUS, DEFAULT_RADIUS_NM),
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
                Text(
                    uiText(UiText.LIVE_MAP_CAPTION),
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonGreen,
                    style = MaterialTheme.typography.bodySmall,
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
    onMapViewReady: (MapView) -> Unit
) {
    val context = LocalContext.current
    val latestTracks = rememberUpdatedState(tracks)
    val mapHolder = remember { arrayOfNulls<MapLibreMap>(1) }
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                mapHolder[0] = map
                map.setStyle("https://demotiles.maplibre.org/style.json") { style ->
                    installAircraftLayer(style, latestTracks.value)
                }
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
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
            mapHolder[0]?.let { map -> updateAircraftLayer(map, tracks) }
        }
    )
}

@Composable
private fun FlightPlanScreen(modifier: Modifier, onBack: () -> Unit) {
    var departure by rememberSaveable { mutableStateOf("HECA") }
    var destination by rememberSaveable { mutableStateOf("HESH") }
    var altitude by rememberSaveable { mutableStateOf("35000") }
    var route by rememberSaveable { mutableStateOf("DCT") }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var valid by rememberSaveable { mutableStateOf(false) }

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
            onValueChange = { departure = it.take(4) },
            label = { Text(uiText(UiText.DEPARTURE_ICAO)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it.take(4) },
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
            onValueChange = { route = it },
            label = { Text(uiText(UiText.ROUTE_AIRWAYS)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Button(
            modifier = Modifier.fillMaxWidth().testTag("validate_plan_button"),
            onClick = {
                val plan = FlightPlanDraft(
                    departure = departure,
                    destination = destination,
                    cruiseAltitudeFeet = altitude.toIntOrNull() ?: 0,
                    route = route
                )
                val errors = plan.validate()
                valid = errors.isEmpty()
                result = if (valid) plan.summary else errors.joinToString("\n") { localizedValidationError(it) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = Color.Black)
        ) {
            Text(uiText(UiText.VALIDATE_PLAN), fontWeight = FontWeight.Bold)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = if (valid) SafePanel else PanelBlack),
            border = BorderStroke(1.dp, if (valid) NeonGreen else NeonViolet.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (valid) uiText(UiText.PLAN_VALID) else uiText(UiText.PLAN_STATUS),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (valid) NeonGreen else RoyalGold,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    result ?: uiText(UiText.READY_VALIDATE),
                    modifier = Modifier.fillMaxWidth(),
                    color = Platinum,
                    textAlign = TextAlign.Start
                )
            }
        }

        FeatureCard(UiText.NEXT_NAV_LAYER, UiText.NEXT_NAV_LAYER_SUB, ElectricBlue)
        FeatureCard(UiText.PROCEDURES, UiText.PROCEDURES_SUB, NeonViolet)
        FeatureCard(UiText.PERFORMANCE, UiText.PERFORMANCE_SUB, NeonGreen)
    }
}

@Composable
private fun localizedValidationError(error: String): String = when (error) {
    "Departure must be a 4-letter ICAO code" -> uiText(UiText.ERROR_DEPARTURE_ICAO)
    "Destination must be a 4-letter ICAO code" -> uiText(UiText.ERROR_DESTINATION_ICAO)
    "Departure and destination must be different" -> uiText(UiText.ERROR_SAME_AIRPORT)
    "Cruise altitude must be between 1,000 and 60,000 ft" -> uiText(UiText.ERROR_ALTITUDE)
    else -> error
}

@Composable
private fun RadarScreen(modifier: Modifier, tracks: List<AircraftTrack>, onBack: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().testTag("page_RADAR").padding(16.dp)
    ) {
        PageHeader(uiText(UiText.LIVE_RADAR, DEFAULT_RADIUS_NM), NeonGreen, onBack)
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = RadarBlack,
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.68f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Canvas(Modifier.fillMaxSize().padding(18.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = minOf(size.width, size.height) * 0.46f

                repeat(4) { ring ->
                    drawCircle(
                        color = NeonGreen.copy(alpha = 0.42f),
                        radius = maxRadius * (ring + 1) / 4f,
                        center = center,
                        style = Stroke(width = 1.2f)
                    )
                }
                drawLine(NeonGreen.copy(alpha = 0.38f), Offset(center.x, center.y - maxRadius), Offset(center.x, center.y + maxRadius), 1f)
                drawLine(NeonGreen.copy(alpha = 0.38f), Offset(center.x - maxRadius, center.y), Offset(center.x + maxRadius, center.y), 1f)
                drawLine(ElectricBlue, center, Offset(center.x + maxRadius * 0.78f, center.y - maxRadius * 0.62f), 2f)

                val longitudeScale = cos(Math.toRadians(DEFAULT_LATITUDE))
                tracks.take(150).forEach { aircraft ->
                    val northNm = (aircraft.latitude - DEFAULT_LATITUDE) * 60.0
                    val eastNm = (aircraft.longitude - DEFAULT_LONGITUDE) * 60.0 * longitudeScale
                    val x = center.x + (eastNm / DEFAULT_RADIUS_NM * maxRadius).toFloat()
                    val y = center.y - (northNm / DEFAULT_RADIUS_NM * maxRadius).toFloat()
                    if (
                        x in (center.x - maxRadius)..(center.x + maxRadius) &&
                        y in (center.y - maxRadius)..(center.y + maxRadius)
                    ) {
                        drawCircle(
                            color = if (aircraft.onGround) MetallicSilver else RoyalGold,
                            radius = if (aircraft.onGround) 3.5f else 5.5f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            uiText(UiText.RADAR_LEGEND, tracks.size),
            modifier = Modifier.fillMaxWidth(),
            color = MetallicSilver,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun TrafficScreen(modifier: Modifier, tracks: List<AircraftTrack>, onBack: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().testTag("page_TRAFFIC")) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            PageHeader(uiText(UiText.LIVE_TRAFFIC), ElectricBlue, onBack)
        }
        HorizontalDivider(color = Gunmetal)
        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    uiText(UiText.NO_TRACKS),
                    color = MetallicSilver,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(tracks.take(250), key = { it.icao24 }) { aircraft ->
                    AircraftRow(aircraft)
                    HorizontalDivider(color = Gunmetal.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
private fun AircraftRow(track: AircraftTrack) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.displayIdentity,
                modifier = Modifier.fillMaxWidth(),
                color = Platinum,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                listOfNotNull(track.aircraftType, track.registration, track.icao24.uppercase()).joinToString(" · "),
                modifier = Modifier.fillMaxWidth(),
                color = MetallicSilver,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(uiText(UiText.ALTITUDE_FT, track.altitudeFeet ?: 0), color = RoyalGold)
            Text(uiText(UiText.SPEED_KT, track.groundSpeedKnots ?: 0), color = ElectricBlue)
        }
    }
}

@Composable
private fun AboutScreen(modifier: Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("page_ABOUT")
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PageHeader(uiText(UiText.ABOUT_TITLE), RoyalGold, onBack)

        AccentCard(RoyalGold) {
            Text(
                uiText(UiText.VERSION, BuildConfig.VERSION_NAME),
                modifier = Modifier.fillMaxWidth(),
                color = Platinum,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                uiText(UiText.ABOUT_DESCRIPTION),
                modifier = Modifier.fillMaxWidth(),
                color = MetallicSilver,
                textAlign = TextAlign.Start
            )
            Text(
                uiText(UiText.SAFETY_NOTICE),
                modifier = Modifier.fillMaxWidth(),
                color = AmberGold,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )
        }

        AccentCard(NeonViolet) {
            Text(
                uiText(UiText.SUPPORTED_LANGUAGES),
                modifier = Modifier.fillMaxWidth(),
                color = NeonViolet,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                "العربية · English · Türkçe · Español · Deutsch · Italiano · Français · اردو · فارسی · Русский",
                modifier = Modifier.fillMaxWidth(),
                color = Platinum,
                textAlign = TextAlign.Start
            )
        }

        Text(
            uiText(UiText.SOCIAL_MEDIA),
            modifier = Modifier.fillMaxWidth(),
            color = RoyalGold,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
        SocialLinkButton(UiText.WEBSITE, "nexvary.com", ExternalLinks.WEBSITE, ElectricBlue, "external_website", context)
        SocialLinkButton(UiText.FACEBOOK, "facebook.com", ExternalLinks.FACEBOOK, NeonViolet, "external_facebook", context)
        SocialLinkButton(UiText.EMAIL, "info@nexvary.com", ExternalLinks.EMAIL, NeonGreen, "external_email", context)
        SocialLinkButton(UiText.YOUTUBE, "youtube.com/@NexvaryInc", ExternalLinks.YOUTUBE, AmberGold, "external_youtube", context)
        SocialLinkButton(UiText.X_TWITTER, "x.com/Nexvary", ExternalLinks.X, Platinum, "external_x", context)
    }
}

@Composable
private fun SocialLinkButton(
    label: UiText,
    display: String,
    uri: String,
    accent: Color,
    tag: String,
    context: Context
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        onClick = { openExternalLink(context, uri) },
        border = BorderStroke(1.dp, accent.copy(alpha = 0.72f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                uiText(label),
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                display,
                modifier = Modifier.fillMaxWidth(),
                color = MetallicSilver,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )
        }
    }
}

private fun openExternalLink(context: Context, uri: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }
}

@Composable
private fun FeatureCard(title: UiText, subtitle: UiText, accent: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                uiText(title),
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                uiText(subtitle),
                modifier = Modifier.fillMaxWidth(),
                color = Platinum,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun AccentCard(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

private fun sectionAccent(section: AppSection): Color = when (section) {
    AppSection.HOME -> RoyalGold
    AppSection.MAP -> ElectricBlue
    AppSection.PLAN -> NeonViolet
    AppSection.RADAR -> NeonGreen
    AppSection.TRAFFIC -> CyanBlue
    AppSection.ABOUT -> AmberGold
}

private val AppBlack = Color(0xFF05070B)
private val PanelBlack = Color(0xFF0D1420)
private val RadarBlack = Color(0xFF030A0E)
private val Gunmetal = Color(0xFF253142)
private val MetallicSilver = Color(0xFFAAB4C4)
private val Platinum = Color(0xFFF4F7FB)
private val RoyalGold = Color(0xFFD6A84B)
private val AmberGold = Color(0xFFF0A43A)
private val ElectricBlue = Color(0xFF1EA8FF)
private val CyanBlue = Color(0xFF39D0FF)
private val NeonGreen = Color(0xFF36E88D)
private val NeonViolet = Color(0xFF8B5CF6)
private val SafePanel = Color(0xFF0B2118)

@Composable
private fun NexvaryAviationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RoyalGold,
            secondary = ElectricBlue,
            tertiary = NeonViolet,
            background = AppBlack,
            surface = PanelBlack,
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = Platinum,
            onSurface = Platinum
        ),
        content = content
    )
}
