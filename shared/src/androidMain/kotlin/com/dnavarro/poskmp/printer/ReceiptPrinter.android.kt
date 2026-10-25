package com.dnavarro.poskmp.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
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
import androidx.core.graphics.createBitmap
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

actual fun createReceiptPrinter(): ReceiptPrinter = AndroidReceiptPrinter()

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val MANROPE_ASSET_PATH = "composeResources/poskmp.shared.generated.resources/font/Manrope.ttf"

private class AndroidReceiptPrinter : ReceiptPrinter {
    @Volatile
    private var cachedManropeRegular: Typeface? = null

    @Volatile
    private var cachedManropeBold: Typeface? = null

    private fun resolveTypeface(context: Context, fontFamily: ReceiptFontFamily, emphasized: Boolean): Typeface {
        if (fontFamily == ReceiptFontFamily.MONOSPACE) {
            return Typeface.create(Typeface.MONOSPACE, if (emphasized) Typeface.BOLD else Typeface.NORMAL)
        }
        if (emphasized) {
            cachedManropeBold?.let { return it }
        } else {
            cachedManropeRegular?.let { return it }
        }

        val loaded = runCatching {
            Typeface.Builder(context.assets, MANROPE_ASSET_PATH)
                .setFontVariationSettings(if (emphasized) "'wght' 700" else "'wght' 500")
                .build()
        }.getOrNull() ?: Typeface.create(Typeface.SANS_SERIF, if (emphasized) Typeface.BOLD else Typeface.NORMAL)

        if (emphasized) {
            cachedManropeBold = loaded
        } else {
            cachedManropeRegular = loaded
        }
        return loaded
    }

    private fun encodeThermalPayload(document: ReceiptDocument, cutPaper: Boolean): ByteArray {
        if (document.fontFamily == ReceiptFontFamily.MANROPE) {
            val context = DatabaseDriverFactory.appContext
            if (context != null) {
                return renderReceiptToEscPosRaster(context, document, cutPaper)
            }
        }
        return EscPosReceiptEncoder.encode(document, cutPaper = cutPaper)
    }

    private fun renderReceiptToEscPosRaster(
        context: Context,
        document: ReceiptDocument,
        cutPaper: Boolean
    ): ByteArray {
        val widthDots = EscPosReceiptEncoder.printableWidthDots(document.paperWidthMm)
        val pixelFontSize = (document.fontSize * 2f).coerceIn(16f, 64f)
        val lineHeight = ceil(pixelFontSize * 1.45f).toInt().coerceAtLeast(20)
        val separatorHeight = ceil(pixelFontSize * 0.95f).toInt().coerceAtLeast(14)

        val totalHeight = document.lines.sumOf { line ->
            if (line.isSeparator) separatorHeight else lineHeight
        } + 16

        val bitmap = createBitmap(widthDots, totalHeight.coerceAtLeast(32))
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = pixelFontSize
        }
        val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
        }

        val leftMargin = 0f
        val rightMargin = widthDots.toFloat()
        var currentTop = 8f

        for ((text, alignment, emphasized, leftCol, rightCol, isSeparator) in document.lines) {
            if (isSeparator) {
                val centerY = currentTop + separatorHeight / 2f
                canvas.drawLine(leftMargin, centerY, rightMargin, centerY, separatorPaint)
                currentTop += separatorHeight
                continue
            }

            textPaint.typeface = resolveTypeface(context, document.fontFamily, emphasized)
            textPaint.isFakeBoldText = emphasized
            val fm = textPaint.fontMetrics
            val baselineY = currentTop - fm.ascent

            if (leftCol != null && rightCol != null) {
                val rightWidth = textPaint.measureText(rightCol)
                val rightX = (rightMargin - rightWidth).coerceAtLeast(leftMargin)
                canvas.drawText(rightCol, rightX, baselineY, textPaint)

                val maxLeftWidth = (rightX - leftMargin - 12f).coerceAtLeast(0f)
                if (maxLeftWidth > 0f) {
                    val fittedLeft = fitTextToWidth(leftCol, textPaint, maxLeftWidth)
                    canvas.drawText(fittedLeft, leftMargin, baselineY, textPaint)
                }
            } else if (text.isNotEmpty()) {
                val fitted = fitTextToWidth(text, textPaint, rightMargin - leftMargin)
                val textWidth = textPaint.measureText(fitted)
                val x = when (alignment) {
                    ReceiptAlignment.CENTER -> leftMargin + ((rightMargin - leftMargin) - textWidth) / 2f
                    ReceiptAlignment.RIGHT -> rightMargin - textWidth
                    ReceiptAlignment.LEFT -> leftMargin
                }.coerceAtLeast(leftMargin)
                canvas.drawText(fitted, x, baselineY, textPaint)
            }

            currentTop += lineHeight
        }

        val pixels = IntArray(widthDots * bitmap.height)
        bitmap.getPixels(pixels, 0, widthDots, 0, 0, widthDots, bitmap.height)
        val bitmapHeight = bitmap.height
        bitmap.recycle()

        return EscPosReceiptEncoder.encodeRasterBitmap(
            widthDots = widthDots,
            heightDots = bitmapHeight,
            openDrawer = document.openCashDrawer,
            feedLines = document.feedLines,
            cutPaper = cutPaper
        ) { x, y ->
            val argb = pixels[y * widthDots + x]
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            val luma = (r * 299 + g * 587 + b * 114) / 1000
            luma < 165
        }
    }

    private fun fitTextToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val ellipsisWidth = paint.measureText(ellipsis)
        if (maxWidth <= ellipsisWidth) return ""
        val count = paint.breakText(text, true, maxWidth - ellipsisWidth, null)
        return if (count <= 0) "" else text.substring(0, count).trimEnd() + ellipsis
    }

    override suspend fun print(document: ReceiptDocument): Result<Unit> {
        val target = document.printerId?.trim()
        if (isBluetoothTarget(target)) {
            return withContext(Dispatchers.IO) {
                runCatching {
                    val macAddress = extractBluetoothMac(target ?: "")
                    val escPosBytes = encodeThermalPayload(document, cutPaper = false)
                    sendBytesViaBluetooth(macAddress, escPosBytes)
                }
            }
        }

        if (target?.startsWith("tcp://") == true || isIpPort(target)) {
            return withContext(Dispatchers.IO) {
                runCatching {
                    val hostPort = if (target?.startsWith("tcp://") == true) target.removePrefix("tcp://") else target ?: ""
                    val parts = hostPort.split(":")
                    val host = parts[0].trim()
                    val port = parts.getOrNull(1)?.toIntOrNull() ?: 9100

                    val escPosBytes = encodeThermalPayload(document, cutPaper = true)
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), 4000)
                        socket.soTimeout = 5000
                        socket.getOutputStream().use { out ->
                            out.write(escPosBytes)
                            out.flush()
                        }
                    }
                }
            }
        }

        return printViaAndroidManager(document)
    }

    override suspend fun openCashDrawer(printerId: String?): Result<Unit> {
        val target = printerId?.trim()
        if (isBluetoothTarget(target)) {
            return withContext(Dispatchers.IO) {
                runCatching {
                    val macAddress = extractBluetoothMac(target ?: "")
                    val kickBytes = EscPosReceiptEncoder.encodeDrawerKick()
                    sendBytesViaBluetooth(macAddress, kickBytes)
                }
            }
        }

        if (target?.startsWith("tcp://") == true || isIpPort(target)) {
            return withContext(Dispatchers.IO) {
                runCatching {
                    val hostPort = if (target?.startsWith("tcp://") == true) target.removePrefix("tcp://") else target ?: ""
                    val parts = hostPort.split(":")
                    val host = parts[0].trim()
                    val port = parts.getOrNull(1)?.toIntOrNull() ?: 9100

                    val kickBytes = EscPosReceiptEncoder.encodeDrawerKick()
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), 4000)
                        socket.getOutputStream().use { out ->
                            out.write(kickBytes)
                            out.flush()
                        }
                    }
                }
            }
        }
        return Result.failure(UnsupportedOperationException("La apertura directa de cajón en Android requiere una impresora conectada por Bluetooth o TCP/Red."))
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendBytesViaBluetooth(macAddress: String, payload: ByteArray) {
        val context = DatabaseDriverFactory.appContext
            ?: throw IllegalStateException("La aplicación no está lista para imprimir.")

        if (!hasBluetoothPrinterPermission()) {
            throw IllegalStateException("Permiso de Bluetooth no concedido. Autoriza el permiso en Ajustes > Ticket e Impresora.")
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
            ?: throw IllegalStateException("Este dispositivo no cuenta con adaptador Bluetooth.")

        if (!adapter.isEnabled) {
            throw IllegalStateException("El Bluetooth está desactivado. Actívalo e intenta de nuevo.")
        }

        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
        } catch (_: Exception) {
        }

        val device = runCatching {
            adapter.bondedDevices?.firstOrNull { it.address.equals(macAddress, ignoreCase = true) }
                ?: adapter.getRemoteDevice(macAddress.uppercase())
        }.getOrElse {
            throw IllegalStateException("Dirección Bluetooth inválida ($macAddress).")
        }

        val socket = connectBluetoothSocket(device, macAddress)
        try {
            val outputStream = socket.outputStream
            val chunkSize = 1024
            var offset = 0
            while (offset < payload.size) {
                val length = minOf(chunkSize, payload.size - offset)
                withContext(Dispatchers.IO) {
                    outputStream.write(payload, offset, length)
                }
                withContext(Dispatchers.IO) {
                    outputStream.flush()
                }
                offset += length
                if (offset < payload.size) {
                    delay(15.milliseconds)
                }
            }
            // Wait for the printer's Bluetooth RFCOMM buffer to finish draining before closing the socket
            delay(350.milliseconds)
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBluetoothSocket(
        device: android.bluetooth.BluetoothDevice,
        macAddress: String
    ): BluetoothSocket {
        var secureSocket: BluetoothSocket? = null
        try {
            secureSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            secureSocket.connect()
            return secureSocket
        } catch (_: IOException) {
            try {
                secureSocket?.close()
            } catch (_: Exception) {
            }
        }

        var insecureSocket: BluetoothSocket? = null
        try {
            insecureSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            insecureSocket.connect()
            return insecureSocket
        } catch (error: IOException) {
            try {
                insecureSocket?.close()
            } catch (_: Exception) {
            }
            val deviceLabel = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: macAddress
            throw IOException(
                "No se pudo conectar a la impresora Bluetooth ($deviceLabel). Verifica que esté encendida y cerca del teléfono.",
                error
            )
        }
    }

    private suspend fun printViaAndroidManager(document: ReceiptDocument): Result<Unit> = suspendCancellableCoroutine { continuation ->
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
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                        color = android.graphics.Color.BLACK
                        textSize = document.fontSize.toFloat()
                    }
                    val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                        pathEffect = DashPathEffect(floatArrayOf(5f, 3f), 0f)
                    }
                    val contentWidth = page.info.pageWidth.toFloat()
                    val left = 16f
                    val right = (contentWidth - 16f).coerceAtLeast(left + 40f)
                    val usableWidth = right - left
                    var y = 24f + document.fontSize
                    val lineHeight = document.fontSize * 1.4f

                    document.lines.forEach { line ->
                        if (line.isSeparator && document.fontFamily == ReceiptFontFamily.MANROPE) {
                            val sepY = y - (document.fontSize * 0.35f)
                            canvas.drawLine(left, sepY, right, sepY, separatorPaint)
                            y += lineHeight
                            return@forEach
                        }

                        paint.typeface = resolveTypeface(context, document.fontFamily, line.emphasized)
                        paint.isFakeBoldText = line.emphasized

                        val leftCol = line.leftText
                        val rightCol = line.rightText
                        if (leftCol != null && rightCol != null) {
                            val rightWidth = paint.measureText(rightCol)
                            val rightX = (right - rightWidth).coerceAtLeast(left)
                            canvas.drawText(rightCol, rightX, y, paint)

                            val maxLeftWidth = (rightX - left - 6f).coerceAtLeast(0f)
                            if (maxLeftWidth > 0f) {
                                val fittedLeft = fitTextToWidth(leftCol, paint, maxLeftWidth)
                                canvas.drawText(fittedLeft, left, y, paint)
                            }
                        } else if (line.text.isNotEmpty()) {
                            val fitted = fitTextToWidth(line.text, paint, usableWidth)
                            val textWidth = paint.measureText(fitted)
                            val x = when (line.alignment) {
                                ReceiptAlignment.CENTER -> left + (usableWidth - textWidth) / 2f
                                ReceiptAlignment.RIGHT -> right - textWidth
                                ReceiptAlignment.LEFT -> left
                            }.coerceAtLeast(left)
                            canvas.drawText(fitted, x, y, paint)
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

    private fun isBluetoothTarget(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val trimmed = value.trim()
        if (trimmed.startsWith("bt://", ignoreCase = true)) return true
        val macRegex = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
        return macRegex.matches(trimmed)
    }

    private fun extractBluetoothMac(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith("bt://", ignoreCase = true)) {
            trimmed.substring(5).trim()
        } else {
            trimmed
        }
    }

    private fun isIpPort(value: String?): Boolean {
        if (value == null) return false
        val regex = Regex("^(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?$")
        return regex.matches(value.trim())
    }
}
