package dev.mtib.ketchup.server.meter

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object MeterRegistry {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}