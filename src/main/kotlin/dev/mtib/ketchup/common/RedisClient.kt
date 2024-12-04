package dev.mtib.ketchup.common

import redis.clients.jedis.JedisPooled

object RedisClient {
    val pool by lazy {
        val redisUrl = System.getenv("REDIS_URL") ?: error("Missing REDIS_URL, like 'redis://localhost:6379'")
        JedisPooled(redisUrl)
    }
}