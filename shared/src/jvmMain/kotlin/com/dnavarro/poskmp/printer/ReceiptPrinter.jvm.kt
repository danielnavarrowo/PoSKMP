package com.dnavarro.poskmp.printer

import com.dnavarro.poskmp.domain.model.PRINTER_SYSTEM_DIALOG_ID
import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.font.TextAttribute
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import java.awt.print.Book
import java.awt.print.PageFormat
import java.awt.print.Paper
import java.awt.print.Printable
import java.awt.print.PrinterJob
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.print.DocFlavor
import javax.print.SimpleDoc
import kotlin.math.ceil

actual fun createReceiptPrinter(): ReceiptPrinter = JvmReceiptPrinter()

private const val MANROPE_RESOURCE_PATH = "composeResources/poskmp.shared.generated.resources/font/Manrope.ttf"

private class JvmReceiptPrinter : ReceiptPrinter {

    @Volatile
    private var cachedManropeBase: Font? = null

    private fun getManropeBaseFont(): Font? {
        cachedManropeBase?.let { return it }
        val loaded = runCatching {
            val stream = Thread.currentThread().contextClassLoader?.getResourceAsStream(MANROPE_RESOURCE_PATH)
                ?: JvmReceiptPrinter::class.java.classLoader?.getResourceAsStream(MANROPE_RESOURCE_PATH)
                ?: JvmReceiptPrinter::class.java.getResourceAsStream("/$MANROPE_RESOURCE_PATH")
            stream?.use { input ->
                Font.createFont(Font.TRUETYPE_FONT, input).also { font ->
                    try {
                        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)
                    } catch (_: Exception) {
                    }
                }
            }
        }.getOrNull()
        if (loaded != null) {
            cachedManropeBase = loaded
        }
        return loaded
    }

    private fun resolveAwtFont(
        fontFamily: ReceiptFontFamily,
        emphasized: Boolean,
        sizePt: Float
    ): Font {
        if (fontFamily == ReceiptFontFamily.MONOSPACE) {
            val style = if (emphasized) Font.BOLD else Font.PLAIN
            return Font(Font.MONOSPACED, style, sizePt.toInt().coerceAtLeast(6)).deriveFont(sizePt)
        }

        val base = getManropeBaseFont()
        if (base != null) {
            val attrs = mapOf(
                TextAttribute.SIZE to sizePt,
                TextAttribute.WEIGHT to if (emphasized) TextAttribute.WEIGHT_BOLD else TextAttribute.WEIGHT_MEDIUM
            )
            return base.deriveFont(attrs)
        }

        val fallbackStyle = if (emphasized) Font.BOLD else Font.PLAIN
        return Font(Font.SANS_SERIF, fallbackStyle, sizePt.toInt().coerceAtLeast(6)).deriveFont(sizePt)
    }

    private fun encodeThermalPayload(document: ReceiptDocument, cutPaper: Boolean): ByteArray {
        return if (document.fontFamily == ReceiptFontFamily.MANROPE) {
            renderReceiptToEscPosRaster(document, cutPaper)
        } else {
            EscPosReceiptEncoder.encode(document, cutPaper = cutPaper)
        }
    }

    private fun renderReceiptToEscPosRaster(
        document: ReceiptDocument,
        cutPaper: Boolean
    ): ByteArray {
        val widthDots = EscPosReceiptEncoder.printableWidthDots(document.paperWidthMm)
        val pixelFontSize = (document.fontSize * 2f).coerceIn(16f, 64f)
        val lineHeight = ceil(pixelFontSize * 1.45f).toInt().coerceAtLeast(20)
        val separatorHeight = ceil(pixelFontSize * 0.95f).toInt().coerceAtLeast(14)

        val totalHeight = (document.lines.sumOf { line ->
            if (line.isSeparator) separatorHeight else lineHeight
        } + 16).coerceAtLeast(32)

        val image = BufferedImage(widthDots, totalHeight, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        try {
            g2d.color = Color.WHITE
            g2d.fillRect(0, 0, widthDots, totalHeight)
            g2d.color = Color.BLACK
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            val dashedStroke = BasicStroke(
                2f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10f,
                floatArrayOf(10f, 6f),
                0f
            )

            val leftMargin = 0f
            val rightMargin = widthDots.toFloat()
            var currentTop = 8f

            for ((text, alignment, emphasized, leftCol, rightCol, isSeparator) in document.lines) {
                if (isSeparator) {
                    val oldStroke = g2d.stroke
                    g2d.stroke = dashedStroke
                    val centerY = currentTop + separatorHeight / 2f
                    g2d.draw(Line2D.Float(leftMargin, centerY, rightMargin, centerY))
                    g2d.stroke = oldStroke
                    currentTop += separatorHeight
                    continue
                }

                g2d.font = resolveAwtFont(document.fontFamily, emphasized, pixelFontSize)
                val fm = g2d.fontMetrics
                val baselineY = currentTop + fm.ascent

                if (leftCol != null && rightCol != null) {
                    val rightWidth = fm.stringWidth(rightCol).toFloat()
                    val rightX = (rightMargin - rightWidth).coerceAtLeast(leftMargin)
                    g2d.drawString(rightCol, rightX, baselineY)

                    val maxLeftWidth = (rightX - leftMargin - 12f).coerceAtLeast(0f)
                    if (maxLeftWidth > 0f) {
                        val fittedLeft = fitTextToWidth(leftCol, fm, maxLeftWidth)
                        g2d.drawString(fittedLeft, leftMargin, baselineY)
                    }
                } else if (text.isNotEmpty()) {
                    val fitted = fitTextToWidth(text, fm, rightMargin - leftMargin)
                    val textWidth = fm.stringWidth(fitted).toFloat()
                    val x = when (alignment) {
                        ReceiptAlignment.CENTER -> leftMargin + ((rightMargin - leftMargin) - textWidth) / 2f
                        ReceiptAlignment.RIGHT -> rightMargin - textWidth
                        ReceiptAlignment.LEFT -> leftMargin
                    }.coerceAtLeast(leftMargin)
                    g2d.drawString(fitted, x, baselineY)
                }

                currentTop += lineHeight
            }
        } finally {
            g2d.dispose()
        }

        val pixels = image.getRGB(0, 0, widthDots, totalHeight, null, 0, widthDots)
        return EscPosReceiptEncoder.encodeRasterBitmap(
            widthDots = widthDots,
            heightDots = totalHeight,
            openDrawer = document.openCashDrawer,
            feedLines = document.feedLines,
            cutPaper = cutPaper
        ) { x, y ->
            val rgb = pixels[y * widthDots + x]
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF
            val luma = (r * 299 + g * 587 + b * 114) / 1000
            luma < 165
        }
    }

    private fun fitTextToWidth(text: String, fm: FontMetrics, maxWidth: Float): String {
        if (fm.stringWidth(text) <= maxWidth) return text
        val ellipsis = "…"
        val ellipsisWidth = fm.stringWidth(ellipsis)
        if (maxWidth <= ellipsisWidth) return ""
        val targetWidth = maxWidth - ellipsisWidth
        var end = text.length
        while (end > 0 && fm.stringWidth(text.substring(0, end)) > targetWidth) {
            end--
        }
        return if (end <= 0) "" else text.substring(0, end).trimEnd() + ellipsis
    }

    override suspend fun print(document: ReceiptDocument): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val target = document.printerId?.trim()

                when {
                    // 1. Direct Character Device (e.g. direct:/dev/usb/lp0, /dev/usb/lp0, /dev/rfcomm0)
                    target?.startsWith("direct:") == true || target?.startsWith("/dev/") == true -> {
                        val devicePath = if (target.startsWith("direct:")) target.removePrefix("direct:") else target
                        printDirectToDevice(devicePath, document)
                    }

                    // 2. Direct Network Printer (e.g. tcp://192.168.1.100:9100 or 192.168.1.100:9100)
                    target?.startsWith("tcp://") == true || isIpPort(target) -> {
                        val hostPort = if (target?.startsWith("tcp://") == true) target.removePrefix("tcp://") else target ?: ""
                        printDirectToNetwork(hostPort, document)
                    }

                    // 3. Explicit Raw ESC/POS System Print Service (raw:<ServiceName>)
                    target?.startsWith("raw:") == true -> {
                        val serviceName = target.removePrefix("raw:").trim()
                        printRawToPrintService(serviceName, document).getOrThrow()
                    }

                    // 4. Named System Print Service or System Dialog (AWT Graphics2D pipeline)
                    else -> {
                        val isSystemDialog = target.isNullOrBlank() ||
                                target == PRINTER_SYSTEM_DIALOG_ID ||
                                target == "android-system"

                        printAwtGraphics(document, isSystemDialog)

                        if (document.openCashDrawer && !isSystemDialog && target.isNotBlank()) {
                            try {
                                sendDrawerKickToPrintService(target)
                            } catch (_: Exception) {
                            }
                        }
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
                        if (!file.canWrite()) {
                            throw IllegalStateException("Sin permisos de escritura en $devicePath (verifica permisos del grupo 'lp' o 'dialout').")
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
                            socket.soTimeout = 5000
                            socket.getOutputStream().use { out ->
                                out.write(kickBytes)
                                out.flush()
                            }
                        }
                    }

                    // 3. Named System Print Service or System Dialog
                    else -> {
                        val cleanTarget = target?.removePrefix("raw:")?.trim()
                        val isSystemDialog = cleanTarget.isNullOrBlank() ||
                                cleanTarget == PRINTER_SYSTEM_DIALOG_ID ||
                                cleanTarget == "android-system"

                        if (!isSystemDialog) {
                            sendDrawerKickToPrintService(cleanTarget)
                        } else {
                            val defaultName = runCatching { PrinterJob.getPrinterJob().printService?.name }.getOrNull()
                            val defaultService = PrinterJob.lookupPrintServices()
                                .firstOrNull { defaultName != null && it.name.equals(defaultName, ignoreCase = true) }
                                ?: PrinterJob.lookupPrintServices().firstOrNull()
                                ?: throw IllegalStateException("No hay impresoras disponibles para abrir el cajón")

                            sendDrawerKickToPrintService(defaultService.name)
                        }
                    }
                }
            }
        }
    }

    private fun sendDrawerKickToPrintService(serviceName: String) {
        val service = PrinterJob.lookupPrintServices()
            .firstOrNull { it.name.equals(serviceName, ignoreCase = true) }
            ?: throw IllegalStateException("Impresora '$serviceName' no encontrada")

        val flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE
        if (service.isDocFlavorSupported(flavor)) {
            val kickBytes = EscPosReceiptEncoder.encodeDrawerKick()
            val doc = SimpleDoc(kickBytes, flavor, null)
            val job = service.createPrintJob()
            job.print(doc, null)
        } else {
            throw UnsupportedOperationException("La impresora '$serviceName' no admite comandos directos ESC/POS")
        }
    }

    private fun printDirectToDevice(devicePath: String, document: ReceiptDocument) {
        val file = File(devicePath)
        if (!file.exists()) {
            throw IllegalStateException("El dispositivo $devicePath no está disponible.")
        }
        if (!file.canWrite()) {
            throw IllegalStateException("Sin permisos de escritura en $devicePath (verifica permisos del grupo 'lp' o 'dialout').")
        }
        val isBtSerial = devicePath.contains("rfcomm", ignoreCase = true)
        val escPosBytes = encodeThermalPayload(document, cutPaper = !isBtSerial)
        FileOutputStream(file).use { stream ->
            stream.write(escPosBytes)
            stream.flush()
        }
    }

    private fun printDirectToNetwork(hostPort: String, document: ReceiptDocument) {
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

    private fun printRawToPrintService(printerName: String, document: ReceiptDocument): Result<Unit> {
        return runCatching {
            val service = PrinterJob.lookupPrintServices()
                .firstOrNull { it.name.equals(printerName, ignoreCase = true) }
                ?: return Result.failure(IllegalStateException("Impresora '$printerName' no encontrada"))

            val flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE
            if (service.isDocFlavorSupported(flavor)) {
                val escPosBytes = encodeThermalPayload(document, cutPaper = true)
                val doc = SimpleDoc(escPosBytes, flavor, null)
                val job = service.createPrintJob()
                job.print(doc, null)
            } else {
                throw UnsupportedOperationException("El modo RAW (AUTOSENSE) no está soportado por '$printerName'")
            }
        }
    }

    private fun printAwtGraphics(document: ReceiptDocument, isSystemDialog: Boolean) {
        val printerJob = PrinterJob.getPrinterJob()

        if (!isSystemDialog) {
            val cleanName = document.printerId?.removePrefix("raw:")?.trim()
            val selectedService = PrinterJob.lookupPrintServices()
                .firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
                ?: throw IllegalStateException("La impresora seleccionada (${cleanName ?: ""}) no está disponible")
            printerJob.printService = selectedService
        } else if (printerJob.printService == null) {
            val firstAvailable = PrinterJob.lookupPrintServices().firstOrNull()
                ?: throw IllegalStateException("No hay ninguna impresora configurada en el sistema")
            printerJob.printService = firstAvailable
        }

        if (isSystemDialog) {
            val shouldPrint = printerJob.printDialog()
            if (!shouldPrint) return
        }

        printerJob.jobName = "Ticket ${document.folio}"
        val rawPageFormat = printerJob.defaultPage().apply {
            orientation = PageFormat.PORTRAIT
            paper = createPaper(document)
        }
        val validatedFormat = printerJob.validatePage(rawPageFormat)
        // Ensure custom paper margins are preserved if the driver shrank imageableWidth excessively
        val finalPageFormat = if (validatedFormat.imageableWidth < rawPageFormat.imageableWidth * 0.6) {
            rawPageFormat
        } else {
            validatedFormat
        }

        val printable = Printable { graphics, format, pageIndex ->
            if (pageIndex > 0) return@Printable Printable.NO_SUCH_PAGE
            val g2d = graphics as Graphics2D
            g2d.color = Color.BLACK
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            val left = format.imageableX.toFloat()
            val usableWidth = format.imageableWidth.toFloat().coerceAtLeast(72f)
            val right = left + usableWidth

            // Auto-scale font size if the longest line exceeds the printable width in MONOSPACE mode
            var effectiveFontSize = document.fontSize.toFloat()
            if (document.fontFamily == ReceiptFontFamily.MONOSPACE) {
                val probeFont = resolveAwtFont(document.fontFamily, emphasized = true, sizePt = effectiveFontSize)
                val probeMetrics = g2d.getFontMetrics(probeFont)
                val widestLine = document.lines.maxOfOrNull { probeMetrics.stringWidth(it.text) } ?: 0
                if (widestLine > usableWidth && widestLine > 0) {
                    effectiveFontSize = (effectiveFontSize * (usableWidth / widestLine)).coerceAtLeast(6f)
                }
            }

            val baseMetrics = g2d.getFontMetrics(resolveAwtFont(document.fontFamily, false, effectiveFontSize))
            val lineHeight = (baseMetrics.height * 1.32f).coerceAtLeast(effectiveFontSize * 1.3f)
            val separatorHeight = (effectiveFontSize * 0.95f).coerceAtLeast(8f)
            var currentTop = format.imageableY.toFloat() + 4f

            val dashedStroke = BasicStroke(
                0.8f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10f,
                floatArrayOf(4f, 3f),
                0f
            )

            for ((text, alignment, emphasized, leftCol, rightCol, isSeparator) in document.lines) {
                if (isSeparator && document.fontFamily == ReceiptFontFamily.MANROPE) {
                    val oldStroke = g2d.stroke
                    g2d.stroke = dashedStroke
                    val sepY = currentTop + separatorHeight / 2f
                    g2d.draw(Line2D.Float(left, sepY, right, sepY))
                    g2d.stroke = oldStroke
                    currentTop += separatorHeight
                    continue
                }

                g2d.font = resolveAwtFont(document.fontFamily, emphasized, effectiveFontSize)
                val fm = g2d.fontMetrics
                val baselineY = currentTop + fm.ascent

                if (leftCol != null && rightCol != null) {
                    val rightWidth = fm.stringWidth(rightCol).toFloat()
                    val rightX = (right - rightWidth).coerceAtLeast(left)
                    g2d.drawString(rightCol, rightX, baselineY)

                    val maxLeftWidth = (rightX - left - 6f).coerceAtLeast(0f)
                    if (maxLeftWidth > 0f) {
                        val fittedLeft = fitTextToWidth(leftCol, fm, maxLeftWidth)
                        g2d.drawString(fittedLeft, left, baselineY)
                    }
                } else if (text.isNotEmpty()) {
                    val fitted = fitTextToWidth(text, fm, usableWidth)
                    val textWidth = fm.stringWidth(fitted).toFloat()
                    val x = when (alignment) {
                        ReceiptAlignment.CENTER -> left + (usableWidth - textWidth) / 2f
                        ReceiptAlignment.RIGHT -> right - textWidth
                        ReceiptAlignment.LEFT -> left
                    }.coerceAtLeast(left)
                    g2d.drawString(fitted, x, baselineY)
                }

                currentTop += lineHeight
            }
            Printable.PAGE_EXISTS
        }

        val book = Book()
        book.append(printable, finalPageFormat)
        printerJob.setPageable(book)
        printerJob.print()
    }

    private fun createPaper(document: ReceiptDocument): Paper {
        val paper = Paper()
        val widthPt = document.paperWidthMm / 25.4 * 72.0
        val lineHeightPt = document.fontSize * 1.45
        val heightPt = (document.lines.size * lineHeightPt + 48.0).coerceAtLeast(200.0)
        val marginPt = 6.0 // ~2.1mm side margin for thermal roll printers
        paper.setSize(widthPt, heightPt)
        paper.setImageableArea(
            marginPt,
            marginPt,
            (widthPt - marginPt * 2).coerceAtLeast(72.0),
            (heightPt - marginPt * 2).coerceAtLeast(100.0)
        )
        return paper
    }

    private fun isIpPort(value: String?): Boolean {
        if (value == null) return false
        val regex = Regex("^(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?$")
        return regex.matches(value.trim())
    }
}
