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
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.print.DocFlavor
import javax.print.SimpleDoc

actual fun createReceiptPrinter(): ReceiptPrinter = JvmReceiptPrinter()

private class JvmReceiptPrinter : ReceiptPrinter {

    override suspend fun print(document: ReceiptDocument): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val target = document.printerId?.trim()

                when {
                    // 1. Direct Character Device (e.g. direct:/dev/usb/lp0 or /dev/usb/lp0)
                    target?.startsWith("direct:") == true || target?.startsWith("/dev/") == true -> {
                        val devicePath = if (target.startsWith("direct:")) target.removePrefix("direct:") else target
                        printDirectToDevice(devicePath, document)
                    }

                    // 2. Direct Network Printer (e.g. tcp://192.168.1.100:9100 or 192.168.1.100:9100)
                    target?.startsWith("tcp://") == true || isIpPort(target) -> {
                        val hostPort = if (target?.startsWith("tcp://") == true) target.removePrefix("tcp://") else target ?: ""
                        printDirectToNetwork(hostPort, document)
                    }

                    // 3. Named System Print Service or System Dialog
                    else -> {
                        val isSystemDialog = target.isNullOrBlank() ||
                                target == PRINTER_SYSTEM_DIALOG_ID ||
                                target == "android-system"

                        if (!isSystemDialog) {
                            // Attempt raw ESC/POS via PrintService AUTOSENSE first
                            val rawResult = printRawToPrintService(target, document)
                            if (rawResult.isSuccess) {
                                return@runCatching
                            }
                        }

                        // Fall back to standard AWT Graphics2D rendering
                        printAwtGraphics(document, isSystemDialog)
                    }
                }
            }
        }
    }

    override suspend fun openCashDrawer(printerId: String?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val target = printerId?.trim()
                val kickBytes = EscPosReceiptEncoder.encodeDrawerKick()

                when {
                    // 1. Direct Character Device (e.g. direct:/dev/usb/lp0 or /dev/usb/lp0)
                    target?.startsWith("direct:") == true || target?.startsWith("/dev/") == true -> {
                        val devicePath = if (target.startsWith("direct:")) target.removePrefix("direct:") else target
                        val file = File(devicePath)
                        if (!file.exists()) {
                            throw IllegalStateException("El dispositivo $devicePath no está disponible.")
                        }
                        FileOutputStream(file).use { stream ->
                            stream.write(kickBytes)
                            stream.flush()
                        }
                    }

                    // 2. Direct Network Printer (e.g. tcp://192.168.1.100:9100 or 192.168.1.100:9100)
                    target?.startsWith("tcp://") == true || isIpPort(target) -> {
                        val hostPort = if (target?.startsWith("tcp://") == true) target.removePrefix("tcp://") else target ?: ""
                        val parts = hostPort.split(":")
                        val host = parts[0].trim()
                        val port = parts.getOrNull(1)?.toIntOrNull() ?: 9100
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(host, port), 4000)
                            socket.getOutputStream().use { out ->
                                out.write(kickBytes)
                                out.flush()
                            }
                        }
                    }

                    // 3. Named System Print Service or System Dialog
                    else -> {
                        val isSystemDialog = target.isNullOrBlank() ||
                                target == PRINTER_SYSTEM_DIALOG_ID ||
                                target == "android-system"

                        if (!isSystemDialog) {
                            val service = PrinterJob.lookupPrintServices()
                                .firstOrNull { it.name.equals(target, ignoreCase = true) }
                                ?: throw IllegalStateException("Impresora no encontrada")

                            val flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE
                            if (service.isDocFlavorSupported(flavor)) {
                                val doc = SimpleDoc(kickBytes, flavor, null)
                                val job = service.createPrintJob()
                                job.print(doc, null)
                            } else {
                                throw UnsupportedOperationException("AUTOSENSE no soportado en la impresora")
                            }
                        } else {
                            val defaultService = PrinterJob.lookupPrintServices()
                                .firstOrNull { it.name.equals(PrinterJob.getPrinterJob().printService?.name, ignoreCase = true) }
                                ?: PrinterJob.lookupPrintServices().firstOrNull()
                                ?: throw IllegalStateException("No hay impresoras disponibles para abrir el cajón")

                            val flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE
                            if (defaultService.isDocFlavorSupported(flavor)) {
                                val doc = SimpleDoc(kickBytes, flavor, null)
                                val job = defaultService.createPrintJob()
                                job.print(doc, null)
                            } else {
                                throw UnsupportedOperationException("La impresora no admite comandos directos ESC/POS")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun printDirectToDevice(devicePath: String, document: ReceiptDocument) {
        val file = File(devicePath)
        if (!file.exists()) {
            throw IllegalStateException("El dispositivo $devicePath no está disponible.")
        }
        val escPosBytes = EscPosReceiptEncoder.encode(document)
        FileOutputStream(file).use { stream ->
            stream.write(escPosBytes)
            stream.flush()
        }
    }

    private fun printDirectToNetwork(hostPort: String, document: ReceiptDocument) {
        val parts = hostPort.split(":")
        val host = parts[0].trim()
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 9100

        val escPosBytes = EscPosReceiptEncoder.encode(document)
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 4000)
            socket.getOutputStream().use { out ->
                out.write(escPosBytes)
                out.flush()
            }
        }
    }

    private fun printRawToPrintService(printerName: String, document: ReceiptDocument): Result<Unit> {
        return runCatching {
            val service = PrinterJob.lookupPrintServices()
                .firstOrNull { it.name.equals(printerName, ignoreCase = true) }
                ?: return Result.failure(IllegalStateException("Impresora no encontrada"))

            val flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE
            if (service.isDocFlavorSupported(flavor)) {
                val escPosBytes = EscPosReceiptEncoder.encode(document)
                val doc = SimpleDoc(escPosBytes, flavor, null)
                val job = service.createPrintJob()
                job.print(doc, null)
            } else {
                throw UnsupportedOperationException("AUTOSENSE no soportado")
            }
        }
    }

    private fun printAwtGraphics(document: ReceiptDocument, isSystemDialog: Boolean) {
        val printerJob = PrinterJob.getPrinterJob()

        if (!isSystemDialog) {
            val selectedService = PrinterJob.lookupPrintServices()
                .firstOrNull { it.name == document.printerId }
                ?: throw IllegalStateException("La impresora seleccionada no está disponible")
            printerJob.printService = selectedService
        } else if (printerJob.printService == null) {
            throw IllegalStateException("No hay una impresora predeterminada configurada")
        }

        printerJob.jobName = "Ticket ${document.folio}"
        val pageFormat = printerJob.defaultPage()
        pageFormat.paper = createPaper(document)
        printerJob.setPrintable({ graphics, format, pageIndex ->
            if (pageIndex > 0) return@setPrintable Printable.NO_SUCH_PAGE
            val graphics2d = graphics as Graphics2D
            graphics2d.font = Font(Font.MONOSPACED, Font.PLAIN, document.fontSize)
            graphics2d.paint = java.awt.Color.BLACK
            val metrics = graphics2d.fontMetrics
            val left = format.imageableX
            val right = format.imageableX + format.imageableWidth
            var y = format.imageableY + metrics.ascent
            val lineHeight = (metrics.height * 1.25).toInt().coerceAtLeast(1)

            document.lines.forEach { line ->
                graphics2d.font = Font(Font.MONOSPACED, if (line.emphasized) Font.BOLD else Font.PLAIN, document.fontSize)
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
            if (!shouldPrint) return
        }

        printerJob.print()
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

    private fun isIpPort(value: String?): Boolean {
        if (value == null) return false
        val regex = Regex("^(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?$")
        return regex.matches(value.trim())
    }
}
