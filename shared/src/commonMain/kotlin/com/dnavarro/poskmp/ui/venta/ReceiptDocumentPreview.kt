package com.dnavarro.poskmp.ui.venta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument

@Composable
fun ReceiptDocumentPreview(
    receipt: ReceiptDocument,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 420.dp
) {
    val paperWidth = (receipt.paperWidthMm / 25.4f * 96f).dp
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .width(paperWidth)
                .background(Color.White, MaterialTheme.shapes.small)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            LazyColumn(modifier = Modifier.heightIn(max = maxHeight)) {
                items(receipt.lines) { line ->
                    Text(
                        text = line.text.ifEmpty { " " },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (line.emphasized) FontWeight.Bold else FontWeight.Normal,
                        fontSize = receipt.fontSize.sp,
                        lineHeight = (receipt.fontSize * 1.35f).sp,
                        textAlign = when (line.alignment) {
                            ReceiptAlignment.LEFT -> TextAlign.Start
                            ReceiptAlignment.CENTER -> TextAlign.Center
                            ReceiptAlignment.RIGHT -> TextAlign.End
                        }
                    )
                }
            }
        }
    }
}