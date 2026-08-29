package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.domain.receipt.ReceiptFormatter
import com.dnavarro.poskmp.printer.ReceiptPrinter
import kotlinx.coroutines.flow.first

class ReprintSaleReceiptUseCase(
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository,
    private val receiptPrinter: ReceiptPrinter
) {
    suspend operator fun invoke(sale: Sale, items: List<SaleItem>? = null): Result<Unit> {
        val resolvedItems = items ?: saleRepository.getItemsBySaleId(sale.id)
        val customerName = sale.customerId?.let { customerRepository.getCustomerById(it)?.nombre }
        val settings = settingsRepository.receiptSettingsFlow.first().copy(openCashDrawerOnCashSale = false)
        val receipt = ReceiptFormatter.createFromSale(
            sale = sale,
            items = resolvedItems,
            customerName = customerName,
            settings = settings
        )
        return receiptPrinter.print(receipt)
    }
}
