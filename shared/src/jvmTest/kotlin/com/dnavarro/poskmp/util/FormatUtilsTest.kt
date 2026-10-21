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

    @Test
    fun testApplySmartWordCapitalization() {
        // Starts with uppercase by default when typing without Shift
        assertEquals("C", applySmartWordCapitalization("", "c", isShiftPressed = false, isDesktop = true))
        assertEquals("Co", applySmartWordCapitalization("C", "Co", isShiftPressed = false, isDesktop = true))
        assertEquals("Coc", applySmartWordCapitalization("Co", "Coc", isShiftPressed = false, isDesktop = true))
        assertEquals("Coca ", applySmartWordCapitalization("Coca", "Coca ", isShiftPressed = false, isDesktop = true))
        assertEquals("Coca C", applySmartWordCapitalization("Coca ", "Coca c", isShiftPressed = false, isDesktop = true))
        assertEquals("Coca-C", applySmartWordCapitalization("Coca-", "Coca-c", isShiftPressed = false, isDesktop = true))
        assertEquals("600m", applySmartWordCapitalization("600", "600m", isShiftPressed = false, isDesktop = true))

        // Typing with Shift keeps word start in lowercase
        assertEquals("c", applySmartWordCapitalization("", "C", isShiftPressed = true, isDesktop = true))
        assertEquals("Pan d", applySmartWordCapitalization("Pan ", "Pan D", isShiftPressed = true, isDesktop = true))
        assertEquals("Pan de", applySmartWordCapitalization("Pan d", "Pan de", isShiftPressed = false, isDesktop = true))
        assertEquals("iP", applySmartWordCapitalization("i", "iP", isShiftPressed = true, isDesktop = true))

        // Deleting the first letter does NOT automatically capitalize the next letter
        assertEquals("oca", applySmartWordCapitalization("Coca", "oca", isShiftPressed = false, isDesktop = true))
        assertEquals("Pan e", applySmartWordCapitalization("Pan De", "Pan e", isShiftPressed = false, isDesktop = true))
        assertEquals("Coc", applySmartWordCapitalization("Coca", "Coc", isShiftPressed = false, isDesktop = true))

        // Mobile / Android behavior (isDesktop = false) preserves input as-is
        assertEquals("c", applySmartWordCapitalization("", "c", isShiftPressed = false, isDesktop = false))
        assertEquals("C", applySmartWordCapitalization("", "C", isShiftPressed = false, isDesktop = false))
        assertEquals("oca", applySmartWordCapitalization("Coca", "oca", isShiftPressed = false, isDesktop = false))

        // Pasted multi-character text is preserved as-is
        assertEquals("pan de muerto", applySmartWordCapitalization("", "pan de muerto", isShiftPressed = false, isDesktop = true))
    }
}
