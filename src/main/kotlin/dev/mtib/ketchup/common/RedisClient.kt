package dev.mtib.ketchup.common

import kotlinx.coroutines.asCoroutineDispatcher
import redis.clients.jedis.JedisPooled
import java.util.concurrent.Executors

object RedisClient {
    val pool by lazy {
        val redisUrl = System.getenv("REDIS_URL") ?: error("Missing REDIS_URL, like 'redis://localhost:6379'")
        JedisPooled(redisUrl)
    }
    val dispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
}