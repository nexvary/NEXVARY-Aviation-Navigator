package com.nexvary.aviationnavigator.domain

data class FlightPlanDraft(
    val departure: String,
    val destination: String,
    val cruiseAltitudeFeet: Int,
    val route: String = "DCT"
) {
    fun normalized(): FlightPlanDraft = copy(
        departure = departure.trim().uppercase(),
        destination = destination.trim().uppercase(),
        route = route.trim().uppercase().ifBlank { "DCT" }
    )

    fun validate(): List<String> {
        val draft = normalized()
        val errors = mutableListOf<String>()
        if (!ICAO_PATTERN.matches(draft.departure)) {
            errors += "Departure must be a 4-letter ICAO code"
        }
        if (!ICAO_PATTERN.matches(draft.destination)) {
            errors += "Destination must be a 4-letter ICAO code"
        }
        if (draft.departure == draft.destination && ICAO_PATTERN.matches(draft.departure)) {
            errors += "Departure and destination must be different"
        }
        if (draft.cruiseAltitudeFeet !in 1_000..60_000) {
            errors += "Cruise altitude must be between 1,000 and 60,000 ft"
        }
        return errors
    }

    val summary: String
        get() {
            val draft = normalized()
            return "${draft.departure} → ${draft.destination} · ${draft.route} · FL${draft.cruiseAltitudeFeet / 100}"
        }

    companion object {
        private val ICAO_PATTERN = Regex("^[A-Z]{4}$")
    }
}
