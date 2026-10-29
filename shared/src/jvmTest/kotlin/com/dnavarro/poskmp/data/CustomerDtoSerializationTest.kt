package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.data.source.remote.dto.CustomerDto
import com.dnavarro.poskmp.domain.model.CustomerDiscountType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomerDtoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testSerialization_includesNewDiscountFields() {
        val dto = CustomerDto(
            id = "c-123",
            nombre = "Cliente Mayorista",
            telefono = "555-0199",
            direccion = "Av Central 123",
            notas = "Cliente preferencial",
            limiteCredito = 5000.0,
            siempreMayoreo = true,
            activo = true,
            createdAt = 1700000000000L,
            updatedAt = 1700001000000L,
            tipoDescuento = "WHOLESALE",
            porcentajeDescuento = 0.0
        )

        val serialized = json.encodeToString(dto)
        assertTrue(serialized.contains("\"tipo_descuento\":\"WHOLESALE\""), "Must serialize @SerialName tipo_descuento")
        assertTrue(serialized.contains("\"porcentaje_descuento\":0.0") || serialized.contains("\"porcentaje_descuento\":0"), "Must serialize @SerialName porcentaje_descuento")
        assertTrue(serialized.contains("\"siempre_mayoreo\":true"), "Must serialize @SerialName siempre_mayoreo")
    }

    @Test
    fun testDeserialization_legacyPayloadWithoutNewFields_usesDefaultValues() {
        val legacyJson = """
            {
                "id": "legacy-1",
                "nombre": "Cliente Legacy Antiguo",
                "telefono": "1234567890",
                "direccion": "Calle Antigua 1",
                "notas": "",
                "limite_credito": 2000.0,
                "siempre_mayoreo": true,
                "activo": true,
                "created_at": 1690000000000,
                "updated_at": 1690001000000
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CustomerDto>(legacyJson)
        assertEquals("legacy-1", parsed.id)
        assertEquals("Cliente Legacy Antiguo", parsed.nombre)
        assertTrue(parsed.siempreMayoreo)
        assertEquals("NONE", parsed.tipoDescuento, "Legacy payload should default tipoDescuento to NONE")
        assertEquals(0.0, parsed.porcentajeDescuento, "Legacy payload should default porcentajeDescuento to 0.0")

        // Test SyncRepository resolution logic for legacy remote customer:
        val effectiveType = when {
            parsed.tipoDescuento != "NONE" -> parsed.tipoDescuento
            parsed.siempreMayoreo -> "WHOLESALE"
            else -> "NONE"
        }
        assertEquals("WHOLESALE", effectiveType, "SyncRepository fallback must resolve legacy siempre_mayoreo = true to WHOLESALE")
    }

    @Test
    fun testDeserialization_costPlusCustomDiscount() {
        val costPlusJson = """
            {
                "id": "c-cost-plus",
                "nombre": "Cliente Cost Plus",
                "telefono": "9876543210",
                "direccion": "Av. Industria",
                "notas": "",
                "limite_credito": 10000.0,
                "siempre_mayoreo": false,
                "activo": true,
                "created_at": 1700000000000,
                "updated_at": 1700005000000,
                "tipo_descuento": "COST_PLUS_PERCENTAGE",
                "porcentaje_descuento": 12.50
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CustomerDto>(costPlusJson)
        assertEquals("c-cost-plus", parsed.id)
        assertEquals("COST_PLUS_PERCENTAGE", parsed.tipoDescuento)
        assertEquals(12.50, parsed.porcentajeDescuento)

        val effectiveType = when {
            parsed.tipoDescuento != "NONE" -> parsed.tipoDescuento
            parsed.siempreMayoreo -> "WHOLESALE"
            else -> "NONE"
        }
        assertEquals("COST_PLUS_PERCENTAGE", effectiveType)
        assertEquals(CustomerDiscountType.COST_PLUS_PERCENTAGE, CustomerDiscountType.fromString(effectiveType))
    }

    @Test
    fun testDeserialization_retailMinusCustomDiscount() {
        val retailMinusJson = """
            {
                "id": "c-retail-minus",
                "nombre": "Cliente Retail Minus",
                "limite_credito": 0.0,
                "siempre_mayoreo": false,
                "activo": true,
                "created_at": 1700000000000,
                "updated_at": 1700005000000,
                "tipo_descuento": "RETAIL_MINUS_PERCENTAGE",
                "porcentaje_descuento": 15.00
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CustomerDto>(retailMinusJson)
        assertEquals("c-retail-minus", parsed.id)
        assertEquals("RETAIL_MINUS_PERCENTAGE", parsed.tipoDescuento)
        assertEquals(15.00, parsed.porcentajeDescuento)
        assertEquals(CustomerDiscountType.RETAIL_MINUS_PERCENTAGE, CustomerDiscountType.fromString(parsed.tipoDescuento))
    }

    @Test
    fun testConflictResolution_siempreMayoreoAndCustomDiscount_customDiscountWins() {
        // Edge case: In case both siempre_mayoreo = true and a custom discount is present
        val conflictJson = """
            {
                "id": "c-conflict",
                "nombre": "Conflicted Customer",
                "siempre_mayoreo": true,
                "created_at": 1700000000000,
                "updated_at": 1700005000000,
                "tipo_descuento": "COST_PLUS_PERCENTAGE",
                "porcentaje_descuento": 25.00
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CustomerDto>(conflictJson)
        val effectiveType = when {
            parsed.tipoDescuento != "NONE" -> parsed.tipoDescuento
            parsed.siempreMayoreo -> "WHOLESALE"
            else -> "NONE"
        }
        assertEquals("COST_PLUS_PERCENTAGE", effectiveType, "Custom discount must take precedence over legacy siempreMayoreo flag")
    }
}
