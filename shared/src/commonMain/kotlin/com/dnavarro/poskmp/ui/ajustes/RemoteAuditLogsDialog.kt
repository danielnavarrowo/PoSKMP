package com.dnavarro.poskmp.ui.ajustes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dnavarro.poskmp.data.source.remote.dto.RemoteAuditLogDto
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.restore
import poskmp.shared.generated.resources.sad_face
import poskmp.shared.generated.resources.supabase_remote_logs_empty
import poskmp.shared.generated.resources.supabase_remote_logs_empty_hint
import poskmp.shared.generated.resources.supabase_remote_logs_filter_all
import poskmp.shared.generated.resources.supabase_remote_logs_filter_cashiers
import poskmp.shared.generated.resources.supabase_remote_logs_filter_customers
import poskmp.shared.generated.resources.supabase_remote_logs_filter_products
import poskmp.shared.generated.resources.supabase_remote_logs_filter_sales
import poskmp.shared.generated.resources.supabase_remote_logs_filter_settings
import poskmp.shared.generated.resources.supabase_remote_logs_operation_delete
import poskmp.shared.generated.resources.supabase_remote_logs_operation_insert
import poskmp.shared.generated.resources.supabase_remote_logs_operation_update
import poskmp.shared.generated.resources.supabase_remote_logs_retry
import poskmp.shared.generated.resources.supabase_remote_logs_subtitle
import poskmp.shared.generated.resources.supabase_remote_logs_title
import poskmp.shared.generated.resources.sync
import poskmp.shared.generated.resources.warning

@Composable
fun RemoteAuditLogsDialog(
    logs: List<RemoteAuditLogDto>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "sales" -> logs.filter { it.tableName == "sales" || it.tableName == "sale_items" }
            "products" -> logs.filter { it.tableName == "products" }
            "customers" -> logs.filter { it.tableName == "customers" || it.tableName == "customer_payments" }
            "cashiers" -> logs.filter { it.tableName == "cashiers" }
            "store_settings" -> logs.filter { it.tableName == "store_settings" }
            else -> logs
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(Res.drawable.restore),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = stringResource(Res.string.supabase_remote_logs_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(Res.string.supabase_remote_logs_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefresh,
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.sync),
                                    contentDescription = "Refrescar bitácora",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                painter = painterResource(Res.drawable.close),
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text(stringResource(Res.string.supabase_remote_logs_filter_all)) },
                            shape = FilterChipDefaults.shape
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "sales",
                            onClick = { selectedFilter = "sales" },
                            label = { Text(stringResource(Res.string.supabase_remote_logs_filter_sales)) },
                            shape = FilterChipDefaults.shape
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "products",
                            onClick = { selectedFilter = "products" },
                            label = { Text(stringResource(Res.string.supabase_remote_logs_filter_products)) },
                            shape = FilterChipDefaults.shape
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "customers",
                            onClick = { selectedFilter = "customers" },
                            label = { Text(stringResource(Res.string.supabase_remote_logs_filter_customers)) },
                            shape = FilterChipDefaults.shape
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "cashiers",
                            onClick = { selectedFilter = "cashiers" },
                            label = { Text(stringResource(Res.string.supabase_remote_logs_filter_cashiers)) },
                            shape = FilterChipDefaults.shape
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "store_settings",
                            onClick = { selectedFilter = "store_settings" },
                            label = { Text(stringResource(Res.string.supabase_remote_logs_filter_settings)) },
                            shape = FilterChipDefaults.shape
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoading && logs.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Consultando bitácora en Supabase...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (errorMessage != null && logs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.warning),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = onRefresh) {
                                Text(stringResource(Res.string.supabase_remote_logs_retry))
                            }
                        }
                    } else if (filteredLogs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.sad_face),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(Res.string.supabase_remote_logs_empty),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(Res.string.supabase_remote_logs_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(filteredLogs, key = { it.id }) { log ->
                                RemoteAuditLogItem(log = log)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mostrando ${filteredLogs.size} registro(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteAuditLogItem(
    log: RemoteAuditLogDto,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeFg, badgeTextRes) = when (log.operation.uppercase()) {
        "INSERT" -> Triple(
            Color(0xFF10B981).copy(alpha = 0.16f),
            Color(0xFF047857),
            Res.string.supabase_remote_logs_operation_insert
        )
        "UPDATE" -> Triple(
            Color(0xFF3B82F6).copy(alpha = 0.16f),
            Color(0xFF1D4ED8),
            Res.string.supabase_remote_logs_operation_update
        )
        "DELETE" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Res.string.supabase_remote_logs_operation_delete
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            null
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Operation Badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = badgeBg
                    ) {
                        Text(
                            text = badgeTextRes?.let { stringResource(it) } ?: log.operation,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Table badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = log.tableName,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Formatted timestamp
                Text(
                    text = formatIsoTimestamp(log.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Summary description
            Text(
                text = log.summary ?: "${log.operation} en ${log.tableName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ID
            if (log.recordId.isNotBlank()) {
                Text(
                    text = "ID: ${log.recordId}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatIsoTimestamp(isoString: String): String {
    if (isoString.isBlank()) return ""
    return try {
        val parts = isoString.split("T")
        if (parts.size >= 2) {
            val date = parts[0]
            val time = parts[1].take(8)
            "$date $time"
        } else {
            isoString
        }
    } catch (_: Exception) {
        isoString
    }
}
