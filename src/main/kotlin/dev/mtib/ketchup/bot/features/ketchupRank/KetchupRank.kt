package dev.mtib.ketchup.bot.features.ketchupRank

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.cache.data.EmojiData
import dev.kord.core.entity.GuildEmoji
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.bot.features.ketchupRank.storage.KetchupGivingTable
import dev.mtib.ketchup.bot.features.ketchupRank.storage.KetchupRankTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.joinToAndString
import dev.mtib.ketchup.bot.utils.rememberOnce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.toJavaInstant
import mu.KotlinLogging
import java.time.Instant

class KetchupRank : Feature {
    val logger = KotlinLogging.logger { }

    companion object {
        const val KETCHUP_EMOJI_STRING = "<:ketchup:1205989646232981574>"
        const val DAILY_KETCHUP_AMOUNT = 5

        val getKetchupEmoji = rememberOnce<Kord, GuildEmoji> { kord ->
            val guildId = getAnywhere<Storage.GuildId>()
            GuildEmoji(
                data = EmojiData(
                    id = Snowflake(1205989646232981574L),
                    name = "ketchup",
                    guildId = guildId.value,
                ),
                kord = kord,
            )
        }
    }

    override fun register(kord: Kord) {
        registerGivingListener(kord)
        registerReactionListener(kord)
    }

    private fun registerReactionListener(kord: Kord) {
        kord.on<ReactionAddEvent> {
            if (this.emoji.mention != getKetchupEmoji(kord).mention) {
                return@on
            }
            val senderKordUser = this.user.asUser()
            val message = this.message.asMessage()
            val targets =
                message.mentionedUsers.filter { !it.isBot && it != senderKordUser }.toList()
                    .distinctBy { it.id.value }
            if (targets.isEmpty()) {
                return@on
            }

            val db = getAnywhere<Database>()
            val (givenKetchup, ketchupRemaining) = db.transaction {
                val senderKetchupUser = KetchupRankTable.safeGet(senderKordUser)
                if (senderKetchupUser.ketchupRemaining == 0) {
                    return@transaction Pair(emptyList(), 0)
                }

                val awardingTargets = targets.take(senderKetchupUser.ketchupRemaining)

                val now = Instant.now()!!
                KetchupRankTable.updateUser(senderKetchupUser.userId) {
                    it[this.ketchupRemaining] = senderKetchupUser.ketchupRemaining - awardingTargets.size
                }
                awardingTargets.map { KetchupRankTable.getOrCreate(it) }.forEach { target ->
                    KetchupRankTable.updateUser(target.userId) {
                        it[this.ketchupCount] = target.ketchupCount.inc()
                    }
                    KetchupGivingTable.create(
                        amount = 1,
                        reason = "Reaction",
                        messageId = message.id.value,
                        givenAt = now,
                        receiverId = target.userId,
                        giverId = senderKetchupUser.userId
                    )
                }
                return@transaction Pair(awardingTargets, senderKetchupUser.ketchupRemaining - awardingTargets.size)
            }

            if (givenKetchup.isEmpty()) {
                this.message.reply {
                    content =
                        "${senderKordUser.mention} tried to give $KETCHUP_EMOJI_STRING by reacting but is out of ketchup."
                }
            } else {
                this.message.reply {
                    content =
                        "${senderKordUser.mention} gave ${givenKetchup.size} $KETCHUP_EMOJI_STRING to ${givenKetchup.joinToAndString { it.mention }}. (You have $ketchupRemaining $KETCHUP_EMOJI_STRING more to give today)"
                }
            }
        }
    }

    private fun registerGivingListener(kord: Kord) {
        val ketchupEmoji = getKetchupEmoji(kord)
        kord.on<MessageCreateEvent> {
            val author = message.author
            if (author?.isBot != false) {
                return@on
            }
            if (message.content.contains(KETCHUP_EMOJI_STRING)) {
                val awardingBottlesPerPerson = message.content.split(KETCHUP_EMOJI_STRING).size - 1
                val targets = message.mentionedUsers.filter { !it.isBot && it != message.author }.toList()
                    .distinctBy { it.id.value }
                if (targets.isEmpty()) {
                    return@on
                }

                val db = getAnywhere<Database>()
                val result = db.transaction {
                    val updatedKetchupUser = KetchupRankTable.safeGet(author)
                    val distribution =
                        distributeKetchupBottles(awardingBottlesPerPerson, updatedKetchupUser.ketchupRemaining, targets)

                    KetchupRankTable.updateUser(updatedKetchupUser.userId) {
                        it[ketchupRemaining] = distribution.remaining
                    }

                    distribution.awarding.forEach { (targetUser, amount) ->
                        val target = KetchupRankTable.getOrCreate(targetUser)

                        KetchupRankTable.updateUser(targetUser) {
                            it[this.ketchupCount] = target.ketchupCount + amount.toBigDecimal()
                        }
                        KetchupGivingTable.create(
                            amount = amount,
                            reason = message.content,
                            messageId = message.id.value,
                            givenAt = message.timestamp.toJavaInstant(),
                            receiverId = target.userId,
                            giverId = updatedKetchupUser.userId
                        )
                    }

                    distribution
                }

                if (result.totalAwarding() == 0) {
                    message.reply {
                        content =
                            "${author.mention} tried to award $KETCHUP_EMOJI_STRING to ${targets.joinToAndString { it.mention }}, but is all out of ketchup to give. Help them out by reacting with $KETCHUP_EMOJI_STRING."
                    }
                } else {
                    message.reply {
                        content = buildString {
                            append("${author.mention} awarded ${result.totalAwarding()} $KETCHUP_EMOJI_STRING! ")
                            append(result.awarding.entries.joinToAndString { (key, value) -> "$value to ${key.mention}" })
                            append(". You have ${result.remaining} $KETCHUP_EMOJI_STRING remaining. ")
                            append("Anyone can also react with $KETCHUP_EMOJI_STRING to award another bottle to the mentioned users.")
                        }
                    }
                }

            }
        }
    }

    data class BottleDistribution(
        val awarding: Map<User, Int>,
        val remaining: Int,
    ) {
        fun totalAwarding() = awarding.values.sum()
    }

    private fun distributeKetchupBottles(
        bottlesPerTarget: Int,
        bottlesAvailable: Int,
        targets: List<User>
    ): BottleDistribution {
        if (targets.isEmpty() || bottlesAvailable == 0) {
            return BottleDistribution(emptyMap(), bottlesAvailable)
        }
        var bottlesRemaining = bottlesAvailable

        var cursor = 0
        val awarding = mutableMapOf<User, Int>()
        var awarded = 0
        while (bottlesRemaining > 0 && awarded < bottlesPerTarget * targets.size) {
            val currentTarget = targets[cursor]
            awarding.compute(currentTarget) { _, state -> (state ?: 0) + 1 }
            awarded += 1
            bottlesRemaining -= 1
            cursor = (cursor + 1).mod(targets.size)
        }
        return BottleDistribution(
            awarding,
            bottlesRemaining
        )
    }
}