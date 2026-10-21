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

    @Test
    fun testCapitalizeWords() {
        assertEquals("", "".capitalizeWords())
        assertEquals("Coca", "coca".capitalizeWords())
        assertEquals("Coca Cola", "coca cola".capitalizeWords())
        assertEquals("Coca Cola Zero", "coca cola zero".capitalizeWords())
        assertEquals("Coca-Cola", "coca-cola".capitalizeWords())
        assertEquals("Frutas/Verduras", "frutas/verduras".capitalizeWords())
        assertEquals("(Lata) Cerveza", "(lata) cerveza".capitalizeWords())
        assertEquals("600ml", "600ml".capitalizeWords())
        assertEquals("1.5l", "1.5l".capitalizeWords())
        assertEquals("Coca Cola 600ml", "coca cola 600ml".capitalizeWords())
        assertEquals("OXXO", "OXXO".capitalizeWords())
        assertEquals("Usb-C", "usb-c".capitalizeWords())
        assertEquals("USB-C", "USB-c".capitalizeWords())
        assertEquals("Árbol Y Piñata", "árbol y piñata".capitalizeWords())
        assertEquals("Mcdonald's", "mcdonald's".capitalizeWords())
        assertEquals("McDonald's", "McDonald's".capitalizeWords())
        assertEquals("Coca ", "coca ".capitalizeWords())
    }
}
