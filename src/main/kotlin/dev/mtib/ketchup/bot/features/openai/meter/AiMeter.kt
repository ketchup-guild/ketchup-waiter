package dev.mtib.ketchup.bot.features.openai.meter

import dev.mtib.ketchup.server.meter.MeterRegistry

object AiMeter {
    private val aiMeterCounter = MeterRegistry.registry.counter("ai_meter_counter")
    private val textMeterCounter = MeterRegistry.registry.counter("ai_meter_text_counter")
    private val imageMeterCounter = MeterRegistry.registry.counter("ai_meter_image_counter")
    private val aiTextCharacterCounter = MeterRegistry.registry.counter("ai_text_character_counter")

    enum class AiType {
        TEXT, IMAGE
    }

    fun incrementAiCounter(type: AiType, count: Double = 1.0) {
        aiMeterCounter.increment(count)
        when (type) {
            AiType.TEXT -> textMeterCounter.increment(count)
            AiType.IMAGE -> imageMeterCounter.increment(count)
        }
    }

    fun incrementAiTextCharacterCounter(count: Double) {
        aiTextCharacterCounter.increment(count)
    }
}