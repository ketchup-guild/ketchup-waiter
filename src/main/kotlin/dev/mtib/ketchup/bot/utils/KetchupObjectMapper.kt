package dev.mtib.ketchup.bot.utils

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule

val ketchupObjectMapper: ObjectMapper by lazy {
    ObjectMapper()
        .configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), false)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .findAndRegisterModules().registerModule(JavaTimeModule()).registerModule(kotlinModule())
}