package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.features.subscriptions.reactions.ReactionSubscriptions
import dev.mtib.ketchup.bot.interactions.helpers.Interactions.shouldIgnore
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName

object ReactionSubscription : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PRIVATE
    override val name: String = "reactions"
    override val description: String = "change subscription to reaction notifications"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.string("action", "subscribe or unsubscribe") {
            required = true
            this.choice("subscribe", "subscribe")
            this.choice("unsubscribe", "unsubscribe")
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        if (event.shouldIgnore()) {
            return
        }
        val response = event.defer()
        val author = event.interaction.user
        when (event.interaction.getStringOptionByName("action")) {
            "subscribe" -> {
                ReactionSubscriptions.subscribe(author)
                response.respond {
                    content = "You are now subscribed to reactions"
                }
            }

            "unsubscribe" -> {
                ReactionSubscriptions.unsubscribe(author)
                response.respond {
                    content = "You are now unsubscribed from reactions"
                }
            }
        }
    }

}