package com.nexvary.aviationnavigator

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexvary.aviationnavigator.domain.AircraftTrack
import kotlin.math.cos

private const val RADAR_LATITUDE = 30.0444
private const val RADAR_LONGITUDE = 31.2357
private const val RADAR_RADIUS_NM = 250

@Composable
internal fun RadarScreen(modifier: Modifier, tracks: List<AircraftTrack>, onBack: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().testTag("page_RADAR").padding(16.dp)
    ) {
        PageHeader(uiText(UiText.LIVE_RADAR, RADAR_RADIUS_NM), NeonGreen, onBack)
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
                drawLine(
                    NeonGreen.copy(alpha = 0.38f),
                    Offset(center.x, center.y - maxRadius),
                    Offset(center.x, center.y + maxRadius),
                    1f
                )
                drawLine(
                    NeonGreen.copy(alpha = 0.38f),
                    Offset(center.x - maxRadius, center.y),
                    Offset(center.x + maxRadius, center.y),
                    1f
                )
                drawLine(
                    ElectricBlue,
                    center,
                    Offset(center.x + maxRadius * 0.78f, center.y - maxRadius * 0.62f),
                    2f
                )

                val longitudeScale = cos(Math.toRadians(RADAR_LATITUDE))
                tracks.take(150).forEach { aircraft ->
                    val northNm = (aircraft.latitude - RADAR_LATITUDE) * 60.0
                    val eastNm = (aircraft.longitude - RADAR_LONGITUDE) * 60.0 * longitudeScale
                    val x = center.x + (eastNm / RADAR_RADIUS_NM * maxRadius).toFloat()
                    val y = center.y - (northNm / RADAR_RADIUS_NM * maxRadius).toFloat()
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
            textAlign = TextAlign.Start
        )
    }
}

@Composable
internal fun TrafficScreen(modifier: Modifier, tracks: List<AircraftTrack>, onBack: () -> Unit) {
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
internal fun AboutScreen(modifier: Modifier, onBack: () -> Unit) {
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
    accent: androidx.compose.ui.graphics.Color,
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
