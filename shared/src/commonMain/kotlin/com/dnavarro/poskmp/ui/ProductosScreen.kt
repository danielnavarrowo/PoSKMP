package com.dnavarro.poskmp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.parseImportFile
import com.dnavarro.poskmp.util.pickFile
import com.dnavarro.poskmp.util.saveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

enum class ProductSortField {
    CODIGO, NOMBRE, CATEGORIA, PRECIO, COSTO, ESTADO
}

enum class ProductSortOrder {
    ASC, DESC
}

@Composable
fun RowScope.TableHeader(
    text: String,
    weight: Float,
    field: ProductSortField,
    currentField: ProductSortField,
    currentOrder: ProductSortOrder,
    onHeaderClick: (ProductSortField) -> Unit
) {
    Row(
        modifier = Modifier
            .weight(weight)
            .clickable { onHeaderClick(field) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = if (field == currentField) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        if (field == currentField) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (currentOrder == ProductSortOrder.ASC) "▲" else "▼",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ProductosScreen(
    repository: ProductRepository,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var productsList by remember { mutableStateOf<List<Products>>(emptyList()) }

    var sortField by remember { mutableStateOf(ProductSortField.NOMBRE) }
    var sortOrder by remember { mutableStateOf(ProductSortOrder.ASC) }

    // Dialog control states
    var showProductDialogFor by remember { mutableStateOf<Products?>(null) } // Null means not showing, a Product with empty ID means "New"
    var showImportDialog by remember { mutableStateOf(false) }
    var showBulkModificationDialog by remember { mutableStateOf(false) }
    var selectedProductIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

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

    val sortedProducts = remember(productsList, sortField, sortOrder) {
        productsList.sortedWith { p1, p2 ->
            val f1 = p1.es_favorito == 1L
            val f2 = p2.es_favorito == 1L
            if (f1 != f2) {
                return@sortedWith if (f1) -1 else 1
            }
            val comparison = when (sortField) {
                ProductSortField.NOMBRE -> p1.nombre.lowercase().compareTo(p2.nombre.lowercase())
                ProductSortField.CODIGO -> {
                    val c1 = try { p1.codigos.replace("[", "").replace("]", "").replace("\"", "") } catch (_: Exception) { "" }
                    val c2 = try { p2.codigos.replace("[", "").replace("]", "").replace("\"", "") } catch (_: Exception) { "" }
                    c1.compareTo(c2)
                }
                ProductSortField.CATEGORIA -> (p1.categoria ?: "").lowercase().compareTo((p2.categoria ?: "").lowercase())
                ProductSortField.PRECIO -> p1.precio.compareTo(p2.precio)
                ProductSortField.COSTO -> p1.costo.compareTo(p2.costo)
                ProductSortField.ESTADO -> p1.activo.compareTo(p2.activo)
            }
            if (sortOrder == ProductSortOrder.ASC) comparison else -comparison
        }
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
                if (selectedProductIds.isNotEmpty()) {
                    Button(
                        onClick = { showBulkModificationDialog = true },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Modificar ${selectedProductIds.size} producto(s)")
                    }
                }

                if (!isAndroid()) {
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar")
                    }
                }

                Button(
                    onClick = {
                        val csvBuilder = StringBuilder("id,codigos,nombre,precio,costo,categoria,activo,por_peso,precio_mayoreo,es_favorito\n")
                        for ((id, codigos, nombre, precio, costo, categoria, activo, por_peso, precio_mayoreo, es_favorito) in productsList) {
                            csvBuilder.append("$id,")
                            csvBuilder.append("\"${codigos.replace("\"", "\"\"")}\",")
                            csvBuilder.append("\"${nombre.replace("\"", "\"\"")}\",")
                            csvBuilder.append("$precio,")
                            csvBuilder.append("$costo,")
                            csvBuilder.append("\"${(categoria ?: "").replace("\"", "\"\"")}\",")
                            csvBuilder.append("$activo,")
                            csvBuilder.append("$por_peso,")
                            csvBuilder.append("$precio_mayoreo,")
                            csvBuilder.append("$es_favorito\n")
                        }
                        val csvText = csvBuilder.toString()
                        saveFile(
                            defaultFileName = "productos_exportados.csv",
                            content = csvText,
                            onSuccess = {
                                exportSuccessMessage = "Catálogo de productos exportado y guardado exitosamente."
                                exportErrorMessage = null
                            },
                            onError = {
                                exportErrorMessage = it
                                exportSuccessMessage = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar")
                }

                Button(
                    onClick = { showProductDialogFor = Products("", "[]", "", 0.0, 0.0, "", 1L, 0L, 0.0, 0L, 0L, "") },
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
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            itemsIndexed(sortedProducts) { index, product ->
                                val shape = if (sortedProducts.size == 1) {
                                    MaterialTheme.shapes.medium
                                } else if (index == 0) {
                                    RoundedCornerShape(
                                        topStart = MaterialTheme.shapes.medium.topStart,
                                        topEnd = MaterialTheme.shapes.medium.topEnd,
                                        bottomStart = MaterialTheme.shapes.extraSmall.bottomStart,
                                        bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                                    )
                                } else if (index == sortedProducts.lastIndex) {
                                    RoundedCornerShape(
                                        topStart = MaterialTheme.shapes.extraSmall.topStart,
                                        topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                                        bottomStart = MaterialTheme.shapes.medium.bottomStart,
                                        bottomEnd = MaterialTheme.shapes.medium.bottomEnd
                                    )
                                } else {
                                    RoundedCornerShape(MaterialTheme.shapes.extraSmall.topStart)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showProductDialogFor = product },
                                    shape = shape,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = product.id in selectedProductIds,
                                                onCheckedChange = { isSelected ->
                                                    selectedProductIds = if (isSelected) selectedProductIds + product.id else selectedProductIds - product.id
                                                }
                                            )
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
                                                    onClick = { showProductDialogFor = product },
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
                                                    text = "Precio: $${product.precio.toString().formatPrice()}",
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
                        val onHeaderClick = { field: ProductSortField ->
                            if (sortField == field) {
                                sortOrder = if (sortOrder == ProductSortOrder.ASC) ProductSortOrder.DESC else ProductSortOrder.ASC
                            } else {
                                sortField = field
                                sortOrder = ProductSortOrder.ASC
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableHeader("Código(s)", 0.18f, ProductSortField.CODIGO, sortField, sortOrder, onHeaderClick)
                            TableHeader("Nombre del Producto", 0.28f, ProductSortField.NOMBRE, sortField, sortOrder, onHeaderClick)
                            TableHeader("Categoría", 0.14f, ProductSortField.CATEGORIA, sortField, sortOrder, onHeaderClick)
                            TableHeader("Precio Venta", 0.10f, ProductSortField.PRECIO, sortField, sortOrder, onHeaderClick)
                            TableHeader("Costo", 0.10f, ProductSortField.COSTO, sortField, sortOrder, onHeaderClick)
                            TableHeader("Estado", 0.10f, ProductSortField.ESTADO, sortField, sortOrder, onHeaderClick)
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
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                itemsIndexed(sortedProducts) { index, product ->
                                    val shape = if (sortedProducts.size == 1) {
                                        MaterialTheme.shapes.medium
                                    } else if (index == 0) {
                                        RoundedCornerShape(
                                            topStart = MaterialTheme.shapes.medium.topStart,
                                            topEnd = MaterialTheme.shapes.medium.topEnd,
                                            bottomStart = MaterialTheme.shapes.extraSmall.bottomStart,
                                            bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                                        )
                                    } else if (index == sortedProducts.lastIndex) {
                                        RoundedCornerShape(
                                            topStart = MaterialTheme.shapes.extraSmall.topStart,
                                            topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                                            bottomStart = MaterialTheme.shapes.medium.bottomStart,
                                            bottomEnd = MaterialTheme.shapes.medium.bottomEnd
                                        )
                                    } else {
                                        RoundedCornerShape(MaterialTheme.shapes.extraSmall.topStart)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                            .clickable { showProductDialogFor = product }
                                            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant), shape)
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = product.id in selectedProductIds,
                                            onCheckedChange = { isSelected ->
                                                selectedProductIds = if (isSelected) selectedProductIds + product.id else selectedProductIds - product.id
                                            },
                                            modifier = Modifier.weight(0.05f)
                                        )
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
                                            text = "$${product.precio.toString().formatPrice()}",
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
                                                onClick = { showProductDialogFor = product },
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
        ProductFormDialog(
            product = if (showProductDialogFor!!.id.isEmpty()) null else showProductDialogFor,
            onDismiss = { showProductDialogFor = null },
            onSave = { updatedProduct ->
                if (showProductDialogFor!!.id.isEmpty()) {
                    repository.insertProduct(updatedProduct)
                } else {
                    repository.updateProduct(updatedProduct)
                }
                showProductDialogFor = null
            }
        )
    }

    // NEW IMPORT/EXPORT DIALOGS
    if (showImportDialog) {
        ImportProductsDialog(
            onDismiss = { showImportDialog = false },
            repository = repository
        )
    }

    if (showBulkModificationDialog) {
        BulkProductModificationDialog(
            selectedCount = selectedProductIds.size,
            onDismiss = { showBulkModificationDialog = false },
            onApply = { modification ->
                repository.getAllProductsList()
                    .filter { it.id in selectedProductIds }
                    .forEach { product ->
                        val updatedProduct = applyBulkProductModification(product, modification)
                        if (updatedProduct == null) {
                            repository.deleteProductHard(product.id)
                        } else {
                            repository.updateProduct(
                                updatedProduct.copy(
                                    updated_at = currentTimeMillis(),
                                    sync_state = "PENDING_UPDATE"
                                )
                            )
                        }
                    }
                selectedProductIds = emptySet()
                showBulkModificationDialog = false
            }
        )
    }

    if (exportSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { exportSuccessMessage = null },
            shape = MaterialTheme.shapes.medium,
            title = { Text("Exportación Exitosa", fontWeight = FontWeight.Bold) },
            text = { Text(exportSuccessMessage!!) },
            confirmButton = {
                Button(
                    onClick = { exportSuccessMessage = null },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (exportErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { exportErrorMessage = null },
            shape = MaterialTheme.shapes.medium,
            title = { Text("Error al Exportar", fontWeight = FontWeight.Bold) },
            text = { Text(exportErrorMessage!!) },
            confirmButton = {
                Button(
                    onClick = { exportErrorMessage = null },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProductsDialog(
    onDismiss: () -> Unit,
    repository: ProductRepository
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var parsedProducts by remember { mutableStateOf<List<Products>>(emptyList()) }
    var importError by remember { mutableStateOf<String?>(null) }
    var importSuccessMessage by remember { mutableStateOf<String?>(null) }
    var updateExistingOption by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var importProgressFraction by remember { mutableFloatStateOf(0f) }
    var importProgressText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (currentStep == 2) 0.95f else 0.85f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header of Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Importar Productos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentStep in 1..3) {
                        Text(
                            text = "Paso $currentStep de 3",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Content based on step
                when (currentStep) {
                    1 -> {
                        // Instructions
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Columnas requeridas en su archivo (.csv o .xlsx):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("• nombre (Texto - Obligatorio)", fontSize = 12.sp)
                                Text("• precio (Número - Obligatorio)", fontSize = 12.sp)
                                Text(
                                    text = "Columnas opcionales: id, codigos, costo, categoria, activo (1/0), por_peso (1/0), precio_mayoreo, es_favorito (1/0)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // File Selection Area Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable {
                                    pickFile(
                                        allowedExtensions = listOf("csv", "xlsx"),
                                        onFilePicked = { name, bytes ->
                                            try {
                                                val prods = parseImportFile(name, bytes)
                                                if (prods.isEmpty()) {
                                                    importError = "El archivo no contiene productos válidos."
                                                } else {
                                                    selectedFileName = name
                                                    selectedFileBytes = bytes
                                                    parsedProducts = prods
                                                    importError = null
                                                    currentStep = 2
                                                }
                                            } catch (e: Exception) {
                                                importError = e.message ?: "Error al analizar el archivo."
                                            }
                                        },
                                        onError = { importError = it }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Seleccionar archivo .csv o .xlsx para importar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Haz clic aquí para abrir el explorador de archivos",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (importError != null) {
                            Text(
                                text = importError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text("Cancelar")
                            }
                        }
                    }

                    2 -> {
                        // Preview Step
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Archivo: $selectedFileName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Se detectaron ${parsedProducts.size} productos en total. A continuación se muestran los primeros 20 productos:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Scrollable table preview
                            Card(
                                modifier = Modifier.fillMaxWidth().height(260.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    // Table Header
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Nombre", modifier = Modifier.weight(0.4f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("Código(s)", modifier = Modifier.weight(0.25f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("Precio", modifier = Modifier.weight(0.18f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("Categoría", modifier = Modifier.weight(0.17f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    // Table Content
                                    items(parsedProducts.take(20)) { product ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = product.nombre,
                                                modifier = Modifier.weight(0.4f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val displayCodes = product.codigos.replace("[", "").replace("]", "").replace("\"", "").trim()
                                            Text(
                                                text = displayCodes.ifEmpty { "N/A" },
                                                modifier = Modifier.weight(0.25f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$${product.precio.toString().formatPrice()}",
                                                modifier = Modifier.weight(0.18f),
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = product.categoria ?: "Sin categoría",
                                                modifier = Modifier.weight(0.17f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { currentStep = 1 },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text("Atrás")
                            }

                            Button(
                                onClick = { currentStep = 3 },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text("Siguiente")
                            }
                        }
                    }

                    3 -> {
                        // Options Step
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Elige la acción en caso de conflictos y confirmación:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Option A
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isProcessing) updateExistingOption = true },
                                border = BorderStroke(
                                    width = if (updateExistingOption) 2.dp else 1.dp,
                                    color = if (updateExistingOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (updateExistingOption) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) 
                                                     else MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = updateExistingOption, 
                                        onClick = { if (!isProcessing) updateExistingOption = true },
                                        enabled = !isProcessing
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Actualizar existentes y agregar nuevos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Conserva tu catálogo actual. Si el producto ya existe (coincidencia de ID o código), actualiza sus datos; de lo contrario, crea uno nuevo.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Option B
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isProcessing) updateExistingOption = false },
                                border = BorderStroke(
                                    width = if (!updateExistingOption) 2.dp else 1.dp,
                                    color = if (!updateExistingOption) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!updateExistingOption) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) 
                                                     else MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = !updateExistingOption, 
                                        onClick = { if (!isProcessing) updateExistingOption = false },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error),
                                        enabled = !isProcessing
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Reemplazar catálogo completo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                        Text("¡Peligro! Borra TODOS los productos actuales de la base de datos antes de importar. Esta acción no se puede deshacer.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            if (!updateExistingOption) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ADVERTENCIA: Se borrarán permanentemente todos los productos registrados antes de importar.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            if (importError != null) {
                                Text(
                                    text = importError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isProcessing) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { importProgressFraction },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(
                                        text = importProgressText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { currentStep = 2 },
                                enabled = !isProcessing,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text("Atrás")
                            }

                            Button(
                                onClick = {
                                    isProcessing = true
                                    importError = null
                                    importProgressFraction = 0f
                                    importProgressText = "Iniciando importación..."
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                if (updateExistingOption) {
                                                    val existingProducts = repository.getAllProductsList()
                                                    val existingById = existingProducts.associateBy { it.id }
                                                    val existingByBarcode = mutableMapOf<String, Products>()
                                                    existingProducts.forEach { prod ->
                                                        val codes = try {
                                                            prod.codigos
                                                                .replace("[", "")
                                                                .replace("]", "")
                                                                .replace("\"", "")
                                                                .split(",")
                                                                .map { it.trim() }
                                                                .filter { it.isNotEmpty() }
                                                        } catch (_: Exception) {
                                                            emptyList()
                                                        }
                                                        codes.forEach { code ->
                                                            existingByBarcode[code] = prod
                                                        }
                                                    }

                                                    var updated = 0
                                                    var inserted = 0
                                                    val total = parsedProducts.size

                                                    for ((index, p) in parsedProducts.withIndex()) {
                                                        var targetId = p.id
                                                        var isExisting = false

                                                        if (existingById.containsKey(targetId)) {
                                                            isExisting = true
                                                        } else {
                                                            val pCodes = try {
                                                                p.codigos
                                                                    .replace("[", "")
                                                                    .replace("]", "")
                                                                    .replace("\"", "")
                                                                    .split(",")
                                                                    .map { it.trim() }
                                                                    .filter { it.isNotEmpty() }
                                                            } catch (_: Exception) {
                                                                emptyList()
                                                            }
                                                            for (code in pCodes) {
                                                                val matched = existingByBarcode[code]
                                                                if (matched != null) {
                                                                    targetId = matched.id
                                                                    isExisting = true
                                                                    break
                                                                }
                                                            }
                                                        }

                                                        val pToInsert = if (isExisting) {
                                                            p.copy(id = targetId, updated_at = currentTimeMillis(), sync_state = "PENDING_UPDATE")
                                                        } else {
                                                            p.copy(id = targetId, updated_at = currentTimeMillis(), sync_state = "PENDING_INSERT")
                                                        }

                                                        repository.insertProduct(pToInsert)
                                                        if (isExisting) updated++ else inserted++
                                                        
                                                        val currentProcessed = index + 1
                                                        withContext(Dispatchers.Main) {
                                                            importProgressFraction = currentProcessed.toFloat() / total
                                                            importProgressText = "Guardando: ${p.nombre} ($currentProcessed de $total)"
                                                        }
                                                        delay(10.milliseconds)
                                                    }
                                                    importSuccessMessage = "Se importaron correctamente los productos. Novedades: $inserted agregados, $updated actualizados."
                                                } else {
                                                    repository.deleteAllProducts()
                                                    val total = parsedProducts.size
                                                    for ((index, p) in parsedProducts.withIndex()) {
                                                        repository.insertProduct(
                                                            p.copy(updated_at = currentTimeMillis(), sync_state = "PENDING_INSERT")
                                                        )
                                                        val currentProcessed = index + 1
                                                        withContext(Dispatchers.Main) {
                                                            importProgressFraction = currentProcessed.toFloat() / total
                                                            importProgressText = "Insertando: ${p.nombre} ($currentProcessed de $total)"
                                                        }
                                                        delay(10.milliseconds)
                                                    }
                                                    importSuccessMessage = "Catálogo reemplazado con éxito. Se insertaron ${parsedProducts.size} productos."
                                                }
                                            }
                                            currentStep = 4
                                        } catch (e: Exception) {
                                            importError = "Error al guardar en la base de datos: ${e.message}"
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (updateExistingOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                ),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(if (updateExistingOption) "Confirmar e Importar" else "Borrar y Reemplazar Todo")
                            }
                        }
                    }

                    4 -> {
                        // Success Step
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981), // Beautiful Green
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "¡Importación Exitosa!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = importSuccessMessage ?: "",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = onDismiss,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text("Cerrar")
                            }
                        }
                    }
                }
            }
        }
    }
}


