package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.sync.SyncStateEnum
import com.dnavarro.poskmp.data.updater.ReleaseAsset
import com.dnavarro.poskmp.data.updater.UpdateCheckResult
import com.dnavarro.poskmp.data.updater.UpdateDownloadState
import com.dnavarro.poskmp.domain.model.Cashier
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.ajustes.AboutSettingsSection
import com.dnavarro.poskmp.ui.ajustes.AjustesCategory
import com.dnavarro.poskmp.ui.ajustes.AjustesViewModel
import com.dnavarro.poskmp.ui.ajustes.AppearanceSettingsSection
import com.dnavarro.poskmp.ui.ajustes.BackupSettingsSection
import com.dnavarro.poskmp.ui.ajustes.GeneralSettingsSection
import com.dnavarro.poskmp.ui.ajustes.PricingSettingsSection
import com.dnavarro.poskmp.ui.ajustes.SyncSettingsSection
import com.dnavarro.poskmp.ui.ajustes.TicketSettingsSection
import com.dnavarro.poskmp.ui.turnos.CashierManagementSection
import com.dnavarro.poskmp.util.AdaptiveScaffoldPredictiveBackHandler
import com.dnavarro.poskmp.util.AppConstants
import com.dnavarro.poskmp.util.isAndroid
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.back
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.expand_more
import poskmp.shared.generated.resources.settings_title

@Composable
fun AjustesScreen(
    viewModel: AjustesViewModel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onNavigateToClientes: (() -> Unit)? = null,
    onNavigateToVentas: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AjustesScreen(
        modifier = modifier,
        isCompact = isCompact,
        onNavigateToClientes = onNavigateToClientes,
        onNavigateToVentas = onNavigateToVentas,
        useDynamicColor = uiState.useDynamicColor,
        onUseDynamicColorChange = { viewModel.setUseDynamicColor(it) },
        seedColor = uiState.seedColor,
        onSeedColorChange = { viewModel.setSeedColor(it) },
        isAmoled = uiState.isAmoled,
        onIsAmoledChange = { viewModel.setIsAmoled(it) },
        darkModeConfig = uiState.darkModeConfig,
        onDarkModeConfigChange = { viewModel.setDarkModeConfig(it) },
        paletteStyle = uiState.paletteStyle,
        onPaletteStyleChange = { viewModel.setPaletteStyle(it) },
        appScale = uiState.appScale,
        onAppScaleChange = { viewModel.setAppScale(it) },
        defaultScreen = uiState.defaultScreen,
        onDefaultScreenChange = { viewModel.setDefaultScreen(it) },
        isChecadorDialog = uiState.isChecadorDialog,
        onIsChecadorDialogChange = { viewModel.setIsChecadorDialog(it) },
        showExtraPricesChecador = uiState.showExtraPricesChecador,
        onShowExtraPricesChecadorChange = { viewModel.setShowExtraPricesChecador(it) },
        useProductTableInCatalog = uiState.useProductTableInCatalog,
        onUseProductTableInCatalogChange = { viewModel.setUseProductTableInCatalog(it) },
        swapVentaLayoutOrder = uiState.swapVentaLayoutOrder,
        onSwapVentaLayoutOrderChange = { viewModel.setSwapVentaLayoutOrder(it) },
        defaultRetailMargin = uiState.defaultRetailMargin,
        onDefaultRetailMarginChange = { viewModel.setDefaultRetailMargin(it) },
        defaultWholesaleMargin = uiState.defaultWholesaleMargin,
        onDefaultWholesaleMarginChange = { viewModel.setDefaultWholesaleMargin(it) },
        isRoundingEnabled = uiState.isRoundingEnabled,
        onIsRoundingEnabledChange = { viewModel.setIsRoundingEnabled(it) },
        roundRetailPrice = uiState.roundRetailPrice,
        onRoundRetailPriceChange = { viewModel.setRoundRetailPrice(it) },
        roundWholesalePrice = uiState.roundWholesalePrice,
        onRoundWholesalePriceChange = { viewModel.setRoundWholesalePrice(it) },
        roundTicketTotal = uiState.roundTicketTotal,
        onRoundTicketTotalChange = { viewModel.setRoundTicketTotal(it) },
        disallowCardPaymentOnWholesale = uiState.disallowCardPaymentOnWholesale,
        onDisallowCardPaymentOnWholesaleChange = { viewModel.setDisallowCardPaymentOnWholesale(it) },
        receiptSettings = uiState.receiptSettings,
        onReceiptSettingsChange = { viewModel.setReceiptSettings(it) },
        supabaseUrl = uiState.supabaseUrl,
        supabaseKey = uiState.supabaseKey,
        lastSyncTimestamp = uiState.lastSyncTimestamp,
        autoSyncEnabled = uiState.autoSyncEnabled,
        onAutoSyncEnabledChange = { viewModel.setAutoSyncEnabled(it) },
        autoBackupEnabled = uiState.autoBackupEnabled,
        onAutoBackupEnabledChange = { viewModel.setAutoBackupEnabled(it) },
        backupDirectoryPath = uiState.backupDirectoryPath,
        lastBackupTimestamp = uiState.lastBackupTimestamp,
        isBackingUp = uiState.isBackingUp,
        backupMessage = uiState.backupMessage,
        onPerformManualBackup = { viewModel.performManualBackup() },
        onDismissBackupMessage = { viewModel.dismissBackupMessage() },
        onBackupDirectoryPathChange = { viewModel.setBackupDirectoryPath(it) },
        onResetBackupDirectoryPath = { viewModel.resetBackupDirectoryPathToDefault() },
        syncState = uiState.syncState,
        isTestingConnection = uiState.isTestingConnection,
        connectionTestResult = uiState.connectionTestResult,
        syncMessage = uiState.syncMessage,
        onTestAndSaveSupabaseConnection = { url, key -> viewModel.testAndSaveConnection(url, key) },
        onSyncNow = { viewModel.syncNow() },
        onForceFullSync = { viewModel.syncNow(forceFullSync = true) },
        currentVersion = uiState.currentVersion,
        isCheckingUpdates = uiState.isCheckingUpdates,
        updateCheckResult = uiState.updateCheckResult,
        downloadState = uiState.downloadState,
        onCheckForUpdates = { viewModel.checkForUpdates() },
        onDownloadAndInstallUpdate = { viewModel.downloadAndInstallUpdate(it) },
        onDismissUpdateResult = { viewModel.dismissUpdateResult() },
        cashiers = uiState.cashiers,
        isSavingCashier = uiState.isSavingCashier,
        isDeletingCashier = uiState.isDeletingCashier,
        cashierActionError = uiState.cashierActionError,
        cashierActionSuccess = uiState.cashierActionSuccess,
        onSaveCashier = { id, name, pin -> viewModel.saveCashier(id, name, pin) },
        onDeleteCashier = { id -> viewModel.deleteCashier(id) },
        onClearCashierActionMessage = { viewModel.clearCashierActionMessage() },
        isResettingApp = uiState.isResettingApp,
        resetAppError = uiState.resetAppError,
        resetAppSuccess = uiState.resetAppSuccess,
        onResetApp = { viewModel.resetAppToFactoryDefaults() },
        onDismissResetAppMessage = { viewModel.clearResetAppMessage() }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onNavigateToClientes: (() -> Unit)? = null,
    onNavigateToVentas: (() -> Unit)? = null,
    useDynamicColor: Boolean = isAndroid(),
    onUseDynamicColorChange: (Boolean) -> Unit = {},
    seedColor: Color = Color(0xFF0061A4),
    onSeedColorChange: (Color) -> Unit = {},
    isAmoled: Boolean = false,
    onIsAmoledChange: (Boolean) -> Unit = {},
    darkModeConfig: DarkModeConfig = DarkModeConfig.SYSTEM,
    onDarkModeConfigChange: (DarkModeConfig) -> Unit = {},
    paletteStyle: PaletteStyle = PaletteStyle.Fidelity,
    onPaletteStyleChange: (PaletteStyle) -> Unit = {},
    appScale: Float = 1.0f,
    onAppScaleChange: (Float) -> Unit = {},
    defaultScreen: Screen = Screen.VENTA,
    onDefaultScreenChange: (Screen) -> Unit = {},
    isChecadorDialog: Boolean = true,
    onIsChecadorDialogChange: (Boolean) -> Unit = {},
    showExtraPricesChecador: Boolean = false,
    onShowExtraPricesChecadorChange: (Boolean) -> Unit = {},
    useProductTableInCatalog: Boolean = false,
    onUseProductTableInCatalogChange: (Boolean) -> Unit = {},
    swapVentaLayoutOrder: Boolean = false,
    onSwapVentaLayoutOrderChange: (Boolean) -> Unit = {},
    defaultRetailMargin: Double = 0.0,
    onDefaultRetailMarginChange: (Double) -> Unit = {},
    defaultWholesaleMargin: Double = 0.0,
    onDefaultWholesaleMarginChange: (Double) -> Unit = {},
    isRoundingEnabled: Boolean = false,
    onIsRoundingEnabledChange: (Boolean) -> Unit = {},
    roundRetailPrice: Boolean = false,
    onRoundRetailPriceChange: (Boolean) -> Unit = {},
    roundWholesalePrice: Boolean = false,
    onRoundWholesalePriceChange: (Boolean) -> Unit = {},
    roundTicketTotal: Boolean = false,
    onRoundTicketTotalChange: (Boolean) -> Unit = {},
    disallowCardPaymentOnWholesale: Boolean = false,
    onDisallowCardPaymentOnWholesaleChange: (Boolean) -> Unit = {},
    receiptSettings: ReceiptSettings = ReceiptSettings(),
    onReceiptSettingsChange: (ReceiptSettings) -> Unit = {},
    supabaseUrl: String = "",
    supabaseKey: String = "",
    lastSyncTimestamp: Long = 0L,
    autoSyncEnabled: Boolean = true,
    onAutoSyncEnabledChange: (Boolean) -> Unit = {},
    autoBackupEnabled: Boolean = true,
    onAutoBackupEnabledChange: (Boolean) -> Unit = {},
    backupDirectoryPath: String = "",
    lastBackupTimestamp: Long = 0L,
    isBackingUp: Boolean = false,
    backupMessage: String? = null,
    onPerformManualBackup: () -> Unit = {},
    onDismissBackupMessage: () -> Unit = {},
    onBackupDirectoryPathChange: (String) -> Unit = {},
    onResetBackupDirectoryPath: () -> Unit = {},
    syncState: SyncStateEnum = SyncStateEnum.IDLE,
    isTestingConnection: Boolean = false,
    connectionTestResult: String? = null,
    syncMessage: String? = null,
    onTestAndSaveSupabaseConnection: (url: String, key: String) -> Unit = { _, _ -> },
    onSyncNow: () -> Unit = {},
    onForceFullSync: () -> Unit = {},
    currentVersion: String = AppConstants.APP_VERSION,
    isCheckingUpdates: Boolean = false,
    updateCheckResult: UpdateCheckResult? = null,
    downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onDownloadAndInstallUpdate: (ReleaseAsset) -> Unit = {},
    onDismissUpdateResult: () -> Unit = {},
    cashiers: List<Cashier> = emptyList(),
    isSavingCashier: Boolean = false,
    isDeletingCashier: Boolean = false,
    cashierActionError: String? = null,
    cashierActionSuccess: String? = null,
    onSaveCashier: (id: String?, nombre: String, pin: String) -> Unit = { _, _, _ -> },
    onDeleteCashier: (id: String) -> Unit = {},
    onClearCashierActionMessage: () -> Unit = {},
    isResettingApp: Boolean = false,
    resetAppError: String? = null,
    resetAppSuccess: String? = null,
    onResetApp: () -> Unit = {},
    onDismissResetAppMessage: () -> Unit = {},
    repository: ProductRepository = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val scaffoldDirective = remember(adaptiveInfo) {
        calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(adaptiveInfo).copy(
            maxVerticalPartitions = 1
        )
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<AjustesCategory>(
        scaffoldDirective = scaffoldDirective
    )

    var currentCategory by rememberSaveable { mutableStateOf(AjustesCategory.GENERAL) }

    LaunchedEffect(navigator.currentDestination) {
        val destination = navigator.currentDestination
        val destinationCategory = destination?.contentKey
        if (destination?.pane == ListDetailPaneScaffoldRole.Detail && destinationCategory != null) {
            currentCategory = destinationCategory
        }
    }
    val isListAndDetailVisible =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
                navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

    AdaptiveScaffoldPredictiveBackHandler(navigator = navigator)

    val categories = remember(isCompact) {
        val base = if (isCompact) {
            listOf(AjustesCategory.CLIENTES, AjustesCategory.VENTAS) +
                    AjustesCategory.entries.filter { it != AjustesCategory.CLIENTES && it != AjustesCategory.VENTAS }
        } else {
            AjustesCategory.entries.filter { it != AjustesCategory.CLIENTES && it != AjustesCategory.VENTAS }
        }
        if (isAndroid()) base.filter { it != AjustesCategory.COPIA_SEGURIDAD } else base
    }

    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        listPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(Res.string.settings_title),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = isListAndDetailVisible && currentCategory == category
                            Card(
                                onClick = {
                                    when (category) {
                                        AjustesCategory.CLIENTES -> onNavigateToClientes?.invoke()
                                        AjustesCategory.VENTAS -> onNavigateToVentas?.invoke()
                                        else -> {
                                            currentCategory = category
                                            coroutineScope.launch {
                                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, category)
                                            }
                                        }
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    contentColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                },
                                                shape = MaterialTheme.shapes.small
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(category.icon),
                                            contentDescription = null,
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(category.titleRes),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(category.subtitleRes),
                                            fontSize = 12.sp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!isListAndDetailVisible) {
                                        Icon(
                                            painter = painterResource(Res.drawable.expand_more),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .rotate(-90f),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        if (!isListAndDetailVisible) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(currentCategory.titleRes),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            navigator.navigateBack()
                                        }
                                    }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.back),
                                            contentDescription = stringResource(Res.string.cancel)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = if (isListAndDetailVisible) 24.dp else 16.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        if (isListAndDetailVisible) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
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
                                        painter = painterResource(currentCategory.icon),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = stringResource(currentCategory.titleRes),
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(currentCategory.subtitleRes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        when (currentCategory) {
                            AjustesCategory.CLIENTES,
                            AjustesCategory.VENTAS -> {
                                // Handled directly from list
                            }

                            AjustesCategory.GENERAL -> {
                                GeneralSettingsSection(
                                    defaultScreen = defaultScreen,
                                    onDefaultScreenChange = onDefaultScreenChange,
                                    isChecadorDialog = isChecadorDialog,
                                    onIsChecadorDialogChange = onIsChecadorDialogChange,
                                    showExtraPricesChecador = showExtraPricesChecador,
                                    onShowExtraPricesChecadorChange = onShowExtraPricesChecadorChange,
                                    useProductTableInCatalog = useProductTableInCatalog,
                                    onUseProductTableInCatalogChange = onUseProductTableInCatalogChange,
                                    swapVentaLayoutOrder = swapVentaLayoutOrder,
                                    onSwapVentaLayoutOrderChange = onSwapVentaLayoutOrderChange
                                )
                            }

                            AjustesCategory.APARIENCIA -> {
                                AppearanceSettingsSection(
                                    useDynamicColor = useDynamicColor,
                                    onUseDynamicColorChange = onUseDynamicColorChange,
                                    seedColor = seedColor,
                                    onSeedColorChange = onSeedColorChange,
                                    isAmoled = isAmoled,
                                    onIsAmoledChange = onIsAmoledChange,
                                    darkModeConfig = darkModeConfig,
                                    onDarkModeConfigChange = onDarkModeConfigChange,
                                    paletteStyle = paletteStyle,
                                    onPaletteStyleChange = onPaletteStyleChange,
                                    appScale = appScale,
                                    onAppScaleChange = onAppScaleChange
                                )
                            }

                            AjustesCategory.PRECIOS_MARGENES -> {
                                PricingSettingsSection(
                                    defaultRetailMargin = defaultRetailMargin,
                                    onDefaultRetailMarginChange = onDefaultRetailMarginChange,
                                    defaultWholesaleMargin = defaultWholesaleMargin,
                                    onDefaultWholesaleMarginChange = onDefaultWholesaleMarginChange,
                                    isRoundingEnabled = isRoundingEnabled,
                                    onIsRoundingEnabledChange = onIsRoundingEnabledChange,
                                    roundRetailPrice = roundRetailPrice,
                                    onRoundRetailPriceChange = onRoundRetailPriceChange,
                                    roundWholesalePrice = roundWholesalePrice,
                                    onRoundWholesalePriceChange = onRoundWholesalePriceChange,
                                    roundTicketTotal = roundTicketTotal,
                                    onRoundTicketTotalChange = onRoundTicketTotalChange,
                                    disallowCardPaymentOnWholesale = disallowCardPaymentOnWholesale,
                                    onDisallowCardPaymentOnWholesaleChange = onDisallowCardPaymentOnWholesaleChange
                                )
                            }

                            AjustesCategory.TICKET_IMPRESORA -> {
                                TicketSettingsSection(
                                    settings = receiptSettings,
                                    onSettingsChange = onReceiptSettingsChange
                                )
                            }

                            AjustesCategory.CAJEROS -> {
                                CashierManagementSection(
                                    cashiers = cashiers,
                                    isSaving = isSavingCashier,
                                    isDeleting = isDeletingCashier,
                                    actionError = cashierActionError,
                                    actionSuccess = cashierActionSuccess,
                                    onSaveCashier = onSaveCashier,
                                    onDeleteCashier = onDeleteCashier,
                                    onClearMessage = onClearCashierActionMessage
                                )
                            }

                            AjustesCategory.SINCRONIZACION -> {
                                SyncSettingsSection(
                                    supabaseUrl = supabaseUrl,
                                    supabaseKey = supabaseKey,
                                    syncState = syncState,
                                    isTestingConnection = isTestingConnection,
                                    connectionTestResult = connectionTestResult,
                                    autoSyncEnabled = autoSyncEnabled,
                                    onAutoSyncEnabledChange = onAutoSyncEnabledChange,
                                    onTestAndSaveSupabaseConnection = onTestAndSaveSupabaseConnection,
                                    onSyncNow = onSyncNow,
                                    onForceFullSync = onForceFullSync,
                                    lastSyncTimestamp = lastSyncTimestamp,
                                    syncMessage = syncMessage
                                )
                            }

                            AjustesCategory.COPIA_SEGURIDAD -> {
                                BackupSettingsSection(
                                    autoBackupEnabled = autoBackupEnabled,
                                    onAutoBackupEnabledChange = onAutoBackupEnabledChange,
                                    backupDirectoryPath = backupDirectoryPath,
                                    lastBackupTimestamp = lastBackupTimestamp,
                                    isBackingUp = isBackingUp,
                                    backupMessage = backupMessage,
                                    onPerformManualBackup = onPerformManualBackup,
                                    onDismissBackupMessage = onDismissBackupMessage,
                                    onBackupDirectoryPathChange = onBackupDirectoryPathChange,
                                    onResetBackupDirectoryPath = onResetBackupDirectoryPath
                                )
                            }

                            AjustesCategory.SISTEMA_ACERCA_DE -> {
                                AboutSettingsSection(
                                    currentVersion = currentVersion,
                                    isCheckingUpdates = isCheckingUpdates,
                                    updateCheckResult = updateCheckResult,
                                    downloadState = downloadState,
                                    onCheckForUpdates = onCheckForUpdates,
                                    onDownloadAndInstallUpdate = onDownloadAndInstallUpdate,
                                    onDismissUpdateResult = onDismissUpdateResult,
                                    repository = repository,
                                    isResettingApp = isResettingApp,
                                    resetAppError = resetAppError,
                                    resetAppSuccess = resetAppSuccess,
                                    onResetApp = onResetApp,
                                    onDismissResetAppMessage = onDismissResetAppMessage
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
