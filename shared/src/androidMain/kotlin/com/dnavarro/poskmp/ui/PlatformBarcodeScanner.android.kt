    package com.dnavarro.poskmp.ui

import android.Manifest
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.dnavarro.poskmp.data.SettingsRepository
import org.koin.compose.koinInject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.formatQuantity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.add
import poskmp.shared.generated.resources.camera_permission_required_desc
import poskmp.shared.generated.resources.camera_permission_required_title
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_scanner_desc
import poskmp.shared.generated.resources.cost_label
import poskmp.shared.generated.resources.decrease_desc
import poskmp.shared.generated.resources.delete
import poskmp.shared.generated.resources.delete_button
import poskmp.shared.generated.resources.flash_off
import poskmp.shared.generated.resources.flash_on
import poskmp.shared.generated.resources.grant_permission_button
import poskmp.shared.generated.resources.header_delivery_price
import poskmp.shared.generated.resources.header_retail_price
import poskmp.shared.generated.resources.increase_desc
import poskmp.shared.generated.resources.photo_camera
import poskmp.shared.generated.resources.pieces_count_label
import poskmp.shared.generated.resources.price_per_piece_short_fmt
import poskmp.shared.generated.resources.product_added_message
import poskmp.shared.generated.resources.remove
import poskmp.shared.generated.resources.torch_desc
import poskmp.shared.generated.resources.wholesale
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

@Suppress("SameReturnValue")
actual fun isCameraScannerAvailable(): Boolean = true

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun PlatformBarcodeScanner(
    onScanResult: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
    statusMessage: String?,
    lastScannedProduct: Products?,
    lastScannedQuantity: Double,
    onUndo: (() -> Unit)?,
    onQuantityChange: ((Double) -> Unit)?,
    isChecadorMode: Boolean,
    prioritizeDeliveryPrice: Boolean
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreviewScreen(
                    onScanResult = onScanResult,
                    onClose = onClose,
                    statusMessage = statusMessage,
                    lastScannedProduct = lastScannedProduct,
                    lastScannedQuantity = lastScannedQuantity,
                    onUndo = onUndo,
                    onQuantityChange = onQuantityChange,
                    isChecadorMode = isChecadorMode,
                    prioritizeDeliveryPrice = prioritizeDeliveryPrice
                )
            } else {
                PermissionRationaleScreen(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                    onCancel = onClose
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewScreen(
    onScanResult: (String) -> Unit,
    onClose: () -> Unit,
    statusMessage: String? = null,
    lastScannedProduct: Products? = null,
    lastScannedQuantity: Double = 1.0,
    onUndo: (() -> Unit)? = null,
    onQuantityChange: ((Double) -> Unit)? = null,
    isChecadorMode: Boolean = false,
    prioritizeDeliveryPrice: Boolean = false
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        BarcodeScanning.getClient(options)
    }
    var isProcessing by remember { mutableStateOf(false) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
    val settingsRepository = koinInject<SettingsRepository>()
    val settingsPrioritizeDelivery by settingsRepository.prioritizeDeliveryPriceFlow.collectAsState(initial = false)
    val effectivePrioritizeDelivery = prioritizeDeliveryPrice || settingsPrioritizeDelivery

    LaunchedEffect(lastScannedProduct) {
        if (lastScannedProduct == null && statusMessage == null) {
            lastScannedBarcode = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (_: Exception) {
            }
            barcodeScanner.close()
            executor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            if (!isProcessing) {
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    isProcessing = true
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawValue =
                                                    barcode.rawValue?.trim() ?: ""
                                                if (rawValue.isNotEmpty()) {
                                                    val trimmed = rawValue.trim()
                                                    if (trimmed != lastScannedBarcode) {
                                                        lastScannedBarcode = trimmed
                                                        try {
                                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                        } catch (_: Exception) {}
                                                        onScanResult(trimmed)
                                                    }
                                                    coroutineScope.launch {
                                                        delay(1200.milliseconds)
                                                        isProcessing = false
                                                    }
                                                    return@addOnSuccessListener
                                                }
                                            }
                                            isProcessing = false
                                        }
                                        .addOnFailureListener {
                                            isProcessing = false
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                    previewView.setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            val factory = SurfaceOrientedMeteringPointFactory(
                                view.width.toFloat(),
                                view.height.toFloat()
                            )
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                .build()

                            camera?.cameraControl?.startFocusAndMetering(action)
                            view.performClick()
                            true
                        } else {
                            false
                        }
                    }
                } catch (_: Exception) {
                }

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(isFlashEnabled) {
            camera?.cameraControl?.enableTorch(isFlashEnabled)
        }

        // Overlay UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Close Button (Top Left)
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    painterResource(Res.drawable.close),
                    tint = Color.White,
                    contentDescription = stringResource(Res.string.close_scanner_desc)
                )
            }

            // Flash Button (Top Right)
            IconButton(
                onClick = { isFlashEnabled = !isFlashEnabled },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    painter = if (isFlashEnabled) painterResource(Res.drawable.flash_on) else painterResource(
                        Res.drawable.flash_off
                    ),
                    tint = Color.White,
                    contentDescription = stringResource(Res.string.torch_desc)
                )
            }

            // Scanned product interactive card overlay at Bottom Center
            AnimatedVisibility(
                visible = lastScannedProduct != null || !statusMessage.isNullOrBlank(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 10.dp,
                    tonalElevation = 6.dp
                ) {
                    if (lastScannedProduct != null) {
                        Column(
                            modifier = Modifier.padding(if (isChecadorMode) 16.dp else 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onUndo != null) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    )
                                    {
                                        Icon(
                                            painter = painterResource(Res.drawable.check),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = stringResource(Res.string.product_added_message),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = onUndo,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        ),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.delete),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            stringResource(Res.string.delete_button),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }
                            }

                            val hasMultiplePieces = lastScannedProduct.piezas != 1.0 && lastScannedProduct.piezas > 0.0
                            val isDeliveryActive = effectivePrioritizeDelivery && lastScannedProduct.precio_delivery > 0.0
                            val effectiveUnitPrice = if (isDeliveryActive) lastScannedProduct.precio_delivery else lastScannedProduct.precio
                            val pricePerPiece = if (hasMultiplePieces) effectiveUnitPrice / lastScannedProduct.piezas else 0.0
                            val retailPerPiece = if (hasMultiplePieces) lastScannedProduct.precio / lastScannedProduct.piezas else 0.0
                            val wholesalePerPiece = if (hasMultiplePieces) lastScannedProduct.precio_mayoreo / lastScannedProduct.piezas else 0.0
                            val deliveryPerPiece = if (hasMultiplePieces && lastScannedProduct.precio_delivery > 0.0) lastScannedProduct.precio_delivery / lastScannedProduct.piezas else 0.0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lastScannedProduct.nombre,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (!isChecadorMode) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$${
                                                (effectiveUnitPrice * lastScannedQuantity).toString()
                                                    .formatPrice()
                                            }",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        )
                                    }
                                }
                            }

                            if (!isChecadorMode && hasMultiplePieces) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(Res.string.pieces_count_label, lastScannedProduct.piezas.toInt().toString()),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = stringResource(Res.string.price_per_piece_short_fmt, pricePerPiece.toString().formatPrice()),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            if (isChecadorMode) {
                                val showDeliveryPrice = lastScannedProduct.precio_delivery > 0.0 || effectivePrioritizeDelivery
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = stringResource(Res.string.cost_label),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$${(lastScannedProduct.costo * lastScannedQuantity).toString().formatPrice()}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        if (hasMultiplePieces) {
                                            Text(
                                                text = stringResource(Res.string.pieces_count_label, lastScannedProduct.piezas.toInt().toString()),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(Res.string.header_retail_price),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$${(lastScannedProduct.precio * lastScannedQuantity).toString().formatPrice()}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        if (hasMultiplePieces) {
                                            Text(
                                                text = stringResource(Res.string.price_per_piece_short_fmt, retailPerPiece.toString().formatPrice()),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = if (showDeliveryPrice) Alignment.CenterHorizontally else Alignment.End) {
                                        Text(
                                            text = stringResource(Res.string.wholesale),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$${(lastScannedProduct.precio_mayoreo * lastScannedQuantity).toString().formatPrice()}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        if (hasMultiplePieces) {
                                            Text(
                                                text = stringResource(Res.string.price_per_piece_short_fmt, wholesalePerPiece.toString().formatPrice()),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }

                                    if (showDeliveryPrice) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = stringResource(Res.string.header_delivery_price),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (lastScannedProduct.precio_delivery > 0.0) {
                                                Text(
                                                    text = "$${(lastScannedProduct.precio_delivery * lastScannedQuantity).toString().formatPrice()}",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                                if (hasMultiplePieces) {
                                                    Text(
                                                        text = stringResource(Res.string.price_per_piece_short_fmt, deliveryPerPiece.toString().formatPrice()),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "-",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (onQuantityChange != null) {
                                val interactionSourceMinus = remember { MutableInteractionSource() }
                                val interactionSourcePlus = remember { MutableInteractionSource() }
                                val viewConfiguration = LocalViewConfiguration.current
                                val step = if (lastScannedProduct.por_peso == 1L) 0.1 else 1.0

                                LaunchedEffect(interactionSourceMinus, lastScannedProduct.id, step) {
                                    var isLongClick = false
                                    interactionSourceMinus.interactions.collectLatest { interaction ->
                                        when (interaction) {
                                            is PressInteraction.Press -> {
                                                isLongClick = false
                                                delay(viewConfiguration.longPressTimeoutMillis.milliseconds)
                                                isLongClick = true
                                                while (true) {
                                                    onQuantityChange(-step)
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    delay(80.milliseconds)
                                                }
                                            }
                                            is PressInteraction.Release -> {
                                                if (!isLongClick) {
                                                    onQuantityChange(-step)
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        }
                                    }
                                }

                                LaunchedEffect(interactionSourcePlus, lastScannedProduct.id, step) {
                                    var isLongClick = false
                                    interactionSourcePlus.interactions.collectLatest { interaction ->
                                        when (interaction) {
                                            is PressInteraction.Press -> {
                                                isLongClick = false
                                                delay(viewConfiguration.longPressTimeoutMillis.milliseconds)
                                                isLongClick = true
                                                while (true) {
                                                    onQuantityChange(step)
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    delay(80.milliseconds)
                                                }
                                            }
                                            is PressInteraction.Release -> {
                                                if (!isLongClick) {
                                                    onQuantityChange(step)
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        }
                                    }
                                }

                                val isWeight = lastScannedProduct.por_peso == 1L
                                var textValue by remember(lastScannedQuantity, lastScannedProduct.id) {
                                    mutableStateOf(lastScannedQuantity.formatQuantity(isWeight))
                                }

                                val commitQuantity = {
                                    val parsed = textValue.toDoubleOrNull()
                                    if (parsed != null && parsed > 0.0) {
                                        val delta = parsed - lastScannedQuantity
                                        if (kotlin.math.abs(delta) > 0.0001) {
                                            onQuantityChange(delta)
                                        }
                                    } else {
                                        textValue = lastScannedQuantity.formatQuantity(isWeight)
                                    }
                                }

                                ButtonGroup(
                                    overflowIndicator = { state ->
                                        ButtonGroupDefaults.OverflowIndicator(
                                            state,
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(),
                                            modifier = Modifier.size(32.dp, 48.dp)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    customItem(
                                        {
                                            FilledTonalIconButton(
                                                onClick = {},
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                ),
                                                shapes = IconButtonDefaults.shapes(),
                                                interactionSource = interactionSourceMinus,
                                                modifier = Modifier
                                                    .size(96.dp, 48.dp)
                                                    .animateWidth(interactionSourceMinus)
                                            ) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.remove),
                                                    contentDescription = stringResource(Res.string.decrease_desc),
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        },
                                        { _ -> }
                                    )

                                    customItem(
                                        {
                                            BasicTextField(
                                                value = textValue,
                                                onValueChange = { newValue ->
                                                    val filtered = if (isWeight) {
                                                        newValue.filter { it.isDigit() || it == '.' }
                                                    } else {
                                                        newValue.filter { it.isDigit() }
                                                    }
                                                    textValue = filtered
                                                },
                                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    textAlign = TextAlign.Center
                                                ),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = if (isWeight) KeyboardType.Decimal else KeyboardType.Number,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = { commitQuantity() }
                                                ),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(.5f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .wrapContentSize(Alignment.Center)
                                                    .onFocusChanged { focusState ->
                                                        if (!focusState.isFocused) {
                                                            commitQuantity()
                                                        }
                                                    }
                                            )
                                        },
                                        {}
                                    )

                                    customItem(
                                        {
                                            FilledTonalIconButton(
                                                onClick = {},
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                ),
                                                shapes = IconButtonDefaults.shapes(),
                                                interactionSource = interactionSourcePlus,
                                                modifier = Modifier
                                                    .size(96.dp, 48.dp)
                                                    .animateWidth(interactionSourcePlus)
                                            ) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.add),
                                                    contentDescription = stringResource(Res.string.increase_desc),
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        },
                                        { _ -> }
                                    )
                                }
                            }

                        }
                    } else statusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun PermissionRationaleScreen(
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painterResource(Res.drawable.photo_camera),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(84.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.camera_permission_required_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.camera_permission_required_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = ShapeDefaults.topListItemShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    stringResource(Res.string.grant_permission_button),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = ShapeDefaults.bottomListItemShape
            ) {
                Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold)
            }
        }
    }
}

