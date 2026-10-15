package com.dnavarro.poskmp.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatUtilsTest {

    @Test
    fun testFormatPriceDouble() {
        assertEquals("0.00", 0.0.formatPrice())
        assertEquals("5.00", 5.0.formatPrice())
        assertEquals("50.50", 50.5.formatPrice())
        assertEquals("123.45", 123.45.formatPrice())
        assertEquals("1,234.56", 1234.56.formatPrice())
        assertEquals("123,456.78", 123456.78.formatPrice())
        assertEquals("1,234,567.89", 1234567.89.formatPrice())
        assertEquals("15,000,000.00", 15000000.0.formatPrice())
        assertEquals("-1,234.56", (-1234.56).formatPrice())
        assertEquals("1,000.05", 1000.05.formatPrice())
    }

    @Test
    fun testFormatPriceString() {
        assertEquals("0.00", "0".formatPrice())
        assertEquals("123,456.78", "123456.78".formatPrice())
        assertEquals("1,234.50", "1234.5".formatPrice())
        assertEquals("123,456.78", "123,456.78".formatPrice())
        assertEquals("-5,000.50", "-5000.5".formatPrice())
    }

    @Test
    fun testFormatWithThousandsSeparator() {
        assertEquals("0", formatWithThousandsSeparator("0"))
        assertEquals("999", formatWithThousandsSeparator("999"))
        assertEquals("1,000", formatWithThousandsSeparator("1000"))
        assertEquals("12,345", formatWithThousandsSeparator("12345"))
        assertEquals("123,456", formatWithThousandsSeparator("123456"))
        assertEquals("1,234,567", formatWithThousandsSeparator("1234567"))
        assertEquals("-1,234,567", formatWithThousandsSeparator("-1234567"))
    }
}
