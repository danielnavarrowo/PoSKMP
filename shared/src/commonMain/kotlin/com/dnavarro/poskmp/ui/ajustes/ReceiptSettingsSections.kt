package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlin.math.abs
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.domain.model.PRINTER_SYSTEM_DIALOG_ID
import com.dnavarro.poskmp.domain.model.ReceiptFontFamily
import com.dnavarro.poskmp.domain.model.ReceiptItem
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.domain.receipt.ReceiptFormatter
import com.dnavarro.poskmp.domain.usecase.PrintReceiptUseCase
import com.dnavarro.poskmp.printer.getReceiptPrinterOptions
import com.dnavarro.poskmp.printer.hasBluetoothPrinterPermission
import com.dnavarro.poskmp.printer.openBluetoothSettings
import com.dnavarro.poskmp.printer.rememberBluetoothPermissionRequester
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.theme.googleFlexBody
import com.dnavarro.poskmp.ui.components.SyncedSettingBadge
import com.dnavarro.poskmp.ui.venta.ReceiptDocumentPreview
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.open_drawer_on_receipt_subtitle
import poskmp.shared.generated.resources.open_drawer_on_receipt_title
import poskmp.shared.generated.resources.paper_width_label
import poskmp.shared.generated.resources.paper_width_value
import poskmp.shared.generated.resources.printer_no_printers_available
import poskmp.shared.generated.resources.printer_section_title
import poskmp.shared.generated.resources.printer_selection_label
import poskmp.shared.generated.resources.printer_system_dialog
import poskmp.shared.generated.resources.receipt_feed_lines_label
import poskmp.shared.generated.resources.receipt_feed_lines_value
import poskmp.shared.generated.resources.receipt_font_size_label
import poskmp.shared.generated.resources.receipt_font_size_value
import poskmp.shared.generated.resources.receipt_footer_label
import poskmp.shared.generated.resources.receipt_footer_placeholder
import poskmp.shared.generated.resources.receipt_preview_settings_title
import poskmp.shared.generated.resources.store_address_label
import poskmp.shared.generated.resources.store_info_section_subtitle
import poskmp.shared.generated.resources.store_info_section_title
import poskmp.shared.generated.resources.store_name_label
import poskmp.shared.generated.resources.store_phone_label
import poskmp.shared.generated.resources.transfer_beneficiary_label
import poskmp.shared.generated.resources.transfer_beneficiary_placeholder
import poskmp.shared.generated.resources.transfer_clabe_label
import poskmp.shared.generated.resources.transfer_clabe_placeholder
import poskmp.shared.generated.resources.transfer_settings_section_subtitle
import poskmp.shared.generated.resources.transfer_settings_section_title
import kotlin.math.roundToInt

@Composable
fun StoreInfoSettingsSection(
    settings: ReceiptSettings,
    onSettingsChange: (ReceiptSettings) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ShapeDefaults.cardShape
) {
    var storeName by remember { mutableStateOf(settings.storeName) }
    var storeAddress by remember { mutableStateOf(settings.storeAddress) }
    var storePhone by remember { mutableStateOf(settings.storePhone) }
    var storeNameFocused by remember { mutableStateOf(false) }
    var storeAddressFocused by remember { mutableStateOf(false) }
    var storePhoneFocused by remember { mutableStateOf(false) }

    LaunchedEffect(settings.storeName, settings.storeAddress, settings.storePhone) {
        if (!storeNameFocused && settings.storeName != storeName) storeName = settings.storeName
        if (!storeAddressFocused && settings.storeAddress != storeAddress) storeAddress = settings.storeAddress
        if (!storePhoneFocused && settings.storePhone != storePhone) storePhone = settings.storePhone
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.store_info_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            SyncedSettingBadge()
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = shape
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.store_info_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text(stringResource(Res.string.store_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                storeNameFocused = true
                            } else if (storeNameFocused) {
                                storeNameFocused = false
                                onSettingsChange(
                                    settings.copy(
                                        storeName = storeName,
                                        storeAddress = storeAddress,
                                        storePhone = storePhone
                                    )
                                )
                            }
                        }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = storeAddress,
                    onValueChange = { storeAddress = it },
                    label = { Text(stringResource(Res.string.store_address_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                storeAddressFocused = true
                            } else if (storeAddressFocused) {
                                storeAddressFocused = false
                                onSettingsChange(
                                    settings.copy(
                                        storeName = storeName,
                                        storeAddress = storeAddress,
                                        storePhone = storePhone
                                    )
                                )
                            }
                        }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = storePhone,
                    onValueChange = { storePhone = it },
                    label = { Text(stringResource(Res.string.store_phone_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                storePhoneFocused = true
                            } else if (storePhoneFocused) {
                                storePhoneFocused = false
                                onSettingsChange(
                                    settings.copy(
                                        storeName = storeName,
                                        storeAddress = storeAddress,
                                        storePhone = storePhone
                                    )
                                )
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun TransferSettingsSection(
    settings: ReceiptSettings,
    onSettingsChange: (ReceiptSettings) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ShapeDefaults.cardShape
) {
    var transferClabe by remember { mutableStateOf(settings.transferClabe) }
    var transferBeneficiary by remember { mutableStateOf(settings.transferBeneficiary) }
    var clabeFocused by remember { mutableStateOf(false) }
    var beneficiaryFocused by remember { mutableStateOf(false) }

    LaunchedEffect(settings.transferClabe, settings.transferBeneficiary) {
        if (!clabeFocused && settings.transferClabe != transferClabe) transferClabe = settings.transferClabe
        if (!beneficiaryFocused && settings.transferBeneficiary != transferBeneficiary) transferBeneficiary = settings.transferBeneficiary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.transfer_settings_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            SyncedSettingBadge()
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = shape
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.transfer_settings_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = transferClabe,
                    onValueChange = { input ->
                        transferClabe = input.filter { it.isDigit() }.take(18)
                    },
                    label = { Text(stringResource(Res.string.transfer_clabe_label)) },
                    placeholder = { Text(stringResource(Res.string.transfer_clabe_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = {
                        if (transferClabe.isNotEmpty()) {
                            Text(
                                text = "Formato: ${transferClabe.chunked(4).joinToString(" ")} (${transferClabe.length} dígitos)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                clabeFocused = true
                            } else if (clabeFocused) {
                                clabeFocused = false
                                onSettingsChange(
                                    settings.copy(
                                        transferClabe = transferClabe,
                                        transferBeneficiary = transferBeneficiary
                                    )
                                )
                            }
                        }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = transferBeneficiary,
                    onValueChange = { transferBeneficiary = it },
                    label = { Text(stringResource(Res.string.transfer_beneficiary_label)) },
                    placeholder = { Text(stringResource(Res.string.transfer_beneficiary_placeholder)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                beneficiaryFocused = true
                            } else if (beneficiaryFocused) {
                                beneficiaryFocused = false
                                onSettingsChange(
                                    settings.copy(
                                        transferClabe = transferClabe,
                                        transferBeneficiary = transferBeneficiary
                                    )
                                )
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun PrinterSettingsSection(
    settings: ReceiptSettings,
    onSettingsChange: (ReceiptSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var footerMessage by remember { mutableStateOf(settings.footerMessage) }
    var footerFocused by remember { mutableStateOf(false) }

    LaunchedEffect(settings.footerMessage) {
        if (!footerFocused && settings.footerMessage != footerMessage) footerMessage = settings.footerMessage
    }

    val systemDialogLabel = stringResource(Res.string.printer_system_dialog)
    var availablePrinters by remember { mutableStateOf<List<ReceiptPrinterOption>>(emptyList()) }
    var hasBtPermission by remember { mutableStateOf(hasBluetoothPrinterPermission()) }
    val coroutineScope = rememberCoroutineScope()
    val printReceiptUseCase = koinInject<PrintReceiptUseCase>()
    var isPrintingTest by remember { mutableStateOf(false) }
    var testPrintStatus by remember { mutableStateOf<String?>(null) }
    var testPrintIsError by remember { mutableStateOf(false) }

    val requestBluetoothPermission = rememberBluetoothPermissionRequester { granted ->
        hasBtPermission = granted
        coroutineScope.launch {
            availablePrinters = getReceiptPrinterOptions()
        }
    }

    LaunchedEffect(Unit) {
        if (isAndroid() && !hasBtPermission) {
            requestBluetoothPermission()
        } else {
            availablePrinters = getReceiptPrinterOptions()
        }
    }

    val printerOptions = remember(availablePrinters, systemDialogLabel) {
        val systemOption = ReceiptPrinterOption(
            id = PRINTER_SYSTEM_DIALOG_ID,
            name = systemDialogLabel,
            isDefault = false
        )
        listOf(systemOption) + availablePrinters.filter {
            it.id != PRINTER_SYSTEM_DIALOG_ID && it.id != "android-system"
        }
    }

    val selectedPrinterId = when {
        settings.printerId.isNullOrBlank() -> PRINTER_SYSTEM_DIALOG_ID
        settings.printerId == "android-system" -> PRINTER_SYSTEM_DIALOG_ID
        printerOptions.any { it.id == settings.printerId } -> settings.printerId
        else -> settings.printerId
    }

    val selectedPrinterName = when (selectedPrinterId) {
        PRINTER_SYSTEM_DIALOG_ID -> systemDialogLabel
        else -> printerOptions.firstOrNull { it.id == selectedPrinterId }?.name
            ?: selectedPrinterId
    }

    val previewReceipt = remember(settings) {
        ReceiptFormatter.create(
            folio = 1001L,
            createdAt = 0L,
            items = listOf(
                ReceiptItem("Producto de prueba", 2.0, 12.0, 24.0),
                ReceiptItem("Artículo de muestra", 1.0, 24.0, 24.0)
            ),
            total = 48.0,
            paid = 50.0,
            change = 2.0,
            paymentMethod = "EFECTIVO",
            customerName = null,
            settings = settings
        )
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.printer_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = ShapeDefaults.topListItemShape,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ReceiptDropdown(
                    label = stringResource(Res.string.printer_selection_label),
                    selectedLabel = selectedPrinterName.ifBlank {
                        stringResource(Res.string.printer_no_printers_available)
                    },
                    options = printerOptions.map { ReceiptDropdownOption(it.id, it.name) },
                    selectedValue = selectedPrinterId,
                    onSelected = { printerId ->
                        if (printerId.isNotBlank()) onSettingsChange(settings.copy(printerId = printerId))
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAndroid()) {
                        TextButton(
                            onClick = { openBluetoothSettings() }
                        ) {
                            Text("Vincular en Bluetooth")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (isAndroid() && !hasBluetoothPrinterPermission()) {
                                requestBluetoothPermission()
                            } else {
                                coroutineScope.launch {
                                    availablePrinters = getReceiptPrinterOptions()
                                }
                            }
                        }
                    ) {
                        Text(if (!isAndroid() || hasBtPermission) "Actualizar lista" else "Permitir Bluetooth")
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = ShapeDefaults.middleListItemShape
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Fuente del ticket",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tipografía utilizada para imprimir los tickets de compra",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val fontOptions = listOf(
                        Pair(ReceiptFontFamily.MONOSPACE, "Monoespaciada"),
                        Pair(ReceiptFontFamily.MANROPE, "Manrope")
                    )
                    fontOptions.forEachIndexed { index, (fontOption, label) ->
                        val isSelected = settings.fontFamily == fontOption
                        ToggleButton(
                            checked = isSelected,
                            onCheckedChange = { onSettingsChange(settings.copy(fontFamily = fontOption)) },
                            colors = ToggleButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                fontOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = when (fontOption) {
                                        ReceiptFontFamily.MONOSPACE -> FontFamily.Monospace
                                        ReceiptFontFamily.MANROPE -> googleFlexBody
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = ShapeDefaults.middleListItemShape) {
            Column (modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.paper_width_label),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.paper_width_value, settings.paperWidthMm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val paperWidthSliderState = rememberSliderState(
                    value = settings.paperWidthMm.toFloat(),
                    steps = 49,
                    trackRange = 55f..105f
                )
                LaunchedEffect(settings.paperWidthMm) {
                    val target = settings.paperWidthMm.toFloat()
                    if (abs(paperWidthSliderState.value - target) > 0.1f) {
                        paperWidthSliderState.value = target
                    }
                }
                Slider(
                    state = paperWidthSliderState,
                    onValueChange = { onSettingsChange(settings.copy(paperWidthMm = it.roundToInt().coerceIn(55, 105))) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = ShapeDefaults.middleListItemShape) {
            Column (modifier = Modifier.padding(20.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.receipt_font_size_label),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.receipt_font_size_value, settings.fontSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val fontSizeSliderState = rememberSliderState(
                    value = settings.fontSize.toFloat(),
                    steps = 23,
                    trackRange = 8f..32f
                )
                LaunchedEffect(settings.fontSize) {
                    val target = settings.fontSize.toFloat()
                    if (abs(fontSizeSliderState.value - target) > 0.1f) {
                        fontSizeSliderState.value = target
                    }
                }
                Slider(
                    state = fontSizeSliderState,
                    onValueChange = { onSettingsChange(settings.copy(fontSize = it.toInt().coerceIn(8, 32))) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = ShapeDefaults.middleListItemShape) {
            Column (modifier = Modifier.padding(20.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.receipt_feed_lines_label),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.receipt_feed_lines_value, settings.feedLines),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val feedLinesSliderState = rememberSliderState(
                    value = settings.feedLines.toFloat(),
                    steps = 9,
                    trackRange = 0f..10f
                )
                LaunchedEffect(settings.feedLines) {
                    val target = settings.feedLines.toFloat()
                    if (abs(feedLinesSliderState.value - target) > 0.1f) {
                        feedLinesSliderState.value = target
                    }
                }
                Slider(
                    state = feedLinesSliderState,
                    onValueChange = { onSettingsChange(settings.copy(feedLines = it.toInt().coerceIn(0, 10))) }
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = ShapeDefaults.middleListItemShape) {
            Column (modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.receipt_footer_label),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SyncedSettingBadge()
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = footerMessage,
                    onValueChange = { footerMessage = it },
                    label = { Text(stringResource(Res.string.receipt_footer_label)) },
                    placeholder = { Text(stringResource(Res.string.receipt_footer_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                footerFocused = true
                            } else if (footerFocused) {
                                footerFocused = false
                                onSettingsChange(settings.copy(footerMessage = footerMessage))
                            }
                        },
                    minLines = 2,
                    maxLines = 3
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = ShapeDefaults.middleListItemShape
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(Res.string.open_drawer_on_receipt_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.open_drawer_on_receipt_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = settings.openCashDrawerOnCashSale,
                    onCheckedChange = { checked ->
                        onSettingsChange(settings.copy(openCashDrawerOnCashSale = checked))
                    }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = ShapeDefaults.bottomListItemShape
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.receipt_preview_settings_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = {
                            if (isPrintingTest) return@Button
                            isPrintingTest = true
                            testPrintStatus = null
                            coroutineScope.launch {
                                val testDoc = ReceiptFormatter.create(
                                    folio = 1001L,
                                    createdAt = currentTimeMillis(),
                                    items = listOf(
                                        ReceiptItem("Producto de prueba", 2.0, 12.0, 24.0),
                                        ReceiptItem("Artículo con ñ y acentos", 1.0, 24.0, 24.0)
                                    ),
                                    total = 48.0,
                                    paid = 50.0,
                                    change = 2.0,
                                    paymentMethod = "EFECTIVO",
                                    customerName = null,
                                    settings = settings
                                )
                                val result = printReceiptUseCase(testDoc)
                                isPrintingTest = false
                                if (result.isSuccess) {
                                    testPrintIsError = false
                                    testPrintStatus = "Ticket de prueba enviado a la impresora."
                                } else {
                                    testPrintIsError = true
                                    testPrintStatus = result.exceptionOrNull()?.message
                                        ?: "Error al imprimir el ticket de prueba."
                                }
                            }
                        },
                        enabled = !isPrintingTest
                    ) {
                        if (isPrintingTest) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Imprimiendo...")
                        } else {
                            Text("Imprimir prueba")
                        }
                    }
                }
                if (testPrintStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testPrintStatus ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testPrintIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                ReceiptDocumentPreview(
                    receipt = previewReceipt
                )
            }
        }
    }
}

private data class ReceiptDropdownOption(
    val value: String,
    val label: String
)

@Composable
private fun ReceiptDropdown(
    label: String,
    selectedLabel: String,
    options: List<ReceiptDropdownOption>,
    selectedValue: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(20.dp)
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (option.value == selectedValue) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(option.value)
                    }
                )
            }
        }
    }
}

@Composable
fun TicketSettingsSection(
    settings: ReceiptSettings,
    onSettingsChange: (ReceiptSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        StoreInfoSettingsSection(
            settings = settings,
            onSettingsChange = onSettingsChange
        )
        Spacer(modifier = Modifier.height(24.dp))
        TransferSettingsSection(
            settings = settings,
            onSettingsChange = onSettingsChange
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrinterSettingsSection(
            settings = settings,
            onSettingsChange = onSettingsChange
        )
    }
}
