package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.pickDirectory
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*

@Composable
fun BackupSettingsSection(
    autoBackupEnabled: Boolean,
    onAutoBackupEnabledChange: (Boolean) -> Unit,
    backupDirectoryPath: String,
    lastBackupTimestamp: Long,
    isBackingUp: Boolean,
    backupMessage: String?,
    onPerformManualBackup: () -> Unit,
    onDismissBackupMessage: () -> Unit,
    onBackupDirectoryPathChange: (String) -> Unit = {},
    onResetBackupDirectoryPath: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEditPathDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(Res.string.backup_section_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.backup_section_subtitle),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Toggle for daily automatic backups
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(Res.string.auto_backup_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.auto_backup_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoBackupEnabled,
                    onCheckedChange = onAutoBackupEnabledChange
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 2. Backup path
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.backup_path_label),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isAndroid()) {
                        OutlinedButton(
                            onClick = { showEditPathDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.edit),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(Res.string.change_backup_path_button),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = backupDirectoryPath.ifEmpty { "—" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 3. Last successful backup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.last_backup_label),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val lastBackupFormatted = if (lastBackupTimestamp > 0L) {
                    formatEpochMillisToDateTime(lastBackupTimestamp)
                } else {
                    stringResource(Res.string.last_backup_never)
                }
                Text(
                    text = lastBackupFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (lastBackupTimestamp > 0L) FontWeight.Medium else FontWeight.Normal
                    ),
                    color = if (lastBackupTimestamp > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Feedback Message Banner if present
            if (backupMessage != null) {
                val isSuccess = backupMessage.startsWith("BACKUP_SUCCESS:")
                val messageContent = if (isSuccess) {
                    val path = backupMessage.removePrefix("BACKUP_SUCCESS:")
                    stringResource(Res.string.backup_success_message, path)
                } else {
                    val error = backupMessage.removePrefix("BACKUP_ERROR:")
                    stringResource(Res.string.backup_error_message, error)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSuccess) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(if (isSuccess) Res.drawable.check else Res.drawable.warning),
                            contentDescription = null,
                            tint = if (isSuccess) Color(0xFF065F46) else Color(0xFF991B1B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = messageContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) Color(0xFF065F46) else Color(0xFF991B1B),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissBackupMessage,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.close),
                                contentDescription = null,
                                tint = if (isSuccess) Color(0xFF065F46) else Color(0xFF991B1B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 5. Button to trigger backup manually
            Button(
                onClick = onPerformManualBackup,
                enabled = !isBackingUp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.backing_up_progress),
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.restore),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.perform_backup_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showEditPathDialog) {
        var tempPath by remember { mutableStateOf(backupDirectoryPath) }
        var pathError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditPathDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.change_backup_path_dialog_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.change_backup_path_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempPath,
                        onValueChange = {
                            tempPath = it
                            pathError = null
                        },
                        label = { Text(stringResource(Res.string.change_backup_path_dialog_label)) },
                        singleLine = true,
                        isError = pathError != null,
                        supportingText = pathError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onResetBackupDirectoryPath()
                                showEditPathDialog = false
                            }
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.undo),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(Res.string.reset_backup_path_button))
                        }

                        FilledTonalButton(
                            onClick = {
                                pickDirectory(
                                    initialPath = tempPath.ifBlank { backupDirectoryPath },
                                    onDirectoryPicked = { selectedPath ->
                                        tempPath = selectedPath
                                        pathError = null
                                    },
                                    onError = { err ->
                                        pathError = err
                                    }
                                )
                            }
                        ) {
                            Text(stringResource(Res.string.browse_folder_button))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = tempPath.trim()
                        if (clean.isNotEmpty()) {
                            onBackupDirectoryPathChange(clean)
                            showEditPathDialog = false
                        } else {
                            onResetBackupDirectoryPath()
                            showEditPathDialog = false
                        }
                    }
                ) {
                    Text(stringResource(Res.string.save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPathDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}
