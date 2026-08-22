package com.dnavarro.poskmp.ui.venta

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.print_receipt_button
import poskmp.shared.generated.resources.receipt_print_error
import poskmp.shared.generated.resources.receipt_printed
import poskmp.shared.generated.resources.receipt_preview_title

@Composable
fun ReceiptPreviewDialog(
    receipt: ReceiptDocument,
    isPrinting: Boolean,
    printSuccessful: Boolean,
    printError: Boolean,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.receipt_preview_title)) },
        text = {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                ReceiptDocumentPreview(receipt = receipt)
                if (isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
                } else if (printSuccessful) {
                    Text(stringResource(Res.string.receipt_printed), modifier = Modifier.padding(top = 12.dp))
                } else if (printError) {
                    Text(stringResource(Res.string.receipt_print_error), modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onPrint, enabled = !isPrinting) {
                Text(stringResource(Res.string.print_receipt_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close_button))
            }
        }
    )
}
