package com.dnavarro.poskmp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dnavarro.poskmp.theme.ShapeDefaults
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.calculate
import poskmp.shared.generated.resources.calculator_title
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_button
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds

private enum class CalcOperation(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷")
}

@Composable
fun CalculatorDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showDialog) return

    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var activeOperator by remember { mutableStateOf<CalcOperation?>(null) }
    var isEnteringSecondOperand by remember { mutableStateOf(false) }
    var isResultState by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }


    fun onDigit(digit: String) {
        if (display == "Error" || isEnteringSecondOperand || isResultState) {
            display = digit
            isEnteringSecondOperand = false
            isResultState = false
        } else {
            if (display == "0") {
                display = digit
            } else if (display.length < 16) {
                display += digit
            }
        }
    }

    fun onDot() {
        if (display == "Error" || isEnteringSecondOperand || isResultState) {
            display = "0."
            isEnteringSecondOperand = false
            isResultState = false
        } else if (!display.contains('.')) {
            display += "."
        }
    }

    fun onClear() {
        if (display != "0" && !isResultState) {
            display = "0"
        } else {
            display = "0"
            expression = ""
            firstOperand = null
            activeOperator = null
            isEnteringSecondOperand = false
            isResultState = false
        }
    }

    fun onBackspace() {
        if (display == "Error" || isResultState) {
            display = "0"
            isResultState = false
        } else if (display.length > 1) {
            val shortened = display.dropLast(1)
            display = if (shortened == "-" || shortened.isEmpty()) "0" else shortened
        } else {
            display = "0"
        }
    }

    fun onNegate() {
        if (display == "Error" || display == "0") return
        display = if (display.startsWith("-")) {
            display.removePrefix("-")
        } else {
            "-$display"
        }
    }

    fun calculateResult(a: Double, op: CalcOperation, b: Double): Double? {
        return when (op) {
            CalcOperation.ADD -> a + b
            CalcOperation.SUBTRACT -> a - b
            CalcOperation.MULTIPLY -> a * b
            CalcOperation.DIVIDE -> if (b == 0.0) null else a / b
        }
    }

    fun onOperator(op: CalcOperation) {
        if (display == "Error") return
        val currentVal = display.toDoubleOrNull() ?: return

        if (firstOperand == null) {
            firstOperand = currentVal
            activeOperator = op
            expression = "${formatNumber(currentVal)} ${op.symbol}"
            isEnteringSecondOperand = true
            isResultState = false
        } else if (isEnteringSecondOperand) {
            activeOperator = op
            expression = "${formatNumber(firstOperand!!)} ${op.symbol}"
        } else {
            val prevOp = activeOperator ?: op
            val result = calculateResult(firstOperand!!, prevOp, currentVal)
            if (result == null) {
                display = "Error"
                expression = ""
                firstOperand = null
                activeOperator = null
                isResultState = true
            } else {
                firstOperand = result
                display = formatNumber(result)
                activeOperator = op
                expression = "${formatNumber(result)} ${op.symbol}"
                isEnteringSecondOperand = true
                isResultState = false
            }
        }
    }

    fun onPercent() {
        if (display == "Error") return
        val currentVal = display.toDoubleOrNull() ?: return
        if (firstOperand != null && activeOperator != null) {
            val percentVal = if (activeOperator == CalcOperation.ADD || activeOperator == CalcOperation.SUBTRACT) {
                firstOperand!! * (currentVal / 100.0)
            } else {
                currentVal / 100.0
            }
            display = formatNumber(percentVal)
        } else {
            display = formatNumber(currentVal / 100.0)
        }
        isResultState = false
    }

    fun onEquals() {
        if (display == "Error") return
        if (firstOperand != null && activeOperator != null) {
            val secondVal = display.toDoubleOrNull() ?: return
            val result = calculateResult(firstOperand!!, activeOperator!!, secondVal)
            if (result == null) {
                expression = "${formatNumber(firstOperand!!)} ${activeOperator!!.symbol} ${formatNumber(secondVal)} ="
                display = "Error"
            } else {
                expression = "${formatNumber(firstOperand!!)} ${activeOperator!!.symbol} ${formatNumber(secondVal)} ="
                display = formatNumber(result)
            }
            firstOperand = null
            activeOperator = null
            isEnteringSecondOperand = false
            isResultState = true
        }
    }

    LaunchedEffect(Unit) {
        delay(60.milliseconds)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .width(660.dp)
                .wrapContentHeight()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (keyEvent.key) {
                        Key.F6 -> {
                            onDismiss()
                            true
                        }
                        Key.Zero, Key.NumPad0 -> { onDigit("0"); true }
                        Key.One, Key.NumPad1 -> { onDigit("1"); true }
                        Key.Two, Key.NumPad2 -> { onDigit("2"); true }
                        Key.Three, Key.NumPad3 -> { onDigit("3"); true }
                        Key.Four, Key.NumPad4 -> { onDigit("4"); true }
                        Key.Five, Key.NumPad5 -> {
                            if (keyEvent.isShiftPressed) onPercent() else onDigit("5")
                            true
                        }
                        Key.Six, Key.NumPad6 -> { onDigit("6"); true }
                        Key.Seven, Key.NumPad7 -> {
                            if (keyEvent.isShiftPressed) onOperator(CalcOperation.DIVIDE) else onDigit("7")
                            true
                        }
                        Key.Eight, Key.NumPad8 -> {
                            if (keyEvent.isShiftPressed) onOperator(CalcOperation.MULTIPLY) else onDigit("8")
                            true
                        }
                        Key.Nine, Key.NumPad9 -> { onDigit("9"); true }
                        Key.Period, Key.NumPadDot, Key.Comma -> { onDot(); true }
                        Key.Plus, Key.NumPadAdd -> { onOperator(CalcOperation.ADD); true }
                        Key.Minus, Key.NumPadSubtract -> { onOperator(CalcOperation.SUBTRACT); true }
                        Key.Multiply, Key.NumPadMultiply -> { onOperator(CalcOperation.MULTIPLY); true }
                        Key.Slash, Key.NumPadDivide -> { onOperator(CalcOperation.DIVIDE); true }
                        Key.Enter, Key.NumPadEnter -> { onEquals(); true }
                        Key.Equals -> {
                            if (keyEvent.isShiftPressed) onOperator(CalcOperation.ADD) else onEquals()
                            true
                        }
                        Key.Backspace -> { onBackspace(); true }
                        Key.Delete, Key.C -> { onClear(); true }
                        else -> false
                    }
                },
            shape = ShapeDefaults.cardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialShapes.Slanted.toShape(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(Res.drawable.calculate),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(Res.string.calculator_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Below,
                            4.dp
                        ),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(Res.string.close_button) + " (Esc / F6)")
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.close),
                                contentDescription = stringResource(Res.string.close_button),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Display Area
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = expression.ifEmpty { " " },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val displayFontSize = when {
                            display.length > 13 -> 24.sp
                            display.length > 9 -> 28.sp
                            else -> 36.sp
                        }
                        Text(
                            text = formatDisplay(display),
                            fontSize = displayFontSize,
                            fontWeight = FontWeight.Bold,
                            color = if (display == "Error") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Keypad Rows
                val numberContainer = MaterialTheme.colorScheme.surfaceContainerLowest
                val numberContent = MaterialTheme.colorScheme.onSurface
                val actionContainer = MaterialTheme.colorScheme.surfaceContainerHighest
                val actionContent = MaterialTheme.colorScheme.onSurfaceVariant
                val operatorNormalContainer = MaterialTheme.colorScheme.secondaryContainer
                val operatorNormalContent = MaterialTheme.colorScheme.onSecondaryContainer
                val operatorActiveContainer = MaterialTheme.colorScheme.primary
                val operatorActiveContent = MaterialTheme.colorScheme.onPrimary

                // Row 1: AC/C, ±, %, ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isClearEntry = display != "0" && !isResultState
                    CalcButton(
                        text = if (isClearEntry) "C" else "AC",
                        onClick = { onClear() },
                        modifier = Modifier.weight(1f),
                        containerColor = if (isClearEntry) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f) else actionContainer,
                        contentColor = if (isClearEntry) MaterialTheme.colorScheme.onErrorContainer else actionContent
                    )
                    CalcButton(
                        text = "±",
                        onClick = { onNegate() },
                        modifier = Modifier.weight(1f),
                        containerColor = actionContainer,
                        contentColor = actionContent
                    )
                    CalcButton(
                        text = "%",
                        onClick = { onPercent() },
                        modifier = Modifier.weight(1f),
                        containerColor = actionContainer,
                        contentColor = actionContent
                    )
                    val isDivActive = activeOperator == CalcOperation.DIVIDE && isEnteringSecondOperand
                    CalcButton(
                        text = "÷",
                        onClick = { onOperator(CalcOperation.DIVIDE) },
                        modifier = Modifier.weight(1f),
                        containerColor = if (isDivActive) operatorActiveContainer else operatorNormalContainer,
                        contentColor = if (isDivActive) operatorActiveContent else operatorNormalContent,
                        fontSize = 24.sp
                    )
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcButton(text = "7", onClick = { onDigit("7") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    CalcButton(text = "8", onClick = { onDigit("8") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    CalcButton(text = "9", onClick = { onDigit("9") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    val isMulActive = activeOperator == CalcOperation.MULTIPLY && isEnteringSecondOperand
                    CalcButton(
                        text = "×",
                        onClick = { onOperator(CalcOperation.MULTIPLY) },
                        modifier = Modifier.weight(1f),
                        containerColor = if (isMulActive) operatorActiveContainer else operatorNormalContainer,
                        contentColor = if (isMulActive) operatorActiveContent else operatorNormalContent,
                        fontSize = 24.sp
                    )
                }

                // Row 3: 4, 5, 6, −
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcButton(text = "4", onClick = { onDigit("4") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    CalcButton(text = "5", onClick = { onDigit("5") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    CalcButton(text = "6", onClick = { onDigit("6") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    val isSubActive = activeOperator == CalcOperation.SUBTRACT && isEnteringSecondOperand
                    CalcButton(
                        text = "−",
                        onClick = { onOperator(CalcOperation.SUBTRACT) },
                        modifier = Modifier.weight(1f),
                        containerColor = if (isSubActive) operatorActiveContainer else operatorNormalContainer,
                        contentColor = if (isSubActive) operatorActiveContent else operatorNormalContent,
                        fontSize = 24.sp
                    )
                }

                // Row 4: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcButton(text = "1", onClick = { onDigit("1") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    CalcButton(text = "2", onClick = { onDigit("2") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    CalcButton(text = "3", onClick = { onDigit("3") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    val isAddActive = activeOperator == CalcOperation.ADD && isEnteringSecondOperand
                    CalcButton(
                        text = "+",
                        onClick = { onOperator(CalcOperation.ADD) },
                        modifier = Modifier.weight(1f),
                        containerColor = if (isAddActive) operatorActiveContainer else operatorNormalContainer,
                        contentColor = if (isAddActive) operatorActiveContent else operatorNormalContent,
                        fontSize = 24.sp
                    )
                }

                // Row 5: 0, ., ⌫, =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcButton(text = "0", onClick = { onDigit("0") }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    CalcButton(text = ".", onClick = { onDot() }, modifier = Modifier.weight(1f), containerColor = numberContainer, contentColor = numberContent)
                    CalcButton(
                        text = "⌫",
                        onClick = { onBackspace() },
                        modifier = Modifier.weight(1f),
                        containerColor = actionContainer,
                        contentColor = actionContent,
                        fontSize = 18.sp
                    )
                    CalcButton(
                        text = "=",
                        onClick = { onEquals() },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    shape: CornerBasedShape = RoundedCornerShape(16.dp)
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Error"
    val rounded = (round(value * 100000000.0) / 100000000.0).let { if (it == -0.0) 0.0 else it }
    val s = rounded.toString()
    return if (s.contains('E') || s.contains('e')) {
        s
    } else if (s.endsWith(".0")) {
        s.substringBefore(".0")
    } else if (s.contains('.')) {
        s.trimEnd('0').trimEnd('.')
    } else {
        s
    }
}

private fun formatDisplay(raw: String): String {
    if (raw == "Error" || raw.isEmpty() || raw == "-") return raw
    val isNegative = raw.startsWith("-")
    val clean = if (isNegative) raw.substring(1) else raw
    val parts = clean.split('.')
    val integerPart = parts[0]
    val formattedInt = buildString {
        val len = integerPart.length
        for (i in 0 until len) {
            if (i > 0 && (len - i) % 3 == 0) append(',')
            append(integerPart[i])
        }
    }
    val result = if (parts.size > 1) {
        "$formattedInt.${parts[1]}"
    } else if (clean.endsWith('.')) {
        "$formattedInt."
    } else {
        formattedInt
    }
    return if (isNegative) "-$result" else result
}
