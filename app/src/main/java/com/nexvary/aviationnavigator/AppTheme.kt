package com.nexvary.aviationnavigator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal val AppBlack = Color(0xFF02050B)
internal val DeepNavy = Color(0xFF050B17)
internal val PanelBlack = Color(0xFF08111F)
internal val PanelRaised = Color(0xFF0C1A2D)
internal val RadarBlack = Color(0xFF010711)
internal val Gunmetal = Color(0xFF26364C)
internal val MetallicSilver = Color(0xFFAEBBCB)
internal val Platinum = Color(0xFFF4F8FC)
internal val BrandSilver = Color(0xFFD8E2EE)
internal val ElectricBlue = Color(0xFF00A8FF)
internal val CobaltBlue = Color(0xFF006BFF)
internal val CyanBlue = Color(0xFF25D8FF)
internal val NeonGreen = Color(0xFF32E6A1)
internal val NeonViolet = Color(0xFF6E8CFF)
internal val SafePanel = Color(0xFF07231C)
internal val RoyalGold = BrandSilver
internal val AmberGold = CyanBlue
internal val NexvaryPanelBrush = Brush.linearGradient(listOf(PanelRaised, PanelBlack, DeepNavy))

@Composable
internal fun NexvaryAviationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ElectricBlue,
            secondary = BrandSilver,
            tertiary = CyanBlue,
            background = AppBlack,
            surface = PanelBlack,
            surfaceVariant = PanelRaised,
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = Platinum,
            onSurface = Platinum
        ),
        content = content
    )
}

@Composable
internal fun PageHeader(title: String, accent: Color, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(modifier = Modifier.testTag("back_button"), onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = uiText(UiText.BACK), tint = accent)
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
internal fun AccentCard(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.background(NexvaryPanelBrush).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
internal fun FeatureCard(title: UiText, subtitle: UiText, accent: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.background(NexvaryPanelBrush).padding(16.dp)) {
            Text(uiText(title), modifier = Modifier.fillMaxWidth(), color = accent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
            Text(uiText(subtitle), modifier = Modifier.fillMaxWidth(), color = Platinum, textAlign = TextAlign.Start)
        }
    }
}

internal fun sectionAccent(section: AppSection): Color = when (section) {
    AppSection.HOME -> BrandSilver
    AppSection.MAP -> ElectricBlue
    AppSection.PLAN -> NeonViolet
    AppSection.RADAR -> NeonGreen
    AppSection.TRAFFIC -> CyanBlue
    AppSection.ABOUT -> CobaltBlue
}
