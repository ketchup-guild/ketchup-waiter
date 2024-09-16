package dev.mtib.ketchup.bot.features.northernlights

import dev.mtib.ketchup.bot.features.northernlights.models.Forecast
import okhttp3.OkHttpClient
import okhttp3.Request

object Client {
    val client = OkHttpClient()
    private fun get3DayForecastTxt(): String? {
        Request.Builder().get().url("https://services.swpc.noaa.gov/text/3-day-forecast.txt").build().let {
            client.newCall(it).execute().use { response ->
                return response.body?.string()
            }
        }
    }

    fun get3DayForecast(): Forecast {
        val txt = get3DayForecastTxt() ?: return Forecast(emptyMap(), emptyList())
        val lines = txt.lines()

        val metadata = lines.takeWhile { it.startsWith(":") }.mapNotNull {
            Regex(":([^:]+): (.+)").matchEntire(it)?.groupValues?.let { (_, key, value) ->
                key to value
            }
        }.toMap()

        val comments = lines.filter { it.startsWith("#") }.map { it.removePrefix("#") }.joinToString("").trim()

        val sections = buildList<Forecast.Section> {
            var header = ""
            var content = ""

            lines.forEach {
                Regex("([A-Z]+)\\. (.+)").matchEntire(it)?.groupValues?.get(2)?.let { newHeader ->
                    if (header.isNotBlank()) {
                        add(Forecast.Section(header, content.trim('\n')))
                        content = ""
                    }
                    header = newHeader
                } ?: run {
                    if (header.isNotBlank()) {
                        content += it + "\n"
                    }
                }
            }
        }

        return Forecast(metadata, sections, comments)
    }
}

fun main() {
    println(Client.get3DayForecast())
}