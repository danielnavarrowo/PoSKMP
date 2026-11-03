package com.dnavarro.poskmp.data.sync

import com.dnavarro.poskmp.data.source.remote.dto.CashMovementDto
import com.dnavarro.poskmp.data.source.remote.dto.CashierDto
import com.dnavarro.poskmp.data.source.remote.dto.CustomerDto
import com.dnavarro.poskmp.data.source.remote.dto.CustomerPaymentDto
import com.dnavarro.poskmp.data.source.remote.dto.DeletedRecordDto
import com.dnavarro.poskmp.data.source.remote.dto.ProductDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleItemDto
import com.dnavarro.poskmp.data.source.remote.dto.ShiftDto
import com.dnavarro.poskmp.data.source.remote.dto.StoreSettingsDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncDtoRoundTripTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Test
    fun testProductDto_roundTripAndCreatedAtFieldParity() {
        val original = ProductDto(
            id = "prod-uuid-1",
            codigos = "[\"75010001\",\"75010002\"]",
            nombre = "Coca Cola 600ml",
            precio = 18.5,
            costo = 12.0,
            categoria = "Bebidas",
            activo = true,
            porPeso = false,
            precioMayoreo = 16.0,
            precioDelivery = 20.0,
            esFavorito = true,
            piezas = 24.0,
            createdAt = 1710000000000L,
            updatedAt = 1710001000000L
        )

        val serialized = json.encodeToString(original)

        // Verify exact column names serialized for Supabase/PostgREST
        assertTrue(serialized.contains("\"id\":\"prod-uuid-1\""))
        assertTrue(serialized.contains("\"created_at\":1710000000000"))
        assertTrue(serialized.contains("\"updated_at\":1710001000000"))
        assertTrue(serialized.contains("\"por_peso\":false"))
        assertTrue(serialized.contains("\"precio_mayoreo\":16.0") || serialized.contains("\"precio_mayoreo\":16"))
        assertTrue(serialized.contains("\"precio_delivery\":20.0") || serialized.contains("\"precio_delivery\":20"))
        assertTrue(serialized.contains("\"es_favorito\":true"))

        val deserialized = json.decodeFromString<ProductDto>(serialized)
        assertEquals(original, deserialized)
        assertEquals(1710000000000L, deserialized.createdAt, "ProductDto.createdAt parity failure")
        assertEquals(1710001000000L, deserialized.updatedAt, "ProductDto.updatedAt parity failure")
    }

    @Test
    fun testSaleDto_roundTripAndCashierIdUpdatedAtFieldParity() {
        val original = SaleDto(
            id = "sale-uuid-1",
            folio = 1042L,
            total = 250.75,
            totalOriginal = 270.0,
            totalCosto = 180.0,
            ganancia = 70.75,
            pagoCon = 300.0,
            cambio = 49.25,
            metodoPago = "MIXTO",
            totalItems = 3.5,
            customerId = "cust-1",
            createdAt = 1710002000000L,
            cashierId = "cashier-uuid-99",
            cashierName = "Juan Perez",
            estado = "COMPLETADA",
            esForanea = true,
            shiftId = "shift-uuid-5",
            montoEfectivo = 150.75,
            montoTarjeta = 100.0,
            montoTransferencia = 0.0,
            montoCredito = 0.0,
            updatedAt = 1710002050000L
        )

        val serialized = json.encodeToString(original)

        // Assert exact Supabase snake_case columns
        assertTrue(serialized.contains("\"cashier_id\":\"cashier-uuid-99\""), "Must serialize cashier_id")
        assertTrue(serialized.contains("\"updated_at\":1710002050000"), "Must serialize updated_at")
        assertTrue(serialized.contains("\"total_original\":270.0") || serialized.contains("\"total_original\":270"))
        assertTrue(serialized.contains("\"total_costo\":180.0") || serialized.contains("\"total_costo\":180"))
        assertTrue(serialized.contains("\"es_foranea\":true"))
        assertTrue(serialized.contains("\"shift_id\":\"shift-uuid-5\""))
        assertTrue(serialized.contains("\"monto_efectivo\":150.75"))
        assertTrue(serialized.contains("\"monto_tarjeta\":100.0") || serialized.contains("\"monto_tarjeta\":100"))

        val deserialized = json.decodeFromString<SaleDto>(serialized)
        assertEquals(original, deserialized)
        assertEquals("cashier-uuid-99", deserialized.cashierId)
        assertEquals(1710002050000L, deserialized.updatedAt)
    }

    @Test
    fun testShiftDto_roundTripAndUpdatedAtFieldParity() {
        val original = ShiftDto(
            id = "shift-uuid-10",
            cashierId = "cashier-1",
            cashierName = "Maria Gomez",
            startTime = 1710000000000L,
            endTime = 1710036000000L,
            initialCash = 500.0,
            finalCashExpected = 2500.0,
            finalCashCounted = 2490.0,
            difference = -10.0,
            notes = "Faltante de 10 pesos por cambio",
            isClosed = true,
            updatedAt = 1710036005000L
        )

        val serialized = json.encodeToString(original)

        assertTrue(serialized.contains("\"cashier_id\":\"cashier-1\""))
        assertTrue(serialized.contains("\"start_time\":1710000000000"))
        assertTrue(serialized.contains("\"end_time\":1710036000000"))
        assertTrue(serialized.contains("\"is_closed\":true"))
        assertTrue(serialized.contains("\"updated_at\":1710036005000"), "Must serialize updated_at on ShiftDto")

        val deserialized = json.decodeFromString<ShiftDto>(serialized)
        assertEquals(original, deserialized)
        assertEquals(1710036005000L, deserialized.updatedAt)
        assertTrue(deserialized.isClosed)
        assertEquals(-10.0, deserialized.difference)
    }

    @Test
    fun testStoreSettingsDto_roundTripAnd5BusinessPricingRulesFieldParity() {
        val original = StoreSettingsDto(
            id = "default",
            storeName = "Abarrotes Don Pepe",
            storeAddress = "Av. Hidalgo 456",
            storePhone = "4491234567",
            transferClabe = "012345678901234567",
            transferBeneficiary = "Jose Perez",
            receiptFooter = "¡Gracias por su compra!",
            defaultRetailMargin = 30.0,
            defaultWholesaleMargin = 15.0,
            defaultDeliveryMargin = 25.0,
            roundRetailPrice = true,
            roundWholesalePrice = false,
            roundDeliveryPrice = true,
            roundTicketTotal = false,
            disallowCardPaymentOnWholesale = true,
            prioritizeDeliveryPrice = true,
            autoWholesaleByQuantity = true,
            autoWholesaleQuantityThreshold = 6,
            autoWholesaleByTicketTotal = true,
            autoWholesaleTicketTotalThreshold = 750.50,
            updatedAt = 1710040000000L
        )

        val serialized = json.encodeToString(original)

        // Explicitly assert all 5 business pricing rules
        assertTrue(serialized.contains("\"prioritize_delivery_price\":true"), "Missing prioritize_delivery_price")
        assertTrue(serialized.contains("\"auto_wholesale_by_quantity\":true"), "Missing auto_wholesale_by_quantity")
        assertTrue(serialized.contains("\"auto_wholesale_quantity_threshold\":6"), "Missing auto_wholesale_quantity_threshold")
        assertTrue(serialized.contains("\"auto_wholesale_by_ticket_total\":true"), "Missing auto_wholesale_by_ticket_total")
        assertTrue(serialized.contains("\"auto_wholesale_ticket_total_threshold\":750.5") || serialized.contains("\"auto_wholesale_ticket_total_threshold\":750.50"), "Missing auto_wholesale_ticket_total_threshold")

        // Assert general store configurations
        assertTrue(serialized.contains("\"store_name\":\"Abarrotes Don Pepe\""))
        assertTrue(serialized.contains("\"disallow_card_payment_on_wholesale\":true"))
        assertTrue(serialized.contains("\"updated_at\":1710040000000"))

        val deserialized = json.decodeFromString<StoreSettingsDto>(serialized)
        assertEquals(original, deserialized)
        assertTrue(deserialized.prioritizeDeliveryPrice)
        assertTrue(deserialized.autoWholesaleByQuantity)
        assertEquals(6, deserialized.autoWholesaleQuantityThreshold)
        assertTrue(deserialized.autoWholesaleByTicketTotal)
        assertEquals(750.50, deserialized.autoWholesaleTicketTotalThreshold)
    }

    @Test
    fun testCustomerDto_roundTripAndDiscountFields() {
        val original = CustomerDto(
            id = "cust-123",
            nombre = "Cliente Mayorista Especial",
            telefono = "555-4321",
            direccion = "Calle Principal 10",
            notas = "Entrega en bodega",
            limiteCredito = 15000.0,
            siempreMayoreo = true,
            activo = true,
            createdAt = 1710000000000L,
            updatedAt = 1710001000000L,
            tipoDescuento = "COST_PLUS_PERCENTAGE",
            porcentajeDescuento = 12.5
        )

        val serialized = json.encodeToString(original)

        assertTrue(serialized.contains("\"limite_credito\":15000.0") || serialized.contains("\"limite_credito\":15000"))
        assertTrue(serialized.contains("\"siempre_mayoreo\":true"))
        assertTrue(serialized.contains("\"tipo_descuento\":\"COST_PLUS_PERCENTAGE\""))
        assertTrue(serialized.contains("\"porcentaje_descuento\":12.5"))

        val deserialized = json.decodeFromString<CustomerDto>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun testCustomerPaymentDto_roundTrip() {
        val original = CustomerPaymentDto(
            id = "pay-1",
            customerId = "cust-1",
            monto = 500.0,
            metodoPago = "TRANSFERENCIA",
            notas = "Abono semana 10",
            createdAt = 1710005000000L
        )

        val serialized = json.encodeToString(original)
        assertTrue(serialized.contains("\"customer_id\":\"cust-1\""))
        assertTrue(serialized.contains("\"metodo_pago\":\"TRANSFERENCIA\""))
        assertTrue(serialized.contains("\"created_at\":1710005000000"))

        val deserialized = json.decodeFromString<CustomerPaymentDto>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun testSaleItemDto_roundTrip() {
        val original = SaleItemDto(
            id = "item-101",
            saleId = "sale-1",
            productId = "prod-1",
            productNombre = "Sabritas Sal 45g",
            cantidad = 2.0,
            precioUnitario = 22.0,
            costoUnitario = 15.0,
            subtotal = 44.0,
            ganancia = 14.0,
            esMayoreo = false,
            esDelivery = false,
            esDescuento = true,
            createdAt = 1710002010000L
        )

        val serialized = json.encodeToString(original)
        assertTrue(serialized.contains("\"sale_id\":\"sale-1\""))
        assertTrue(serialized.contains("\"product_id\":\"prod-1\""))
        assertTrue(serialized.contains("\"product_nombre\":\"Sabritas Sal 45g\""))
        assertTrue(serialized.contains("\"precio_unitario\":22.0") || serialized.contains("\"precio_unitario\":22"))
        assertTrue(serialized.contains("\"costo_unitario\":15.0") || serialized.contains("\"costo_unitario\":15"))
        assertTrue(serialized.contains("\"es_descuento\":true"))

        val deserialized = json.decodeFromString<SaleItemDto>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun testCashMovementDto_roundTrip() {
        val original = CashMovementDto(
            id = "mov-1",
            shiftId = "shift-1",
            cashierId = "cashier-1",
            tipo = "RETIRO",
            monto = 200.0,
            motivo = "Pago a proveedor de pan",
            createdAt = 1710010000000L
        )

        val serialized = json.encodeToString(original)
        assertTrue(serialized.contains("\"shift_id\":\"shift-1\""))
        assertTrue(serialized.contains("\"cashier_id\":\"cashier-1\""))
        assertTrue(serialized.contains("\"tipo\":\"RETIRO\""))
        assertTrue(serialized.contains("\"created_at\":1710010000000"))

        val deserialized = json.decodeFromString<CashMovementDto>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun testCashierDto_roundTrip() {
        val original = CashierDto(
            id = "cashier-test-1",
            nombre = "Carlos Santana",
            pin = "4321",
            activo = true,
            createdAt = 1710000000000L,
            updatedAt = 1710002000000L,
            deviceId = "terminal-pos-02"
        )

        val serialized = json.encodeToString(original)
        assertTrue(serialized.contains("\"device_id\":\"terminal-pos-02\""))
        assertTrue(serialized.contains("\"pin\":\"4321\""))

        val deserialized = json.decodeFromString<CashierDto>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun testDeletedRecordDto_roundTrip() {
        val original = DeletedRecordDto(
            id = "del-prod-99",
            entityType = "PRODUCT",
            deletedAt = 1710099000000L
        )

        val serialized = json.encodeToString(original)
        assertTrue(serialized.contains("\"entity_type\":\"PRODUCT\""))
        assertTrue(serialized.contains("\"deleted_at\":1710099000000"))

        val deserialized = json.decodeFromString<DeletedRecordDto>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun testLegacyPayloadDefaults_safetyCheck() {
        // Test that legacy payloads from older server versions lacking new fields parse safely
        val legacySaleJson = """
            {
                "id": "legacy-sale-1",
                "folio": 100,
                "total": 100.0,
                "total_original": 100.0,
                "total_costo": 70.0,
                "ganancia": 30.0,
                "pago_con": 100.0,
                "cambio": 0.0,
                "created_at": 1700000000000
            }
        """.trimIndent()

        val parsedSale = json.decodeFromString<SaleDto>(legacySaleJson)
        assertNull(parsedSale.cashierId, "Legacy sale should default cashierId to null")
        assertEquals(0L, parsedSale.updatedAt, "Legacy sale should default updatedAt to 0L")

        val legacyProductJson = """
            {
                "id": "legacy-prod-1",
                "nombre": "Legacy Product",
                "updated_at": 1700000500000
            }
        """.trimIndent()

        val parsedProduct = json.decodeFromString<ProductDto>(legacyProductJson)
        assertEquals(0L, parsedProduct.createdAt, "Legacy product should default createdAt to 0L")

        val legacyShiftJson = """
            {
                "id": "legacy-shift-1",
                "cashier_id": "c-1",
                "cashier_name": "Admin",
                "start_time": 1700000000000
            }
        """.trimIndent()

        val parsedShift = json.decodeFromString<ShiftDto>(legacyShiftJson)
        assertEquals(0L, parsedShift.updatedAt, "Legacy shift should default updatedAt to 0L")
        assertFalse(parsedShift.isClosed)

        val legacySettingsJson = """
            {
                "id": "default",
                "updated_at": 1700000000000
            }
        """.trimIndent()

        val parsedSettings = json.decodeFromString<StoreSettingsDto>(legacySettingsJson)
        assertFalse(parsedSettings.prioritizeDeliveryPrice)
        assertFalse(parsedSettings.autoWholesaleByQuantity)
        assertEquals(3, parsedSettings.autoWholesaleQuantityThreshold)
        assertFalse(parsedSettings.autoWholesaleByTicketTotal)
        assertEquals(500.0, parsedSettings.autoWholesaleTicketTotalThreshold)
    }
}
