package com.dnavarro.poskmp.printer

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.PrinterType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun createReceiptPrinter(): ReceiptPrinter = AndroidReceiptPrinter()

private class AndroidReceiptPrinter : ReceiptPrinter {
    override suspend fun print(document: ReceiptDocument): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val context = DatabaseDriverFactory.appContext
        if (context == null) {
            continuation.resume(Result.failure(IllegalStateException("La aplicación no está lista para imprimir")))
            return@suspendCancellableCoroutine
        }

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            continuation.resume(Result.failure(IllegalStateException("El servicio de impresión no está disponible")))
            return@suspendCancellableCoroutine
        }

        fun finish(result: Result<Unit>) {
            if (continuation.isActive) continuation.resume(result)
        }

        val attributes = createPrintAttributes(document)
        val adapter = object : PrintDocumentAdapter() {
            private var currentAttributes: PrintAttributes = attributes

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal,
                callback: LayoutResultCallback,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal.isCanceled) {
                    callback.onLayoutCancelled()
                    finish(Result.failure(IllegalStateException("La impresión fue cancelada")))
                    return
                }
                currentAttributes = newAttributes
                callback.onLayoutFinished(
                    PrintDocumentInfo.Builder("ticket-${document.folio}")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build(),
                    oldAttributes == null || oldAttributes != newAttributes
                )
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal.isCanceled) {
                    callback.onWriteCancelled()
                    finish(Result.failure(IllegalStateException("La impresión fue cancelada")))
                    return
                }

                val pdf = PrintedPdfDocument(context, currentAttributes)
                try {
                    val page = pdf.startPage(0)
                    val canvas = page.canvas
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.BLACK
                        textSize = document.fontSize.toFloat()
                        typeface = document.fontFamily.toTypeface(emphasized = false)
                    }
                    val contentWidth = page.info.pageWidth.toFloat()
                    val left = 24f
                    val right = contentWidth - 24f
                    var y = 28f + document.fontSize
                    val lineHeight = document.fontSize * 1.35f

                    document.lines.forEach { line ->
                        paint.typeface = document.fontFamily.toTypeface(line.emphasized)
                        if (line.text.isNotEmpty()) {
                            val textWidth = paint.measureText(line.text)
                            val x = when (line.alignment) {
                                ReceiptAlignment.CENTER -> (contentWidth - textWidth) / 2f
                                ReceiptAlignment.RIGHT -> right - textWidth
                                ReceiptAlignment.LEFT -> left
                            }.coerceAtLeast(left)
                            canvas.drawText(line.text, x, y, paint)
                        }
                        y += lineHeight
                    }
                    pdf.finishPage(page)
                    ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                        pdf.writeTo(output)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    finish(Result.success(Unit))
                } catch (error: Throwable) {
                    callback.onWriteFailed(error.message ?: "No se pudo generar el ticket")
                    finish(Result.failure(error))
                } finally {
                    pdf.close()
                }
            }
        }

        try {
            printManager.print("Ticket ${document.folio}", adapter, attributes)
        } catch (error: Throwable) {
            finish(Result.failure(error))
        }
    }

    private fun createPrintAttributes(document: ReceiptDocument): PrintAttributes {
        val widthMils = (document.paperWidthMm * 1000.0 / 25.4).toInt().coerceAtLeast(2000)
        val heightMils = (document.lines.size * 180 + 600).coerceAtLeast(4000)
        val mediaSize = PrintAttributes.MediaSize(
            "POS_CUSTOM_WIDTH",
            "${document.paperWidthMm} mm",
            widthMils,
            heightMils
        )
        return PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setResolution(PrintAttributes.Resolution("POSKMP", "POSKMP", 203, 203))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
    }

    private fun String.toTypeface(emphasized: Boolean): Typeface =
        Typeface.create(this, if (emphasized) Typeface.BOLD else Typeface.NORMAL)
}
