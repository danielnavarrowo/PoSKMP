package com.dnavarro.poskmp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
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
import poskmp.shared.generated.resources.cost_label
import poskmp.shared.generated.resources.header_retail_price
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.per_kg_suffix
import poskmp.shared.generated.resources.pieces_count_label
import poskmp.shared.generated.resources.price_checker_title
import poskmp.shared.generated.resources.price_per_piece_fmt
import poskmp.shared.generated.resources.product_not_found
import poskmp.shared.generated.resources.scan_with_camera_desc
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.search_desc
import poskmp.shared.generated.resources.warning
import poskmp.shared.generated.resources.wholesale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun ChecadorContent(
    repository: ProductRepository,
    onClose: (() -> Unit)? = null,
    showHeaderTitle: Boolean = true,
    showExtraPrices: Boolean = false,
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
    var checadorQuantity by remember { mutableDoubleStateOf(1.0) }

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

    // 10-second auto-reset timer back to initial state after scanning a product or error
    LaunchedEffect(searchedProduct, hasSearched) {
        if (hasSearched || searchedProduct != null) {
            delay(10.seconds)
            searchedProduct = null
            hasSearched = false
            barcodeInputValue = TextFieldValue(text = "", selection = TextRange.Zero)
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
            delay(900.milliseconds)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        color = if (barcodeInputValue.text.isNotEmpty())
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        else
                            MaterialTheme.colorScheme.surfaceContainer,
                        shape = ShapeDefaults.cardShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.search),
                        contentDescription = stringResource(Res.string.search_desc),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (barcodeInputValue.text.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.barcode_input_placeholder),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                textAlign = TextAlign.Start
                            )
                        }

                        BasicTextField(
                            value = barcodeInputValue,
                            onValueChange = { barcodeInputValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (barcodeInputValue.text.isNotEmpty()) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                barcodeInputValue = TextFieldValue(text = "", selection = TextRange.Zero)
                                searchedProduct = null
                                hasSearched = false
                                focusRequester.requestFocus()
                            }
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.close),
                                contentDescription = stringResource(Res.string.clear_desc),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (isCameraScannerAvailable()) {
                        if (barcodeInputValue.text.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = { showCameraScanner = true }
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.barcode_scanner),
                                contentDescription = stringResource(Res.string.scan_with_camera_desc),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Product details display area (For Desktop or manual search when camera scanner is not active)
            if (searchedProduct != null && (!isAndroid() || !isCameraScannerAvailable())) {
                val product = searchedProduct!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    val hasMultiplePieces = product.piezas != 1.0 && product.piezas > 0.0
                    val pricePerPiece = if (hasMultiplePieces) product.precio / product.piezas else 0.0
                    val wholesalePerPiece = if (hasMultiplePieces) product.precio_mayoreo / product.piezas else 0.0

                    if (showExtraPrices) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(Res.string.cost_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${product.costo.toString().formatPrice()}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                if (hasMultiplePieces) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(Res.string.pieces_count_label, product.piezas.toInt().toString()),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(Res.string.header_retail_price),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${product.precio.toString().formatPrice()}$suffix",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                if (hasMultiplePieces) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(Res.string.price_per_piece_fmt, pricePerPiece.toString().formatPrice()),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(Res.string.wholesale),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${product.precio_mayoreo.toString().formatPrice()}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                if (hasMultiplePieces) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(Res.string.price_per_piece_fmt, wholesalePerPiece.toString().formatPrice()),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "$${product.precio.toString().formatPrice()}$suffix",
                            fontWeight = FontWeight.Black,
                            fontSize = 42.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        if (hasMultiplePieces) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(Res.string.pieces_count_label, product.piezas.toInt().toString()),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(Res.string.price_per_piece_fmt, pricePerPiece.toString().formatPrice()),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

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
            isChecadorMode = showExtraPrices
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecadorDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    repository: ProductRepository,
    showExtraPrices: Boolean = false
) {
    if (!showDialog) return

    if (isAndroid() && isCameraScannerAvailable()) {
        ChecadorContent(
            repository = repository,
            onClose = onDismiss,
            showHeaderTitle = false,
            showExtraPrices = showExtraPrices
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
                    showHeaderTitle = true,
                    showExtraPrices = showExtraPrices
                )
            }
        )
    }
}

@Composable
fun ChecadorScreen(
    repository: ProductRepository,
    showExtraPrices: Boolean = false,
    currentDateText: String = "",
    currentTimeText: String = "",
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
    var checadorQuantity by remember { mutableDoubleStateOf(1.0) }

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
                text = code,
                selection = TextRange(0, code.length)
            )
        }
    }

    // 10-second auto-reset timer back to initial state after scanning a product or error
    LaunchedEffect(searchedProduct, hasSearched) {
        if (hasSearched || searchedProduct != null) {
            delay(10.seconds)
            searchedProduct = null
            hasSearched = false
            barcodeInputValue = TextFieldValue(text = "", selection = TextRange.Zero)
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

    if (showCameraScanner && isAndroid() && isCameraScannerAvailable()) {
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
            },
            statusMessage = cameraScannerFeedback,
            lastScannedProduct = lastScannedProduct,
            lastScannedQuantity = checadorQuantity,
            onQuantityChange = { delta ->
                checadorQuantity = (checadorQuantity + delta).coerceAtLeast(1.0)
            },
            isChecadorMode = showExtraPrices
        )
    } else {
        val backgroundGradient = remember {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8C3A4D),
                    Color(0xFF6B305B),
                    Color(0xFF3A3660),
                    Color(0xFF1C4352),
                    Color(0xFF134C54)
                )
            )
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .clickable { focusRequester.requestFocus() }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                    ) {
                        performSearch()
                        true
                    } else false
                }
        ) {
            // Hidden but always-focused Input TextField to process scanner/keyboard input seamlessly
            BasicTextField(
                value = barcodeInputValue,
                onValueChange = { newValue ->
                    barcodeInputValue = newValue
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = { performSearch() }),
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0.01f)
                    .focusRequester(focusRequester)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Two Columns Row taking the available height above date/time
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN: "Checador de precios" + Product name (or idle title) with black font weight and dynamic font size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {

                            // Dynamic product name taking the available height with FontWeight.Black
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchedProduct != null) {
                                    AutoSizingText(
                                        text = searchedProduct!!.nombre,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        minFontSize = 24.sp,
                                        maxFontSize = 360.sp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (hasSearched) {
                                    AutoSizingText(
                                        text = stringResource(Res.string.product_not_found),
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Black,
                                        minFontSize = 24.sp,
                                        maxFontSize = 360.sp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AutoSizingText(
                                        text = stringResource(Res.string.price_checker_title),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        minFontSize = 28.sp,
                                        maxFontSize = 360.sp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Prices and metrics in rows
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                    ) {
                        if (searchedProduct != null) {
                            val product = searchedProduct!!
                            val perKgSuffix = stringResource(Res.string.per_kg_suffix)
                            val suffix = if (product.por_peso == 1L) perKgSuffix else ""
                            val hasMultiplePieces = product.piezas != 1.0 && product.piezas > 0.0
                            val pricePerPiece = if (hasMultiplePieces) product.precio / product.piezas else 0.0
                            val wholesalePerPiece = if (hasMultiplePieces) product.precio_mayoreo / product.piezas else 0.0

                            // Row 1: Retail Price (Primary)
                            ChecadorMetricRow(
                                title = stringResource(Res.string.header_retail_price),
                                value = "$${product.precio.toString().formatPrice()}$suffix",
                                subtitle =  null,
                                isPrimary = true
                            )

                            // Row 2: Wholesale Price
                            if (product.precio_mayoreo > 0.0 && showExtraPrices) {
                                ChecadorMetricRow(
                                    title = stringResource(Res.string.wholesale),
                                    value = "$${product.precio_mayoreo.toString().formatPrice()}",
                                    subtitle = if (hasMultiplePieces) {
                                        stringResource(Res.string.price_per_piece_fmt, wholesalePerPiece.toString().formatPrice())
                                    } else null
                                )
                            }

                            // Row 3: Cost (if enabled)
                            if (showExtraPrices) {
                                ChecadorMetricRow(
                                    title = stringResource(Res.string.cost_label),
                                    value = "$${product.costo.toString().formatPrice()}"
                                )
                            }

                            // Row 4: Pieces per packaging (if multiple pieces)
                            if (hasMultiplePieces) {
                                ChecadorMetricRow(
                                    title = stringResource(Res.string.price_per_piece_fmt, ""),
                                    value = "$${pricePerPiece.toString().formatPrice()}",
                                    subtitle = "Contiene ${product.piezas.toInt()} piezas"
                                )
                            }

                        } else if (hasSearched) {
                            Box(Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(Res.drawable.warning),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(256.dp)
                                )
                            }

                        } else {
                            // Idle state right column: Input card & scanner prompt
                            Box(

                                modifier = Modifier.fillMaxWidth().background(color = Color.Transparent,
                                    shape = ShapeDefaults.cardShape
                                ),

                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.barcode_scanner),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(256.dp)
                                    )

                                    // Visual input field for on-screen typing if needed
                                    BasicTextField(
                                        value = barcodeInputValue,
                                        onValueChange = { newValue ->
                                            barcodeInputValue = newValue
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        cursorBrush = SolidColor(Color.White),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        keyboardActions = KeyboardActions(onDone = { performSearch() }),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .background(
                                                        color = Color.White.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = Color.White.copy(alpha = 0.25f),
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (barcodeInputValue.text.isEmpty()) {
                                                    Text(
                                                        text = "Acerca el código de barras al escáner...",
                                                        fontSize = 16.sp,
                                                        color = Color.White.copy(alpha = 0.6f),
                                                        textAlign = TextAlign.Center,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        },
                                        modifier = Modifier.focusRequester(focusRequester)
                                    )
                                }
                            }
                        }
                    }
                }

                // BOTTOM: Date and Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentDateText,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = currentTimeText,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoSizingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontWeight: FontWeight = FontWeight.Black,
    textAlign: TextAlign = TextAlign.Start,
    minFontSize: TextUnit = 24.sp,
    maxFontSize: TextUnit = 360.sp
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }

        val optimalFontSize = remember(text, widthPx, heightPx, fontWeight, minFontSize, maxFontSize) {
            if (widthPx <= 0 || heightPx <= 0 || text.isBlank()) {
                minFontSize
            } else {
                var low = minFontSize.value
                var high = maxFontSize.value
                var best = minFontSize.value

                for (i in 0..14) {
                    if (high - low < 1.0f) break
                    val mid = (low + high) / 2f
                    val measured = textMeasurer.measure(
                        text = AnnotatedString(text),
                        style = TextStyle(
                            fontSize = mid.sp,
                            fontWeight = fontWeight,
                            lineHeight = (mid * 1.05f).sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        constraints = Constraints(
                            maxWidth = widthPx,
                            maxHeight = Constraints.Infinity
                        )
                    )

                    val fitsWidth = measured.size.width <= widthPx && !measured.hasVisualOverflow
                    val fitsHeight = measured.size.height <= heightPx
                    val noLineOverflow = (0 until measured.lineCount).all { lineIdx ->
                        (measured.getLineRight(lineIdx) - measured.getLineLeft(lineIdx)) <= widthPx
                    }

                    if (fitsWidth && fitsHeight && noLineOverflow) {
                        best = mid
                        low = mid
                    } else {
                        high = mid
                    }
                }
                best.sp
            }
        }

        Text(
            text = text,
            color = color,
            fontWeight = fontWeight,
            fontSize = optimalFontSize,
            lineHeight = optimalFontSize * 1.05f,
            textAlign = textAlign,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChecadorMetricRow(
    title: String,
    value: String,
    subtitle: String? = null,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f)
        ),
        shape = ShapeDefaults.cardShape,
        border = BorderStroke(
            1.dp,
            if (isPrimary) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = if (isPrimary) 20.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color.White.copy(alpha = if (isPrimary) 0.9f else 0.7f)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = value,
                style = if (isPrimary) {
                    MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 128.sp
                    )
                } else {
                    MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                textAlign = TextAlign.End
            )
        }
    }
}
