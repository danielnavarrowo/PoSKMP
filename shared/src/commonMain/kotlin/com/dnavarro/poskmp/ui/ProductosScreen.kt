package com.dnavarro.poskmp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProductosScreen(
    repository: ProductRepository,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var productsList by remember { mutableStateOf<List<Products>>(emptyList()) }

    // Dialog control states
    var showProductDialogFor by remember { mutableStateOf<Products?>(null) } // Null means not showing, a Product with empty ID means "New"
    var showImportExportDialog by remember { mutableStateOf(false) }

    // Form inputs state
    var formNombre by remember { mutableStateOf("") }
    var formCodigo by remember { mutableStateOf("") } // Comma-separated barcodes
    var formPrecio by remember { mutableStateOf("") }
    var formCosto by remember { mutableStateOf("") }
    var formPrecioMayoreo by remember { mutableStateOf("") }
    var formPrecioNota by remember { mutableStateOf("") }
    var formCategoria by remember { mutableStateOf("") }
    var formActivo by remember { mutableStateOf(true) }
    var formPorPeso by remember { mutableStateOf(false) }
    var formEsFavorito by remember { mutableStateOf(false) }

    // Import/Export state
    var csvText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var importSuccess by remember { mutableStateOf(false) }

    // Observe products from DB
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            repository.getAllProducts().collectLatest {
                productsList = it
            }
        } else {
            repository.searchProducts(searchQuery).collectLatest {
                productsList = it
            }
        }
    }

    // Populate form fields helper
    fun openProductForm(product: Products?) {
        if (product == null) {
            // New product
            formNombre = ""
            formCodigo = ""
            formPrecio = ""
            formCosto = ""
            formPrecioMayoreo = ""
            formPrecioNota = "Pza"
            formCategoria = "Abarrotes"
            formActivo = true
            formPorPeso = false
            formEsFavorito = false
            showProductDialogFor = Products("", "[]", "", 0.0, 0.0, "", "", 1L, 0L, 0.0, 0L, 0L, "")
        } else {
            // Edit existing product
            formNombre = product.nombre
            // Parse JSON array back to comma-separated list for easy editing
            formCodigo = try {
                product.codigos
                    .replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
            } catch (_: Exception) {
                ""
            }
            formPrecio = product.precio.toString()
            formCosto = product.costo.toString()
            formPrecioMayoreo = product.precio_mayoreo.toString()
            formPrecioNota = product.precio_nota ?: ""
            formCategoria = product.categoria ?: "Sin categoría"
            formActivo = product.activo == 1L
            formPorPeso = product.por_peso == 1L
            formEsFavorito = product.es_favorito == 1L
            showProductDialogFor = product
        }
    }

    // Save product to DB helper
    fun saveProduct() {
        val original = showProductDialogFor ?: return
        val id = original.id.ifEmpty { generateUUID() }
        
        // Format codes as JSON string array
        val formattedCodes = formCodigo.split(",")
            .map { it.trim().replace("\"", "") }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]") { it }
            .let { if (it == "[\"\"]") "[]" else it }

        val p = Products(
            id = id,
            codigos = formattedCodes,
            nombre = formNombre.trim(),
            precio = formPrecio.toDoubleOrNull() ?: 0.0,
            costo = formCosto.toDoubleOrNull() ?: 0.0,
            precio_nota = formPrecioNota.trim(),
            categoria = formCategoria.trim().ifEmpty { "Sin categoría" },
            activo = if (formActivo) 1L else 0L,
            por_peso = if (formPorPeso) 1L else 0L,
            precio_mayoreo = formPrecioMayoreo.toDoubleOrNull() ?: 0.0,
            es_favorito = if (formEsFavorito) 1L else 0L,
            updated_at = currentTimeMillis(),
            sync_state = if (original.id.isEmpty()) "PENDING_INSERT" else "PENDING_UPDATE"
        )

        if (original.id.isEmpty()) {
            repository.insertProduct(p)
        } else {
            repository.updateProduct(p)
        }
        showProductDialogFor = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Administración de Productos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Catálogo local de productos y control de inventario",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val csvBuilder = StringBuilder("id,codigos,nombre,precio,costo,precio_nota,categoria,activo,por_peso,precio_mayoreo,es_favorito\n")
                        for ((id, codigos, nombre, precio, costo, precio_nota, categoria, activo, por_peso, precio_mayoreo, es_favorito) in productsList) {
                            csvBuilder.append("$id,")
                            csvBuilder.append("\"${codigos.replace("\"", "\"\"")}\",")
                            csvBuilder.append("\"${nombre.replace("\"", "\"\"")}\",")
                            csvBuilder.append("$precio,")
                            csvBuilder.append("$costo,")
                            csvBuilder.append("\"${(precio_nota ?: "").replace("\"", "\"\"")}\",")
                            csvBuilder.append("\"${(categoria ?: "").replace("\"", "\"\"")}\",")
                            csvBuilder.append("$activo,")
                            csvBuilder.append("$por_peso,")
                            csvBuilder.append("$precio_mayoreo,")
                            csvBuilder.append("$es_favorito\n")
                        }
                        csvText = csvBuilder.toString()
                        importError = null
                        importSuccess = false
                        showImportExportDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importar / Exportar")
                }

                Button(
                    onClick = { openProductForm(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nuevo Producto")
                }
            }
        }

        // SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
            placeholder = { Text("Buscar por nombre, código de barra o categoría...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PRODUCTS TABLE
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isCompact = maxWidth < 720.dp

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (isCompact) {
                    // Mobile Compact List
                    if (productsList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay productos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(productsList) { product ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = product.nombre,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { openProductForm(product) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { repository.deleteProductSoft(product.id, currentTimeMillis()) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Barcodes
                                        val codesDisplay = try {
                                            product.codigos
                                                .replace("[", "")
                                                .replace("]", "")
                                                .replace("\"", "")
                                                .split(",")
                                                .filter { it.isNotEmpty() }
                                                .joinToString(", ")
                                                .ifEmpty { "N/A" }
                                        } catch (_: Exception) {
                                            "N/A"
                                        }
                                        Text("Código: $codesDisplay", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Category
                                        Text("Categoría: ${product.categoria ?: "Sin categoría"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Prices & Cost
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Precio: $${product.precio.toString().formatPrice()}${if (product.precio_nota.isNullOrBlank()) "" else " / ${product.precio_nota}"}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text("Costo: $${product.costo.toString().formatPrice()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (product.es_favorito == 1L) {
                                                    Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                                                        Text("★", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                    }
                                                }
                                                if (product.activo == 1L) {
                                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                                        Text("Activo", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                    }
                                                } else {
                                                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                                                        Text("Inactivo", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Desktop Table Layout
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Código(s)", modifier = Modifier.weight(0.18f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("Nombre del Producto", modifier = Modifier.weight(0.28f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("Categoría", modifier = Modifier.weight(0.14f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("Precio Venta", modifier = Modifier.weight(0.10f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("Costo", modifier = Modifier.weight(0.10f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("Estado", modifier = Modifier.weight(0.10f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("Acciones", modifier = Modifier.weight(0.10f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }

                        if (productsList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No hay productos registrados en el catálogo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                items(productsList) { product ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant))
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val codesDisplay = try {
                                            product.codigos
                                                .replace("[", "")
                                                .replace("]", "")
                                                .replace("\"", "")
                                                .split(",")
                                                .filter { it.isNotEmpty() }
                                                .joinToString(", ")
                                                .ifEmpty { "N/A" }
                                        } catch (e: Exception) {
                                            "N/A"
                                        }
                                        Text(
                                            text = codesDisplay,
                                            modifier = Modifier.weight(0.18f),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            modifier = Modifier.weight(0.28f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = product.nombre,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (product.es_favorito == 1L) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.Favorite,
                                                    contentDescription = "Favorito",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = product.categoria ?: "Sin categoría",
                                            modifier = Modifier.weight(0.14f),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = "$${product.precio.toString().formatPrice()}${if (product.precio_nota.isNullOrBlank()) "" else " / ${product.precio_nota}"}",
                                            modifier = Modifier.weight(0.10f),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = "$${product.costo.toString().formatPrice()}",
                                            modifier = Modifier.weight(0.10f),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Box(modifier = Modifier.weight(0.10f)) {
                                            if (product.activo == 1L) {
                                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                                    Text("Activo", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                }
                                            } else {
                                                Badge(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                                                    Text("Inactivo", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.weight(0.10f),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { openProductForm(product) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = { repository.deleteProductSoft(product.id, currentTimeMillis()) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Quitar",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

    // PRODUCT FORM DIALOG
    if (showProductDialogFor != null) {
        val isNew = showProductDialogFor!!.id.isEmpty()
        AlertDialog(
            onDismissRequest = { showProductDialogFor = null },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = {
                Text(
                    text = if (isNew) "Registrar Nuevo Producto" else "Modificar Producto",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = formNombre,
                        onValueChange = { formNombre = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre del Producto *") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formCodigo,
                        onValueChange = { formCodigo = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código(s) de Barra (separados por coma)") },
                        placeholder = { Text("e.g. 75010001, 75010002") },
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formPrecio,
                            onValueChange = { formPrecio = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Precio de Venta *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formCosto,
                            onValueChange = { formCosto = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Costo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formPrecioMayoreo,
                            onValueChange = { formPrecioMayoreo = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Precio Mayoreo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formPrecioNota,
                            onValueChange = { formPrecioNota = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Unidad (e.g. Kg, Pza, L)") },
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = formCategoria,
                        onValueChange = { formCategoria = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Categoría") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // SWITCHES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vendido por Peso", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = formPorPeso,
                            onCheckedChange = { formPorPeso = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Es Producto Favorito", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = formEsFavorito,
                            onCheckedChange = { formEsFavorito = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Producto Activo (Venta disponible)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = formActivo,
                            onCheckedChange = { formActivo = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { saveProduct() },
                    enabled = formNombre.trim().isNotEmpty() && formPrecio.toDoubleOrNull() != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProductDialogFor = null }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // CSV IMPORT/EXPORT DIALOG
    if (showImportExportDialog) {
        AlertDialog(
            onDismissRequest = { showImportExportDialog = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("Importar / Exportar en CSV", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Puedes copiar el CSV a continuación para respaldar los datos, o pegar tu propio texto CSV y presionar 'Importar' para actualizar el catálogo.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = { Text("id,codigos,nombre,precio...") },
                        maxLines = 10
                    )

                    if (importError != null) {
                        Text(importError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (importSuccess) {
                        Text("Catálogo importado exitosamente.", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            try {
                                val lines = csvText.split("\n")
                                if (lines.size < 2) throw Exception("CSV vacío o sin suficientes líneas.")
                                val header = lines[0].split(",")
                                if (header.size < 4) throw Exception("Encabezados inválidos (mínimo: id, codigos, nombre, precio).")
                                
                                var count = 0
                                for (i in 1 until lines.size) {
                                    val line = lines[i].trim()
                                    if (line.isEmpty()) continue
                                    
                                    val cols = parseCsvLine(line)
                                    if (cols.size < 4) continue // Skip invalid lines
                                    
                                    val id = cols[0].ifEmpty { generateUUID() }
                                    val codigos = cols[1].ifEmpty { "[]" }
                                    val nombre = cols[2]
                                    val precio = cols[3].toDoubleOrNull() ?: 0.0
                                    val costo = cols.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                                    val precioNota = cols.getOrNull(5) ?: "Pza"
                                    val categoria = cols.getOrNull(6) ?: "Sin categoría"
                                    val activo = cols.getOrNull(7)?.toLongOrNull() ?: 1L
                                    val porPeso = cols.getOrNull(8)?.toLongOrNull() ?: 0L
                                    val precioMayoreo = cols.getOrNull(9)?.toDoubleOrNull() ?: 0.0
                                    val esFavorito = cols.getOrNull(10)?.toLongOrNull() ?: 0L
 
                                    val p = Products(
                                        id = id,
                                        codigos = codigos,
                                        nombre = nombre,
                                        precio = precio,
                                        costo = costo,
                                        precio_nota = precioNota,
                                        categoria = categoria,
                                        activo = activo,
                                        por_peso = porPeso,
                                        precio_mayoreo = precioMayoreo,
                                        es_favorito = esFavorito,
                                        updated_at = currentTimeMillis(),
                                        sync_state = "PENDING_INSERT"
                                    )
                                    repository.insertProduct(p)
                                    count++
                                }
                                importSuccess = true
                                importError = null
                            } catch (e: Exception) {
                                importError = "Error al importar: ${e.message}"
                                importSuccess = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Importar CSV")
                    }

                    Button(
                        onClick = { showImportExportDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        )
    }
}

// Simple multiplatform-friendly CSV line parser
fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var curVal = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        if (inQuotes) {
            if (ch == '\"') {
                if (i + 1 < line.length && line[i + 1] == '\"') {
                    curVal.append('\"')
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                curVal.append(ch)
            }
        } else {
            when (ch) {
                '\"' -> {
                    inQuotes = true
                }
                ',' -> {
                    result.add(curVal.toString().trim())
                    curVal = StringBuilder()
                }
                else -> {
                    curVal.append(ch)
                }
            }
        }
        i++
    }
    result.add(curVal.toString().trim())
    return result
}
