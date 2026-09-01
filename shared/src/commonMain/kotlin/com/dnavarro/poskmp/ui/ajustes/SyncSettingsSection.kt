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
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.data.sync.SyncStateEnum
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.restore
import poskmp.shared.generated.resources.supabase_auto_sync_subtitle
import poskmp.shared.generated.resources.supabase_auto_sync_title
import poskmp.shared.generated.resources.supabase_connection_failed
import poskmp.shared.generated.resources.supabase_connection_success
import poskmp.shared.generated.resources.supabase_force_full_sync_button
import poskmp.shared.generated.resources.supabase_hide_key
import poskmp.shared.generated.resources.supabase_key_label
import poskmp.shared.generated.resources.supabase_key_placeholder
import poskmp.shared.generated.resources.supabase_last_sync_format
import poskmp.shared.generated.resources.supabase_last_sync_never
import poskmp.shared.generated.resources.supabase_save_and_test_button
import poskmp.shared.generated.resources.supabase_section_subtitle
import poskmp.shared.generated.resources.supabase_section_title
import poskmp.shared.generated.resources.supabase_server_title
import poskmp.shared.generated.resources.supabase_show_key
import poskmp.shared.generated.resources.supabase_status_connected_desc
import poskmp.shared.generated.resources.supabase_status_error_desc
import poskmp.shared.generated.resources.supabase_status_syncing_desc
import poskmp.shared.generated.resources.supabase_status_unconfigured_desc
import poskmp.shared.generated.resources.supabase_sync_failed
import poskmp.shared.generated.resources.supabase_sync_success
import poskmp.shared.generated.resources.supabase_url_label
import poskmp.shared.generated.resources.supabase_url_placeholder
import poskmp.shared.generated.resources.sync
import poskmp.shared.generated.resources.sync_now_button
import poskmp.shared.generated.resources.warning

@Composable
fun SyncSettingsSection(
    supabaseUrl: String,
    supabaseKey: String,
    syncState: SyncStateEnum,
    isTestingConnection: Boolean,
    connectionTestResult: String?,
    autoSyncEnabled: Boolean,
    onAutoSyncEnabledChange: (Boolean) -> Unit,
    onTestAndSaveSupabaseConnection: (String, String) -> Unit,
    onSyncNow: () -> Unit,
    onForceFullSync: () -> Unit,
    lastSyncTimestamp: Long,
    syncMessage: String?,
    modifier: Modifier = Modifier
) {
    var localSupabaseUrl by remember(supabaseUrl) { mutableStateOf(supabaseUrl) }
    var localSupabaseKey by remember(supabaseKey) { mutableStateOf(supabaseKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.supabase_section_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.supabase_section_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Connection Status Banner
                val isConfigured = localSupabaseUrl.isNotBlank() && localSupabaseKey.isNotBlank()
                val statusIcon = when {
                    syncState == SyncStateEnum.SYNCING -> Res.drawable.sync
                    syncState == SyncStateEnum.ERROR -> Res.drawable.warning
                    !isConfigured -> Res.drawable.warning
                    else -> Res.drawable.check
                }
                val statusColor = when {
                    syncState == SyncStateEnum.SYNCING -> MaterialTheme.colorScheme.primary
                    syncState == SyncStateEnum.ERROR -> Color(0xFFEF4444)
                    !isConfigured -> Color(0xFF9CA3AF)
                    else -> Color(0xFF10B981)
                }
                val statusBadgeBg = when {
                    syncState == SyncStateEnum.SYNCING -> MaterialTheme.colorScheme.primaryContainer
                    syncState == SyncStateEnum.ERROR -> Color(0xFFFEE2E2)
                    !isConfigured -> Color(0xFFF3F4F6)
                    else -> Color(0xFFD1FAE5)
                }
                val statusBadgeText = when {
                    syncState == SyncStateEnum.SYNCING -> MaterialTheme.colorScheme.onPrimaryContainer
                    syncState == SyncStateEnum.ERROR -> Color(0xFF991B1B)
                    !isConfigured -> Color(0xFF4B5563)
                    else -> Color(0xFF065F46)
                }
                val statusDesc = when {
                    syncState == SyncStateEnum.SYNCING -> stringResource(Res.string.supabase_status_syncing_desc)
                    syncState == SyncStateEnum.ERROR -> stringResource(Res.string.supabase_status_error_desc)
                    !isConfigured -> stringResource(Res.string.supabase_status_unconfigured_desc)
                    else -> stringResource(Res.string.supabase_status_connected_desc)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(statusIcon),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(Res.string.supabase_server_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = statusDesc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Badge(
                        containerColor = statusBadgeBg,
                        contentColor = statusBadgeText
                    ) {
                        Text(
                            text = when {
                                syncState == SyncStateEnum.SYNCING -> "Sincronizando"
                                syncState == SyncStateEnum.ERROR -> "Error"
                                !isConfigured -> "No configurado"
                                else -> "Conectado"
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // URL TextField
                OutlinedTextField(
                    value = localSupabaseUrl,
                    onValueChange = { localSupabaseUrl = it },
                    label = { Text(stringResource(Res.string.supabase_url_label)) },
                    placeholder = { Text(stringResource(Res.string.supabase_url_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // API Key TextField with show/hide toggle
                OutlinedTextField(
                    value = localSupabaseKey,
                    onValueChange = { localSupabaseKey = it },
                    label = { Text(stringResource(Res.string.supabase_key_label)) },
                    placeholder = { Text(stringResource(Res.string.supabase_key_placeholder)) },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Text(
                                text = stringResource(if (isKeyVisible) Res.string.supabase_hide_key else Res.string.supabase_show_key),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Test and Save Button
                Button(
                    onClick = {
                        onTestAndSaveSupabaseConnection(localSupabaseUrl, localSupabaseKey)
                    },
                    enabled = !isTestingConnection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingConnection) {
                        ContainedLoadingIndicator(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(Res.string.supabase_save_and_test_button),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Connection Result Feedback
                if (connectionTestResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (connectionTestResult == "SUCCESS") {
                        Text(
                            text = stringResource(Res.string.supabase_connection_success),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.supabase_connection_failed, connectionTestResult),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Auto Sync Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.supabase_auto_sync_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.supabase_auto_sync_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = onAutoSyncEnabledChange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sync Now & Full Sync Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSyncNow,
                        enabled = isConfigured && syncState != SyncStateEnum.SYNCING,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (syncState == SyncStateEnum.SYNCING) {
                            ContainedLoadingIndicator(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(Res.string.sync_now_button),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onForceFullSync,
                        enabled = isConfigured && syncState != SyncStateEnum.SYNCING,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.restore),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.supabase_force_full_sync_button),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Last Sync Timestamp
                val lastSyncText = if (lastSyncTimestamp > 0L) {
                    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", LocalLocale.current.platformLocale)
                        .format(java.util.Date(lastSyncTimestamp))
                } else null

                Text(
                    text = if (lastSyncText != null) {
                        stringResource(Res.string.supabase_last_sync_format, lastSyncText)
                    } else {
                        stringResource(Res.string.supabase_last_sync_never)
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Sync Message Feedback
                if (syncMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (syncMessage.startsWith("SYNC_SUCCESS")) {
                        val parts = syncMessage.split(":")
                        val pushed = parts.getOrNull(1) ?: "0"
                        val pulled = parts.getOrNull(2) ?: "0"
                        Text(
                            text = stringResource(Res.string.supabase_sync_success, pushed, pulled),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else if (syncMessage.startsWith("SYNC_ERROR")) {
                        val err = syncMessage.removePrefix("SYNC_ERROR:")
                        Text(
                            text = stringResource(Res.string.supabase_sync_failed, err),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
