package com.nexvary.aviationnavigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexvary.aviationnavigator.data.AdsbLolProvider
import com.nexvary.aviationnavigator.domain.AircraftTrack
import com.nexvary.aviationnavigator.domain.PreparedFlightPlan
import com.nexvary.aviationnavigator.domain.TrafficQuery
import kotlinx.coroutines.launch
import org.maplibre.android.maps.MapView

private const val DEFAULT_LATITUDE = 30.0444
private const val DEFAULT_LONGITUDE = 31.2357
private const val DEFAULT_RADIUS_NM = 250

class MainActivity : ComponentActivity() {
    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }
    override fun onDestroy() { mapView?.onDestroy(); mapView = null; super.onDestroy() }
}

internal enum class AppSection(val text: UiText, val icon: ImageVector) {
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
    var preparedPlan by remember { mutableStateOf<PreparedFlightPlan?>(null) }
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
                AppSection.MAP -> LiveMapScreen(
                    modifier = Modifier.weight(1f),
                    tracks = tracks,
                    preparedPlan = preparedPlan,
                    onBack = { selected = AppSection.HOME },
                    onMapViewReady = onMapViewReady
                )
                AppSection.PLAN -> FlightPlanScreen(
                    modifier = Modifier.weight(1f),
                    currentPlan = preparedPlan,
                    onPlanPrepared = { preparedPlan = it },
                    onOpenMap = { selected = AppSection.MAP },
                    onBack = { selected = AppSection.HOME }
                )
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "NEXVARY AVIATION NAVIGATOR",
                    modifier = Modifier.fillMaxWidth(),
                    color = RoyalGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )
                Text(
                    status,
                    modifier = Modifier.fillMaxWidth(),
                    color = Platinum,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start,
                    maxLines = 1
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start
        )

        ResponsiveStats(tracks.size, airborne, ground)

        AccentCard(NeonViolet) {
            Text(
                uiText(UiText.LIVE_HIGHLIGHTS),
                modifier = Modifier.fillMaxWidth(),
                color = NeonViolet,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                highest?.let { uiText(UiText.HIGHEST, it.displayIdentity, it.altitudeFeet ?: 0) } ?: "—",
                modifier = Modifier.fillMaxWidth(),
                color = Platinum,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start,
                maxLines = 1
            )
            Text(
                fastest?.let { uiText(UiText.FASTEST, it.displayIdentity, it.groundSpeedKnots ?: 0) } ?: "—",
                modifier = Modifier.fillMaxWidth(),
                color = Platinum,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start,
                maxLines = 1
            )
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
        modifier = Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                section.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    uiText(title),
                    modifier = Modifier.fillMaxWidth(),
                    color = Platinum,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )
                Text(
                    uiText(subtitle),
                    modifier = Modifier.fillMaxWidth(),
                    color = MetallicSilver,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )
            }
            Text(
                uiText(UiText.OPEN),
                color = accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
