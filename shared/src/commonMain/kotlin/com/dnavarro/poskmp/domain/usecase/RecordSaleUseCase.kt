package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID
import com.dnavarro.poskmp.util.roundPrice

class RecordSaleUseCase(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(
        cartItems: List<CartItem>,
        pagoCon: Double,
        cambio: Double,
        metodoPago: String = "EFECTIVO",
        customerId: String? = null,
        roundTicketTotal: Boolean = false
    ): Long {
        if (cartItems.isEmpty()) return 0L

        val now = currentTimeMillis()
        val saleId = generateUUID()
        val nextFolio = saleRepository.getNextFolio()

        var total = 0.0
        var totalOriginal = 0.0
        var totalCosto = 0.0
        var totalItemsCount = 0.0

        val saleItems = cartItems.map { cartItem ->
            val qty = cartItem.quantity
            val unitPrice = cartItem.product.precio
            val origUnitPrice = cartItem.originalPrice
            val unitCost = cartItem.product.costo

            val itemSubtotal = unitPrice * qty
            val itemOrigSubtotal = origUnitPrice * qty
            val itemCost = unitCost * qty
            val itemProfit = itemSubtotal - itemCost
            val isWholesale = unitPrice == cartItem.product.precio_mayoreo && cartItem.product.precio_mayoreo > 0.0

            total += itemSubtotal
            totalOriginal += itemOrigSubtotal
            totalCosto += itemCost
            totalItemsCount += qty

            SaleItem(
                id = generateUUID(),
                saleId = saleId,
                productId = cartItem.product.id,
                productNombre = cartItem.product.nombre,
                cantidad = qty,
                precioUnitario = unitPrice,
                costoUnitario = unitCost,
                subtotal = itemSubtotal,
                ganancia = itemProfit,
                esMayoreo = isWholesale,
                createdAt = now
            )
        }

        val finalTotal = if (roundTicketTotal) roundPrice(total) else total

        if (metodoPago == "CREDITO" && customerId.isNullOrBlank()) {
            throw IllegalArgumentException("Las ventas a crédito requieren un cliente registrado.")
        }
        if (metodoPago == "MIXTO" && pagoCon < finalTotal && customerId.isNullOrBlank()) {
            throw IllegalArgumentException("Las ventas mixtas con saldo a crédito requieren un cliente registrado.")
        }

        val netProfit = finalTotal - totalCosto

        val sale = Sale(
            id = saleId,
            folio = nextFolio,
            total = finalTotal,
            totalOriginal = totalOriginal,
            totalCosto = totalCosto,
            ganancia = netProfit,
            pagoCon = pagoCon,
            cambio = cambio,
            metodoPago = metodoPago,
            totalItems = totalItemsCount,
            customerId = customerId,
            createdAt = now,
            syncState = "PENDING_INSERT"
        )

        return saleRepository.recordSale(sale, saleItems)
    }
}
