package com.nexvary.aviationnavigator

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.nexvary.aviationnavigator.domain.TrafficQuery
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
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
                AviationNavigatorApp(
                    onMapViewReady = { mapView = it }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        mapView = null
        super.onDestroy()
    }
}

private enum class AppSection(val label: String) {
    MAP("MAP"),
    PLAN("PLAN"),
    RADAR("RADAR"),
    TRAFFIC("TRAFFIC")
}

@Composable
private fun AviationNavigatorApp(onMapViewReady: (MapView) -> Unit) {
    val provider = remember { AdsbLolProvider() }
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf(AppSection.MAP) }
    var tracks by remember { mutableStateOf<List<AircraftTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Connecting to live traffic…") }

    fun refresh() {
        if (loading) return
        scope.launch {
            loading = true
            val result = provider.fetch(
                TrafficQuery(
                    centerLatitude = DEFAULT_LATITUDE,
                    centerLongitude = DEFAULT_LONGITUDE,
                    radiusNm = DEFAULT_RADIUS_NM
                )
            )
            result.onSuccess {
                tracks = it
                status = "${it.size} aircraft · ADSB.lol"
            }.onFailure {
                status = "Live traffic unavailable: ${it.message ?: "unknown error"}"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

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
                                text = section.label.take(1),
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
            StatusHeader(
                status = status,
                loading = loading,
                onRefresh = ::refresh
            )

            when (selected) {
                AppSection.MAP -> LiveMapScreen(
                    modifier = Modifier.weight(1f),
                    tracks = tracks,
                    onMapViewReady = onMapViewReady
                )
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
                text = "NEXVARY AVIATION NAVIGATOR",
                color = RoyalGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status,
                color = Platinum,
                style = MaterialTheme.typography.bodySmall
            )
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
            ) {
                Text("REFRESH", fontWeight = FontWeight.Bold)
            }
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
        MapLibreSurface(
            modifier = Modifier.fillMaxSize(),
            onMapViewReady = onMapViewReady
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBlack.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("LIVE AIRSPACE", color = RoyalGold, fontWeight = FontWeight.Bold)
                Text("Radius $DEFAULT_RADIUS_NM NM", color = MetallicSilver)
                Text("Tracked ${tracks.size}", color = Platinum)
                Text("Aircraft markers are the next map layer", color = ElectricBlue)
            }
        }
    }
}

@Composable
private fun MapLibreSurface(
    modifier: Modifier,
    onMapViewReady: (MapView) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle("https://demotiles.maplibre.org/style.json")
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
        modifier = modifier
    )
}

@Composable
private fun FlightPlanScreen(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("FLIGHT PLANNER")
        FeatureCard("ROUTE", "Departure → Airways → Arrival")
        FeatureCard("PROCEDURES", "SID / STAR / Approach data layer")
        FeatureCard("VERTICAL PROFILE", "TOC / Cruise / TOD / altitude constraints")
        FeatureCard("PERFORMANCE", "Aircraft profile, time and fuel model")
        Text(
            "Planner engine foundation is reserved for the next implementation slice; live tracking is already wired independently so route planning will not depend on a traffic provider.",
            color = MetallicSilver
        )
    }
}

@Composable
private fun RadarScreen(modifier: Modifier, tracks: List<AircraftTrack>) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionTitle("LIVE RADAR · $DEFAULT_RADIUS_NM NM")
        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Color(0xFF050A08),
            shape = RoundedCornerShape(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
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

                    if (x in (center.x - maxRadius)..(center.x + maxRadius) &&
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
        text = text,
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
