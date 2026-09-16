package com.nexvary.aviationnavigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

enum class UiText {
    HOME, MAP, PLAN, RADAR, TRAFFIC, ABOUT, BACK, REFRESH, STATUS_CONNECTING, STATUS_AIRCRAFT, STATUS_UNAVAILABLE, WORKSPACE, WORKSPACE_SUBTITLE, TRACKED, AIRBORNE, GROUND, LIVE_HIGHLIGHTS, HIGHEST, FASTEST, QUICK_ACCESS, LIVE_MAP, LIVE_MAP_SUB, FLIGHT_PLAN, FLIGHT_PLAN_SUB, RADAR_SUB, TRAFFIC_SUB, ABOUT_SUB, OPEN, LIVE_AIRSPACE, RADIUS, TRACKED_COUNT, LIVE_MAP_CAPTION, FLIGHT_PLANNER, FLIGHT_PLANNER_INTRO, DEPARTURE_ICAO, DESTINATION_ICAO, CRUISE_ALTITUDE, ROUTE_AIRWAYS, VALIDATE_PLAN, PLAN_VALID, PLAN_STATUS, READY_VALIDATE, NEXT_NAV_LAYER, NEXT_NAV_LAYER_SUB, PROCEDURES, PROCEDURES_SUB, PERFORMANCE, PERFORMANCE_SUB, LIVE_RADAR, RADAR_LEGEND, LIVE_TRAFFIC, NO_TRACKS, ABOUT_TITLE, VERSION, ABOUT_DESCRIPTION, SAFETY_NOTICE, SOCIAL_MEDIA, WEBSITE, FACEBOOK, EMAIL, YOUTUBE, X_TWITTER, SUPPORTED_LANGUAGES, ALTITUDE_FT, SPEED_KT, ERROR_DEPARTURE_ICAO, ERROR_DESTINATION_ICAO, ERROR_SAME_AIRPORT, ERROR_ALTITUDE
}

val supportedLanguageCodes: Set<String> = setOf("en", "ar", "tr", "es", "de", "it", "fr", "ur", "fa", "ru")

internal fun isRtlLanguageCode(language: String): Boolean =
    language.lowercase(Locale.ROOT) in setOf("ar", "fa", "ur")

private val translations: Map<String, List<String>> = mapOf(
    "en" to EN_STRINGS,
    "ar" to AR_STRINGS,
    "tr" to TR_STRINGS,
    "es" to ES_STRINGS,
    "de" to DE_STRINGS,
    "it" to IT_STRINGS,
    "fr" to FR_STRINGS,
    "ur" to UR_STRINGS,
    "fa" to FA_STRINGS,
    "ru" to RU_STRINGS
)

@Composable
fun uiText(key: UiText, vararg args: Any): String {
    val locale = LocalConfiguration.current.locales[0]
    val language = locale.language.lowercase(Locale.ROOT)
    val selected = translations[language] ?: EN_STRINGS
    val template = selected.getOrElse(key.ordinal) { EN_STRINGS[key.ordinal] }
    return if (args.isEmpty()) template else String.format(locale, template, *args)
}

@Composable
fun LocalizedLayout(content: @Composable () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val direction = if (isRtlLanguageCode(locale.language)) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction, content = content)
}
