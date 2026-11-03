package com.dnavarro.poskmp.data.sync

import androidx.compose.ui.graphics.Color
import com.dnavarro.poskmp.data.DEFAULT_PRODUCT_TABLE_COLUMN_NAMES
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.domain.model.DeviceRole
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.Screen
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TestSettingsRepository(
    initialSupabaseUrl: String = "https://test.supabase.co",
    initialSupabaseKey: String = "test-anon-key",
    initialLastSyncTimestamp: Long = 0L,
    initialAutoSyncEnabled: Boolean = true,
    initialDeviceRole: DeviceRole = DeviceRole.ADMIN
) : SettingsRepository {

    private val _useDynamicColorFlow = MutableStateFlow(false)
    override val useDynamicColorFlow: Flow<Boolean> = _useDynamicColorFlow.asStateFlow()

    private val _seedColorFlow = MutableStateFlow(Color(0xFF6750A4))
    override val seedColorFlow: Flow<Color> = _seedColorFlow.asStateFlow()

    private val _isAmoledFlow = MutableStateFlow(false)
    override val isAmoledFlow: Flow<Boolean> = _isAmoledFlow.asStateFlow()

    private val _darkModeConfigFlow = MutableStateFlow(DarkModeConfig.SYSTEM)
    override val darkModeConfigFlow: Flow<DarkModeConfig> = _darkModeConfigFlow.asStateFlow()

    private val _paletteStyleFlow = MutableStateFlow(PaletteStyle.TonalSpot)
    override val paletteStyleFlow: Flow<PaletteStyle> = _paletteStyleFlow.asStateFlow()

    private val _appScaleFlow = MutableStateFlow(1.0f)
    override val appScaleFlow: Flow<Float> = _appScaleFlow.asStateFlow()

    private val _defaultScreenFlow = MutableStateFlow(Screen.VENTA)
    override val defaultScreenFlow: Flow<Screen> = _defaultScreenFlow.asStateFlow()

    private val _isChecadorDialogFlow = MutableStateFlow(false)
    override val isChecadorDialogFlow: Flow<Boolean> = _isChecadorDialogFlow.asStateFlow()

    private val _showExtraPricesChecadorFlow = MutableStateFlow(false)
    override val showExtraPricesChecadorFlow: Flow<Boolean> = _showExtraPricesChecadorFlow.asStateFlow()

    private val _lowResourceChecadorShaderFlow = MutableStateFlow(false)
    override val lowResourceChecadorShaderFlow: Flow<Boolean> = _lowResourceChecadorShaderFlow.asStateFlow()

    private val _useProductTableInCatalogFlow = MutableStateFlow(false)
    override val useProductTableInCatalogFlow: Flow<Boolean> = _useProductTableInCatalogFlow.asStateFlow()

    private val _swapVentaLayoutOrderFlow = MutableStateFlow(false)
    override val swapVentaLayoutOrderFlow: Flow<Boolean> = _swapVentaLayoutOrderFlow.asStateFlow()

    private val _ventaSplitRatioFlow = MutableStateFlow(0.5f)
    override val ventaSplitRatioFlow: Flow<Float> = _ventaSplitRatioFlow.asStateFlow()

    private val _beepOnBarcodeScanFlow = MutableStateFlow(false)
    override val beepOnBarcodeScanFlow: Flow<Boolean> = _beepOnBarcodeScanFlow.asStateFlow()

    private val _defaultRetailMarginFlow = MutableStateFlow(30.0)
    override val defaultRetailMarginFlow: Flow<Double> = _defaultRetailMarginFlow.asStateFlow()

    private val _defaultWholesaleMarginFlow = MutableStateFlow(15.0)
    override val defaultWholesaleMarginFlow: Flow<Double> = _defaultWholesaleMarginFlow.asStateFlow()

    private val _defaultDeliveryMarginFlow = MutableStateFlow(20.0)
    override val defaultDeliveryMarginFlow: Flow<Double> = _defaultDeliveryMarginFlow.asStateFlow()

    private val _roundRetailPriceFlow = MutableStateFlow(false)
    override val roundRetailPriceFlow: Flow<Boolean> = _roundRetailPriceFlow.asStateFlow()

    private val _roundWholesalePriceFlow = MutableStateFlow(false)
    override val roundWholesalePriceFlow: Flow<Boolean> = _roundWholesalePriceFlow.asStateFlow()

    private val _roundDeliveryPriceFlow = MutableStateFlow(false)
    override val roundDeliveryPriceFlow: Flow<Boolean> = _roundDeliveryPriceFlow.asStateFlow()

    private val _roundTicketTotalFlow = MutableStateFlow(false)
    override val roundTicketTotalFlow: Flow<Boolean> = _roundTicketTotalFlow.asStateFlow()

    private val _disallowCardPaymentOnWholesaleFlow = MutableStateFlow(false)
    override val disallowCardPaymentOnWholesaleFlow: Flow<Boolean> = _disallowCardPaymentOnWholesaleFlow.asStateFlow()

    private val _prioritizeDeliveryPriceFlow = MutableStateFlow(false)
    override val prioritizeDeliveryPriceFlow: Flow<Boolean> = _prioritizeDeliveryPriceFlow.asStateFlow()

    private val _autoWholesaleByQuantityFlow = MutableStateFlow(false)
    override val autoWholesaleByQuantityFlow: Flow<Boolean> = _autoWholesaleByQuantityFlow.asStateFlow()

    private val _autoWholesaleQuantityThresholdFlow = MutableStateFlow(3)
    override val autoWholesaleQuantityThresholdFlow: Flow<Int> = _autoWholesaleQuantityThresholdFlow.asStateFlow()

    private val _autoWholesaleByTicketTotalFlow = MutableStateFlow(false)
    override val autoWholesaleByTicketTotalFlow: Flow<Boolean> = _autoWholesaleByTicketTotalFlow.asStateFlow()

    private val _autoWholesaleTicketTotalThresholdFlow = MutableStateFlow(500.0)
    override val autoWholesaleTicketTotalThresholdFlow: Flow<Double> = _autoWholesaleTicketTotalThresholdFlow.asStateFlow()

    private val _autoLookupBarcodeProductsFlow = MutableStateFlow(false)
    override val autoLookupBarcodeProductsFlow: Flow<Boolean> = _autoLookupBarcodeProductsFlow.asStateFlow()

    private val _geminiGroundingEnabledFlow = MutableStateFlow(false)
    override val geminiGroundingEnabledFlow: Flow<Boolean> = _geminiGroundingEnabledFlow.asStateFlow()

    private val _geminiApiKeyFlow = MutableStateFlow("")
    override val geminiApiKeyFlow: Flow<String> = _geminiApiKeyFlow.asStateFlow()

    private val _googleSearchEngineIdFlow = MutableStateFlow("")
    override val googleSearchEngineIdFlow: Flow<String> = _googleSearchEngineIdFlow.asStateFlow()

    private val _googleSearchApiKeyFlow = MutableStateFlow("")
    override val googleSearchApiKeyFlow: Flow<String> = _googleSearchApiKeyFlow.asStateFlow()

    private val _productTableVisibleColumnsFlow = MutableStateFlow(DEFAULT_PRODUCT_TABLE_COLUMN_NAMES)
    override val productTableVisibleColumnsFlow: Flow<Set<String>> = _productTableVisibleColumnsFlow.asStateFlow()

    private val _supabaseUrlFlow = MutableStateFlow(initialSupabaseUrl)
    override val supabaseUrlFlow: Flow<String> = _supabaseUrlFlow.asStateFlow()

    private val _supabaseKeyFlow = MutableStateFlow(initialSupabaseKey)
    override val supabaseKeyFlow: Flow<String> = _supabaseKeyFlow.asStateFlow()

    private val _lastSyncTimestampFlow = MutableStateFlow(initialLastSyncTimestamp)
    override val lastSyncTimestampFlow: Flow<Long> = _lastSyncTimestampFlow.asStateFlow()

    private val _autoSyncEnabledFlow = MutableStateFlow(initialAutoSyncEnabled)
    override val autoSyncEnabledFlow: Flow<Boolean> = _autoSyncEnabledFlow.asStateFlow()

    private val _autoBackupEnabledFlow = MutableStateFlow(false)
    override val autoBackupEnabledFlow: Flow<Boolean> = _autoBackupEnabledFlow.asStateFlow()

    private val _lastBackupTimestampFlow = MutableStateFlow(0L)
    override val lastBackupTimestampFlow: Flow<Long> = _lastBackupTimestampFlow.asStateFlow()

    private val _backupDirectoryPathFlow = MutableStateFlow("")
    override val backupDirectoryPathFlow: Flow<String> = _backupDirectoryPathFlow.asStateFlow()

    private val _businessSettingsUpdatedAtFlow = MutableStateFlow(0L)
    override val businessSettingsUpdatedAtFlow: Flow<Long> = _businessSettingsUpdatedAtFlow.asStateFlow()

    private val _receiptSettingsFlow = MutableStateFlow(ReceiptSettings())
    override val receiptSettingsFlow: Flow<ReceiptSettings> = _receiptSettingsFlow.asStateFlow()

    private val _deviceRoleFlow = MutableStateFlow(initialDeviceRole)
    override val deviceRoleFlow: Flow<DeviceRole> = _deviceRoleFlow.asStateFlow()

    private val _terminalPrefixFlow = MutableStateFlow("POS1-")
    override val terminalPrefixFlow: Flow<String> = _terminalPrefixFlow.asStateFlow()

    private val _localActiveShiftIdFlow = MutableStateFlow<String?>(null)
    override val localActiveShiftIdFlow: Flow<String?> = _localActiveShiftIdFlow.asStateFlow()

    private val _deviceIdFlow = MutableStateFlow("device-test-uuid")
    override val deviceIdFlow: Flow<String> = _deviceIdFlow.asStateFlow()

    override suspend fun getOrCreateDeviceId(): String = _deviceIdFlow.value

    override suspend fun setLocalActiveShiftId(shiftId: String?) {
        _localActiveShiftIdFlow.value = shiftId
    }

    override suspend fun setDeviceRole(role: DeviceRole) {
        _deviceRoleFlow.value = role
    }

    override suspend fun setTerminalPrefix(prefix: String) {
        _terminalPrefixFlow.value = prefix
    }

    override suspend fun setUseDynamicColor(useDynamic: Boolean) {
        _useDynamicColorFlow.value = useDynamic
    }

    override suspend fun setSeedColor(color: Color) {
        _seedColorFlow.value = color
    }

    override suspend fun setIsAmoled(isAmoled: Boolean) {
        _isAmoledFlow.value = isAmoled
    }

    override suspend fun setDarkModeConfig(config: DarkModeConfig) {
        _darkModeConfigFlow.value = config
    }

    override suspend fun setPaletteStyle(style: PaletteStyle) {
        _paletteStyleFlow.value = style
    }

    override suspend fun setAppScale(scale: Float) {
        _appScaleFlow.value = scale
    }

    override suspend fun setDefaultScreen(screen: Screen) {
        _defaultScreenFlow.value = screen
    }

    override suspend fun setIsChecadorDialog(isDialog: Boolean) {
        _isChecadorDialogFlow.value = isDialog
    }

    override suspend fun setShowExtraPricesChecador(show: Boolean) {
        _showExtraPricesChecadorFlow.value = show
    }

    override suspend fun setLowResourceChecadorShader(enabled: Boolean) {
        _lowResourceChecadorShaderFlow.value = enabled
    }

    override suspend fun setUseProductTableInCatalog(enabled: Boolean) {
        _useProductTableInCatalogFlow.value = enabled
    }

    override suspend fun setSwapVentaLayoutOrder(swap: Boolean) {
        _swapVentaLayoutOrderFlow.value = swap
    }

    override suspend fun setVentaSplitRatio(ratio: Float) {
        _ventaSplitRatioFlow.value = ratio
    }

    override suspend fun setBeepOnBarcodeScan(enabled: Boolean) {
        _beepOnBarcodeScanFlow.value = enabled
    }

    override suspend fun setDefaultRetailMargin(margin: Double) {
        _defaultRetailMarginFlow.value = margin
    }

    override suspend fun setDefaultWholesaleMargin(margin: Double) {
        _defaultWholesaleMarginFlow.value = margin
    }

    override suspend fun setDefaultDeliveryMargin(margin: Double) {
        _defaultDeliveryMarginFlow.value = margin
    }

    override suspend fun setRoundRetailPrice(enabled: Boolean) {
        _roundRetailPriceFlow.value = enabled
    }

    override suspend fun setRoundWholesalePrice(enabled: Boolean) {
        _roundWholesalePriceFlow.value = enabled
    }

    override suspend fun setRoundDeliveryPrice(enabled: Boolean) {
        _roundDeliveryPriceFlow.value = enabled
    }

    override suspend fun setRoundTicketTotal(enabled: Boolean) {
        _roundTicketTotalFlow.value = enabled
    }

    override suspend fun setDisallowCardPaymentOnWholesale(disallow: Boolean) {
        _disallowCardPaymentOnWholesaleFlow.value = disallow
    }

    override suspend fun setPrioritizeDeliveryPrice(prioritize: Boolean) {
        _prioritizeDeliveryPriceFlow.value = prioritize
    }

    override suspend fun setAutoWholesaleByQuantity(enabled: Boolean) {
        _autoWholesaleByQuantityFlow.value = enabled
    }

    override suspend fun setAutoWholesaleQuantityThreshold(threshold: Int) {
        _autoWholesaleQuantityThresholdFlow.value = threshold
    }

    override suspend fun setAutoWholesaleByTicketTotal(enabled: Boolean) {
        _autoWholesaleByTicketTotalFlow.value = enabled
    }

    override suspend fun setAutoWholesaleTicketTotalThreshold(threshold: Double) {
        _autoWholesaleTicketTotalThresholdFlow.value = threshold
    }

    override suspend fun setAutoLookupBarcodeProducts(enabled: Boolean) {
        _autoLookupBarcodeProductsFlow.value = enabled
    }

    override suspend fun setGeminiGroundingEnabled(enabled: Boolean) {
        _geminiGroundingEnabledFlow.value = enabled
    }

    override suspend fun setGeminiApiKey(apiKey: String) {
        _geminiApiKeyFlow.value = apiKey
    }

    override suspend fun setGoogleSearchEngineId(id: String) {
        _googleSearchEngineIdFlow.value = id
    }

    override suspend fun setGoogleSearchApiKey(key: String) {
        _googleSearchApiKeyFlow.value = key
    }

    override suspend fun setProductTableVisibleColumns(columns: Set<String>) {
        _productTableVisibleColumnsFlow.value = columns
    }

    override suspend fun toggleProductTableColumn(columnName: String, defaultColumns: Set<String>) {
        val current = _productTableVisibleColumnsFlow.value.toMutableSet()
        if (current.contains(columnName)) current.remove(columnName) else current.add(columnName)
        _productTableVisibleColumnsFlow.value = current
    }

    override suspend fun setBusinessSettings(
        defaultRetailMargin: Double,
        defaultWholesaleMargin: Double,
        defaultDeliveryMargin: Double,
        roundRetailPrice: Boolean,
        roundWholesalePrice: Boolean,
        roundDeliveryPrice: Boolean,
        roundTicketTotal: Boolean,
        disallowCardPaymentOnWholesale: Boolean,
        prioritizeDeliveryPrice: Boolean,
        autoWholesaleByQuantity: Boolean,
        autoWholesaleQuantityThreshold: Int,
        autoWholesaleByTicketTotal: Boolean,
        autoWholesaleTicketTotalThreshold: Double,
        storeName: String,
        storeAddress: String,
        storePhone: String,
        transferClabe: String,
        transferBeneficiary: String,
        receiptFooter: String,
        updatedAt: Long
    ) {
        _defaultRetailMarginFlow.value = defaultRetailMargin
        _defaultWholesaleMarginFlow.value = defaultWholesaleMargin
        _defaultDeliveryMarginFlow.value = defaultDeliveryMargin
        _roundRetailPriceFlow.value = roundRetailPrice
        _roundWholesalePriceFlow.value = roundWholesalePrice
        _roundDeliveryPriceFlow.value = roundDeliveryPrice
        _roundTicketTotalFlow.value = roundTicketTotal
        _disallowCardPaymentOnWholesaleFlow.value = disallowCardPaymentOnWholesale
        _prioritizeDeliveryPriceFlow.value = prioritizeDeliveryPrice
        _autoWholesaleByQuantityFlow.value = autoWholesaleByQuantity
        _autoWholesaleQuantityThresholdFlow.value = autoWholesaleQuantityThreshold
        _autoWholesaleByTicketTotalFlow.value = autoWholesaleByTicketTotal
        _autoWholesaleTicketTotalThresholdFlow.value = autoWholesaleTicketTotalThreshold
        _receiptSettingsFlow.value = _receiptSettingsFlow.value.copy(
            storeName = storeName,
            storeAddress = storeAddress,
            storePhone = storePhone,
            transferClabe = transferClabe,
            transferBeneficiary = transferBeneficiary,
            footerMessage = receiptFooter
        )
        _businessSettingsUpdatedAtFlow.value = updatedAt
    }

    override suspend fun setSupabaseUrl(url: String) {
        _supabaseUrlFlow.value = url
    }

    override suspend fun setSupabaseKey(key: String) {
        _supabaseKeyFlow.value = key
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        _lastSyncTimestampFlow.value = timestamp
    }

    override suspend fun setAutoSyncEnabled(enabled: Boolean) {
        _autoSyncEnabledFlow.value = enabled
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        _autoBackupEnabledFlow.value = enabled
    }

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        _lastBackupTimestampFlow.value = timestamp
    }

    override suspend fun setBackupDirectoryPath(path: String) {
        _backupDirectoryPathFlow.value = path
    }

    override suspend fun setReceiptSettings(settings: ReceiptSettings) {
        _receiptSettingsFlow.value = settings
    }

    override suspend fun clearAllSettings(preserveSupabaseCredentials: Boolean) {
        val url = _supabaseUrlFlow.value
        val key = _supabaseKeyFlow.value
        _lastSyncTimestampFlow.value = 0L
        if (!preserveSupabaseCredentials) {
            _supabaseUrlFlow.value = ""
            _supabaseKeyFlow.value = ""
        }
    }
}
