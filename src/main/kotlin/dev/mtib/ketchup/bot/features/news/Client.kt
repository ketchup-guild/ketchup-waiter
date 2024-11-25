package dev.mtib.ketchup.bot.features.news

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

object Client {
    private const val VISIT_COPENHAGEN_BASE = "https://www.visitcopenhagen.com"
    private const val VISIT_COPENHAGEN_EVENTS = "$VISIT_COPENHAGEN_BASE/search?group=true&group.field=its_product_pid"
    private const val WONDERFUL_COPENHAGEN_BASE = "https://www.wonderfulcopenhagen.com"
    private const val WONDERFUL_COPENHAGEN_PRESS = "$WONDERFUL_COPENHAGEN_BASE/press"
    private val httpClient = OkHttpClient()
    private val logger = KotlinLogging.logger { }

    abstract class Post {
        abstract val title: String
        abstract val description: String
        abstract val url: String
    }

    class WonderfulCopenhagenPost(
        val data: JsonNode
    ) : Post() {
        val id = data.get("id").asText()!!
        val attibutes: JsonNode = data.get("attributes")
        override val title = attibutes.get("title").asText().trim()
        override val description = attibutes.get("meta_info").get("meta").find {
            it.get("attributes").get("name").asText() == "description"
        }!!.get("attributes").get("content").asText()!!
        val path = attibutes.get("path").get("alias").asText()!!
        override val url = "$WONDERFUL_COPENHAGEN_BASE$path"

        override fun toString(): String {
            return "Post(id='$id', title='$title', description='$description', url='$url')"
        }
    }

    class VisitCopenhagenPost(
        val data: JsonNode
    ) : Post() {
        val id = data.get("id").asText()!!
        override val title: String = data.get("sort_X3b_en_product_title").asText()!!
        override val description: String =
            data.get("sort_X3b_en_product_spes11")?.asText() ?: data.get("sort_X3b_da_product_hoved").asText()!!
        override val url: String = data.get("sort_X3b_en_field_canonical_url").asText()!!
        val dateRangeStart = data.get("ds_date_range_period_start_date")?.asText()?.let { Instant.parse(it) }
        val dateRangeEnd = data.get("ds_date_range_period_end_date")?.asText()?.let { Instant.parse(it) }

        fun relevantFor(date: Instant): Boolean {
            if (dateRangeStart == null || dateRangeEnd == null) {
                // Event has no date range (yet)
                return false
            }
            if (dateRangeEnd.isBefore(date)) {
                // Event already over
                return false
            }
            if (dateRangeStart.isBefore(date)) {
                // Event ongoing
                return true
            }
            val durationUntilStart = (dateRangeStart.toEpochMilli() - date.toEpochMilli()).milliseconds

            return durationUntilStart < 60.days
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
        html.write(Request.Builder().get().url(WONDERFUL_COPENHAGEN_PRESS).build().let {
            httpClient.newCall(it).execute().use { response ->
                response.body!!.string()
            }
        })
        html.end()

        val wonderfulCopenhagenNews = htmlHandler.getData()
            .get("props")
            .get("initialReduxState")
            .get("content")
            .get("data")
            .get("node--news")

        val visitCopenhagenData =
            jsonMapper().readTree(
                Request.Builder()
                    .post(
                        """
                        {
                            "params": {
                                "wt": "json"
                            },
                            "filter": [],
                            "query": "ss_search_api_datasource:\"entity:product\" AND ss_search_api_language:\"en\" AND its_product_category:\"58\" AND its_product_subcategory:\"59\" AND -bs_field_is_deleted:true AND (ds_date_range_period_end_date:[NOW TO *] OR (*:* NOT ds_date_range_period_end_date:[* TO *]))",
                            "limit": 10000,
                            "sort": "bs_field_period_date_range_exists desc, ds_date_range_period_start_date asc, ds_date_range_period_end_date asc, sort_product_title asc, sort_X3b_und_product_title asc",
                            "facet": {
                                "date-range": {
                                    "type": "terms",
                                    "field": "drs_date_range_period",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "date-range"
                                    }
                                },
                                "region": {
                                    "type": "terms",
                                    "field": "itm_product_regions",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "region"
                                    }
                                },
                                "place": {
                                    "type": "terms",
                                    "field": "itm_product_place",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "place"
                                    }
                                },
                                "sm_facet_182": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_182"
                                    },
                                    "prefix": "182//"
                                },
                                "sm_facet_269": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_269"
                                    },
                                    "prefix": "269//"
                                },
                                "sm_facet_342": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_342"
                                    },
                                    "prefix": "342//"
                                },
                                "sm_facet_404": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_404"
                                    },
                                    "prefix": "404//"
                                },
                                "sm_facet_558": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_558"
                                    },
                                    "prefix": "558//"
                                },
                                "sm_facet_482": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_482"
                                    },
                                    "prefix": "482//"
                                },
                                "sm_facet_643": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_643"
                                    },
                                    "prefix": "643//"
                                },
                                "sm_facet_717": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_717"
                                    },
                                    "prefix": "717//"
                                },
                                "sm_facet_804": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_804"
                                    },
                                    "prefix": "804//"
                                },
                                "sm_facet_183": {
                                    "type": "terms",
                                    "field": "sm_product_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_183"
                                    },
                                    "prefix": "183//"
                                },
                                "sm_facet_922": {
                                    "type": "terms",
                                    "field": "sm_field_sustainability_facet",
                                    "sort": {
                                        "index": "asc"
                                    },
                                    "limit": -1,
                                    "domain": {
                                        "excludeTags": "sm_facet_922"
                                    },
                                    "prefix": "922//"
                                }
                            }
                        }
                    """.trimIndent().toRequestBody("application/json".toMediaType())
                    )
                    .url(VISIT_COPENHAGEN_EVENTS)
                    .header("accept", "application/json")
                    .build()
                    .let {
                        httpClient.newCall(it).execute().use { response ->
                            response.body!!.string()
                        }
                    }
            )
                .get("grouped")
                .get("its_product_pid")
                .get("groups")

        val posts = buildList<Post> {
            visitCopenhagenData.forEach {
                try {
                    val post = VisitCopenhagenPost(it.get("doclist").get("docs").first())
                    if (post.relevantFor(Instant.now())) {
                        add(post)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to parse post" }
                    logger.trace { it }
                }
            }
            wonderfulCopenhagenNews.fields().forEach {
                try {
                    add(WonderfulCopenhagenPost(it.value))
                } catch (e: Exception) {
                    logger.error(e) { "Failed to parse post: ${it.key}: $e" }
                    logger.trace { it.value }
                }
            }
        }

        logger.trace { posts }

        return posts
    }
}