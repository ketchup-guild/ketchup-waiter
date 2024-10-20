package dev.mtib.ketchup.bot.features.homeassistant

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

object Client {
    private val client = OkHttpClient()
    private val token: String? by lazy { System.getenv("HOME_ASSISTANT_TOKEN") }
    private val baseUrl: String? by lazy { System.getenv("HOME_ASSISTANT_BASE_URL") }

    /**
     * @param color The color to set the light to
     * @param brightness The brightness to set the light to (0-255)
     * @param entities The entities to set the light on
     */
    fun setLight(color: String, brightness: Int, vararg entities: String) {
        if (baseUrl == null || token == null) {
            throw IllegalStateException("Home Assistant token or base URL not set")
        }
        val url = "$baseUrl/api/services/light/turn_on"
        val headers = mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
        val body = mapOf(
            "entity_id" to entities,
            "brightness" to brightness,
            "color_name" to color,
            "transition" to 0
        )
        val request = okhttp3.Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .post(jacksonObjectMapper().writeValueAsBytes(body).toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to set light: ${response.code} ${response.body?.string()}")
            }
        }
    }

    fun setLight(on: Boolean = false, vararg entities: String) {
        if (baseUrl == null || token == null) {
            throw IllegalStateException("Home Assistant token or base URL not set")
        }
        val url = "$baseUrl/api/services/light/turn_${if (on) "on" else "off"}"
        val headers = mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
        val body = mapOf(
            "entity_id" to entities,
        )
        val request = okhttp3.Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .post(jacksonObjectMapper().writeValueAsBytes(body).toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to set light: ${response.code} ${response.body?.string()}")
            }
        }
    }

    val keepColors = setOf(
        "aqua",
        "azure",
        "blue",
        "chocolate",
        "coral",
        "crimson",
        "cyan",
        "ghostwhite",
        "gold",
        "green",
        "hotpink",
        "indigo",
        "lavender",
        "lime",
        "magenta",
        "orange",
        "pink",
        "purple",
        "red",
    ).also {
        require(it.size <= 25) { "Too many colors: ${it.size} > 25" }
    }

    val allowedColors by lazy {
        services
            .find { it.get("domain").asText() == "light" }!!
            .get("services")
            .get("turn_on")
            .get("fields")
            .get("advanced_fields")
            .get("fields")
            .get("color_name")
            .get("selector")
            .get("select")
            .get("options")
            .map { it.asText()!! }
    }

    private val services: JsonNode by lazy {
        if (baseUrl == null || token == null) {
            throw IllegalStateException("Home Assistant token or base URL not set")
        }
        val url = "$baseUrl/api/services"
        val headers = mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
        val request = okhttp3.Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to get services: ${response.code} ${response.body?.string()}")
            }
            jacksonObjectMapper().readTree(response.body?.string())
        }
    }
}

suspend fun main(): Unit {
    println(Client.allowedColors)
    println(Client.allowedColors.size)
}