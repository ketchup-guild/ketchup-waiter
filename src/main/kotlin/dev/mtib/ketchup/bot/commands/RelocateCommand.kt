package dev.mtib.ketchup.bot.commands

class RelocateCommand : ChannelCommand(
    "relocate",
    "Relocate yourself to another location",
    "Relocate yourself to another location, like when you move to another city.",
) {
    override val category: Category = Category.Role
    override val completeness: Completeness = Completeness.Stubbed
}