package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.ui.PriceTier
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID
import com.dnavarro.poskmp.util.roundPrice
import kotlin.math.roundToLong

class RecordSaleUseCase(
    private val saleRepository: SaleRepository,
    private val shiftRepository: ShiftRepository? = null
) {
    suspend operator fun invoke(
        cartItems: List<CartItem>,
        pagoCon: Double,
        cambio: Double,
        metodoPago: String = "EFECTIVO",
        customerId: String? = null,
        roundTicketTotal: Boolean = false,
        shiftId: String? = null,
        cashierId: String? = null,
        cashierName: String? = null,
        esForanea: Boolean = false,
        montoEfectivo: Double = 0.0,
        montoTarjeta: Double = 0.0,
        montoTransferencia: Double = 0.0,
        montoCredito: Double = 0.0
    ): Long {
        if (cartItems.isEmpty()) return 0L

        val now = currentTimeMillis()
        val saleId = generateUUID()
        val nextFolio = saleRepository.getNextFolio()

        // Si no se pasó shiftId explícito, obtener del turno activo
        val activeShift = if (shiftId == null && shiftRepository != null) {
            shiftRepository.getActiveShift()
        } else null

        val finalShiftId = shiftId ?: activeShift?.id
        val finalCashierId = cashierId ?: activeShift?.cashierId
        val finalCashierName = cashierName ?: activeShift?.cashierName

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
            val isWholesale = cartItem.appliedTier == PriceTier.WHOLESALE || (unitPrice == cartItem.product.precio_mayoreo && cartItem.product.precio_mayoreo > 0.0)
            val isDelivery = !isWholesale && unitPrice == cartItem.product.precio_delivery && cartItem.product.precio_delivery > 0.0
            val isDiscount = cartItem.appliedTier == PriceTier.CUSTOMER_DISCOUNT || (!isWholesale && !isDelivery && (unitPrice < origUnitPrice - 0.001 || cartItem.manualTier == PriceTier.CUSTOMER_DISCOUNT))

            total += itemSubtotal
            totalOriginal += itemOrigSubtotal
            totalCosto += itemCost
            totalItemsCount += if (cartItem.product.por_peso == 1L) 1.0 else qty

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
                esDelivery = isDelivery,
                esDescuento = isDiscount,
                createdAt = now
            )
        }

        val roundedRawTotal = (total * 100.0).roundToLong() / 100.0
        val finalTotal = if (roundTicketTotal) roundPrice(total) else roundedRawTotal

        val (finalCash, finalCard, finalTransfer, finalCredit) = when (metodoPago) {
            "EFECTIVO" -> listOf(finalTotal, 0.0, 0.0, 0.0)
            "TARJETA" -> listOf(0.0, finalTotal, 0.0, 0.0)
            "TRANSFERENCIA" -> listOf(0.0, 0.0, finalTotal, 0.0)
            "CREDITO" -> listOf(0.0, 0.0, 0.0, finalTotal)
            "MIXTO" -> {
                if (montoEfectivo == 0.0 && montoTarjeta == 0.0 && montoTransferencia == 0.0 && montoCredito == 0.0) {
                    val cashPart = (pagoCon - cambio).coerceAtLeast(0.0)
                    val creditPart = if (finalTotal > pagoCon) (finalTotal - pagoCon) else 0.0
                    listOf(cashPart, 0.0, 0.0, creditPart)
                } else {
                    listOf(montoEfectivo, montoTarjeta, montoTransferencia, montoCredito)
                }
            }
            else -> listOf(finalTotal, 0.0, 0.0, 0.0)
        }

        if (metodoPago == "CREDITO" && customerId.isNullOrBlank()) {
            throw IllegalArgumentException("Las ventas a crédito requieren un cliente registrado.")
        }
        if (metodoPago == "MIXTO" && finalCredit > 0.0 && customerId.isNullOrBlank()) {
            throw IllegalArgumentException("Las ventas mixtas con saldo a crédito requieren un cliente registrado.")
        }

        val finalTotalOriginal = (totalOriginal * 100.0).roundToLong() / 100.0
        val finalTotalCosto = (totalCosto * 100.0).roundToLong() / 100.0
        val netProfit = ((finalTotal - finalTotalCosto) * 100.0).roundToLong() / 100.0

        val sale = Sale(
            id = saleId,
            folio = nextFolio,
            total = finalTotal,
            totalOriginal = finalTotalOriginal,
            totalCosto = finalTotalCosto,
            ganancia = netProfit,
            pagoCon = pagoCon,
            cambio = cambio,
            metodoPago = metodoPago,
            totalItems = totalItemsCount,
            customerId = customerId,
            createdAt = now,
            syncState = "PENDING_INSERT",
            shiftId = finalShiftId,
            cashierId = finalCashierId,
            cashierName = finalCashierName,
            esForanea = esForanea,
            montoEfectivo = finalCash,
            montoTarjeta = finalCard,
            montoTransferencia = finalTransfer,
            montoCredito = finalCredit
        )

        return saleRepository.recordSale(sale, saleItems)
    }
}
