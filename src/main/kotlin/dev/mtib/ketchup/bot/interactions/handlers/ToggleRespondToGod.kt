package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.Visibility
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.Visibility.PRIVATE
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import dev.mtib.ketchup.bot.storage.Storage.MagicWord
import dev.mtib.ketchup.bot.utils.getAnywhere

object ToggleRespondToGod : Interaction {
    private var _respondToGod = true
    val respondToGod: Boolean
        get() = _respondToGod

    override val visibility: Visibility = PRIVATE

    override val name: String = "toggle-respond-to-god"
    override val description: String = "Toggle whether the bot should respond to god"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.string("magic", "The magic word to toggle the bot's response to god") {
            required = true
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val selectedMagicWord = event.interaction.getStringOptionByName("magic")
        val ownMagicWord = getAnywhere<MagicWord>()

        if (ownMagicWord.toString() == selectedMagicWord) {
            _respondToGod = !_respondToGod
            event.defer().respond {
                content =
                    "Responding to god for instance with magic word $ownMagicWord is now ${if (_respondToGod) "enabled" else "disabled"}"
            }
        }
    }
}