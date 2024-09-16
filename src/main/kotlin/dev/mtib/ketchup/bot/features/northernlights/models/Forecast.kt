package dev.mtib.ketchup.bot.features.northernlights.models

data class Forecast(
    val metadata: Map<String, String>,
    val sections: List<Section>,
    val comment: String? = null
) {
    data class Section(
        val title: String,
        val content: String
    ) {
        val markdownContent: String
            get() {
                return buildList<String> {
                    var inCodeBlock = false
                    content.lines().forEach {
                        if (!inCodeBlock && it.startsWith(" ")) {
                            inCodeBlock = true
                            add("```")
                        }
                        if (inCodeBlock && it.isBlank()) {
                            inCodeBlock = false
                            add("```")
                        }
                        when {
                            inCodeBlock -> add(it)
                            it.isBlank() -> add(it)
                            isEmpty() -> add(it)
                            else -> this[size - 1] = (this[size - 1] + " $it").trim()
                        }
                    }
                }.let { list ->
                    list.filterIndexed() { index, currentLine ->
                        index == 0 || !(list[index - 1].isBlank() && currentLine.isBlank())
                    }
                        .joinToString("\n")
                }
            }
    }
}