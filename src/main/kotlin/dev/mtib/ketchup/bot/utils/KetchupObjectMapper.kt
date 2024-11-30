package dev.mtib.ketchup.bot.utils

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

val ketchupObjectMapper: JsonMapper by lazy {
    JsonMapper.builder().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION).addModules(JavaTimeModule())
        .findAndAddModules().build()
}