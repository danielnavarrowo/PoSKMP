package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.dnavarro.poskmp.theme.ShapeDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.updater.ReleaseAsset
import com.dnavarro.poskmp.data.updater.UpdateCheckResult
import com.dnavarro.poskmp.data.updater.UpdateDownloadState
import com.dnavarro.poskmp.ui.ImportProductsDialog
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.saveFile
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.app_up_to_date
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.check_updates_button
import poskmp.shared.generated.resources.checking_updates
import poskmp.shared.generated.resources.download_and_install_button
import poskmp.shared.generated.resources.downloading_update
import poskmp.shared.generated.resources.download
import poskmp.shared.generated.resources.export_button
import poskmp.shared.generated.resources.export_success_message
import poskmp.shared.generated.resources.import_button
import poskmp.shared.generated.resources.installing_update
import poskmp.shared.generated.resources.no_compatible_asset
import poskmp.shared.generated.resources.settings
import poskmp.shared.generated.resources.sync
import poskmp.shared.generated.resources.system_info_title
import poskmp.shared.generated.resources.system_version
import poskmp.shared.generated.resources.update_available_title
import poskmp.shared.generated.resources.update_error
import poskmp.shared.generated.resources.update_notes_title
import poskmp.shared.generated.resources.updates_section_subtitle
import poskmp.shared.generated.resources.updates_section_title
import poskmp.shared.generated.resources.upload

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutSettingsSection(
    currentVersion: String,
    isCheckingUpdates: Boolean,
    updateCheckResult: UpdateCheckResult?,
    downloadState: UpdateDownloadState,
    onCheckForUpdates: () -> Unit,
    onDownloadAndInstallUpdate: (ReleaseAsset) -> Unit,
    onDismissUpdateResult: () -> Unit,
    repository: ProductRepository,
    isResettingApp: Boolean,
    resetAppError: String?,
    resetAppSuccess: String?,
    onResetApp: () -> Unit,
    onDismissResetAppMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Card: Info del Sistema
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.settings),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.system_info_title),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.system_version, currentVersion),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Card: Gestión del Catálogo (Importar y Exportar CSV)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Gestión del Catálogo",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Importa o exporta el catálogo de productos en CSV / Excel",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isAndroid()) {
                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.upload),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.import_button),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val defaultExportSuccess = stringResource(Res.string.export_success_message)
                    Button(
                        onClick = {
                            scope.launch {
                                val products = repository.getAllProductsList()
                                val csvBuilder =
                                    StringBuilder("id,codigos,nombre,precio,costo,categoria,activo,por_peso,precio_mayoreo,precio_delivery,es_favorito\n")
                                for ((id, codigos, nombre, precio, costo, categoria, activo, por_peso, precio_mayoreo, es_favorito, _, precio_delivery) in products) {
                                    csvBuilder.append("$id,")
                                    csvBuilder.append("\"${codigos.replace("\"", "\"\"")}\",")
                                    csvBuilder.append("\"${nombre.replace("\"", "\"\"")}\",")
                                    csvBuilder.append("$precio,")
                                    csvBuilder.append("$costo,")
                                    csvBuilder.append("\"${(categoria ?: "").replace("\"", "\"\"")}\",")
                                    csvBuilder.append("$activo,")
                                    csvBuilder.append("$por_peso,")
                                    csvBuilder.append("$precio_mayoreo,")
                                    csvBuilder.append("$precio_delivery,")
                                    csvBuilder.append("$es_favorito\n")
                                }
                                val csvText = csvBuilder.toString()
                                saveFile(
                                    defaultFileName = "productos_exportados.csv",
                                    content = csvText,
                                    onSuccess = {
                                        exportSuccessMessage = defaultExportSuccess
                                        exportErrorMessage = null
                                    },
                                    onError = {
                                        exportErrorMessage = it
                                        exportSuccessMessage = null
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.download),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.export_button),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (exportSuccessMessage != null || exportErrorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = exportSuccessMessage ?: exportErrorMessage ?: "",
                        fontSize = 12.sp,
                        color = if (exportSuccessMessage != null) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Card: Actualizaciones del Sistema
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.updates_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.updates_section_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = "v$currentVersion",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdates && downloadState !is UpdateDownloadState.Downloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isCheckingUpdates) {
                            ContainedLoadingIndicator(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.checking_updates),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.check_updates_button),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (updateCheckResult is UpdateCheckResult.UpToDate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "✓",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = stringResource(Res.string.app_up_to_date),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                } else if (updateCheckResult is UpdateCheckResult.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = stringResource(Res.string.update_error, updateCheckResult.message),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Restablecer de Fábrica Card (Zona de Peligro)
        FactoryResetSettingsSection(
            isResettingApp = isResettingApp,
            resetAppError = resetAppError,
            resetAppSuccess = resetAppSuccess,
            onResetApp = onResetApp,
            onDismissMessage = onDismissResetAppMessage
        )
    }

    if (showImportDialog) {
        ImportProductsDialog(
            onDismiss = { showImportDialog = false },
            repository = repository
        )
    }

    if (updateCheckResult is UpdateCheckResult.UpdateAvailable) {
        val isDownloading = downloadState is UpdateDownloadState.Downloading
        val isInstalling = downloadState is UpdateDownloadState.Installing

        AlertDialog(
            onDismissRequest = {
                if (!isDownloading && !isInstalling) {
                    onDismissUpdateResult()
                }
            },
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
            shape = ShapeDefaults.cardShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = {
                Text(
                    text = stringResource(
                        Res.string.update_available_title,
                        updateCheckResult.releaseInfo.tagName
                    ),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (updateCheckResult.releaseInfo.releaseNotes.isNotBlank()) {
                        Text(
                            text = stringResource(Res.string.update_notes_title),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .verticalScroll(rememberScrollState())
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = updateCheckResult.releaseInfo.releaseNotes,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    when (downloadState) {
                        is UpdateDownloadState.Downloading -> {
                            val progress = downloadState.progress
                            val pct = (progress * 100).toInt()
                            Text(
                                text = stringResource(Res.string.downloading_update, pct),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearWavyProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        is UpdateDownloadState.Installing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ContainedLoadingIndicator(modifier = Modifier.size(18.dp))
                                Text(
                                    text = stringResource(Res.string.installing_update),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is UpdateDownloadState.Error -> {
                            Text(
                                text = stringResource(Res.string.update_error, downloadState.message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        UpdateDownloadState.Idle -> {
                            if (updateCheckResult.matchingAsset == null) {
                                Text(
                                    text = stringResource(Res.string.no_compatible_asset),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val matchingAsset = updateCheckResult.matchingAsset
                if (matchingAsset != null && downloadState !is UpdateDownloadState.Installing) {
                    Button(
                        onClick = { onDownloadAndInstallUpdate(matchingAsset) },
                        enabled = !isDownloading,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(stringResource(Res.string.download_and_install_button))
                    }
                }
            },
            dismissButton = {
                if (!isDownloading && !isInstalling) {
                    TextButton(
                        onClick = onDismissUpdateResult,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }
        )
    }
}
