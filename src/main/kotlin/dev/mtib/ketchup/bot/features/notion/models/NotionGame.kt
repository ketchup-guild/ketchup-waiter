package dev.mtib.ketchup.bot.features.notion.models

import com.fasterxml.jackson.databind.JsonNode

data class NotionGame(
    val title: String,
    val genres: List<Genre>,
    val owners: List<Owner>,
    val minPlayers: Int,
    val maxPlayers: Int,
    val rating: Int,
    val styles: List<Style>,
    val publicUrl: String,
) {
    data class Genre(
        val name: String,
    )

    data class Owner(
        val name: String,
    )

    data class Style(
        val name: String,
    )

    companion object {
        fun fromNotionPageJson(page: JsonNode): NotionGame {
            return NotionGame(
                title = page["properties"]["Name"]["title"][0]["plain_text"].asText(),
                genres = page["properties"]["Genre"]["multi_select"].map { Genre(it["name"].asText()) },
                owners = page["properties"]["Owner"]["multi_select"].map { Owner(it["name"].asText()) },
                minPlayers = page["properties"]["Min Players"]["number"].asInt(),
                maxPlayers = page["properties"]["Max Players"]["number"].asInt(),
                rating = page["properties"]["Rating"]["number"].asInt(),
                styles = page["properties"]["Style"]["multi_select"].map { Style(it["name"].asText()) },
                publicUrl = page["public_url"].asText(),
            )
        }

        fun Iterable<NotionGame>.toOwnerMarkdown(): String = buildString {
            val ownerGameMap = buildMap<Owner, MutableSet<NotionGame>> {
                this@toOwnerMarkdown.forEach {
                    it.owners.forEach { owner ->
                        val ownerGames = getOrPut(owner) { mutableSetOf<NotionGame>() }
                        ownerGames.add(it)
                    }
                }
            }.mapValues { it.value.toSet() }

            appendLine("# Games")
            ownerGameMap.forEach { (owner, games) ->
                appendLine("## ${owner.name}")
                games.sortedBy { -it.rating }.forEach {
                    appendLine("- **${it.title}** ${it.minPlayers}–${it.maxPlayers}")
                }
            }
        }

        /**
         * Removes non-matching owners from games
         */
        fun Iterable<NotionGame>.filterByOwnerRegex(ownerRegex: Regex): List<NotionGame> {
            return this.mapNotNull { game ->
                val newOwners = game.owners.filter { owner -> ownerRegex.containsMatchIn(owner.name) }
                if (newOwners.isEmpty()) null else game.copy(owners = newOwners)
            }
        }
    }
}