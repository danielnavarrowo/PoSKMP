package com.dnavarro.poskmp.printer

import com.dnavarro.poskmp.domain.model.PRINTER_SYSTEM_DIALOG_ID
import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Font
import java.awt.Graphics2D
import java.awt.print.Paper
import java.awt.print.Printable
import java.awt.print.PrinterJob

actual fun createReceiptPrinter(): ReceiptPrinter = JvmReceiptPrinter()

private class JvmReceiptPrinter : ReceiptPrinter {
    override suspend fun print(document: ReceiptDocument): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val printerJob = PrinterJob.getPrinterJob()
                val isSystemDialog = document.printerId.isNullOrBlank() ||
                        document.printerId == PRINTER_SYSTEM_DIALOG_ID ||
                        document.printerId == "android-system"

                if (!isSystemDialog) {
                    val selectedService = PrinterJob.lookupPrintServices()
                        .firstOrNull { it.name == document.printerId }
                        ?: return@withContext Result.failure(
                            IllegalStateException("La impresora seleccionada no está disponible")
                        )
                    printerJob.printService = selectedService
                } else if (printerJob.printService == null) {
                    return@withContext Result.failure(
                        IllegalStateException("No hay una impresora predeterminada configurada")
                    )
                }

                printerJob.jobName = "Ticket ${document.folio}"
                val pageFormat = printerJob.defaultPage()
                pageFormat.paper = createPaper(document)
                printerJob.setPrintable({ graphics, format, pageIndex ->
                    if (pageIndex > 0) return@setPrintable Printable.NO_SUCH_PAGE
                    val graphics2d = graphics as Graphics2D
                    graphics2d.font = document.fontFamily.toAwtFont(document.fontSize, emphasized = false)
                    graphics2d.paint = java.awt.Color.BLACK
                    val metrics = graphics2d.fontMetrics
                    val left = format.imageableX
                    val right = format.imageableX + format.imageableWidth
                    var y = format.imageableY + metrics.ascent
                    val lineHeight = (metrics.height * 1.25).toInt().coerceAtLeast(1)

                    document.lines.forEach { line ->
                        graphics2d.font = document.fontFamily.toAwtFont(document.fontSize, line.emphasized)
                        val lineMetrics = graphics2d.fontMetrics
                        if (line.text.isNotEmpty()) {
                            val textWidth = lineMetrics.stringWidth(line.text).toDouble()
                            val x = when (line.alignment) {
                                ReceiptAlignment.CENTER -> format.imageableX + (format.imageableWidth - textWidth) / 2.0
                                ReceiptAlignment.RIGHT -> right - textWidth
                                ReceiptAlignment.LEFT -> left
                            }
                            graphics2d.drawString(line.text, x.toFloat(), y.toFloat())
                        }
                        y += lineHeight
                    }
                    Printable.PAGE_EXISTS
                }, pageFormat)

                if (isSystemDialog) {
                    val shouldPrint = printerJob.printDialog()
                    if (!shouldPrint) {
                        return@withContext Result.success(Unit)
                    }
                }

                printerJob.print()
                Result.success(Unit)
            }
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private fun createPaper(document: ReceiptDocument): Paper {
        val paper = Paper()
        val width = document.paperWidthMm / 25.4 * 72.0
        val lineHeight = document.fontSize * 1.25
        val height = (document.lines.size * lineHeight + 48.0).coerceAtLeast(200.0)
        paper.setSize(width, height)
        paper.setImageableArea(12.0, 12.0, width - 24.0, height - 24.0)
        return paper
    }

    private fun String.toAwtFont(size: Int, emphasized: Boolean): Font =
        Font(this, if (emphasized) Font.BOLD else Font.PLAIN, size)
}
