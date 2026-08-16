package com.dnavarro.poskmp.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import com.dnavarro.poskmp.data.updater.ReleaseAsset
import com.dnavarro.poskmp.data.updater.UpdateCheckResult
import com.dnavarro.poskmp.data.updater.UpdateDownloadState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.ajustes.AjustesViewModel
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.saveFile
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import poskmp.shared.generated.resources.*
import kotlin.math.roundToInt

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
        onDarkModeConfigChange = { viewModel.setDarkModeConfig(it) },
        appScale = uiState.appScale,
        onAppScaleChange = { viewModel.setAppScale(it) },
        defaultScreen = uiState.defaultScreen,
        onDefaultScreenChange = { viewModel.setDefaultScreen(it) },
        isChecadorDialog = uiState.isChecadorDialog,
        onIsChecadorDialogChange = { viewModel.setIsChecadorDialog(it) },
        showExtraPricesChecador = uiState.showExtraPricesChecador,
        onShowExtraPricesChecadorChange = { viewModel.setShowExtraPricesChecador(it) },
        defaultRetailMargin = uiState.defaultRetailMargin,
        onDefaultRetailMarginChange = { viewModel.setDefaultRetailMargin(it) },
        defaultWholesaleMargin = uiState.defaultWholesaleMargin,
        onDefaultWholesaleMarginChange = { viewModel.setDefaultWholesaleMargin(it) },
        currentVersion = uiState.currentVersion,
        isCheckingUpdates = uiState.isCheckingUpdates,
        updateCheckResult = uiState.updateCheckResult,
        downloadState = uiState.downloadState,
        onCheckForUpdates = { viewModel.checkForUpdates() },
        onDownloadAndInstallUpdate = { viewModel.downloadAndInstallUpdate(it) },
        onDismissUpdateResult = { viewModel.dismissUpdateResult() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    onDarkModeConfigChange: (DarkModeConfig) -> Unit = {},
    appScale: Float = 1.0f,
    onAppScaleChange: (Float) -> Unit = {},
    defaultScreen: Screen = Screen.VENTA,
    onDefaultScreenChange: (Screen) -> Unit = {},
    isChecadorDialog: Boolean = true,
    onIsChecadorDialogChange: (Boolean) -> Unit = {},
    showExtraPricesChecador: Boolean = false,
    onShowExtraPricesChecadorChange: (Boolean) -> Unit = {},
    defaultRetailMargin: Double = 0.0,
    onDefaultRetailMarginChange: (Double) -> Unit = {},
    defaultWholesaleMargin: Double = 0.0,
    onDefaultWholesaleMarginChange: (Double) -> Unit = {},
    currentVersion: String = "0.0.1",
    isCheckingUpdates: Boolean = false,
    updateCheckResult: UpdateCheckResult? = null,
    downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onDownloadAndInstallUpdate: (ReleaseAsset) -> Unit = {},
    onDismissUpdateResult: () -> Unit = {},
    repository: ProductRepository = koinInject()
) {
    val presetColorItems = rememberPresetSeedColorItems()
    var showImportDialog by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var retailMarginText by remember(defaultRetailMargin) {
        mutableStateOf(if (defaultRetailMargin > 0.0) {
            if (defaultRetailMargin % 1.0 == 0.0) defaultRetailMargin.toLong().toString() else defaultRetailMargin.toString()
        } else "")
    }
    var wholesaleMarginText by remember(defaultWholesaleMargin) {
        mutableStateOf(if (defaultWholesaleMargin > 0.0) {
            if (defaultWholesaleMargin % 1.0 == 0.0) defaultWholesaleMargin.toLong().toString() else defaultWholesaleMargin.toString()
        } else "")
    }
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

                        // Row 0: Default Screen (Pantalla Principal al Abrir)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(Res.string.default_screen_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.default_screen_subtitle),
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
                                        Screen.VENTA,
                                        stringResource(Res.string.tab_venta),
                                        Res.drawable.point_of_sale
                                    ),
                                    Triple(
                                        Screen.PRODUCTOS,
                                        stringResource(Res.string.tab_productos),
                                        Res.drawable.products
                                    ),
                                    Triple(
                                        Screen.CHECADOR,
                                        stringResource(Res.string.tab_checador),
                                        Res.drawable.barcode_scanner
                                    )
                                )
                                options.forEachIndexed { index, (screenOption, label, iconRes) ->
                                    val isSelected = defaultScreen == screenOption
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { onDefaultScreenChange(screenOption) },
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
                                                painter = painterResource(iconRes),
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

                        if (!isAndroid()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(Res.string.checador_layout_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(Res.string.checador_layout_subtitle),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val checadorOptions = listOf(
                                        Triple(
                                            true,
                                            stringResource(Res.string.checador_layout_dialog),
                                            Res.drawable.pip
                                        ),
                                        Triple(
                                            false,
                                            stringResource(Res.string.checador_layout_fullscreen),
                                            Res.drawable.fullscreen
                                        )
                                    )
                                    checadorOptions.forEachIndexed { index, (isDialogOption, label, icon) ->
                                        val isSelected = isChecadorDialog == isDialogOption
                                        ToggleButton(
                                            checked = isSelected,
                                            onCheckedChange = { onIsChecadorDialogChange(isDialogOption) },
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
                                                checadorOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
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
                        }

                        // Show Extra Prices in Checador Toggle (Costo y Mayoreo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = stringResource(Res.string.show_extra_prices_checador_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(Res.string.show_extra_prices_checador_subtitle),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = showExtraPricesChecador,
                                onCheckedChange = onShowExtraPricesChecadorChange
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
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

            // Card: Márgenes de Ganancia Predeterminados
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
                            text = stringResource(Res.string.default_margins_section_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.default_margins_section_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = retailMarginText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        retailMarginText = input
                                        onDefaultRetailMarginChange(input.toDoubleOrNull() ?: 0.0)
                                    }
                                },
                                label = { Text(stringResource(Res.string.retail_margin_label)) },
                                suffix = { Text("%", fontWeight = FontWeight.Bold) },
                                placeholder = { Text("0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = wholesaleMarginText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        wholesaleMarginText = input
                                        onDefaultWholesaleMarginChange(input.toDoubleOrNull() ?: 0.0)
                                    }
                                },
                                label = { Text(stringResource(Res.string.wholesale_margin_label)) },
                                suffix = { Text("%", fontWeight = FontWeight.Bold) },
                                placeholder = { Text("0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Card 4: Gestión de Catálogo (Importar y Exportar)
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
                            text = "Gestión del Catálogo",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Importa o exporta el catálogo de productos en CSV / Excel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!isAndroid()) {
                                Button(
                                    onClick = { showImportDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.upload),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.import_button),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val defaultExportSuccess = stringResource(Res.string.export_success_message)
                            Button(
                                onClick = {
                                    scope.launch {
                                        val products = repository.getAllProductsList()
                                        val csvBuilder =
                                            StringBuilder("id,codigos,nombre,precio,costo,categoria,activo,por_peso,precio_mayoreo,es_favorito\n")
                                        for (p in products) {
                                            csvBuilder.append("${p.id},")
                                            csvBuilder.append("\"${p.codigos.replace("\"", "\"\"")}\",")
                                            csvBuilder.append("\"${p.nombre.replace("\"", "\"\"")}\",")
                                            csvBuilder.append("${p.precio},")
                                            csvBuilder.append("${p.costo},")
                                            csvBuilder.append("\"${(p.categoria ?: "").replace("\"", "\"\"")}\",")
                                            csvBuilder.append("${p.activo},")
                                            csvBuilder.append("${p.por_peso},")
                                            csvBuilder.append("${p.precio_mayoreo},")
                                            csvBuilder.append("${p.es_favorito}\n")
                                        }
                                        val csvText = csvBuilder.toString()
                                        saveFile(
                                            defaultFileName = "productos_exportados.csv",
                                            content = csvText,
                                            onSuccess = {
                                                exportSuccessMessage = defaultExportSuccess
                                                exportErrorMessage = null
                                            },
                                            onError = {
                                                exportErrorMessage = it
                                                exportSuccessMessage = null
                                            }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.download),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(Res.string.export_button),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (exportSuccessMessage != null || exportErrorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = exportSuccessMessage ?: exportErrorMessage ?: "",
                                fontSize = 12.sp,
                                color = if (exportSuccessMessage != null) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Actualizaciones del Sistema Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.updates_section_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.updates_section_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(
                                    text = "v$currentVersion",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onCheckForUpdates,
                                enabled = !isCheckingUpdates && downloadState !is UpdateDownloadState.Downloading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.checking_updates),
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(Res.drawable.sync),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.check_updates_button),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (updateCheckResult is UpdateCheckResult.UpToDate) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF10B981).copy(alpha = 0.12f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "✓",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                    Text(
                                        text = stringResource(Res.string.app_up_to_date),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        } else if (updateCheckResult is UpdateCheckResult.Error) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = stringResource(Res.string.update_error, updateCheckResult.message),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showImportDialog) {
            ImportProductsDialog(
                onDismiss = { showImportDialog = false },
                repository = repository
            )
        }

        if (updateCheckResult is UpdateCheckResult.UpdateAvailable) {
            val isDownloading = downloadState is UpdateDownloadState.Downloading
            val isInstalling = downloadState is UpdateDownloadState.Installing

            AlertDialog(
                onDismissRequest = {
                    if (!isDownloading && !isInstalling) {
                        onDismissUpdateResult()
                    }
                },
                title = {
                    Text(
                        text = stringResource(Res.string.update_available_title,
                            updateCheckResult.releaseInfo.tagName),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (updateCheckResult.releaseInfo.releaseNotes.isNotBlank()) {
                            Text(
                                text = stringResource(Res.string.update_notes_title),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .verticalScroll(rememberScrollState())
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = updateCheckResult.releaseInfo.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        when (downloadState) {
                            is UpdateDownloadState.Downloading -> {
                                val progress = downloadState.progress
                                val pct = (progress * 100).toInt()
                                Text(
                                    text = stringResource(Res.string.downloading_update, pct),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            is UpdateDownloadState.Installing -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = stringResource(Res.string.installing_update),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            is UpdateDownloadState.Error -> {
                                Text(
                                    text = stringResource(Res.string.update_error, downloadState.message),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            UpdateDownloadState.Idle -> {
                                if (updateCheckResult.matchingAsset == null) {
                                    Text(
                                        text = stringResource(Res.string.no_compatible_asset),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    val matchingAsset = updateCheckResult.matchingAsset
                    if (matchingAsset != null && downloadState !is UpdateDownloadState.Installing) {
                        Button(
                            onClick = { onDownloadAndInstallUpdate(matchingAsset) },
                            enabled = !isDownloading,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(stringResource(Res.string.download_and_install_button))
                        }
                    }
                },
                dismissButton = {
                    if (!isDownloading && !isInstalling) {
                        TextButton(
                            onClick = onDismissUpdateResult,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(stringResource(Res.string.cancel))
                        }
                    }
                }
            )
        }
    }
}

