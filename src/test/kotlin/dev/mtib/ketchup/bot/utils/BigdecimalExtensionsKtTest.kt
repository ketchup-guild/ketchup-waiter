package dev.mtib.ketchup.bot.utils

import kotlin.test.Test

class BigdecimalExtensionsKtTest {

    @Test
    fun testStripTrailingFractionalZeros() {
        data class TestCase(val bigDecimal: String, val expected: String)

        val testCases = listOf(
            TestCase("1.000", "1"),
            TestCase("10.000", "10"),
            TestCase("10000000.000", "10000000"),
            TestCase("10000000.001", "10000000.001"),
            TestCase("10000000.0010", "10000000.001"),
            TestCase("11.000", "11"),
            TestCase("11.001", "11.001"),
            TestCase("0.001", "0.001"),
            TestCase("0.0010", "0.001"),
        )
        testCases.forEach {
            val result = it.bigDecimal.toBigDecimal().stripTrailingFractionalZeros()
            kotlin.test.assertEquals(it.expected, result.toString())

        }
    }
}
