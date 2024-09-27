package dev.mtib.ketchup.bot.features.northernlights

import dev.mtib.ketchup.bot.features.northernlights.models.Forecast


object ScoreCalculator {

    private object boring {
        val geomagnetic = Boring(
            "NOAA Geomagnetic Activity Observation and Forecast",
            "Rationale: No G1 (Minor) or greater geomagnetic storms are expected."
        )
        val radiation = Boring(
            "NOAA Solar Radiation Activity Observation and Forecast",
            "Rationale: No S1 (Minor) or greater solar radiation storms are expected."
        )

        val all = listOf(geomagnetic, radiation)

        data class Boring(
            val title: String,
            val needle: String,
        )
    }

    fun score(forecast: Forecast): Score {
        return Score(
            geomagnetic = forecast.sections.find { it.title == boring.geomagnetic.title }?.content?.contains(boring.geomagnetic.needle) == false,
            radiation = forecast.sections.find { it.title == boring.radiation.title }?.content?.contains(boring.radiation.needle) == false,
        )
    }

    data class Score(
        val geomagnetic: Boolean,
        val radiation: Boolean,
    ) {
        val interesting: Boolean
            get() = geomagnetic && radiation
    }
}