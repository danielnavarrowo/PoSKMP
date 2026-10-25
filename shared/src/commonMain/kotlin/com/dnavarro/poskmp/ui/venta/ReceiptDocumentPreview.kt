package com.dnavarro.poskmp.ui.venta

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptFontFamily
import com.dnavarro.poskmp.theme.googleFlexBody

@Composable
fun ReceiptDocumentPreview(
    receipt: ReceiptDocument,
    modifier: Modifier = Modifier
) {
    val paperWidth = (receipt.paperWidthMm * 5.2).dp
    val resolvedFontFamily = when (receipt.fontFamily) {
        ReceiptFontFamily.MONOSPACE -> FontFamily.Monospace
        ReceiptFontFamily.MANROPE -> googleFlexBody
    }
    val lineHeightSp = (receipt.fontSize * 1.35f).sp

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
            receipt.lines.forEach { line ->
                val weight = if (line.emphasized) FontWeight.Bold else FontWeight.Normal

                if (line.isSeparator && receipt.fontFamily == ReceiptFontFamily.MANROPE) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((receipt.fontSize * 1.1f).dp)
                    ) {
                        val y = size.height / 2f
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }
                } else if (line.leftText != null && line.rightText != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = line.leftText,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            color = Color.Black,
                            fontFamily = resolvedFontFamily,
                            fontWeight = weight,
                            fontSize = receipt.fontSize.sp,
                            lineHeight = lineHeightSp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = line.rightText,
                            color = Color.Black,
                            fontFamily = resolvedFontFamily,
                            fontWeight = weight,
                            fontSize = receipt.fontSize.sp,
                            lineHeight = lineHeightSp,
                            maxLines = 1,
                            textAlign = TextAlign.End
                        )
                    }
                } else {
                    Text(
                        text = line.text.ifEmpty { " " },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black,
                        fontFamily = resolvedFontFamily,
                        fontWeight = weight,
                        fontSize = receipt.fontSize.sp,
                        lineHeight = lineHeightSp,
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