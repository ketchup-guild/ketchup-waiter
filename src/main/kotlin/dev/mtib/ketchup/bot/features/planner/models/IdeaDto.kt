package dev.mtib.ketchup.bot.features.planner.models

data class IdeaDto(
    val channelName: String,
    val summary: String,
    val callToAction: String,
    val setup: String,
    val minPersons: Int,
    val maxPersons: Int,
    val scheduled: Boolean,
) {
    fun toDiscordMarkdownString(): String {
        return """
            |**Channel:** `#$channelName`
            |**Summary:** $summary
            |**Call to action:** $callToAction
            |**Setup:** $setup
            |**Min persons:** $minPersons
            |**Max persons:** $maxPersons
            |**Scheduled:** $scheduled
        """.trimMargin()
    }
}