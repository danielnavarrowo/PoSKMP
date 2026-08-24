package com.dnavarro.poskmp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.data.backup.BackupRepository
import com.dnavarro.poskmp.data.sync.SyncRepository
import com.dnavarro.poskmp.data.sync.SyncStateEnum
import com.dnavarro.poskmp.di.initKoin
import kotlinx.coroutines.flow.first
import com.dnavarro.poskmp.theme.AppTheme
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.AjustesScreen
import com.dnavarro.poskmp.ui.ChecadorDialog
import com.dnavarro.poskmp.ui.ChecadorScreen
import com.dnavarro.poskmp.ui.ClientesScreen
import com.dnavarro.poskmp.ui.ProductosScreen
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.ui.VentaScreen
import com.dnavarro.poskmp.ui.VentasScreen
import com.dnavarro.poskmp.ui.ajustes.AjustesViewModel
import com.dnavarro.poskmp.ui.clientes.ClientesViewModel
import com.dnavarro.poskmp.ui.productos.ProductosViewModel
import com.dnavarro.poskmp.ui.venta.VentaViewModel
import com.dnavarro.poskmp.ui.ventas.VentasViewModel
import com.dnavarro.poskmp.util.formatCurrentDate
import com.dnavarro.poskmp.util.formatCurrentTime
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import poskmp.shared.generated.resources.*
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

internal fun navigationSuiteTypeForWidth(width: Dp): NavigationSuiteType = when {
    width >= 1200.dp -> NavigationSuiteType.WideNavigationRailExpanded
    width >= 800.dp -> NavigationSuiteType.WideNavigationRailCollapsed
    else -> NavigationSuiteType.None
}

private data class ToolbarItem(
    val label: String,
    val icon: DrawableResource,
    val isSelected: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

private enum class ExitProgressStep {
    IDLE,
    BACKING_UP,
    SYNCING_CLOUD,
    SUCCESS,
    FAILURE
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun App(
    modifier: Modifier = Modifier,
    isExiting: Boolean = false,
    onExitCompleted: () -> Unit = {}
) {
    initKoin()

    val repository = koinInject<ProductRepository>()
    val syncRepository = koinInject<SyncRepository>()
    val backupRepository = koinInject<BackupRepository>()
    val settingsRepository = koinInject<SettingsRepository>()
    val ajustesViewModel = koinViewModel<AjustesViewModel>()

    val autoBackupEnabled by settingsRepository.autoBackupEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    var exitStep by remember { mutableStateOf(ExitProgressStep.IDLE) }
    var exitErrorMessage by remember { mutableStateOf<String?>(null) }

    val syncState by syncRepository.syncState.collectAsStateWithLifecycle()
    val isSyncing = syncState == SyncStateEnum.SYNCING
    val coroutineScope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition()
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            syncRepository.syncAll()
        }
    }

    LaunchedEffect(isExiting) {
        if (isExiting) {
            if (!autoBackupEnabled || isAndroid()) {
                onExitCompleted()
                return@LaunchedEffect
            }

            var hadError = false
            var errorDetails: String? = null

            // Paso 1: Copia de seguridad local
            exitStep = ExitProgressStep.BACKING_UP
            val backupResult = withContext(Dispatchers.IO) {
                try {
                    backupRepository.performBackup()
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            if (backupResult.isFailure) {
                hadError = true
                errorDetails = backupResult.exceptionOrNull()?.message ?: "Error al crear respaldo"
            }

            // Paso 2: Sincronización con la nube
            exitStep = ExitProgressStep.SYNCING_CLOUD
            val syncResult = withContext(Dispatchers.IO) {
                try {
                    syncRepository.syncAll(isManual = true)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            if (syncResult.isFailure && !hadError) {
                hadError = true
                errorDetails = syncResult.exceptionOrNull()?.message ?: "Error al sincronizar con la nube"
            }

            // Paso 3: Resultado final
            if (hadError) {
                exitStep = ExitProgressStep.FAILURE
                exitErrorMessage = errorDetails
            } else {
                exitStep = ExitProgressStep.SUCCESS
            }

            // Mostrar el resultado durante 2 segundos antes de cerrar
            delay(2.seconds)
            onExitCompleted()
        }
    }

    val ajustesUiState by ajustesViewModel.uiState.collectAsStateWithLifecycle()
            val defaultScreen = ajustesUiState.defaultScreen

            // 2. Navigation State
            var selectedScreen by rememberSaveable { mutableStateOf<Screen?>(null) }
            val currentScreen = selectedScreen ?: defaultScreen
            var showPriceCheckerDialog by rememberSaveable { mutableStateOf(false) }

            var currentDateText by remember { mutableStateOf(formatCurrentDate()) }
            var currentTimeText by remember { mutableStateOf(formatCurrentTime()) }
            val currentDateTimeText = remember(currentDateText, currentTimeText) { "$currentDateText\n$currentTimeText" }

            LaunchedEffect(Unit) {
                while (isActive) {
                    val now = LocalDateTime.now()
                    currentDateText = formatCurrentDate(now)
                    currentTimeText = formatCurrentTime(now)
                    delay(1.seconds)
                }
            }

            val isDesktop = !isAndroid()
            val tabVentaLabel =
                stringResource(if (isDesktop) Res.string.tab_venta_desktop else Res.string.tab_venta)
            val tabProductosLabel =
                stringResource(if (isDesktop) Res.string.tab_productos_desktop else Res.string.tab_productos)
            val tabClientesLabel =
                stringResource(if (isDesktop) Res.string.nav_clientes_desktop else Res.string.nav_clientes)
            val tabVentasLabel =
                stringResource(if (isDesktop) Res.string.tab_ventas_historial_desktop else Res.string.tab_ventas_historial)
            val tabChecadorLabel =
                stringResource(if (isDesktop) Res.string.tab_checador_desktop else Res.string.tab_checador)
            val tabAjustesLabel = stringResource(Res.string.tab_ajustes)

            val isChecadorDialog = ajustesUiState.isChecadorDialog

            val toolbarItems = remember(
                currentScreen,
                isDesktop,
                isChecadorDialog,
                tabVentaLabel,
                tabProductosLabel,
                tabClientesLabel,
                tabVentasLabel,
                tabChecadorLabel,
                tabAjustesLabel
            ) {
                listOf(
                    ToolbarItem(
                        label = tabVentaLabel,
                        icon = Res.drawable.point_of_sale,
                        isSelected = currentScreen == Screen.VENTA,
                        onCheckedChange = { if (it) selectedScreen = Screen.VENTA }
                    ),
                    ToolbarItem(
                        label = tabChecadorLabel,
                        icon = Res.drawable.barcode_scanner,
                        isSelected = if (isChecadorDialog) showPriceCheckerDialog else currentScreen == Screen.CHECADOR,
                        onCheckedChange = {
                            if (isChecadorDialog) {
                                showPriceCheckerDialog = true
                            } else if (it) {
                                selectedScreen = Screen.CHECADOR
                            }
                        }
                    ),
                    ToolbarItem(
                        label = tabProductosLabel,
                        icon = Res.drawable.products,
                        isSelected = currentScreen == Screen.PRODUCTOS,
                        onCheckedChange = { if (it) selectedScreen = Screen.PRODUCTOS }
                    ),
                    ToolbarItem(
                        label = tabVentasLabel,
                        icon = Res.drawable.analytics,
                        isSelected = currentScreen == Screen.VENTAS,
                        onCheckedChange = { if (it) selectedScreen = Screen.VENTAS }
                    ),
                    ToolbarItem(
                        label = tabClientesLabel,
                        icon = Res.drawable.person,
                        isSelected = currentScreen == Screen.CLIENTES,
                        onCheckedChange = { if (it) selectedScreen = Screen.CLIENTES }
                    ),
                    ToolbarItem(
                        label = tabAjustesLabel,
                        icon = Res.drawable.settings,
                        isSelected = currentScreen == Screen.AJUSTES,
                        onCheckedChange = { if (it) selectedScreen = Screen.AJUSTES }
                    )
                )
            }

            val useDynamicColor = ajustesUiState.useDynamicColor
            val seedColor = ajustesUiState.seedColor
            val isAmoled = ajustesUiState.isAmoled
            val darkModeConfig = ajustesUiState.darkModeConfig
            val paletteStyle = ajustesUiState.paletteStyle

            val appScale = ajustesUiState.appScale

            val saleRepository = koinInject<SaleRepository>()
            val lastSale by saleRepository.getLastSale().collectAsStateWithLifecycle(initialValue = null)

            val systemInDark = isSystemInDarkTheme()
            val darkTheme = when (darkModeConfig) {
                DarkModeConfig.SYSTEM -> systemInDark
                DarkModeConfig.LIGHT -> false
                DarkModeConfig.DARK -> true
            }

            val currentDensity = LocalDensity.current
            val customDensity = remember(currentDensity, appScale) {
                Density(
                    density = currentDensity.density * appScale,
                    fontScale = currentDensity.fontScale * appScale
                )
            }

            CompositionLocalProvider(LocalDensity provides customDensity) {
                AppTheme(
                    seedColor = seedColor,
                    useDynamicColor = useDynamicColor,
                    isAmoled = isAmoled,
                    paletteStyle = paletteStyle,
                    darkTheme = darkTheme
                ) {
                    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                    val appMaxWidth = maxWidth
                    val isCompact = appMaxWidth < 600.dp
                    val showNavLayout = !(currentScreen == Screen.CHECADOR && !isChecadorDialog)
                    val navigationLayoutType = if (showNavLayout) navigationSuiteTypeForWidth(appMaxWidth) else NavigationSuiteType.None

                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(currentScreen, isChecadorDialog) {
                        if (currentScreen == Screen.CHECADOR && isChecadorDialog) {
                            showPriceCheckerDialog = true
                        }
                        if (!isAndroid() && currentScreen != Screen.VENTA) {
                            try {
                                focusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                        }
                    }

                    NavigationSuiteScaffoldLayout(
                        layoutType = navigationLayoutType,
                        navigationSuite = {
                            if (navigationLayoutType != NavigationSuiteType.None) {
                                val isExpanded = navigationLayoutType == NavigationSuiteType.WideNavigationRailExpanded
                                Surface(
                                    modifier = Modifier
                                        .width(if (isExpanded) 180.dp else 80.dp)
                                        .fillMaxHeight(),
                                    color = NavigationSuiteDefaults.colors().navigationRailContainerColor,
                                    contentColor = NavigationSuiteDefaults.colors().navigationRailContentColor
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            toolbarItems.fastForEach { navItem ->
                                                if (isExpanded) {
                                                    NavigationDrawerItem(
                                                        selected = navItem.isSelected,
                                                        onClick = { navItem.onCheckedChange(true) },
                                                        icon = {
                                                            Icon(
                                                                painter = painterResource(navItem.icon),
                                                                contentDescription = navItem.label
                                                            )
                                                        },
                                                        label = { Text(navItem.label) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                } else {
                                                    NavigationRailItem(
                                                        selected = navItem.isSelected,
                                                        onClick = { navItem.onCheckedChange(true) },
                                                        icon = {
                                                            Icon(
                                                                painter = painterResource(navItem.icon),
                                                                contentDescription = navItem.label
                                                            )
                                                        },
                                                        label = { Text(navItem.label) }
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Sincronización Manual (Above Last Sale)
                                            val lastSyncTimestamp = ajustesUiState.lastSyncTimestamp
                                            if (isExpanded) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        if (!isSyncing) {
                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                syncRepository.syncAll(isManual = true)
                                                            }
                                                        }
                                                    },
                                                    enabled = !isSyncing,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    shape = MaterialTheme.shapes.medium
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.sync),
                                                        contentDescription = stringResource(Res.string.sync_now_button),
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .rotate(if (isSyncing) syncRotation else 0f)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isSyncing) stringResource(Res.string.supabase_status_syncing_desc) else stringResource(Res.string.sync_now_button),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(3.dp))
                                                val lastSyncFormatted = if (lastSyncTimestamp > 0L) {
                                                    formatEpochMillisToDateTime(lastSyncTimestamp)
                                                } else {
                                                    null
                                                }
                                                Text(
                                                    text = if (lastSyncFormatted != null) {
                                                        stringResource(Res.string.supabase_last_sync_format, lastSyncFormatted)
                                                    } else {
                                                        stringResource(Res.string.supabase_last_sync_never)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 2
                                                )
                                            } else {
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        if (!isSyncing) {
                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                syncRepository.syncAll(isManual = true)
                                                            }
                                                        }
                                                    },
                                                    enabled = !isSyncing,
                                                    shape = MaterialTheme.shapes.medium
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.sync),
                                                        contentDescription = stringResource(Res.string.sync_now_button),
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .rotate(if (isSyncing) syncRotation else 0f)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))
                                                val shortSyncText = if (lastSyncTimestamp > 0L) {
                                                    val instant = java.time.Instant.ofEpochMilli(lastSyncTimestamp)
                                                    val time = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                                                        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
                                                    time
                                                } else {
                                                    "--:--"
                                                }
                                                Text(
                                                    text = shortSyncText,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            lastSale?.let { sale ->
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = MaterialTheme.shapes.medium,
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.last_sale_title),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Text(
                                                            text = stringResource(Res.string.last_sale_total, sale.total.toString().formatPrice()),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Text(
                                                            text = stringResource(Res.string.last_sale_paid, sale.pagoCon.toString().formatPrice()),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Text(
                                                            text = stringResource(Res.string.last_sale_change, sale.cambio.toString().formatPrice()),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        val itemsFormatted = if (sale.totalItems % 1.0 == 0.0) sale.totalItems.toLong().toString() else sale.totalItems.toString()
                                                        Text(
                                                            text = stringResource(Res.string.last_sale_items, itemsFormatted),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                            }

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = MaterialTheme.shapes.medium,
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = currentDateTimeText,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Medium,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                NavigationSuite(
                                    layoutType = navigationLayoutType
                                ) {
                                    toolbarItems.fastForEach { navItem ->
                                        item(
                                            selected = navItem.isSelected,
                                            onClick = { navItem.onCheckedChange(true) },
                                            icon = {
                                                Icon(
                                                    painter = painterResource(navItem.icon),
                                                    contentDescription = navItem.label
                                                )
                                            },
                                            label = { Text(navItem.label) }
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .then(
                                    if (!isAndroid()) {
                                        Modifier
                                            .focusRequester(focusRequester)
                                            .focusable()
                                            .onPreviewKeyEvent { keyEvent ->
                                                keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                                                    Key.F1 -> {
                                                        selectedScreen = Screen.VENTA
                                                        true
                                                    }

                                                    Key.F2 -> {
                                                        if (isChecadorDialog) {
                                                            showPriceCheckerDialog = true
                                                        } else {
                                                            selectedScreen = Screen.CHECADOR
                                                        }
                                                        true
                                                    }

                                                    Key.F3 -> {
                                                        selectedScreen = Screen.PRODUCTOS
                                                        true
                                                    }

                                                    Key.F4 -> {
                                                        selectedScreen = Screen.VENTAS
                                                        true
                                                    }

                                                    else -> false
                                                }
                                            }
                                    } else Modifier.statusBarsPadding()
                                ),
                            containerColor = MaterialTheme.colorScheme.background,
                            bottomBar = {
                                if (showNavLayout && navigationLayoutType == NavigationSuiteType.None) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                            .navigationBarsPadding(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        HorizontalFloatingToolbar(
                                            expanded = false,
                                            modifier = Modifier
                                                .zIndex(1f)
                                                .widthIn(max = appMaxWidth - 32.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                var index = 0
                                                toolbarItems.fastForEach { item ->
                                                    TooltipBox(
                                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                                            TooltipAnchorPosition.Above
                                                        ),
                                                        tooltip = { PlainTooltip { Text(item.label) } },
                                                        state = rememberTooltipState(),
                                                    ) {
                                                        ToggleButton(
                                                            checked = item.isSelected,
                                                            onCheckedChange = item.onCheckedChange,
                                                            shapes = ToggleButtonDefaults.shapes(
                                                                CircleShape,
                                                                CircleShape,
                                                                CircleShape
                                                            ),
                                                            modifier = Modifier.height(56.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 8.dp)
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(item.icon),
                                                                    contentDescription = item.label
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (index < toolbarItems.lastIndex) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    index++
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        ) { contentPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding)
                            ) {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220))) togetherWith
                                                fadeOut(animationSpec = tween(180))
                                    },
                                    label = "ScreenTransition"
                                ) { targetScreen ->
                                    when (targetScreen) {
                                        Screen.VENTA -> VentaScreen(
                                            viewModel = koinViewModel<VentaViewModel>(),
                                            isCompact = isCompact
                                        )

                                        Screen.PRODUCTOS -> ProductosScreen(viewModel = koinViewModel<ProductosViewModel>())
                                        Screen.CLIENTES -> ClientesScreen(viewModel = koinViewModel<ClientesViewModel>())
                                        Screen.VENTAS -> VentasScreen(viewModel = koinViewModel<VentasViewModel>())
                                        Screen.AJUSTES -> AjustesScreen(viewModel = ajustesViewModel)
                                        Screen.CHECADOR -> {
                                            if (isChecadorDialog) {
                                                VentaScreen(
                                                    viewModel = koinViewModel<VentaViewModel>(),
                                                    isCompact = isCompact
                                                )
                                            } else {
                                                ChecadorScreen(
                                                    repository = repository,
                                                    showExtraPrices = ajustesUiState.showExtraPricesChecador,
                                                    currentDateText = currentDateText,
                                                    currentTimeText = currentTimeText
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ChecadorDialog(
                        showDialog = showPriceCheckerDialog,
                        onDismiss = { showPriceCheckerDialog = false },
                        repository = repository,
                        showExtraPrices = ajustesUiState.showExtraPricesChecador
                    )

                    if (isExiting && autoBackupEnabled && !isAndroid() && exitStep != ExitProgressStep.IDLE) {
                        val titleText = when (exitStep) {
                            ExitProgressStep.BACKING_UP -> stringResource(Res.string.exit_backup_sync_title_backing_up)
                            ExitProgressStep.SYNCING_CLOUD -> stringResource(Res.string.exit_backup_sync_title_syncing)
                            ExitProgressStep.SUCCESS -> stringResource(Res.string.exit_backup_sync_title_success)
                            ExitProgressStep.FAILURE -> stringResource(Res.string.exit_backup_sync_title_failure)
                            ExitProgressStep.IDLE -> stringResource(Res.string.exit_backup_sync_dialog_title)
                        }

                        AlertDialog(
                            onDismissRequest = {},
                            title = {
                                Text(
                                    text = titleText,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    when (exitStep) {
                                        ExitProgressStep.BACKING_UP -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 3.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = stringResource(Res.string.exit_backup_sync_step_backup),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        ExitProgressStep.SYNCING_CLOUD -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 3.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = stringResource(Res.string.exit_backup_sync_step_sync),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        ExitProgressStep.SUCCESS -> {
                                            Icon(
                                                painter = painterResource(Res.drawable.check),
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = stringResource(Res.string.exit_backup_sync_success),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF065F46)
                                            )
                                        }
                                        ExitProgressStep.FAILURE -> {
                                            Icon(
                                                painter = painterResource(Res.drawable.warning),
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = exitErrorMessage?.let { stringResource(Res.string.exit_backup_sync_error, it) }
                                                    ?: stringResource(Res.string.exit_backup_sync_error, ""),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF991B1B)
                                            )
                                        }
                                        ExitProgressStep.IDLE -> {}
                                    }
                                }
                            },
                            confirmButton = {}
                        )
                    }
                }
            }
        }
    }