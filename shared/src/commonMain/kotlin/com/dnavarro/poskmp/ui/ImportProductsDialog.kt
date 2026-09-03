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
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.formatBarcodesForDisplay
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.normalizeBarcode
import com.dnavarro.poskmp.util.parseBarcodes
import com.dnavarro.poskmp.util.parseImportFile
import com.dnavarro.poskmp.util.pickFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.back_button
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.close_button
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
import poskmp.shared.generated.resources.import_warning_replace_all
import poskmp.shared.generated.resources.next_button
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.upload
import poskmp.shared.generated.resources.warning
import kotlin.time.Duration.Companion.milliseconds

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

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            modifier = Modifier
                .widthIn(max = if (currentStep == 2) 980.dp else 680.dp)
                .fillMaxWidth(if (currentStep == 2) 0.95f else 0.85f)
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
                                            try {
                                                val prods = parseImportFile(name, bytes)
                                                if (prods.isEmpty()) {
                                                    importError = noValidProductsErr
                                                } else {
                                                    val duplicateErr = validateImportedBarcodes(prods)
                                                    if (duplicateErr != null) {
                                                        importError = duplicateErr
                                                    } else {
                                                        selectedFileName = name
                                                        selectedFileBytes = bytes
                                                        parsedProducts = prods
                                                        importError = null
                                                        currentStep = 2
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                importError = e.message ?: parseErr
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
                                    importProgressFraction = 0f
                                    importProgressText = startingImportText
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                if (updateExistingOption) {
                                                    val existingProducts =
                                                        repository.getAllProductsList()
                                                    val existingById =
                                                        existingProducts.associateBy { it.id }
                                                    val existingByBarcode =
                                                        mutableMapOf<String, Products>()
                                                    existingProducts.forEach { prod ->
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

                                                    var updated = 0
                                                    var inserted = 0
                                                    val total = parsedProducts.size

                                                    for ((index, p) in parsedProducts.withIndex()) {
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
                                                        }

                                                        val pToInsert = if (isExisting) {
                                                            p.copy(
                                                                id = targetId,
                                                                updated_at = currentTimeMillis(),
                                                                sync_state = "PENDING_UPDATE"
                                                            )
                                                        } else {
                                                            p.copy(
                                                                id = targetId,
                                                                updated_at = currentTimeMillis(),
                                                                sync_state = "PENDING_INSERT"
                                                            )
                                                        }

                                                        repository.insertProduct(pToInsert)
                                                        if (isExisting) updated++ else inserted++

                                                        val currentProcessed = index + 1
                                                        withContext(Dispatchers.Main) {
                                                            importProgressFraction =
                                                                currentProcessed.toFloat() / total
                                                            importProgressText =
                                                                savingFmt.replace(
                                                                    $$"%1$s",
                                                                    p.nombre
                                                                )
                                                                    .replace(
                                                                        $$"%2$d",
                                                                        currentProcessed.toString()
                                                                    ).replace(
                                                                        $$"%3$d",
                                                                        total.toString()
                                                                    )
                                                        }
                                                        delay(10.milliseconds)
                                                    }
                                                    importSuccessMessage = updateSuccessFmt.replace(
                                                        $$"%1$d",
                                                        inserted.toString()
                                                    ).replace($$"%2$d", updated.toString())
                                                } else {
                                                    repository.deleteAllProducts()
                                                    val total = parsedProducts.size
                                                    for ((index, p) in parsedProducts.withIndex()) {
                                                        repository.insertProduct(
                                                            p.copy(
                                                                updated_at = currentTimeMillis(),
                                                                sync_state = "PENDING_INSERT"
                                                            )
                                                        )
                                                        val currentProcessed = index + 1
                                                        withContext(Dispatchers.Main) {
                                                            importProgressFraction =
                                                                currentProcessed.toFloat() / total
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
                                                        delay(10.milliseconds)
                                                    }
                                                    importSuccessMessage =
                                                        replaceSuccessFmt.replace(
                                                            $$"%1$d",
                                                            parsedProducts.size.toString()
                                                        )
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
                        // Success Step
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.check),
                                contentDescription = null,
                                tint = Color(0xFF10B981), // Green
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = stringResource(Res.string.import_success_title),
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
                                Text(stringResource(Res.string.close_button))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun validateImportedBarcodes(products: List<Products>): String? {
    val seenBarcodes = mutableMapOf<String, String>() // normalized barcode -> productName
    for (product in products) {
        val codes = product.parseBarcodes()
        for (code in codes) {
            val norm = normalizeBarcode(code)
            if (norm.isNotEmpty()) {
                if (seenBarcodes.containsKey(norm)) {
                    val otherProductName = seenBarcodes[norm]
                    return "El código de barras '$code' se encuentra duplicado en el archivo importado (en los productos '$otherProductName' y '${product.nombre}')."
                }
                seenBarcodes[norm] = product.nombre
            }
        }
    }
    return null
}
