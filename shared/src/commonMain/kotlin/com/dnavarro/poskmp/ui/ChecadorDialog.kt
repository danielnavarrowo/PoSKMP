package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.SoundManager
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.barcode_input_placeholder
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.category_label_format
import poskmp.shared.generated.resources.clear_desc
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.per_kg_suffix
import poskmp.shared.generated.resources.price_checker_title
import poskmp.shared.generated.resources.product_not_found
import poskmp.shared.generated.resources.query_button
import poskmp.shared.generated.resources.scan_with_camera_desc
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.warning
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ChecadorContent(
    repository: ProductRepository,
    onClose: (() -> Unit)? = null,
    showHeaderTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    var barcodeInputValue by remember {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange.Zero))
    }
    var searchedProduct by remember { mutableStateOf<Products?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var lastScannedProduct by remember { mutableStateOf<Products?>(null) }
    var cameraScannerFeedback by remember { mutableStateOf<String?>(null) }
    var checadorQuantity by remember { mutableStateOf(1.0) }

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun performSearch() {
        val code = barcodeInputValue.text.trim()
        if (code.isEmpty()) return

        scope.launch {
            val result = repository.findProductByBarcode(code)
            searchedProduct = result
            hasSearched = true
            lastScannedProduct = result
            checadorQuantity = 1.0
            cameraScannerFeedback = if (result == null) "Producto no encontrado: $code" else null
            if (result == null) {
                SoundManager.playErrorSound()
            }
            barcodeInputValue = TextFieldValue(
                text = barcodeInputValue.text,
                selection = TextRange(0, barcodeInputValue.text.length)
            )
        }
    }

    // Persistent focus loop: ensure focus is always kept on the barcode input field
    LaunchedEffect(Unit) {
        if (isCameraScannerAvailable()) {
            showCameraScanner = true
        }
        while (isActive) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
            delay(250.milliseconds)
        }
    }

    if (!showCameraScanner) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                    ) {
                        performSearch()
                        true
                    } else false
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showHeaderTitle) {
                Text(
                    text = stringResource(Res.string.price_checker_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Input field
            OutlinedTextField(
                value = barcodeInputValue,
                onValueChange = { barcodeInputValue = it },
                placeholder = { Text(stringResource(Res.string.barcode_input_placeholder)) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.search), contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCameraScannerAvailable()) {
                            IconButton(onClick = { showCameraScanner = true }) {
                                Icon(painter = painterResource(Res.drawable.barcode_scanner), contentDescription = stringResource(Res.string.scan_with_camera_desc))
                            }
                        }
                        if (barcodeInputValue.text.isNotEmpty()) {
                            IconButton(onClick = {
                                barcodeInputValue = TextFieldValue(text = "", selection = TextRange.Zero)
                                searchedProduct = null
                                hasSearched = false
                                focusRequester.requestFocus()
                            }) {
                                Icon(painter = painterResource(Res.drawable.close), contentDescription = stringResource(Res.string.clear_desc))
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

            // Product details display area (For Desktop or manual search when camera scanner is not active)
            if (searchedProduct != null && (!isAndroid() || !isCameraScannerAvailable())) {
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

                    val perKgSuffix = stringResource(Res.string.per_kg_suffix)
                    val suffix = if (product.por_peso == 1L) perKgSuffix else ""
                    Text(
                        text = "$${product.precio.toString().formatPrice()}$suffix",
                        fontWeight = FontWeight.Black,
                        fontSize = 42.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val categoryText = product.categoria ?: stringResource(Res.string.no_category)
                    Text(
                        text = stringResource(Res.string.category_label_format, categoryText),
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
                        painter = painterResource(Res.drawable.warning),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.product_not_found),
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
                if (onClose != null) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(stringResource(Res.string.close_button))
                    }
                }

                Button(
                    onClick = { performSearch() },
                    modifier = Modifier.weight(if (onClose != null) 1f else 2f),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(Res.string.query_button))
                }
            }
        }
    }

    if (showCameraScanner) {
        PlatformBarcodeScanner(
            onScanResult = { scannedCode ->
                scope.launch {
                    val code = scannedCode.trim()
                    val product = repository.findProductByBarcode(code)
                    if (product != null) {
                        searchedProduct = product
                        hasSearched = true
                        lastScannedProduct = product
                        checadorQuantity = 1.0
                        cameraScannerFeedback = null
                        barcodeInputValue = TextFieldValue(
                            text = code,
                            selection = TextRange(0, code.length)
                        )
                    } else {
                        lastScannedProduct = null
                        cameraScannerFeedback = "Producto no encontrado: $code"
                        SoundManager.playErrorSound()
                    }
                }
            },
            onClose = {
                showCameraScanner = false
                lastScannedProduct = null
                cameraScannerFeedback = null
                if (isAndroid() && isCameraScannerAvailable() && onClose != null) {
                    onClose()
                }
            },
            statusMessage = cameraScannerFeedback,
            lastScannedProduct = lastScannedProduct,
            lastScannedQuantity = checadorQuantity,
            onQuantityChange = { delta ->
                checadorQuantity = (checadorQuantity + delta).coerceAtLeast(1.0)
            },
            isChecadorMode = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecadorDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    repository: ProductRepository
) {
    if (!showDialog) return

    if (isAndroid() && isCameraScannerAvailable()) {
        ChecadorContent(
            repository = repository,
            onClose = onDismiss,
            showHeaderTitle = false
        )
    } else {
        BasicAlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium
                )
                .padding(24.dp)
                .fillMaxWidth(),
            content = {
                ChecadorContent(
                    repository = repository,
                    onClose = onDismiss,
                    showHeaderTitle = true
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecadorScreen(
    repository: ProductRepository,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.price_checker_title),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    ChecadorContent(
                        repository = repository,
                        onClose = null,
                        showHeaderTitle = false
                    )
                }
            }
        }
    }
}
