package dev.mtib.ketchup.bot.commands

class InviteCommand : ChannelCommand(
    "invite",
    "Invite a user to the current channel",
    "Sends an invite to a user on this guild in a DM. They can then join the channel.",
) {
    override val category: Category
        get() = Category.Role
}