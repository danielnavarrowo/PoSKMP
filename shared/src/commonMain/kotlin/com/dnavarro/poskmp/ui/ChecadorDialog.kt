package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.formatPrice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecadorDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    repository: ProductRepository
) {
    if (!showDialog) return

    var barcodeInputValue by remember {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange.Zero))
    }
    var searchedProduct by remember { mutableStateOf<Products?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun performSearch() {
        val code = barcodeInputValue.text.trim()
        if (code.isEmpty()) return

        scope.launch {
            val result = repository.findProductByBarcode(code)
            searchedProduct = result
            hasSearched = true
            // Select all text in the field so the next scan overrides it automatically
            barcodeInputValue = TextFieldValue(
                text = barcodeInputValue.text,
                selection = TextRange(0, barcodeInputValue.text.length)
            )
        }
    }

    LaunchedEffect(Unit) {
        if (isCameraScannerAvailable()) {
            showCameraScanner = true
        }
        delay(50.milliseconds)
        focusRequester.requestFocus()
    }


    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.large)
            .padding(24.dp)
            .fillMaxWidth()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && 
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    performSearch()
                    true
                } else false
            },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Verificador de Precios",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input field
                OutlinedTextField(
                    value = barcodeInputValue,
                    onValueChange = { barcodeInputValue = it },
                    placeholder = { Text("Escribe o escanea código de barras...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCameraScannerAvailable()) {
                                IconButton(onClick = { showCameraScanner = true }) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Escanear con cámara")
                                }
                            }
                            if (barcodeInputValue.text.isNotEmpty()) {
                                IconButton(onClick = {
                                    barcodeInputValue = TextFieldValue(text = "", selection = TextRange.Zero)
                                    searchedProduct = null
                                    hasSearched = false
                                    focusRequester.requestFocus()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Product details display area
                if (searchedProduct != null) {
                    val product = searchedProduct!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = product.nombre,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large beautiful price text
                        val suffix = if (product.por_peso == 1L) " / Kg" else ""
                        Text(
                            text = "$${product.precio.toString().formatPrice()}$suffix",
                            fontWeight = FontWeight.Black,
                            fontSize = 42.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Categoría: ${product.categoria ?: "Sin categoría"}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (hasSearched) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Producto no encontrado",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Cerrar")
                    }

                    Button(
                        onClick = { performSearch() },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Consultar")
                    }
                }
            }
        }
    )

    if (showCameraScanner) {
        PlatformBarcodeScanner(
            onScanResult = { scannedCode ->
                barcodeInputValue = TextFieldValue(
                    text = scannedCode,
                    selection = TextRange(0, scannedCode.length)
                )
                showCameraScanner = false
                performSearch()
            },
            onClose = { showCameraScanner = false }
        )
    }
}
