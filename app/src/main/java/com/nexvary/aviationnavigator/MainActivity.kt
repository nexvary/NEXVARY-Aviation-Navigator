package com.nexvary.aviationnavigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                AviationNavigatorApp(onMapViewReady = { mapView = it })
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

private enum class AppSection(val label: String, val glyph: String) {
    HOME("HOME", "H"),
    MAP("MAP", "M"),
    PLAN("PLAN", "P"),
    RADAR("RADAR", "R"),
    TRAFFIC("TRAFFIC", "T")
}

@Composable
private fun AviationNavigatorApp(onMapViewReady: (MapView) -> Unit) {
    val provider = remember { AdsbLolProvider() }
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf(AppSection.HOME) }
    var tracks by remember { mutableStateOf<List<AircraftTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Connecting to live traffic…") }

    BackHandler(enabled = selected != AppSection.HOME) {
        selected = AppSection.HOME
    }

    fun refresh() {
        if (loading) return
        scope.launch {
            loading = true
            provider.fetch(
                TrafficQuery(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_RADIUS_NM)
            ).onSuccess {
                tracks = it
                status = "${it.size} aircraft · ADSB.lol · Cairo FIR view"
            }.onFailure {
                status = "Live traffic unavailable: ${it.message ?: "unknown error"}"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        containerColor = AppBlack,
        bottomBar = {
            NavigationBar(containerColor = PanelBlack) {
                AppSection.entries.forEach { section ->
                    NavigationBarItem(
                        selected = selected == section,
                        onClick = { selected = section },
                        icon = {
                            Text(
                                section.glyph,
                                color = if (selected == section) RoyalGold else MetallicSilver,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        label = {
                            Text(
                                section.label,
                                color = if (selected == section) Platinum else MetallicSilver
                            )
                        }
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
            StatusHeader(status, loading, ::refresh)
            when (selected) {
                AppSection.HOME -> DashboardScreen(Modifier.weight(1f), tracks) { selected = it }
                AppSection.MAP -> LiveMapScreen(Modifier.weight(1f), tracks, onMapViewReady)
                AppSection.PLAN -> FlightPlanScreen(Modifier.weight(1f))
                AppSection.RADAR -> RadarScreen(Modifier.weight(1f), tracks)
                AppSection.TRAFFIC -> TrafficScreen(Modifier.weight(1f), tracks)
            }
        }
    }
}

@Composable
private fun StatusHeader(status: String, loading: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "NEXVARY AVIATION NAVIGATOR",
                color = RoyalGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(status, color = Platinum, style = MaterialTheme.typography.bodySmall)
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = ElectricBlue,
                strokeWidth = 2.dp
            )
        } else {
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = Color.Black
                )
            ) { Text("REFRESH", fontWeight = FontWeight.Bold) }
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "AVIATION WORKSPACE",
            color = Platinum,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Live traffic, map, radar and flight planning in one mobile workspace.",
            color = MetallicSilver
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("TRACKED", tracks.size.toString(), Modifier.weight(1f))
            StatCard("AIRBORNE", airborne.toString(), Modifier.weight(1f))
            StatCard("GROUND", ground.toString(), Modifier.weight(1f))
        }

        if (highest != null || fastest != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PanelBlack),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LIVE HIGHLIGHTS", color = RoyalGold, fontWeight = FontWeight.Bold)
                    highest?.let {
                        Text("Highest: ${it.displayIdentity} · ${it.altitudeFeet ?: 0} ft", color = Platinum)
                    }
                    fastest?.let {
                        Text("Fastest: ${it.displayIdentity} · ${it.groundSpeedKnots ?: 0} kt", color = Platinum)
                    }
                }
            }
        }

        Text("QUICK ACCESS", color = RoyalGold, fontWeight = FontWeight.Bold)
        QuickAction("LIVE MAP", "MapLibre + live ADS-B aircraft") { navigate(AppSection.MAP) }
        QuickAction("FLIGHT PLAN", "Build and validate an ICAO route draft") { navigate(AppSection.PLAN) }
        QuickAction("RADAR", "250 NM situational traffic scope") { navigate(AppSection.RADAR) }
        QuickAction("TRAFFIC", "Inspect callsign, altitude and speed") { navigate(AppSection.TRAFFIC) }

        Card(
            colors = CardDefaults.cardColors(containerColor = PanelBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ABOUT", color = RoyalGold, fontWeight = FontWeight.Bold)
                Text("Version ${BuildConfig.VERSION_NAME}", color = Platinum)
                Text(
                    "Designed for planning, simulation and situational awareness. Not certified for primary navigation, ATC separation or collision avoidance.",
                    color = MetallicSilver
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = MetallicSilver, style = MaterialTheme.typography.labelSmall)
            Text(value, color = RoyalGold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickAction(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Platinum, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MetallicSilver, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Gunmetal, contentColor = Platinum)
            ) { Text("OPEN") }
        }
    }
}

@Composable
private fun LiveMapScreen(
    modifier: Modifier,
    tracks: List<AircraftTrack>,
    onMapViewReady: (MapView) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        MapLibreSurface(Modifier.fillMaxSize(), tracks, onMapViewReady)
        Card(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBlack.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("LIVE AIRSPACE", color = RoyalGold, fontWeight = FontWeight.Bold)
                Text("Radius $DEFAULT_RADIUS_NM NM", color = MetallicSilver)
                Text("Tracked ${tracks.size}", color = Platinum)
                Text("Live ADS-B aircraft plotted on MapLibre", color = ElectricBlue)
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
private fun FlightPlanScreen(modifier: Modifier) {
    var departure by rememberSaveable { mutableStateOf("HECA") }
    var destination by rememberSaveable { mutableStateOf("HESH") }
    var altitude by rememberSaveable { mutableStateOf("35000") }
    var route by rememberSaveable { mutableStateOf("DCT") }
    var result by rememberSaveable { mutableStateOf("Ready to validate a route draft") }
    var valid by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("FLIGHT PLANNER")
        Text(
            "Create a basic ICAO route draft. Navigation database, procedures and performance calculations will plug into this engine next.",
            color = MetallicSilver
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = departure,
                onValueChange = { departure = it.take(4) },
                label = { Text("Departure ICAO") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it.take(4) },
                label = { Text("Destination ICAO") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = altitude,
            onValueChange = { altitude = it.filter(Char::isDigit).take(5) },
            label = { Text("Cruise altitude ft") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = route,
            onValueChange = { route = it },
            label = { Text("Route / Airways") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Button(
            onClick = {
                val plan = FlightPlanDraft(
                    departure = departure,
                    destination = destination,
                    cruiseAltitudeFeet = altitude.toIntOrNull() ?: 0,
                    route = route
                )
                val errors = plan.validate()
                valid = errors.isEmpty()
                result = if (valid) plan.summary else errors.joinToString("\n")
            },
            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = Color.Black)
        ) {
            Text("VALIDATE PLAN", fontWeight = FontWeight.Bold)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = if (valid) SafePanel else PanelBlack),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(if (valid) "PLAN VALID" else "PLAN STATUS", color = if (valid) SafeGreen else RoyalGold, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(result, color = Platinum)
            }
        }

        FeatureCard("NEXT NAV LAYER", "Airport/runway database, waypoints, VOR/NDB and airways")
        FeatureCard("PROCEDURES", "SID / STAR / Approach selection")
        FeatureCard("PERFORMANCE", "Aircraft profile, fuel, time, TOC/TOD and vertical profile")
    }
}

@Composable
private fun RadarScreen(modifier: Modifier, tracks: List<AircraftTrack>) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        SectionTitle("LIVE RADAR · $DEFAULT_RADIUS_NM NM")
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = Color(0xFF050A08),
            shape = RoundedCornerShape(16.dp)
        ) {
            Canvas(Modifier.fillMaxSize().padding(18.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = minOf(size.width, size.height) * 0.46f
                val ringColor = Color(0xFF315845)
                val sweepColor = Color(0xFF6A88A0)

                repeat(4) { ring ->
                    drawCircle(
                        color = ringColor,
                        radius = maxRadius * (ring + 1) / 4f,
                        center = center,
                        style = Stroke(width = 1.2f)
                    )
                }
                drawLine(ringColor, Offset(center.x, center.y - maxRadius), Offset(center.x, center.y + maxRadius), 1f)
                drawLine(ringColor, Offset(center.x - maxRadius, center.y), Offset(center.x + maxRadius, center.y), 1f)
                drawLine(sweepColor, center, Offset(center.x + maxRadius * 0.78f, center.y - maxRadius * 0.62f), 2f)

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
            "Gold: airborne · Silver: ground · ${tracks.size} live tracks",
            color = MetallicSilver,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TrafficScreen(modifier: Modifier, tracks: List<AircraftTrack>) {
    Column(modifier = modifier.fillMaxSize()) {
        SectionTitle("LIVE TRAFFIC", Modifier.padding(16.dp))
        HorizontalDivider(color = Gunmetal)
        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No live tracks loaded", color = MetallicSilver)
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
            Text(track.displayIdentity, color = Platinum, fontWeight = FontWeight.Bold)
            Text(
                listOfNotNull(track.aircraftType, track.registration, track.icao24.uppercase()).joinToString(" · "),
                color = MetallicSilver,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("${track.altitudeFeet ?: 0} ft", color = RoyalGold)
            Text("${track.groundSpeedKnots ?: 0} kt", color = ElectricBlue)
        }
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = RoyalGold, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Platinum)
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = RoyalGold,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

private val AppBlack = Color(0xFF07090C)
private val PanelBlack = Color(0xFF111317)
private val Gunmetal = Color(0xFF2E3945)
private val MetallicSilver = Color(0xFF9E9B98)
private val Platinum = Color(0xFFF2F2F2)
private val RoyalGold = Color(0xFFD4AF37)
private val ElectricBlue = Color(0xFF6A88A0)
private val SafeGreen = Color(0xFF7FD49A)
private val SafePanel = Color(0xFF102219)

@Composable
private fun NexvaryAviationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RoyalGold,
            secondary = ElectricBlue,
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
