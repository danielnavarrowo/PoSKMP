package com.dnavarro.poskmp.ui.ajustes
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.dnavarro.poskmp.ui.components.SyncedSettingBadge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.PRINTER_SYSTEM_DIALOG_ID
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.domain.receipt.ReceiptFormatter
import com.dnavarro.poskmp.domain.model.ReceiptItem
import com.dnavarro.poskmp.printer.getReceiptPrinterOptions
import com.dnavarro.poskmp.ui.venta.ReceiptDocumentPreview
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.receipt_feed_lines_label
import poskmp.shared.generated.resources.receipt_feed_lines_value
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
import kotlin.math.roundToInt
import poskmp.shared.generated.resources.paper_width_label
import poskmp.shared.generated.resources.paper_width_value
import poskmp.shared.generated.resources.store_address_label
import poskmp.shared.generated.resources.store_info_section_subtitle
import poskmp.shared.generated.resources.store_info_section_title
import poskmp.shared.generated.resources.store_name_label
import poskmp.shared.generated.resources.store_phone_label
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import poskmp.shared.generated.resources.open_drawer_on_receipt_title
import poskmp.shared.generated.resources.open_drawer_on_receipt_subtitle
import poskmp.shared.generated.resources.transfer_settings_section_title
import poskmp.shared.generated.resources.transfer_settings_section_subtitle
import poskmp.shared.generated.resources.transfer_clabe_label
import poskmp.shared.generated.resources.transfer_beneficiary_label
import poskmp.shared.generated.resources.transfer_clabe_placeholder
import poskmp.shared.generated.resources.transfer_beneficiary_placeholder

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.store_info_section_title),
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                SyncedSettingBadge()
            }
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
fun TransferSettingsSection(
    settings: ReceiptSettings,
    onSettingsChange: (ReceiptSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var transferClabe by remember { mutableStateOf(settings.transferClabe) }
    var transferBeneficiary by remember { mutableStateOf(settings.transferBeneficiary) }
    var clabeFocused by remember { mutableStateOf(false) }
    var beneficiaryFocused by remember { mutableStateOf(false) }

    LaunchedEffect(settings.transferClabe) {
        if (!clabeFocused && settings.transferClabe != transferClabe) transferClabe = settings.transferClabe
    }
    LaunchedEffect(settings.transferBeneficiary) {
        if (!beneficiaryFocused && settings.transferBeneficiary != transferBeneficiary) transferBeneficiary = settings.transferBeneficiary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.transfer_settings_section_title),
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                SyncedSettingBadge()
            }
            Text(
                text = stringResource(Res.string.transfer_settings_section_subtitle),
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
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
    LaunchedEffect(Unit) {
        availablePrinters = getReceiptPrinterOptions()
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
                selectedValue = selectedPrinterId,
                onSelected = { printerId ->
                    if (printerId.isNotBlank()) onSettingsChange(settings.copy(printerId = printerId))
                }
            )
            Text(
                text = stringResource(Res.string.paper_width_label),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Slider(
                value = settings.paperWidthMm.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(paperWidthMm = it.roundToInt().coerceIn(55, 105))) },
                valueRange = 55f..105f,
                steps = 49
            )
            Text(
                text = stringResource(Res.string.paper_width_value, settings.paperWidthMm),
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.receipt_footer_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                SyncedSettingBadge()
            }
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
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.open_drawer_on_receipt_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.open_drawer_on_receipt_subtitle),
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = settings.openCashDrawerOnCashSale,
                    onCheckedChange = { checked ->
                        onSettingsChange(settings.copy(openCashDrawerOnCashSale = checked))
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
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
                receipt = previewReceipt
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
    onSelected: (String) -> Unit
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

@Composable
fun TicketSettingsSection(
    settings: ReceiptSettings,
    onSettingsChange: (ReceiptSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StoreInfoSettingsSection(
            settings = settings,
            onSettingsChange = onSettingsChange
        )
        TransferSettingsSection(
            settings = settings,
            onSettingsChange = onSettingsChange
        )
        PrinterSettingsSection(
            settings = settings,
            onSettingsChange = onSettingsChange
        )
    }
}
