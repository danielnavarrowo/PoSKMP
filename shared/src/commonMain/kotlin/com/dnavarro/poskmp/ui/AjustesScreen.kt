package com.dnavarro.poskmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.ui.ajustes.AjustesViewModel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.util.isAndroid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.amoled_mode_subtitle
import poskmp.shared.generated.resources.amoled_mode_title
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.cloud_sync_section_title
import poskmp.shared.generated.resources.dark_mode_off
import poskmp.shared.generated.resources.dark_mode_on
import poskmp.shared.generated.resources.dark_mode_subtitle
import poskmp.shared.generated.resources.dark_mode_system
import poskmp.shared.generated.resources.dark_mode_title
import poskmp.shared.generated.resources.database_section_title
import poskmp.shared.generated.resources.dynamic_color_subtitle
import poskmp.shared.generated.resources.dynamic_color_title
import poskmp.shared.generated.resources.local_db_connected_desc
import poskmp.shared.generated.resources.local_db_status_connected
import poskmp.shared.generated.resources.local_db_title
import poskmp.shared.generated.resources.seed_color_subtitle
import poskmp.shared.generated.resources.seed_color_title
import poskmp.shared.generated.resources.settings
import poskmp.shared.generated.resources.settings_title
import poskmp.shared.generated.resources.status_connected
import poskmp.shared.generated.resources.status_pending
import poskmp.shared.generated.resources.supabase_offline_desc
import poskmp.shared.generated.resources.supabase_server_title
import poskmp.shared.generated.resources.supabase_status_pending_desc
import poskmp.shared.generated.resources.sync
import poskmp.shared.generated.resources.sync_now_button
import poskmp.shared.generated.resources.system_info_title
import poskmp.shared.generated.resources.system_version
import poskmp.shared.generated.resources.theme_section_title
import poskmp.shared.generated.resources.warning

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape

data class PresetColorItem(
    val color: Color,
    val name: String,
    val shape: Shape
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

@Composable
fun AjustesScreen(
    viewModel: AjustesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AjustesScreen(
        modifier = modifier,
        useDynamicColor = uiState.useDynamicColor,
        onUseDynamicColorChange = { viewModel.setUseDynamicColor(it) },
        seedColor = uiState.seedColor,
        onSeedColorChange = { viewModel.setSeedColor(it) },
        isAmoled = uiState.isAmoled,
        onIsAmoledChange = { viewModel.setIsAmoled(it) },
        darkModeConfig = uiState.darkModeConfig,
        onDarkModeConfigChange = { viewModel.setDarkModeConfig(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    modifier: Modifier = Modifier,
    useDynamicColor: Boolean = isAndroid(),
    onUseDynamicColorChange: (Boolean) -> Unit = {},
    seedColor: Color = Color(0xFF0061A4),
    onSeedColorChange: (Color) -> Unit = {},
    isAmoled: Boolean = false,
    onIsAmoledChange: (Boolean) -> Unit = {},
    darkModeConfig: DarkModeConfig = DarkModeConfig.SYSTEM,
    onDarkModeConfigChange: (DarkModeConfig) -> Unit = {}
) {
    val presetColorItems = rememberPresetSeedColorItems()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.settings_title),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.shapes.small
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.settings),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(Res.string.system_info_title),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(Res.string.system_version),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {

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

                        // Row 1: Dynamic Color Toggle
                        if (isAndroid()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(Res.string.dynamic_color_title),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isAndroid()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Text(
                                                    "Android",
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(
                                                        horizontal = 6.dp,
                                                        vertical = 2.dp
                                                    ),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
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

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val options = listOf(
                                    Triple(
                                        DarkModeConfig.SYSTEM,
                                        stringResource(Res.string.dark_mode_system),
                                        Icons.Outlined.BrightnessAuto
                                    ),
                                    Triple(
                                        DarkModeConfig.LIGHT,
                                        stringResource(Res.string.dark_mode_off),
                                        Icons.Outlined.LightMode
                                    ),
                                    Triple(
                                        DarkModeConfig.DARK,
                                        stringResource(Res.string.dark_mode_on),
                                        Icons.Outlined.DarkMode
                                    )
                                )
                                options.forEachIndexed { index, (config, label, icon) ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = options.size
                                        ),
                                        onClick = { onDarkModeConfigChange(config) },
                                        selected = darkModeConfig == config,
                                        icon = {
                                            SegmentedButtonDefaults.Icon(active = darkModeConfig == config) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                                )
                                            }
                                        }
                                    ) {
                                        Text(text = label, maxLines = 1)
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

                        // Row 2: AMOLED Mode Toggle
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

                        // Seed Color Picker (visible when Dynamic Color is disabled or always for selection)
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
                            }
                        }
                    }
                }


            }
            // Card 2: Apariencia y Tema (Dynamic Color, AMOLED & Seed Color)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(Res.string.database_section_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(Res.drawable.check),
                                    contentDescription = stringResource(Res.string.local_db_connected_desc),
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(Res.string.local_db_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(Res.string.local_db_status_connected),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Badge(
                                containerColor = Color(0xFFD1FAE5),
                                contentColor = Color(0xFF065F46)
                            ) {
                                Text(
                                    stringResource(Res.string.status_connected),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            // Card 3: Database Status


            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(Res.string.cloud_sync_section_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(Res.drawable.warning),
                                    contentDescription = stringResource(Res.string.supabase_offline_desc),
                                    tint = Color(0xFFF59E0B),
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
                                        text = stringResource(Res.string.supabase_status_pending_desc),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Badge(
                                containerColor = Color(0xFFFEF3C7),
                                contentColor = Color(0xFF92400E)
                            ) {
                                Text(
                                    stringResource(Res.string.status_pending),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { /* Force sync triggers here later */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.sync_now_button),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }


        }
    }
}

