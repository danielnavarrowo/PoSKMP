package com.dnavarro.poskmp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import com.dnavarro.poskmp.theme.ShapeDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.disambiguateProductName
import com.dnavarro.poskmp.util.formatBarcodesForDisplay
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.normalizeBarcode
import com.dnavarro.poskmp.util.parseBarcodes
import com.dnavarro.poskmp.util.parseImportFile
import com.dnavarro.poskmp.util.pickFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.back_button
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.codes_display_label
import poskmp.shared.generated.resources.confirm_and_import_button
import poskmp.shared.generated.resources.delete_and_replace_button
import poskmp.shared.generated.resources.header_category
import poskmp.shared.generated.resources.header_codes
import poskmp.shared.generated.resources.header_name
import poskmp.shared.generated.resources.header_price
import poskmp.shared.generated.resources.import_choose_action
import poskmp.shared.generated.resources.import_col_name_req
import poskmp.shared.generated.resources.import_col_price_req
import poskmp.shared.generated.resources.import_db_save_error
import poskmp.shared.generated.resources.import_detected_products
import poskmp.shared.generated.resources.import_file_label
import poskmp.shared.generated.resources.import_no_valid_products_error
import poskmp.shared.generated.resources.import_opt_replace_desc
import poskmp.shared.generated.resources.import_opt_replace_title
import poskmp.shared.generated.resources.import_opt_update_desc
import poskmp.shared.generated.resources.import_opt_update_title
import poskmp.shared.generated.resources.import_optional_columns_hint
import poskmp.shared.generated.resources.import_parse_error
import poskmp.shared.generated.resources.import_products_title
import poskmp.shared.generated.resources.import_progress_inserting
import poskmp.shared.generated.resources.import_progress_saving
import poskmp.shared.generated.resources.import_progress_starting
import poskmp.shared.generated.resources.import_required_columns_title
import poskmp.shared.generated.resources.import_select_file_hint
import poskmp.shared.generated.resources.import_select_file_title
import poskmp.shared.generated.resources.import_step_format
import poskmp.shared.generated.resources.import_success_replace_message
import poskmp.shared.generated.resources.import_success_title
import poskmp.shared.generated.resources.import_success_update_message
import poskmp.shared.generated.resources.import_completed_with_errors_title
import poskmp.shared.generated.resources.import_completed_with_modifications_title
import poskmp.shared.generated.resources.import_failed_all_title
import poskmp.shared.generated.resources.import_failed_products_header
import poskmp.shared.generated.resources.import_failed_products_hint
import poskmp.shared.generated.resources.import_modified_products_header
import poskmp.shared.generated.resources.import_modified_products_hint
import poskmp.shared.generated.resources.import_modified_summary_format
import poskmp.shared.generated.resources.import_original_name_label
import poskmp.shared.generated.resources.import_none_imported_message
import poskmp.shared.generated.resources.import_skipped_summary_format
import poskmp.shared.generated.resources.import_warning_replace_all
import poskmp.shared.generated.resources.info
import poskmp.shared.generated.resources.next_button
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.upload
import poskmp.shared.generated.resources.warning

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    var failedProductsList by remember { mutableStateOf<List<FailedImportItem>>(emptyList()) }
    var modifiedProductsList by remember { mutableStateOf<List<ModifiedImportItem>>(emptyList()) }
    var updateExistingOption by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var importProgressFraction by remember { mutableFloatStateOf(0f) }
    var importProgressText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val noValidProductsErr = stringResource(Res.string.import_no_valid_products_error)
    val parseErr = stringResource(Res.string.import_parse_error)
    val startingImportText = stringResource(Res.string.import_progress_starting)
    val savingFmt = stringResource(Res.string.import_progress_saving)
    val updateSuccessFmt = stringResource(Res.string.import_success_update_message)
    val insertingFmt = stringResource(Res.string.import_progress_inserting)
    val replaceSuccessFmt = stringResource(Res.string.import_success_replace_message)
    val dbSaveErrFmt = stringResource(Res.string.import_db_save_error)
    val skippedSummaryFmt = stringResource(Res.string.import_skipped_summary_format)
    val modifiedSummaryFmt = stringResource(Res.string.import_modified_summary_format)
    val noneImportedMsg = stringResource(Res.string.import_none_imported_message)

    val isWideStep = currentStep == 2 || (currentStep == 4 && (failedProductsList.isNotEmpty() || modifiedProductsList.isNotEmpty()))

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 360.dp, max = if (isWideStep) 920.dp else 640.dp)
                .fillMaxWidth(if (isWideStep) 0.94f else 0.88f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = ShapeDefaults.cardShape,
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
                        text = stringResource(Res.string.import_products_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentStep in 1..3) {
                        Text(
                            text = stringResource(Res.string.import_step_format, currentStep),
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
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.import_required_columns_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(Res.string.import_col_name_req),
                                    fontSize = 12.sp
                                )
                                Text(
                                    stringResource(Res.string.import_col_price_req),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = stringResource(Res.string.import_optional_columns_hint),
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
                                        allowedExtensions = listOf("csv", "xlsx", "json"),
                                        onFilePicked = { name, bytes ->
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val prods = parseImportFile(name, bytes)
                                                    withContext(Dispatchers.Main) {
                                                        if (prods.isEmpty()) {
                                                            importError = noValidProductsErr
                                                        } else {
                                                            selectedFileName = name
                                                            selectedFileBytes = bytes
                                                            parsedProducts = prods
                                                            importError = null
                                                            failedProductsList = emptyList()
                                                            modifiedProductsList = emptyList()
                                                            currentStep = 2
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        importError = e.message ?: parseErr
                                                    }
                                                }
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
                                    painter = painterResource(Res.drawable.upload),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.import_select_file_title),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.import_select_file_hint),
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
                                Text(stringResource(Res.string.cancel))
                            }
                        }
                    }

                    2 -> {
                        // Preview Step
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(
                                    Res.string.import_file_label,
                                    selectedFileName ?: ""
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(
                                    Res.string.import_detected_products,
                                    parsedProducts.size
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Scrollable table preview
                            Card(
                                modifier = Modifier.fillMaxWidth().height(260.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
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
                                            Text(
                                                stringResource(Res.string.header_name),
                                                modifier = Modifier.weight(0.4f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                stringResource(Res.string.header_codes),
                                                modifier = Modifier.weight(0.25f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                stringResource(Res.string.header_price),
                                                modifier = Modifier.weight(0.18f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                stringResource(Res.string.header_category),
                                                modifier = Modifier.weight(0.17f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
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
                                            Text(
                                                text = product.formatBarcodesForDisplay(),
                                                modifier = Modifier.weight(0.25f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$${
                                                    product.precio.toString().formatPrice()
                                                }",
                                                modifier = Modifier.weight(0.18f),
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = product.categoria
                                                    ?: stringResource(Res.string.no_category),
                                                modifier = Modifier.weight(0.17f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
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
                                Text(stringResource(Res.string.back_button))
                            }

                            Button(
                                onClick = { currentStep = 3 },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(stringResource(Res.string.next_button))
                            }
                        }
                    }

                    3 -> {
                        // Options Step
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(Res.string.import_choose_action),
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
                                    containerColor = if (updateExistingOption) MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.15f
                                    )
                                    else MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = updateExistingOption,
                                        onClick = {
                                            if (!isProcessing) updateExistingOption = true
                                        },
                                        enabled = !isProcessing
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            stringResource(Res.string.import_opt_update_title),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            stringResource(Res.string.import_opt_update_desc),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                                    containerColor = if (!updateExistingOption) MaterialTheme.colorScheme.errorContainer.copy(
                                        alpha = 0.1f
                                    )
                                    else MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = !updateExistingOption,
                                        onClick = {
                                            if (!isProcessing) updateExistingOption = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error),
                                        enabled = !isProcessing
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            stringResource(Res.string.import_opt_replace_title),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            stringResource(Res.string.import_opt_replace_desc),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (!updateExistingOption) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.warning),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(Res.string.import_warning_replace_all),
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
                                    LinearWavyProgressIndicator(
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
                                Text(stringResource(Res.string.back_button))
                            }

                            Button(
                                onClick = {
                                    isProcessing = true
                                    importError = null
                                    failedProductsList = emptyList()
                                    modifiedProductsList = emptyList()
                                    importProgressFraction = 0f
                                    importProgressText = startingImportText
                                    scope.launch {
                                        try {
                                            var updatedCount = 0
                                            var insertedCount = 0
                                            val failedItems = mutableListOf<FailedImportItem>()
                                            val modifiedItems = mutableListOf<ModifiedImportItem>()

                                            withContext(Dispatchers.IO) {
                                                if (updateExistingOption) {
                                                    val existingProducts =
                                                        repository.getAllProductsList()
                                                    val existingById =
                                                        existingProducts.associateBy { it.id }.toMutableMap()
                                                    val existingByBarcode =
                                                        mutableMapOf<String, Products>()
                                                    val existingByName =
                                                        mutableMapOf<String, Products>()
                                                    existingProducts.forEach { prod ->
                                                        val cleanName = prod.nombre.trim().lowercase()
                                                        if (cleanName.isNotEmpty()) {
                                                            existingByName.putIfAbsent(cleanName, prod)
                                                        }
                                                        val codes = prod.parseBarcodes()
                                                        codes.forEach { code ->
                                                            val trimmed = code.trim()
                                                            if (trimmed.isNotEmpty()) {
                                                                existingByBarcode[trimmed] = prod
                                                                val norm = normalizeBarcode(trimmed)
                                                                if (norm.isNotEmpty()) {
                                                                    existingByBarcode.putIfAbsent(norm, prod)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    val total = parsedProducts.size
                                                    val updateInterval = maxOf(1, total / 20)
                                                    val toSave = ArrayList<Products>(total)

                                                    for ((index, p) in parsedProducts.withIndex()) {
                                                        try {
                                                            var targetId = p.id
                                                            var isExisting = false

                                                            if (existingById.containsKey(targetId)) {
                                                                isExisting = true
                                                            } else {
                                                                val pCodes = p.parseBarcodes()
                                                                for (code in pCodes) {
                                                                    val trimmed = code.trim()
                                                                    val matched =
                                                                        existingByBarcode[trimmed] ?: existingByBarcode[normalizeBarcode(trimmed)]
                                                                    if (matched != null) {
                                                                        targetId = matched.id
                                                                        isExisting = true
                                                                        break
                                                                    }
                                                                }
                                                                if (!isExisting && pCodes.isEmpty()) {
                                                                    val cleanName = p.nombre.trim().lowercase()
                                                                    val matched = existingByName[cleanName]
                                                                    if (matched != null && matched.parseBarcodes().isEmpty()) {
                                                                        targetId = matched.id
                                                                        isExisting = true
                                                                    }
                                                                }
                                                            }

                                                            // Pre-validación y resolución de nombre duplicado con otro producto
                                                            val originalName = p.nombre.trim()
                                                            val cleanName = originalName.lowercase()
                                                            val nameMatch = existingByName[cleanName]
                                                            val isNameConflict = nameMatch != null && nameMatch.id != targetId

                                                            val finalName = if (isNameConflict) {
                                                                val disambiguated = disambiguateProductName(originalName) { candidate ->
                                                                    val candidateKey = candidate.trim().lowercase()
                                                                    val match = existingByName[candidateKey]
                                                                    match != null && match.id != targetId
                                                                }
                                                                disambiguated
                                                            } else {
                                                                originalName
                                                            }

                                                            if (finalName != originalName) {
                                                                modifiedItems.add(
                                                                    ModifiedImportItem(
                                                                        originalName = originalName,
                                                                        newName = finalName,
                                                                        product = p.copy(nombre = finalName)
                                                                    )
                                                                )
                                                            }

                                                            // Pre-validación de código de barras duplicado con otro producto
                                                            val pCodes = p.parseBarcodes()
                                                            for (code in pCodes) {
                                                                val trimmed = code.trim()
                                                                if (trimmed.isNotEmpty()) {
                                                                    val codeMatch = existingByBarcode[trimmed] ?: existingByBarcode[normalizeBarcode(trimmed)]
                                                                    if (codeMatch != null && codeMatch.id != targetId) {
                                                                        throw IllegalArgumentException("Código '$trimmed' duplicado con '${codeMatch.nombre}'")
                                                                    }
                                                                }
                                                            }

                                                            val pToInsert = if (isExisting) {
                                                                p.copy(
                                                                    id = targetId,
                                                                    nombre = finalName,
                                                                    updated_at = currentTimeMillis(),
                                                                    sync_state = "PENDING_UPDATE"
                                                                )
                                                            } else {
                                                                p.copy(
                                                                    id = targetId,
                                                                    nombre = finalName,
                                                                    updated_at = currentTimeMillis(),
                                                                    sync_state = "PENDING_INSERT"
                                                                )
                                                            }

                                                            val previousProduct = existingById[targetId]
                                                            if (previousProduct != null) {
                                                                val oldCleanName = previousProduct.nombre.trim().lowercase()
                                                                if (oldCleanName != finalName.trim().lowercase()) {
                                                                    existingByName.remove(oldCleanName)
                                                                }
                                                                val oldCodes = previousProduct.parseBarcodes()
                                                                val newCodes = pToInsert.parseBarcodes()
                                                                for (oldCode in oldCodes) {
                                                                    val trimmed = oldCode.trim()
                                                                    if (trimmed.isNotEmpty() && !newCodes.any { it.equals(trimmed, ignoreCase = true) }) {
                                                                        existingByBarcode.remove(trimmed)
                                                                        val norm = normalizeBarcode(trimmed)
                                                                        if (norm.isNotEmpty()) existingByBarcode.remove(norm)
                                                                    }
                                                                }
                                                            }

                                                            existingById[targetId] = pToInsert
                                                            val insertedCodes = pToInsert.parseBarcodes()
                                                            for (code in insertedCodes) {
                                                                val trimmed = code.trim()
                                                                if (trimmed.isNotEmpty()) {
                                                                    existingByBarcode[trimmed] = pToInsert
                                                                    val norm = normalizeBarcode(trimmed)
                                                                    if (norm.isNotEmpty()) existingByBarcode[norm] = pToInsert
                                                                }
                                                            }
                                                            val insertedName = pToInsert.nombre.trim().lowercase()
                                                            if (insertedName.isNotEmpty()) {
                                                                existingByName[insertedName] = pToInsert
                                                            }

                                                            toSave.add(pToInsert)
                                                            if (isExisting) updatedCount++ else insertedCount++
                                                        } catch (e: Throwable) {
                                                            val readableError = formatImportError(e)
                                                            failedItems.add(FailedImportItem(product = p, reason = readableError))
                                                        }

                                                        val currentProcessed = index + 1
                                                        if (currentProcessed % updateInterval == 0 || currentProcessed == total) {
                                                            withContext(Dispatchers.Main) {
                                                                importProgressFraction =
                                                                    0.1f + 0.6f * (currentProcessed.toFloat() / total)
                                                                importProgressText =
                                                                    savingFmt.replace(
                                                                        $$"%1$s",
                                                                        p.nombre
                                                                    ).replace(
                                                                        $$"%2$d",
                                                                        currentProcessed.toString()
                                                                    ).replace(
                                                                        $$"%3$d",
                                                                        total.toString()
                                                                    )
                                                            }
                                                        }
                                                    }

                                                    if (toSave.isNotEmpty()) {
                                                        withContext(Dispatchers.Main) {
                                                            importProgressFraction = 0.85f
                                                        }
                                                        try {
                                                            repository.insertProducts(toSave)
                                                        } catch (_: Throwable) {
                                                            var fallbackUpdated = 0
                                                            var fallbackInserted = 0
                                                            for (item in toSave) {
                                                                try {
                                                                    repository.insertProduct(item)
                                                                    if (item.sync_state == "PENDING_UPDATE") fallbackUpdated++ else fallbackInserted++
                                                                } catch (err: Throwable) {
                                                                    failedItems.add(FailedImportItem(product = item, reason = formatImportError(err)))
                                                                    modifiedItems.removeAll { it.product.id == item.id }
                                                                }
                                                            }
                                                            updatedCount = fallbackUpdated
                                                            insertedCount = fallbackInserted
                                                        }
                                                    }
                                                } else {
                                                    val total = parsedProducts.size
                                                    val updateInterval = maxOf(1, total / 20)
                                                    val existingByName = mutableMapOf<String, Products>()
                                                    val existingByBarcode = mutableMapOf<String, Products>()
                                                    val toSave = ArrayList<Products>(total)

                                                    for ((index, p) in parsedProducts.withIndex()) {
                                                        try {
                                                            val originalName = p.nombre.trim()
                                                            val cleanName = originalName.lowercase()
                                                            val nameMatch = existingByName[cleanName]
                                                            val finalName = if (nameMatch != null) {
                                                                val disambiguated = disambiguateProductName(originalName) { candidate ->
                                                                    existingByName.containsKey(candidate.trim().lowercase())
                                                                }
                                                                disambiguated
                                                            } else {
                                                                originalName
                                                            }

                                                            if (finalName != originalName) {
                                                                modifiedItems.add(
                                                                    ModifiedImportItem(
                                                                        originalName = originalName,
                                                                        newName = finalName,
                                                                        product = p.copy(nombre = finalName)
                                                                    )
                                                                )
                                                            }

                                                            val pCodes = p.parseBarcodes()
                                                            for (code in pCodes) {
                                                                val trimmed = code.trim()
                                                                if (trimmed.isNotEmpty()) {
                                                                    val codeMatch = existingByBarcode[trimmed] ?: existingByBarcode[normalizeBarcode(trimmed)]
                                                                    if (codeMatch != null) {
                                                                        throw IllegalArgumentException("Código '$trimmed' duplicado en el archivo (coincide con '${codeMatch.nombre}')")
                                                                    }
                                                                }
                                                            }

                                                            val pToInsert = p.copy(
                                                                nombre = finalName,
                                                                updated_at = currentTimeMillis(),
                                                                sync_state = "PENDING_INSERT"
                                                            )

                                                            val insertedName = pToInsert.nombre.trim().lowercase()
                                                            if (insertedName.isNotEmpty()) {
                                                                existingByName[insertedName] = pToInsert
                                                            }
                                                            for (code in pCodes) {
                                                                val trimmed = code.trim()
                                                                if (trimmed.isNotEmpty()) {
                                                                    existingByBarcode[trimmed] = pToInsert
                                                                    val norm = normalizeBarcode(trimmed)
                                                                    if (norm.isNotEmpty()) existingByBarcode[norm] = pToInsert
                                                                }
                                                            }

                                                            toSave.add(pToInsert)
                                                            insertedCount++
                                                        } catch (e: Throwable) {
                                                            val readableError = formatImportError(e)
                                                            failedItems.add(FailedImportItem(product = p, reason = readableError))
                                                        }

                                                        val currentProcessed = index + 1
                                                        if (currentProcessed % updateInterval == 0 || currentProcessed == total) {
                                                            withContext(Dispatchers.Main) {
                                                                importProgressFraction =
                                                                    0.1f + 0.6f * (currentProcessed.toFloat() / total)
                                                                importProgressText =
                                                                    insertingFmt.replace(
                                                                        $$"%1$s",
                                                                        p.nombre
                                                                    ).replace(
                                                                        $$"%2$d",
                                                                        currentProcessed.toString()
                                                                    ).replace(
                                                                        $$"%3$d",
                                                                        total.toString()
                                                                    )
                                                            }
                                                        }
                                                    }

                                                    if (toSave.isNotEmpty()) {
                                                        withContext(Dispatchers.Main) {
                                                            importProgressFraction = 0.85f
                                                        }
                                                        repository.deleteAllProducts()
                                                        try {
                                                            repository.insertProducts(toSave)
                                                        } catch (_: Throwable) {
                                                            var fallbackInserted = 0
                                                            for (item in toSave) {
                                                                try {
                                                                    repository.insertProduct(item)
                                                                    fallbackInserted++
                                                                } catch (err: Throwable) {
                                                                    failedItems.add(FailedImportItem(product = item, reason = formatImportError(err)))
                                                                    modifiedItems.removeAll { it.product.id == item.id }
                                                                }
                                                            }
                                                            insertedCount = fallbackInserted
                                                        }
                                                    }
                                                }

                                                withContext(Dispatchers.Main) {
                                                    importProgressFraction = 1f
                                                }
                                            }

                                            failedProductsList = failedItems
                                            modifiedProductsList = modifiedItems
                                            val totalFailed = failedItems.size
                                            val totalModified = modifiedItems.size
                                            if (insertedCount == 0 && updatedCount == 0 && totalFailed > 0) {
                                                importSuccessMessage = noneImportedMsg
                                            } else {
                                                val baseMessage = if (updateExistingOption) {
                                                    updateSuccessFmt.replace($$"%1$d", insertedCount.toString()).replace($$"%2$d", updatedCount.toString())
                                                } else {
                                                    replaceSuccessFmt.replace($$"%1$d", insertedCount.toString())
                                                }
                                                val withSkipped = if (totalFailed > 0) {
                                                    "$baseMessage\n" + skippedSummaryFmt.replace($$"%1$d", totalFailed.toString())
                                                } else {
                                                    baseMessage
                                                }
                                                importSuccessMessage = if (totalModified > 0) {
                                                    "$withSkipped\n" + modifiedSummaryFmt.replace($$"%1$d", totalModified.toString())
                                                } else {
                                                    withSkipped
                                                }
                                            }
                                            currentStep = 4
                                        } catch (e: Exception) {
                                            importError =
                                                dbSaveErrFmt.replace($$"%1$s", e.message ?: "")
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
                                Text(
                                    if (updateExistingOption) stringResource(Res.string.confirm_and_import_button) else stringResource(
                                        Res.string.delete_and_replace_button
                                    )
                                )
                            }
                        }
                    }

                    4 -> {
                        // Results Step
                        val allFailed = failedProductsList.size == parsedProducts.size && parsedProducts.isNotEmpty()
                        val hasErrors = failedProductsList.isNotEmpty()
                        val hasModified = modifiedProductsList.isNotEmpty()

                        val iconRes = when {
                            allFailed -> Res.drawable.warning
                            hasErrors -> Res.drawable.warning
                            hasModified -> Res.drawable.info
                            else -> Res.drawable.check
                        }
                        val iconColor = when {
                            allFailed -> MaterialTheme.colorScheme.error
                            hasErrors -> Color(0xFFF59E0B) // Amber
                            hasModified -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFF10B981) // Green
                        }
                        val titleText = when {
                            allFailed -> stringResource(Res.string.import_failed_all_title)
                            hasErrors -> stringResource(Res.string.import_completed_with_errors_title)
                            hasModified -> stringResource(Res.string.import_completed_with_modifications_title)
                            else -> stringResource(Res.string.import_success_title)
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = titleText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = importSuccessMessage ?: "",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (hasModified) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.small,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.info),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = stringResource(
                                                        Res.string.import_modified_products_header,
                                                        modifiedProductsList.size
                                                    ),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            Text(
                                                text = stringResource(Res.string.import_modified_products_hint),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))

                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = if (hasErrors) 160.dp else 240.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(modifiedProductsList) { item ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = MaterialTheme.shapes.extraSmall,
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    ),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = item.newName,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                            val codesDisplay = item.product.formatBarcodesForDisplay()
                                                            if (codesDisplay.isNotBlank()) {
                                                                Text(
                                                                    text = stringResource(Res.string.codes_display_label, codesDisplay),
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(start = 8.dp)
                                                                )
                                                            }
                                                        }
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = stringResource(Res.string.import_original_name_label, item.originalName),
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (hasErrors) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.small,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    Res.string.import_failed_products_header,
                                                    failedProductsList.size
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = stringResource(Res.string.import_failed_products_hint),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.25f))

                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = if (hasModified) 160.dp else 280.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(failedProductsList) { item ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = MaterialTheme.shapes.extraSmall,
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    ),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = item.product.nombre,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                            val codesDisplay = item.product.formatBarcodesForDisplay()
                                                            if (codesDisplay.isNotBlank()) {
                                                                Text(
                                                                    text = stringResource(Res.string.codes_display_label, codesDisplay),
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(start = 8.dp)
                                                                )
                                                            }
                                                        }
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(Res.drawable.warning),
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = item.reason,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.error,
                                                                fontWeight = FontWeight.Medium
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

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = onDismiss,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(stringResource(Res.string.close_button))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class FailedImportItem(
    val product: Products,
    val reason: String
)

data class ModifiedImportItem(
    val originalName: String,
    val newName: String,
    val product: Products
)

private fun formatImportError(e: Throwable): String {
    val message = e.message ?: ""
    return when {
        message.contains("UNIQUE constraint failed: products.nombre", ignoreCase = true) ||
        message.contains("idx_products_nombre", ignoreCase = true) -> {
            "Nombre duplicado (ya registrado)"
        }
        message.contains("UNIQUE constraint failed: products.codigos", ignoreCase = true) ||
        message.contains("idx_products_codigos", ignoreCase = true) ||
        message.contains("product_barcodes", ignoreCase = true) -> {
            "Código de barras duplicado (ya registrado)"
        }
        message.startsWith("Nombre duplicado", ignoreCase = true) ||
        message.startsWith("Código", ignoreCase = true) -> {
            message
        }
        else -> {
            message.ifBlank { "Error al guardar el producto" }
        }
    }
}
