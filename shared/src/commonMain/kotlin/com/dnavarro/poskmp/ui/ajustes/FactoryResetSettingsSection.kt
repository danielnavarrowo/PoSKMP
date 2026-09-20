package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.theme.ShapeDefaults
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.delete
import poskmp.shared.generated.resources.factory_reset_button
import poskmp.shared.generated.resources.factory_reset_confirm_button
import poskmp.shared.generated.resources.factory_reset_desc
import poskmp.shared.generated.resources.factory_reset_dialog_message
import poskmp.shared.generated.resources.factory_reset_dialog_title
import poskmp.shared.generated.resources.factory_reset_error
import poskmp.shared.generated.resources.factory_reset_in_progress
import poskmp.shared.generated.resources.factory_reset_success
import poskmp.shared.generated.resources.factory_reset_title
import poskmp.shared.generated.resources.warning

@Composable
fun FactoryResetSettingsSection(
    isResettingApp: Boolean,
    resetAppError: String?,
    resetAppSuccess: String?,
    onResetApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
            shape = ShapeDefaults.cardShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.factory_reset_title),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.factory_reset_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = !isResettingApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isResettingApp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.factory_reset_in_progress))
                    } else {
                        Icon(
                            painter = painterResource(Res.drawable.delete),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.factory_reset_button),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Feedback message
                if (resetAppSuccess != null || resetAppError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (resetAppSuccess != null) {
                            stringResource(Res.string.factory_reset_success)
                        } else {
                            stringResource(Res.string.factory_reset_error, resetAppError ?: "")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (resetAppSuccess != null) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResettingApp) showConfirmDialog = false
            },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(Res.string.factory_reset_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.factory_reset_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onResetApp()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(Res.string.factory_reset_confirm_button),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            shape = ShapeDefaults.cardShape,
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
        )
    }
}
