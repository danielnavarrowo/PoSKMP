package com.dnavarro.poskmp.ui.turnos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.ExitProgressStep
import com.dnavarro.poskmp.domain.model.CashierShift
import com.dnavarro.poskmp.util.isAndroid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.exit_backup_sync_error
import poskmp.shared.generated.resources.exit_backup_sync_step_backup
import poskmp.shared.generated.resources.exit_backup_sync_step_sync
import poskmp.shared.generated.resources.exit_backup_sync_success
import poskmp.shared.generated.resources.exit_dialog_cancel_button
import poskmp.shared.generated.resources.exit_dialog_close_shift_and_exit_button
import poskmp.shared.generated.resources.exit_dialog_leave_shift_open_and_exit_button
import poskmp.shared.generated.resources.exit_dialog_waiting_backup
import poskmp.shared.generated.resources.exit_with_open_shift_message
import poskmp.shared.generated.resources.exit_with_open_shift_title
import poskmp.shared.generated.resources.warning

@Composable
fun ExitWithOpenShiftDialog(
    activeShift: CashierShift,
    exitStep: ExitProgressStep,
    exitErrorMessage: String?,
    autoBackupEnabled: Boolean,
    isWaitingToExit: Boolean,
    onPerformCutAndExit: () -> Unit,
    onExitLeavingShiftOpen: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = { if (!isWaitingToExit) onCancel() },
        modifier = modifier.width(460.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.exit_with_open_shift_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.exit_with_open_shift_message, activeShift.cashierName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Progreso de Respaldo y Sincronización en Segundo Plano (Desktop)
                if (autoBackupEnabled && !isAndroid() && exitStep != ExitProgressStep.IDLE) {
                    Surface(
                        color = when (exitStep) {
                            ExitProgressStep.SUCCESS -> Color(0xFF10B981).copy(alpha = 0.12f)
                            ExitProgressStep.FAILURE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (exitStep) {
                                ExitProgressStep.BACKING_UP -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(Res.string.exit_backup_sync_step_backup),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                ExitProgressStep.SYNCING_CLOUD -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(Res.string.exit_backup_sync_step_sync),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                ExitProgressStep.SUCCESS -> {
                                    Icon(
                                        painter = painterResource(Res.drawable.check),
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(Res.string.exit_backup_sync_success),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                ExitProgressStep.FAILURE -> {
                                    Icon(
                                        painter = painterResource(Res.drawable.warning),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = exitErrorMessage?.let { stringResource(Res.string.exit_backup_sync_error, it) }
                                            ?: stringResource(Res.string.exit_backup_sync_error, ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                ExitProgressStep.IDLE -> {}
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPerformCutAndExit,
                        enabled = !isWaitingToExit,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.exit_dialog_close_shift_and_exit_button))
                    }

                    FilledTonalButton(
                        onClick = onExitLeavingShiftOpen,
                        enabled = !isWaitingToExit,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isWaitingToExit) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.exit_dialog_waiting_backup))
                        } else {
                            Text(stringResource(Res.string.exit_dialog_leave_shift_open_and_exit_button))
                        }
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isWaitingToExit,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.exit_dialog_cancel_button))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
