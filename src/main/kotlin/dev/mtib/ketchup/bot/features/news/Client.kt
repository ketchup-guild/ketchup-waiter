package dev.mtib.ketchup.bot.features.news

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request

object Client {
    private const val PRESS_URL = "https://www.wonderfulcopenhagen.com/press"
    private val httpClient = OkHttpClient()
    private val logger = KotlinLogging.logger { }

    class Post(
        val data: JsonNode
    ) {
        val id = data.get("id").asText()!!
        val attibutes: JsonNode = data.get("attributes")
        val title = attibutes.get("title").asText().trim()
        val description = attibutes.get("meta_info").get("meta").find {
            it.get("attributes").get("name").asText() == "description"
        }!!.get("attributes").get("content").asText()!!
        val path = attibutes.get("path").get("alias").asText()!!
        val url = "https://www.wonderfulcopenhagen.com$path"

        override fun toString(): String {
            return "Post(id='$id', title='$title', description='$description', url='$url')"
        }
    }

    fun fetchNews(): Iterable<Post> {
        val htmlHandler = object : KsoupHtmlHandler {
            private var inDataScript = false
            private var data: JsonNode? = null

            override fun onText(text: String) {
                if (inDataScript) {
                    data = jsonMapper().readTree(text)!!
                }
                inDataScript = false
            }

            override fun onOpenTag(name: String, attributes: Map<String, String>, isImplied: Boolean) {
                if (name == "script" && attributes["id"] == "__NEXT_DATA__") {
                    inDataScript = true
                }
            }

            fun getData(): JsonNode = data!!
        }
        val html = KsoupHtmlParser(htmlHandler)
        html.write(Request.Builder().get().url(PRESS_URL).build().let {
            httpClient.newCall(it).execute().use { response ->
                response.body!!.string()
            }
        })
        html.end()

        val news = htmlHandler.getData()
            .get("props")
            .get("initialReduxState")
            .get("content")
            .get("data")
            .get("node--news")

        val posts = buildList<Post> {
            news.fields().forEach {
                try {
                    add(Post(it.value))
                } catch (e: Exception) {
                    logger.error(e) { "Failed to parse post: ${it.key}: $e" }
                }
            }
        }

        logger.trace { posts }

        return posts
    }
}