package com.dnavarro.poskmp.util

import com.dnavarro.poskmp.db.Products
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import poskmp.shared.generated.resources.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.swing.SwingUtilities
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

import androidx.compose.runtime.Composable

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun generateUUID(): String = UUID.randomUUID().toString()

@Suppress("SameReturnValue")
actual fun isAndroid(): Boolean = false

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on Desktop JVM
}

actual fun playSoundAlert(bytes: ByteArray) {
    kotlin.concurrent.thread(isDaemon = true) {
        try {
            val player = javazoom.jl.player.Player(ByteArrayInputStream(bytes))
            player.play()
        } catch (_: Exception) {}
    }
}

actual fun pickFile(
    allowedExtensions: List<String>,
    onFilePicked: (fileName: String, content: ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    SwingUtilities.invokeLater {
        try {
            val title = runBlocking { getString(Res.string.select_file_dialog_title) }
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            dialog.isVisible = true
            val file = dialog.file
            val directory = dialog.directory
            if (file != null && directory != null) {
                val selectedFile = File(directory, file)
                val extension = selectedFile.extension.lowercase()
                if (allowedExtensions.isEmpty() || allowedExtensions.any { it.equals(extension, ignoreCase = true) }) {
                    val bytes = selectedFile.readBytes()
                    onFilePicked(file, bytes)
                } else {
                    val err = runBlocking { getString(Res.string.unsupported_file_format, extension) }
                    onError(err)
                }
            }
        } catch (e: Exception) {
            val err = runBlocking { getString(Res.string.file_explorer_open_error, e.message ?: "") }
            onError(err)
        }
    }
}

actual fun saveFile(
    defaultFileName: String,
    content: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    SwingUtilities.invokeLater {
        try {
            val title = runBlocking { getString(Res.string.save_file_dialog_title) }
            val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
            dialog.file = defaultFileName
            dialog.isVisible = true
            val file = dialog.file
            val directory = dialog.directory
            if (file != null && directory != null) {
                val destinationFile = File(directory, file)
                destinationFile.writeText(content, Charsets.UTF_8)
                onSuccess()
            }
        } catch (e: Exception) {
            val err = runBlocking { getString(Res.string.save_file_error, e.message ?: "") }
            onError(err)
        }
    }
}

actual fun pickDirectory(
    initialPath: String,
    onDirectoryPicked: (path: String) -> Unit,
    onError: (String) -> Unit
) {
    SwingUtilities.invokeLater {
        try {
            val chooser = javax.swing.JFileChooser().apply {
                fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                dialogTitle = runBlocking { getString(Res.string.change_backup_path_dialog_title) }
                isAcceptAllFileFilterUsed = false
                val initFile = File(initialPath).takeIf { it.exists() }
                if (initFile != null) {
                    currentDirectory = initFile
                    selectedFile = initFile
                }
            }
            val result = chooser.showOpenDialog(null)
            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                val selected = chooser.selectedFile
                if (selected != null) {
                    onDirectoryPicked(selected.absolutePath)
                }
            }
        } catch (e: Exception) {
            onError(e.message ?: "Error al seleccionar carpeta")
        }
    }
}

actual fun parseImportFile(
    fileName: String,
    content: ByteArray
): List<Products> {
    return when (val extension = fileName.substringAfterLast(".", "").lowercase()) {
        "csv" -> parseCsvContent(content)
        "xlsx" -> parseXlsxContent(content)
        else -> {
            val err = runBlocking { getString(Res.string.unsupported_file_format, extension) }
            throw IllegalArgumentException(err)
        }
    }
}

private fun parseCsvContent(content: ByteArray): List<Products> {
    val text = String(content, Charsets.UTF_8)
    val lines = text.split(Regex("\\r?\\n"))
    if (lines.isEmpty()) {
        val err = runBlocking { getString(Res.string.csv_empty_error) }
        throw Exception(err)
    }
    
    val headerLine = lines.firstOrNull { it.trim().isNotEmpty() } ?: run {
        val err = runBlocking { getString(Res.string.csv_no_header_error) }
        throw Exception(err)
    }
    val headerCols = parseCsvLine(headerLine).map { it.lowercase().trim() }
    
    val nameIndex = headerCols.indexOfFirst { it == "nombre" || it == "name" }
    val priceIndex = headerCols.indexOfFirst { it == "precio" || it == "price" || it == "precio_venta" }
    
    if (nameIndex == -1 || priceIndex == -1) {
        val err = runBlocking { getString(Res.string.csv_invalid_headers_error) }
        throw Exception(err)
    }
    
    val idIndex = headerCols.indexOfFirst { it == "id" }
    val codesIndex = headerCols.indexOfFirst { it == "codigos" || it == "codigo" || it == "barcodes" || it == "barcode" }
    val costIndex = headerCols.indexOfFirst { it == "costo" || it == "cost" }
    val categoryIndex = headerCols.indexOfFirst { it == "categoria" || it == "category" }
    val activeIndex = headerCols.indexOfFirst { it == "activo" || it == "active" }
    val weightIndex = headerCols.indexOfFirst { it == "por_peso" || it == "by_weight" }
    val wholesaleIndex = headerCols.indexOfFirst { it == "precio_mayoreo" || it == "wholesale_price" }
    val favoriteIndex = headerCols.indexOfFirst { it == "es_favorito" || it == "favorite" }
    val piezasIndex = headerCols.indexOfFirst { it == "piezas" || it == "pieces" }

    val products = mutableListOf<Products>()
    val startIndex = lines.indexOf(headerLine) + 1
    
    for (i in startIndex until lines.size) {
        val line = lines[i].trim()
        if (line.isEmpty()) continue
        
        val cols = parseCsvLine(line)
        if (cols.size <= maxOf(nameIndex, priceIndex)) continue
        
        val nombre = cols.getOrNull(nameIndex)?.trim() ?: continue
        if (nombre.isEmpty()) continue
        
        val precioStr = cols.getOrNull(priceIndex)?.trim() ?: "0"
        val precio = precioStr.toDoubleOrNull() ?: 0.0
        
        val id = if (idIndex != -1) cols.getOrNull(idIndex)?.trim()?.ifEmpty { generateUUID() } ?: generateUUID() else generateUUID()
        
        val codigos = if (codesIndex != -1) {
            val rawCodes = cols.getOrNull(codesIndex)?.trim() ?: "[]"
            if (rawCodes.startsWith("[")) rawCodes else "[\"$rawCodes\"]"
        } else "[]"
        
        val costo = if (costIndex != -1) cols.getOrNull(costIndex)?.toDoubleOrNull() ?: 0.0 else 0.0
        val category = if (categoryIndex != -1) cols.getOrNull(categoryIndex)?.trim() ?: "" else ""
        
        val active = if (activeIndex != -1) {
            val activeStr = cols.getOrNull(activeIndex)?.trim() ?: "1"
            if (activeStr == "1" || activeStr == "1.0" || activeStr.lowercase() == "true") 1L else 0L
        } else 1L
        
        val porPeso = if (weightIndex != -1) {
            val weightStr = cols.getOrNull(weightIndex)?.trim() ?: "0"
            if (weightStr == "1" || weightStr == "1.0" || weightStr.lowercase() == "true") 1L else 0L
        } else 0L
        
        val precioMayoreo = if (wholesaleIndex != -1) cols.getOrNull(wholesaleIndex)?.toDoubleOrNull() ?: 0.0 else 0.0
        
        val esFavorito = if (favoriteIndex != -1) {
            val favStr = cols.getOrNull(favoriteIndex)?.trim() ?: "0"
            if (favStr == "1" || favStr == "1.0" || favStr.lowercase() == "true") 1L else 0L
        } else 0L
        
        val piezas = if (piezasIndex != -1) cols.getOrNull(piezasIndex)?.toDoubleOrNull() ?: 1.0 else 1.0

        products.add(
            Products(
                id = id,
                codigos = codigos,
                nombre = nombre,
                precio = precio,
                costo = costo,
                categoria = category,
                activo = active,
                por_peso = porPeso,
                precio_mayoreo = precioMayoreo,
                es_favorito = esFavorito,
                piezas = piezas,
                updated_at = currentTimeMillis(),
                sync_state = "PENDING_INSERT"
            )
        )
    }
    
    return products
}

private fun parseXlsxContent(content: ByteArray): List<Products> {
    val sharedStrings = mutableListOf<String>()
    var sheetXmlBytes: ByteArray? = null

    try {
        ZipInputStream(ByteArrayInputStream(content)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    val entryBytes = zip.readBytes()
                    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(entryBytes))
                    val tList = doc.getElementsByTagName("t")
                    for (i in 0 until tList.length) {
                        sharedStrings.add(tList.item(i).textContent)
                    }
                } else if (entry.name == "xl/worksheets/sheet1.xml") {
                    sheetXmlBytes = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
    } catch (e: Exception) {
        val err = runBlocking { getString(Res.string.excel_read_error, e.message ?: "") }
        throw Exception(err)
    }

    if (sheetXmlBytes == null) {
        val err = runBlocking { getString(Res.string.excel_no_sheet_error) }
        throw Exception(err)
    }

    val rows = mutableListOf<List<String>>()
    try {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(sheetXmlBytes))
        val rowList = doc.getElementsByTagName("row")
        for (i in 0 until rowList.length) {
            val rowEl = rowList.item(i) as Element
            val cellList = rowEl.getElementsByTagName("c")
            
            val rowData = mutableMapOf<Int, String>()
            var maxColIndex = -1
            for (j in 0 until cellList.length) {
                val cellEl = cellList.item(j) as Element
                val ref = cellEl.getAttribute("r")
                val colLetter = ref.filter { it.isLetter() }
                val colIndex = excelColLetterToIndex(colLetter)
                if (colIndex > maxColIndex) maxColIndex = colIndex
                
                val type = cellEl.getAttribute("t")
                val vEl = cellEl.getElementsByTagName("v").item(0)
                val rawValue = vEl?.textContent ?: ""
                
                val value = if (type == "s" && rawValue.isNotEmpty()) {
                    val idx = rawValue.toIntOrNull()
                    if (idx != null && idx in sharedStrings.indices) {
                        sharedStrings[idx]
                    } else {
                        rawValue
                    }
                } else {
                    rawValue
                }
                rowData[colIndex] = value
            }
            
            val rowCells = ArrayList<String>(maxColIndex + 1)
            for (col in 0..maxColIndex) {
                rowCells.add(rowData[col] ?: "")
            }
            rows.add(rowCells)
        }
    } catch (e: Exception) {
        val err = runBlocking { getString(Res.string.excel_parse_error, e.message ?: "") }
        throw Exception(err)
    }

    if (rows.isEmpty()) {
        val err = runBlocking { getString(Res.string.excel_empty_rows_error) }
        throw Exception(err)
    }

    val headerRow = rows.first()
    val headerCols = headerRow.map { it.lowercase().trim() }
    
    val nameIndex = headerCols.indexOfFirst { it == "nombre" || it == "name" }
    val priceIndex = headerCols.indexOfFirst { it == "precio" || it == "price" || it == "precio_venta" }
    
    if (nameIndex == -1 || priceIndex == -1) {
        val err = runBlocking { getString(Res.string.excel_invalid_headers_error) }
        throw Exception(err)
    }
    
    val idIndex = headerCols.indexOfFirst { it == "id" }
    val codesIndex = headerCols.indexOfFirst { it == "codigos" || it == "codigo" || it == "barcodes" || it == "barcode" }
    val costIndex = headerCols.indexOfFirst { it == "costo" || it == "cost" }
    val categoryIndex = headerCols.indexOfFirst { it == "categoria" || it == "category" }
    val activeIndex = headerCols.indexOfFirst { it == "activo" || it == "active" }
    val weightIndex = headerCols.indexOfFirst { it == "por_peso" || it == "by_weight" }
    val wholesaleIndex = headerCols.indexOfFirst { it == "precio_mayoreo" || it == "wholesale_price" }
    val favoriteIndex = headerCols.indexOfFirst { it == "es_favorito" || it == "favorite" }
    val piezasIndex = headerCols.indexOfFirst { it == "piezas" || it == "pieces" }

    val products = mutableListOf<Products>()
    for (i in 1 until rows.size) {
        val cols = rows[i]
        if (cols.size <= maxOf(nameIndex, priceIndex)) continue
        
        val nombre = cols.getOrNull(nameIndex)?.trim() ?: continue
        if (nombre.isEmpty()) continue
        
        val precioStr = cols.getOrNull(priceIndex)?.trim() ?: "0"
        val precio = precioStr.toDoubleOrNull() ?: 0.0
        
        val id = if (idIndex != -1) cols.getOrNull(idIndex)?.trim()?.ifEmpty { generateUUID() } ?: generateUUID() else generateUUID()
        
        val codigos = if (codesIndex != -1) {
            val rawCodes = cols.getOrNull(codesIndex)?.trim() ?: "[]"
            if (rawCodes.startsWith("[")) rawCodes else "[\"$rawCodes\"]"
        } else "[]"
        
        val costo = if (costIndex != -1) cols.getOrNull(costIndex)?.toDoubleOrNull() ?: 0.0 else 0.0
        val category = if (categoryIndex != -1) cols.getOrNull(categoryIndex)?.trim() ?: "" else ""
        
        val active = if (activeIndex != -1) {
            val activeStr = cols.getOrNull(activeIndex)?.trim() ?: "1"
            if (activeStr == "1" || activeStr == "1.0" || activeStr.lowercase() == "true") 1L else 0L
        } else 1L
        
        val porPeso = if (weightIndex != -1) {
            val weightStr = cols.getOrNull(weightIndex)?.trim() ?: "0"
            if (weightStr == "1" || weightStr == "1.0" || weightStr.lowercase() == "true") 1L else 0L
        } else 0L
        
        val precioMayoreo = if (wholesaleIndex != -1) cols.getOrNull(wholesaleIndex)?.toDoubleOrNull() ?: 0.0 else 0.0
        
        val esFavorito = if (favoriteIndex != -1) {
            val favStr = cols.getOrNull(favoriteIndex)?.trim() ?: "0"
            if (favStr == "1" || favStr == "1.0" || favStr.lowercase() == "true") 1L else 0L
        } else 0L
        
        val piezas = if (piezasIndex != -1) cols.getOrNull(piezasIndex)?.toDoubleOrNull() ?: 1.0 else 1.0

        products.add(
            Products(
                id = id,
                codigos = codigos,
                nombre = nombre,
                precio = precio,
                costo = costo,
                categoria = category,
                activo = active,
                por_peso = porPeso,
                precio_mayoreo = precioMayoreo,
                es_favorito = esFavorito,
                piezas = piezas,
                updated_at = currentTimeMillis(),
                sync_state = "PENDING_INSERT"
            )
        )
    }
    
    return products
}

private fun excelColLetterToIndex(colLetter: String): Int {
    var index = 0
    for (char in colLetter) {
        index = index * 26 + (char - 'A' + 1)
    }
    return index - 1
}
