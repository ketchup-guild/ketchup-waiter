package dev.mtib.ketchup.bot.commands

class ColourboxSearchCommand : ChannelCommand(
    "colourbox search",
    "Search for images on Colourbox",
    "Search for images on Colourbox",
) {
    override val category: Category
        get() = Category.Misc
    override val completeness: Completeness
        get() = Completeness.Stubbed
}