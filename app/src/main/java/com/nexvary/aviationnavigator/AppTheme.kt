package com.nexvary.aviationnavigator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal val AppBlack = Color(0xFF05070B)
internal val PanelBlack = Color(0xFF0D1420)
internal val RadarBlack = Color(0xFF030A0E)
internal val Gunmetal = Color(0xFF253142)
internal val MetallicSilver = Color(0xFFAAB4C4)
internal val Platinum = Color(0xFFF4F7FB)
internal val RoyalGold = Color(0xFFD6A84B)
internal val AmberGold = Color(0xFFF0A43A)
internal val ElectricBlue = Color(0xFF1EA8FF)
internal val CyanBlue = Color(0xFF39D0FF)
internal val NeonGreen = Color(0xFF36E88D)
internal val NeonViolet = Color(0xFF8B5CF6)
internal val SafePanel = Color(0xFF0B2118)

@Composable
internal fun NexvaryAviationTheme(content: @Composable () -> Unit) {
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

@Composable
internal fun PageHeader(title: String, accent: Color, onBack: () -> Unit) {
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
internal fun AccentCard(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
internal fun FeatureCard(title: UiText, subtitle: UiText, accent: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
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

internal fun sectionAccent(section: AppSection): Color = when (section) {
    AppSection.HOME -> RoyalGold
    AppSection.MAP -> ElectricBlue
    AppSection.PLAN -> NeonViolet
    AppSection.RADAR -> NeonGreen
    AppSection.TRAFFIC -> CyanBlue
    AppSection.ABOUT -> AmberGold
}
