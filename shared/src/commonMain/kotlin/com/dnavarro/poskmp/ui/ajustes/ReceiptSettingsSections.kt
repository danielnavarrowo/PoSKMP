package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.PRINTER_SYSTEM_DIALOG_ID
import com.dnavarro.poskmp.domain.model.PrinterType
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.domain.receipt.ReceiptFormatter
import com.dnavarro.poskmp.domain.model.ReceiptItem
import com.dnavarro.poskmp.printer.getReceiptPrinterOptions
import com.dnavarro.poskmp.printer.getSystemReceiptFontFamilies
import com.dnavarro.poskmp.printer.receiptFontFamily
import com.dnavarro.poskmp.ui.venta.ReceiptDocumentPreview
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.receipt_feed_lines_label
import poskmp.shared.generated.resources.receipt_feed_lines_value
import poskmp.shared.generated.resources.receipt_font_label
import poskmp.shared.generated.resources.receipt_font_size_label
import poskmp.shared.generated.resources.receipt_font_size_value
import poskmp.shared.generated.resources.receipt_footer_label
import poskmp.shared.generated.resources.receipt_footer_placeholder
import poskmp.shared.generated.resources.receipt_preview_settings_subtitle
import poskmp.shared.generated.resources.receipt_preview_settings_title
import poskmp.shared.generated.resources.printer_no_printers_available
import poskmp.shared.generated.resources.printer_selection_label
import poskmp.shared.generated.resources.printer_system_dialog
import poskmp.shared.generated.resources.printer_section_subtitle
import poskmp.shared.generated.resources.printer_section_title
import poskmp.shared.generated.resources.printer_type_a4
import poskmp.shared.generated.resources.printer_type_label
import poskmp.shared.generated.resources.printer_type_letter
import poskmp.shared.generated.resources.printer_type_thermal_80mm
import poskmp.shared.generated.resources.store_address_label
import poskmp.shared.generated.resources.store_info_section_subtitle
import poskmp.shared.generated.resources.store_info_section_title
import poskmp.shared.generated.resources.store_name_label
import poskmp.shared.generated.resources.store_phone_label

@Composable
fun StoreInfoSettingsSection(
    settings: ReceiptSettings,
    onSettingsChange: (ReceiptSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var storeName by remember { mutableStateOf(settings.storeName) }
    var storeAddress by remember { mutableStateOf(settings.storeAddress) }
    var storePhone by remember { mutableStateOf(settings.storePhone) }
    var storeNameFocused by remember { mutableStateOf(false) }
    var storeAddressFocused by remember { mutableStateOf(false) }
    var storePhoneFocused by remember { mutableStateOf(false) }
    LaunchedEffect(settings.storeName) {
        if (!storeNameFocused && settings.storeName != storeName) storeName = settings.storeName
    }
    LaunchedEffect(settings.storeAddress) {
        if (!storeAddressFocused && settings.storeAddress != storeAddress) storeAddress = settings.storeAddress
    }
    LaunchedEffect(settings.storePhone) {
        if (!storePhoneFocused && settings.storePhone != storePhone) storePhone = settings.storePhone
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(Res.string.store_info_section_title),
                fontWeight = FontWeight.Bold,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(Res.string.store_info_section_subtitle),
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val availablePrinters = remember { getReceiptPrinterOptions() }
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
    val systemFontFamilies = remember { getSystemReceiptFontFamilies() }
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
    val fontFamilies = (systemFontFamilies + settings.fontFamily)
        .filter { it.isNotBlank() }
        .distinct()
        .sortedBy { it.lowercase() }
    val previewReceipt = ReceiptFormatter.create(
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(Res.string.printer_section_title),
                fontWeight = FontWeight.Bold,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(Res.string.printer_section_subtitle),
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.printer_selection_label),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            ReceiptDropdown(
                label = stringResource(Res.string.printer_selection_label),
                selectedLabel = selectedPrinterName.ifBlank {
                    stringResource(Res.string.printer_no_printers_available)
                },
                options = printerOptions.map { ReceiptDropdownOption(it.id, it.name) },
                selectedValue = selectedPrinterId.orEmpty(),
                onSelected = { printerId ->
                    if (printerId.isNotBlank()) onSettingsChange(settings.copy(printerId = printerId))
                }
            )
            val paperOptions = listOf(
                PrinterType.THERMAL_80MM to stringResource(Res.string.printer_type_thermal_80mm),
                PrinterType.A4 to stringResource(Res.string.printer_type_a4),
                PrinterType.LETTER to stringResource(Res.string.printer_type_letter)
            )
            Text(
                text = stringResource(Res.string.printer_type_label),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                paperOptions.forEach { (type, label) ->
                    val selected = settings.printerType == type
                    Button(
                        onClick = { onSettingsChange(settings.copy(printerType = type)) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 6.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }
            ReceiptDropdown(
                label = stringResource(Res.string.receipt_font_label),
                selectedLabel = settings.fontFamily,
                options = fontFamilies.map { ReceiptDropdownOption(it, it) },
                selectedValue = settings.fontFamily,
                onSelected = { fontFamily ->
                    onSettingsChange(settings.copy(fontFamily = fontFamily))
                },
                optionFontFamily = { receiptFontFamily(it) }
            )
            Text(stringResource(Res.string.receipt_font_size_label), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Slider(
                value = settings.fontSize.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(fontSize = it.toInt().coerceIn(8, 32))) },
                valueRange = 8f..32f,
                steps = 23
            )
            Text(
                stringResource(Res.string.receipt_font_size_value, settings.fontSize),
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(Res.string.receipt_feed_lines_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = settings.feedLines.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(feedLines = it.toInt().coerceIn(0, 10))) },
                valueRange = 0f..10f,
                steps = 9
            )
            Text(
                stringResource(Res.string.receipt_feed_lines_value, settings.feedLines),
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
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
            Text(
                text = stringResource(Res.string.receipt_preview_settings_title),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = stringResource(Res.string.receipt_preview_settings_subtitle),
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            ReceiptDocumentPreview(
                receipt = previewReceipt,
                maxHeight = 360.dp
            )
        }
    }
}

private data class ReceiptDropdownOption(
    val value: String,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptDropdown(
    label: String,
    selectedLabel: String,
    options: List<ReceiptDropdownOption>,
    selectedValue: String,
    onSelected: (String) -> Unit,
    optionFontFamily: ((String) -> FontFamily)? = null
) {
    val expanded = remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = { expanded.value = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontFamily = optionFontFamily?.invoke(option.value),
                            fontWeight = if (option.value == selectedValue) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded.value = false
                        onSelected(option.value)
                    }
                )
            }
        }
    }
}
