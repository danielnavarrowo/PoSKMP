package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.domain.model.AccountStatementItem
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDebtSummary
import com.dnavarro.poskmp.domain.model.CustomerDiscountType
import com.dnavarro.poskmp.domain.model.CustomerPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveCustomerUseCaseDiscountTest {

    private class FakeCustomerRepository : CustomerRepository {
        val savedCustomers = mutableListOf<Customer>()

        override fun getActiveCustomers(): Flow<List<Customer>> = flowOf(savedCustomers)
        override fun getAllCustomers(): Flow<List<Customer>> = flowOf(savedCustomers)
        override suspend fun getCustomerById(id: String): Customer? = savedCustomers.find { it.id == id }
        override suspend fun searchActiveCustomers(query: String): List<Customer> = savedCustomers.filter { it.nombre.contains(query, ignoreCase = true) }
        override suspend fun saveCustomer(customer: Customer) {
            savedCustomers.removeAll { it.id == customer.id }
            savedCustomers.add(customer)
        }
        override suspend fun setCustomerActiveStatus(id: String, activo: Boolean) {}
        override suspend fun deleteCustomer(id: String) {}
        override suspend fun recordCustomerPayment(payment: CustomerPayment) {}
        override suspend fun deleteCustomerPayment(paymentId: String) {}
        override suspend fun getCustomerDebtSummary(): CustomerDebtSummary = CustomerDebtSummary(0, 0.0, 0)
        override suspend fun getCustomerAccountStatement(customerId: String): List<AccountStatementItem> = emptyList()
    }

    @Test
    fun testSaveCustomer_withCostPlusPercentage() = runBlocking {
        val repo = FakeCustomerRepository()
        val useCase = SaveCustomerUseCase(repo)

        val result = useCase(
            id = "c-1",
            nombre = "Cliente Costo Mas",
            telefono = "555-1111",
            direccion = "Calle 1",
            notas = "Markup 18.5%",
            limiteCredito = 5000.0,
            siempreMayoreo = false,
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 18.5
        )

        assertTrue(result.isSuccess)
        val saved = repo.savedCustomers.first()
        assertEquals(CustomerDiscountType.COST_PLUS_PERCENTAGE, saved.tipoDescuento)
        assertEquals(18.5, saved.porcentajeDescuento)
        assertFalse(saved.siempreMayoreo, "siempreMayoreo should be false for custom markup")
        assertTrue(saved.hasSpecialPricing)
        assertEquals(CustomerDiscountType.COST_PLUS_PERCENTAGE, saved.effectiveDiscountType)
    }

    @Test
    fun testSaveCustomer_withRetailMinusPercentage() = runBlocking {
        val repo = FakeCustomerRepository()
        val useCase = SaveCustomerUseCase(repo)

        val result = useCase(
            id = "c-2",
            nombre = "Cliente Menos Retail",
            telefono = "555-2222",
            limiteCredito = 1000.0,
            siempreMayoreo = false,
            tipoDescuento = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            porcentajeDescuento = 15.0
        )

        assertTrue(result.isSuccess)
        val saved = repo.savedCustomers.first()
        assertEquals(CustomerDiscountType.RETAIL_MINUS_PERCENTAGE, saved.tipoDescuento)
        assertEquals(15.0, saved.porcentajeDescuento)
        assertFalse(saved.siempreMayoreo)
        assertTrue(saved.hasSpecialPricing)
        assertEquals(CustomerDiscountType.RETAIL_MINUS_PERCENTAGE, saved.effectiveDiscountType)
    }

    @Test
    fun testSaveCustomer_withAutomaticWholesale() = runBlocking {
        val repo = FakeCustomerRepository()
        val useCase = SaveCustomerUseCase(repo)

        val result = useCase(
            id = "c-3",
            nombre = "Cliente Mayorista",
            telefono = "555-3333",
            limiteCredito = 20000.0,
            siempreMayoreo = true,
            tipoDescuento = CustomerDiscountType.WHOLESALE,
            porcentajeDescuento = 0.0
        )

        assertTrue(result.isSuccess)
        val saved = repo.savedCustomers.first()
        assertEquals(CustomerDiscountType.WHOLESALE, saved.tipoDescuento)
        assertTrue(saved.siempreMayoreo, "siempreMayoreo must be true when WHOLESALE is chosen")
        assertEquals(0.0, saved.porcentajeDescuento)
        assertTrue(saved.hasSpecialPricing)
        assertEquals(CustomerDiscountType.WHOLESALE, saved.effectiveDiscountType)
    }

    @Test
    fun testSaveCustomer_legacyAlwaysWholesaleCompatibility() = runBlocking {
        val repo = FakeCustomerRepository()
        val useCase = SaveCustomerUseCase(repo)

        // Legacy invocation passing only siempreMayoreo = true and default tipoDescuento
        val result = useCase(
            id = "c-legacy",
            nombre = "Cliente Legacy",
            siempreMayoreo = true
        )

        assertTrue(result.isSuccess)
        val saved = repo.savedCustomers.first()
        assertEquals(CustomerDiscountType.WHOLESALE, saved.tipoDescuento)
        assertTrue(saved.siempreMayoreo)
        assertEquals(0.0, saved.porcentajeDescuento)
        assertEquals(CustomerDiscountType.WHOLESALE, saved.effectiveDiscountType)
    }

    @Test
    fun testSaveCustomer_withNone() = runBlocking {
        val repo = FakeCustomerRepository()
        val useCase = SaveCustomerUseCase(repo)

        val result = useCase(
            id = "c-none",
            nombre = "Cliente Regular",
            siempreMayoreo = false,
            tipoDescuento = CustomerDiscountType.NONE,
            porcentajeDescuento = 0.0
        )

        assertTrue(result.isSuccess)
        val saved = repo.savedCustomers.first()
        assertEquals(CustomerDiscountType.NONE, saved.tipoDescuento)
        assertFalse(saved.siempreMayoreo)
        assertEquals(0.0, saved.porcentajeDescuento)
        assertFalse(saved.hasSpecialPricing)
        assertEquals(CustomerDiscountType.NONE, saved.effectiveDiscountType)
    }

    @Test
    fun testSaveCustomer_negativePercentageCoercedToZero() = runBlocking {
        val repo = FakeCustomerRepository()
        val useCase = SaveCustomerUseCase(repo)

        val result = useCase(
            id = "c-neg",
            nombre = "Cliente Porcentaje Negativo",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = -10.0
        )

        assertTrue(result.isSuccess)
        val saved = repo.savedCustomers.first()
        assertEquals(0.0, saved.porcentajeDescuento, "Negative percentage must be coerced to 0.0")
    }

    @Test
    fun testCustomerFormDialogValidationRules() {
        // Test decimal regex filtering as used in CustomerFormDialog: Regex("^\\d*\\.?\\d{0,2}$")
        val regex = Regex("^\\d*\\.?\\d{0,2}$")
        assertTrue("".matches(regex))
        assertTrue("1".matches(regex))
        assertTrue("12".matches(regex))
        assertTrue("12.".matches(regex))
        assertTrue("12.5".matches(regex))
        assertTrue("12.50".matches(regex))
        assertFalse("12.505".matches(regex), "More than 2 decimals should be rejected")
        assertFalse("abc".matches(regex))
        assertFalse("-5".matches(regex))

        // Test percentage validation logic for COST_PLUS_PERCENTAGE (must be > 0.0)
        fun isCostPlusValid(text: String): Boolean {
            val v = text.toDoubleOrNull()
            return v != null && v > 0.0
        }
        assertTrue(isCostPlusValid("10"))
        assertTrue(isCostPlusValid("150.5")) // markups can exceed 100%
        assertFalse(isCostPlusValid("0"))
        assertFalse(isCostPlusValid("-5"))
        assertFalse(isCostPlusValid(""))

        // Test percentage validation logic for RETAIL_MINUS_PERCENTAGE (must be > 0.0 and < 100.0)
        fun isRetailMinusValid(text: String): Boolean {
            val v = text.toDoubleOrNull()
            return v != null && v > 0.0 && v < 100.0
        }
        assertTrue(isRetailMinusValid("10"))
        assertTrue(isRetailMinusValid("99.99"))
        assertFalse(isRetailMinusValid("100"))
        assertFalse(isRetailMinusValid("105"))
        assertFalse(isRetailMinusValid("0"))
        assertFalse(isRetailMinusValid(""))
    }
}
