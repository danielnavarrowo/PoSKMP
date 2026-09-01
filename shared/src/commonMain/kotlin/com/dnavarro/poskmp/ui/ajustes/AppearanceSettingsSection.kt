package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.util.isAndroid
import com.materialkolor.PaletteStyle
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.accept_button
import poskmp.shared.generated.resources.add
import poskmp.shared.generated.resources.amoled_mode_subtitle
import poskmp.shared.generated.resources.amoled_mode_title
import poskmp.shared.generated.resources.app_scale_subtitle
import poskmp.shared.generated.resources.app_scale_title
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.dark_mode
import poskmp.shared.generated.resources.dark_mode_off
import poskmp.shared.generated.resources.dark_mode_on
import poskmp.shared.generated.resources.dark_mode_subtitle
import poskmp.shared.generated.resources.dark_mode_system
import poskmp.shared.generated.resources.dark_mode_title
import poskmp.shared.generated.resources.dynamic_color_subtitle
import poskmp.shared.generated.resources.dynamic_color_title
import poskmp.shared.generated.resources.info
import poskmp.shared.generated.resources.light_mode
import poskmp.shared.generated.resources.palette_style_content
import poskmp.shared.generated.resources.palette_style_content_desc
import poskmp.shared.generated.resources.palette_style_expressive
import poskmp.shared.generated.resources.palette_style_expressive_desc
import poskmp.shared.generated.resources.palette_style_fidelity
import poskmp.shared.generated.resources.palette_style_fidelity_desc
import poskmp.shared.generated.resources.palette_style_fruit_salad
import poskmp.shared.generated.resources.palette_style_fruit_salad_desc
import poskmp.shared.generated.resources.palette_style_monochrome
import poskmp.shared.generated.resources.palette_style_monochrome_desc
import poskmp.shared.generated.resources.palette_style_neutral
import poskmp.shared.generated.resources.palette_style_neutral_desc
import poskmp.shared.generated.resources.palette_style_rainbow
import poskmp.shared.generated.resources.palette_style_rainbow_desc
import poskmp.shared.generated.resources.palette_style_subtitle
import poskmp.shared.generated.resources.palette_style_title
import poskmp.shared.generated.resources.palette_style_tonal_spot
import poskmp.shared.generated.resources.palette_style_tonal_spot_desc
import poskmp.shared.generated.resources.palette_style_vibrant
import poskmp.shared.generated.resources.palette_style_vibrant_desc
import poskmp.shared.generated.resources.remove
import poskmp.shared.generated.resources.reset_scale_button
import poskmp.shared.generated.resources.seed_color_subtitle
import poskmp.shared.generated.resources.seed_color_title
import poskmp.shared.generated.resources.star
import poskmp.shared.generated.resources.theme_section_title

data class PresetColorItem(
    val color: Color,
    val name: String,
    val shape: Shape
)

@Composable
fun rememberPresetSeedColorItems(): List<PresetColorItem> {
    val sunny = MaterialShapes.Sunny.toShape()
    val verySunny = MaterialShapes.VerySunny.toShape()
    val clover4 = MaterialShapes.Clover4Leaf.toShape()
    val cookie6 = MaterialShapes.Cookie6Sided.toShape()
    val cookie7 = MaterialShapes.Cookie7Sided.toShape()
    val burst = MaterialShapes.Burst.toShape()
    val pixelCircle = MaterialShapes.PixelCircle.toShape()
    val cookie9 = MaterialShapes.Cookie9Sided.toShape()
    val puffyDiamond = MaterialShapes.PuffyDiamond.toShape()
    val clover8 = MaterialShapes.Clover8Leaf.toShape()
    val oval = MaterialShapes.Oval.toShape()
    val cookie4 = MaterialShapes.Cookie4Sided.toShape()
    val flower = MaterialShapes.Flower.toShape()
    val softBurst = MaterialShapes.SoftBurst.toShape()

    return remember(
        sunny, verySunny, clover4, cookie6, cookie7, burst,
        pixelCircle, cookie9, puffyDiamond,
        clover8, oval, cookie4, flower, softBurst
    ) {
        listOf(
            PresetColorItem(Color(0xFFEB4CFF), "Azul Clásico", sunny),
            PresetColorItem(Color(0xFF2FC991), "Verde Esmeralda", verySunny),
            PresetColorItem(Color(0xFF6750A4), "Púrpura Material", clover4),
            PresetColorItem(Color(0xFFEC7E00), "Naranja Sol", cookie7),
            PresetColorItem(Color(0xFFF7FF00), "Verde Bosque", cookie6),
            PresetColorItem(Color(0xFFFF88B4), "Rosa Neón", pixelCircle),
            PresetColorItem(Color(0xFF00ACC1), "Cian Eléctrico", puffyDiamond),
            PresetColorItem(Color(0xFFFA8072), "Índigo Profundo", softBurst),
            PresetColorItem(Color(0xFF664F49), "Mocha", flower)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceSettingsSection(
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    seedColor: Color,
    onSeedColorChange: (Color) -> Unit,
    isAmoled: Boolean,
    onIsAmoledChange: (Boolean) -> Unit,
    darkModeConfig: DarkModeConfig,
    onDarkModeConfigChange: (DarkModeConfig) -> Unit,
    paletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    appScale: Float,
    onAppScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetColorItems = rememberPresetSeedColorItems()
    var styleForDescriptionDialog by remember { mutableStateOf<PaletteStyle?>(null) }

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
                    text = stringResource(Res.string.theme_section_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Row 1: Dynamic Color Toggle (Android Only)
                if (isAndroid()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(Res.string.dynamic_color_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.dynamic_color_subtitle),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = useDynamicColor,
                            onCheckedChange = onUseDynamicColorChange,
                            enabled = isAndroid()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Row 2: Dark Mode 3-step Switch (Follow System, Off, On)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.dark_mode_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.dark_mode_subtitle),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val options = listOf(
                            Triple(
                                DarkModeConfig.SYSTEM,
                                stringResource(Res.string.dark_mode_system),
                                Res.drawable.star
                            ),
                            Triple(
                                DarkModeConfig.LIGHT,
                                stringResource(Res.string.dark_mode_off),
                                Res.drawable.light_mode
                            ),
                            Triple(
                                DarkModeConfig.DARK,
                                stringResource(Res.string.dark_mode_on),
                                Res.drawable.dark_mode
                            )
                        )
                        options.forEachIndexed { index, (config, label, icon) ->
                            val isSelected = darkModeConfig == config
                            ToggleButton(
                                checked = isSelected,
                                onCheckedChange = { onDarkModeConfigChange(config) },
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // App Scale Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(Res.string.app_scale_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.app_scale_subtitle),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "${(appScale * 100).roundToInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val newScale = (appScale - 0.05f).coerceAtLeast(0.75f)
                                onAppScaleChange((newScale * 100).roundToInt() / 100f)
                            },
                            enabled = appScale > 0.75f
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.remove), contentDescription = null
                            )
                        }

                        Slider(
                            value = appScale,
                            onValueChange = { onAppScaleChange((it * 100).roundToInt() / 100f) },
                            valueRange = 0.75f..1.35f,
                            steps = 11,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                val newScale = (appScale + 0.05f).coerceAtMost(1.35f)
                                onAppScaleChange((newScale * 100).roundToInt() / 100f)
                            },
                            enabled = appScale < 1.35f
                        ) {
                            Icon(
                                painterResource(Res.drawable.add),
                                contentDescription = "Aumentar escala"
                            )
                        }
                    }

                    if ((appScale * 100).roundToInt() != 100) {
                        TextButton(
                            onClick = { onAppScaleChange(1.0f) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = stringResource(Res.string.reset_scale_button),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // AMOLED Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.amoled_mode_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.amoled_mode_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isAmoled,
                        onCheckedChange = onIsAmoledChange
                    )
                }

                // Seed Color Picker (visible when Dynamic Color is disabled)
                AnimatedVisibility(
                    visible = !useDynamicColor,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 20.dp)) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(Res.string.seed_color_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.seed_color_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(presetColorItems) { preset ->
                                val color = preset.color
                                val shape = preset.shape
                                val isSelected = !useDynamicColor && seedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(shape)
                                        .background(color)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 3.dp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    shape = shape
                                                )
                                            } else Modifier
                                        )
                                        .clickable {
                                            if (useDynamicColor) {
                                                onUseDynamicColorChange(false)
                                            }
                                            onSeedColorChange(color)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(Res.drawable.check),
                                            contentDescription = preset.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Palette Style Dropdown
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(Res.string.palette_style_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.palette_style_subtitle),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            var expandedPaletteStyle by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expandedPaletteStyle,
                                onExpandedChange = { expandedPaletteStyle = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = stringResource(paletteStyle.titleRes()),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaletteStyle)
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedPaletteStyle,
                                    onDismissRequest = { expandedPaletteStyle = false }
                                ) {
                                    PaletteStyle.entries.forEach { style ->
                                        val isSelected = style == paletteStyle
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(style.titleRes()),
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            leadingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.check),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else null,
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = { styleForDescriptionDialog = style },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.info),
                                                        contentDescription = stringResource(style.titleRes()),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                onPaletteStyleChange(style)
                                                expandedPaletteStyle = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pointerInput(style) {
                                                    detectTapGestures(
                                                        onTap = {
                                                            onPaletteStyleChange(style)
                                                            expandedPaletteStyle = false
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Palette Style Description Dialog
    styleForDescriptionDialog?.let { style ->
        AlertDialog(
            onDismissRequest = { styleForDescriptionDialog = null },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(style.titleRes()),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(style.descriptionRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { styleForDescriptionDialog = null },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(Res.string.accept_button))
                }
            }
        )
    }
}

fun PaletteStyle.titleRes(): StringResource = when (this) {
    PaletteStyle.TonalSpot -> Res.string.palette_style_tonal_spot
    PaletteStyle.Neutral -> Res.string.palette_style_neutral
    PaletteStyle.Vibrant -> Res.string.palette_style_vibrant
    PaletteStyle.Expressive -> Res.string.palette_style_expressive
    PaletteStyle.Rainbow -> Res.string.palette_style_rainbow
    PaletteStyle.FruitSalad -> Res.string.palette_style_fruit_salad
    PaletteStyle.Monochrome -> Res.string.palette_style_monochrome
    PaletteStyle.Fidelity -> Res.string.palette_style_fidelity
    PaletteStyle.Content -> Res.string.palette_style_content
}

fun PaletteStyle.descriptionRes(): StringResource = when (this) {
    PaletteStyle.TonalSpot -> Res.string.palette_style_tonal_spot_desc
    PaletteStyle.Neutral -> Res.string.palette_style_neutral_desc
    PaletteStyle.Vibrant -> Res.string.palette_style_vibrant_desc
    PaletteStyle.Expressive -> Res.string.palette_style_expressive_desc
    PaletteStyle.Rainbow -> Res.string.palette_style_rainbow_desc
    PaletteStyle.FruitSalad -> Res.string.palette_style_fruit_salad_desc
    PaletteStyle.Monochrome -> Res.string.palette_style_monochrome_desc
    PaletteStyle.Fidelity -> Res.string.palette_style_fidelity_desc
    PaletteStyle.Content -> Res.string.palette_style_content_desc
}
