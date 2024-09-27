package dev.mtib.ketchup.bot.features.northernlights

import dev.mtib.ketchup.bot.features.northernlights.models.Forecast
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreCalculatorTest {

    @Test
    fun `empty forecast is boring`() {
        val forecast = Forecast(emptyMap(), emptyList(), "")
        val score = ScoreCalculator.score(forecast)
        assertFalse(score.geomagnetic)
        assertFalse(score.radiation)
        assertFalse(score.interesting)
    }

    @Test
    fun `boring forecast is boring`() {
        val forecast = Forecast(
            emptyMap(),
            listOf(
                Forecast.Section(
                    "NOAA Geomagnetic Activity Observation and Forecast",
                    "Rationale: No G1 (Minor) or greater geomagnetic storms are expected."
                ),
                Forecast.Section(
                    "NOAA Solar Radiation Activity Observation and Forecast",
                    "Rationale: No S1 (Minor) or greater solar radiation storms are expected."
                )
            ),
            ""
        )
        val score = ScoreCalculator.score(forecast)
        assertFalse(score.geomagnetic)
        assertFalse(score.radiation)
        assertFalse(score.interesting)
    }

    @Test
    fun `boring if only one is interesting`() {
        val forecast = Forecast(
            emptyMap(),
            listOf(
                Forecast.Section(
                    "NOAA Geomagnetic Activity Observation and Forecast",
                    "Rationale: This will be a wild night."
                ),
                Forecast.Section(
                    "NOAA Solar Radiation Activity Observation and Forecast",
                    "Rationale: No S1 (Minor) or greater solar radiation storms are expected."
                )
            ),
            ""
        )
        val score = ScoreCalculator.score(forecast)
        assertTrue(score.geomagnetic)
        assertFalse(score.radiation)
        assertFalse(score.interesting)
    }

    @Test
    fun `interesting if both are interesting`() {
        val forecast = Forecast(
            emptyMap(),
            listOf(
                Forecast.Section(
                    "NOAA Geomagnetic Activity Observation and Forecast",
                    "Rationale: This will be a wild night."
                ),
                Forecast.Section(
                    "NOAA Solar Radiation Activity Observation and Forecast",
                    "Rationale: This will be a wild night."
                )
            ),
            ""
        )
        val score = ScoreCalculator.score(forecast)
        assertTrue(score.geomagnetic)
        assertTrue(score.radiation)
        assertTrue(score.interesting)
    }
}