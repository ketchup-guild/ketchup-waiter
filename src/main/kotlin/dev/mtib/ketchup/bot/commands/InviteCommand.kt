package dev.mtib.ketchup.bot.commands

import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere

class InviteCommand : ChannelCommand(
    COMMAND,
    "Sends an invite to the current channel to the mentioned channel.",
    "Sends an invite, similar to the one when initially creating a club or event, to the mentioned channel. Users there can react with an emoji to join.\n" +
            "Usage: `${getAnywhere<Storage.MagicWord>()} $COMMAND <channel>`\n" +
            "Example: `${getAnywhere<Storage.MagicWord>()} $COMMAND #general` (make sure to allow Discord to link the channel)",
) {
    companion object {
        const val COMMAND = "invite"
    }

    override val category: Category = Category.Role
    override val completeness: Completeness = Completeness.Stubbed
}