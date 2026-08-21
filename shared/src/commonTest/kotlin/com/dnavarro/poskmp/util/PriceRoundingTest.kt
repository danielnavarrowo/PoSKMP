package com.dnavarro.poskmp.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PriceRoundingTest {

    @Test
    fun testExactZeroAndIntegerAmounts() {
        assertEquals(0.0, roundPrice(0.0))
        assertEquals(10.0, roundPrice(10.0))
        assertEquals(100.0, roundPrice(100.00))
    }

    @Test
    fun testCentsBetween1And49RoundTo50Cents() {
        assertEquals(10.50, roundPrice(10.01))
        assertEquals(10.50, roundPrice(10.15))
        assertEquals(10.50, roundPrice(10.25))
        assertEquals(10.50, roundPrice(10.49))
        assertEquals(0.50, roundPrice(0.20))
        assertEquals(0.50, roundPrice(0.01))
        assertEquals(0.50, roundPrice(0.49))
    }

    @Test
    fun testExact50CentsRemains50Cents() {
        assertEquals(10.50, roundPrice(10.50))
        assertEquals(0.50, roundPrice(0.50))
        assertEquals(99.50, roundPrice(99.50))
    }

    @Test
    fun testCentsBetween51And99RoundToNextPeso() {
        assertEquals(11.0, roundPrice(10.51))
        assertEquals(11.0, roundPrice(10.75))
        assertEquals(11.0, roundPrice(10.99))
        assertEquals(1.0, roundPrice(0.51))
        assertEquals(1.0, roundPrice(0.85))
        assertEquals(1.0, roundPrice(0.99))
        assertEquals(100.0, roundPrice(99.51))
    }
}
