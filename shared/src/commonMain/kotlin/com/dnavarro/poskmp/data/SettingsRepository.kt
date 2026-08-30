package com.dnavarro.poskmp.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.domain.model.DEFAULT_PAPER_WIDTH_MM
import com.dnavarro.poskmp.domain.model.MIN_PAPER_WIDTH_MM
import com.dnavarro.poskmp.domain.model.MAX_PAPER_WIDTH_MM
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.util.isAndroid
import com.materialkolor.PaletteStyle
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

/**
 * Interface defining settings data operations.
 */
interface SettingsRepository {
    val useDynamicColorFlow: Flow<Boolean>
    val seedColorFlow: Flow<Color>
    val isAmoledFlow: Flow<Boolean>
    val darkModeConfigFlow: Flow<DarkModeConfig>
    val paletteStyleFlow: Flow<PaletteStyle>
    val appScaleFlow: Flow<Float>
    val defaultScreenFlow: Flow<Screen>
    val isChecadorDialogFlow: Flow<Boolean>
    val showExtraPricesChecadorFlow: Flow<Boolean>
    val useProductTableInCatalogFlow: Flow<Boolean>
    val defaultRetailMarginFlow: Flow<Double>
    val defaultWholesaleMarginFlow: Flow<Double>
    val isRoundingEnabledFlow: Flow<Boolean>
    val roundRetailPriceFlow: Flow<Boolean>
    val roundWholesalePriceFlow: Flow<Boolean>
    val roundTicketTotalFlow: Flow<Boolean>
    val disallowCardPaymentOnWholesaleFlow: Flow<Boolean>
    val supabaseUrlFlow: Flow<String>
    val supabaseKeyFlow: Flow<String>
    val lastSyncTimestampFlow: Flow<Long>
    val autoSyncEnabledFlow: Flow<Boolean>
    val autoBackupEnabledFlow: Flow<Boolean>
    val lastBackupTimestampFlow: Flow<Long>
    val backupDirectoryPathFlow: Flow<String>
    val businessSettingsUpdatedAtFlow: Flow<Long>
    val receiptSettingsFlow: Flow<ReceiptSettings>

    suspend fun setUseDynamicColor(useDynamic: Boolean)
    suspend fun setSeedColor(color: Color)
    suspend fun setIsAmoled(isAmoled: Boolean)
    suspend fun setDarkModeConfig(config: DarkModeConfig)
    suspend fun setPaletteStyle(style: PaletteStyle)
    suspend fun setAppScale(scale: Float)
    suspend fun setDefaultScreen(screen: Screen)
    suspend fun setIsChecadorDialog(isDialog: Boolean)
    suspend fun setShowExtraPricesChecador(show: Boolean)
    suspend fun setUseProductTableInCatalog(enabled: Boolean)
    suspend fun setDefaultRetailMargin(margin: Double)
    suspend fun setDefaultWholesaleMargin(margin: Double)
    suspend fun setIsRoundingEnabled(enabled: Boolean)
    suspend fun setRoundRetailPrice(enabled: Boolean)
    suspend fun setRoundWholesalePrice(enabled: Boolean)
    suspend fun setRoundTicketTotal(enabled: Boolean)
    suspend fun setDisallowCardPaymentOnWholesale(disallow: Boolean)
    suspend fun setBusinessSettings(
        defaultRetailMargin: Double,
        defaultWholesaleMargin: Double,
        isRoundingEnabled: Boolean,
        roundRetailPrice: Boolean,
        roundWholesalePrice: Boolean,
        roundTicketTotal: Boolean,
        disallowCardPaymentOnWholesale: Boolean = false,
        storeName: String = "",
        storeAddress: String = "",
        storePhone: String = "",
        transferClabe: String = "",
        transferBeneficiary: String = "",
        receiptFooter: String = "",
        updatedAt: Long
    )

    suspend fun setSupabaseUrl(url: String)
    suspend fun setSupabaseKey(key: String)
    suspend fun setLastSyncTimestamp(timestamp: Long)
    suspend fun setAutoSyncEnabled(enabled: Boolean)
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setLastBackupTimestamp(timestamp: Long)
    suspend fun setBackupDirectoryPath(path: String)
    suspend fun setReceiptSettings(settings: ReceiptSettings)
}

/**
 * Concrete DataStore implementation of [SettingsRepository].
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences> = getDataStore()
) : SettingsRepository {

    private object PreferenceKeys {
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SEED_COLOR = intPreferencesKey("seed_color_argb")
        val IS_AMOLED = booleanPreferencesKey("is_amoled")
        val DARK_MODE_CONFIG = stringPreferencesKey("dark_mode_config")
        val PALETTE_STYLE = stringPreferencesKey("palette_style")
        val APP_SCALE = floatPreferencesKey("app_scale")
        val DEFAULT_SCREEN = stringPreferencesKey("default_screen")
        val IS_CHECADOR_DIALOG = booleanPreferencesKey("is_checador_dialog")
        val SHOW_EXTRA_PRICES_CHECADOR = booleanPreferencesKey("show_extra_prices_checador")
        val USE_PRODUCT_TABLE_IN_CATALOG = booleanPreferencesKey("use_product_table_in_catalog")
        val DEFAULT_RETAIL_MARGIN = doublePreferencesKey("default_retail_margin_percentage")
        val DEFAULT_WHOLESALE_MARGIN = doublePreferencesKey("default_wholesale_margin_percentage")
        val IS_ROUNDING_ENABLED = booleanPreferencesKey("is_rounding_enabled")
        val ROUND_RETAIL_PRICE = booleanPreferencesKey("round_retail_price")
        val ROUND_WHOLESALE_PRICE = booleanPreferencesKey("round_wholesale_price")
        val ROUND_TICKET_TOTAL = booleanPreferencesKey("round_ticket_total")
        val DISALLOW_CARD_PAYMENT_ON_WHOLESALE = booleanPreferencesKey("disallow_card_payment_on_wholesale")
        val SUPABASE_URL = stringPreferencesKey("supabase_url")
        val SUPABASE_KEY = stringPreferencesKey("supabase_key")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val BACKUP_DIRECTORY_PATH = stringPreferencesKey("backup_directory_path")
        val BUSINESS_SETTINGS_UPDATED_AT = longPreferencesKey("business_settings_updated_at")
        val STORE_NAME = stringPreferencesKey("store_name")
        val STORE_ADDRESS = stringPreferencesKey("store_address")
        val STORE_PHONE = stringPreferencesKey("store_phone")
        val TRANSFER_CLABE = stringPreferencesKey("transfer_clabe")
        val TRANSFER_BENEFICIARY = stringPreferencesKey("transfer_beneficiary")
        val PAPER_WIDTH_MM = intPreferencesKey("paper_width_mm")
        val PRINTER_TYPE = stringPreferencesKey("printer_type")
        val PRINTER_ID = stringPreferencesKey("printer_id")
        val RECEIPT_FONT_SIZE = intPreferencesKey("receipt_font_size")
        val RECEIPT_FEED_LINES = intPreferencesKey("receipt_feed_lines")
        val RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
        val OPEN_CASH_DRAWER_ON_RECEIPT = booleanPreferencesKey("open_cash_drawer_on_receipt")
        val OPEN_CASH_DRAWER_ON_CASH_SALE = booleanPreferencesKey("open_cash_drawer_on_cash_sale")
    }

    override val businessSettingsUpdatedAtFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] ?: 0L
    }

    override val useDynamicColorFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.USE_DYNAMIC_COLOR] ?: isAndroid()
    }

    override val seedColorFlow: Flow<Color> = dataStore.data.map { preferences ->
        val argb = preferences[PreferenceKeys.SEED_COLOR] ?: 0xFF0061A4.toInt()
        Color(argb)
    }

    override val isAmoledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.IS_AMOLED] ?: false
    }

    override val darkModeConfigFlow: Flow<DarkModeConfig> = dataStore.data.map { preferences ->
        val configName = preferences[PreferenceKeys.DARK_MODE_CONFIG] ?: DarkModeConfig.SYSTEM.name
        try {
            DarkModeConfig.valueOf(configName)
        } catch (_: Exception) {
            DarkModeConfig.SYSTEM
        }
    }

    override val paletteStyleFlow: Flow<PaletteStyle> = dataStore.data.map { preferences ->
        val styleName = preferences[PreferenceKeys.PALETTE_STYLE] ?: PaletteStyle.Fidelity.name
        try {
            PaletteStyle.valueOf(styleName)
        } catch (_: Exception) {
            PaletteStyle.Fidelity
        }
    }

    override val appScaleFlow: Flow<Float> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.APP_SCALE] ?: 1.0f
    }

    override val defaultScreenFlow: Flow<Screen> = dataStore.data.map { preferences ->
        val screenName = preferences[PreferenceKeys.DEFAULT_SCREEN] ?: Screen.VENTA.name
        try {
            val screen = Screen.valueOf(screenName)
            if (screen == Screen.AJUSTES) Screen.VENTA else screen
        } catch (_: Exception) {
            Screen.VENTA
        }
    }

    override val isChecadorDialogFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.IS_CHECADOR_DIALOG] ?: true
    }

    override val showExtraPricesChecadorFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.SHOW_EXTRA_PRICES_CHECADOR] ?: false
    }

    override val useProductTableInCatalogFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.USE_PRODUCT_TABLE_IN_CATALOG] ?: false
    }

    override val defaultRetailMarginFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.DEFAULT_RETAIL_MARGIN] ?: 0.0
    }

    override val defaultWholesaleMarginFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.DEFAULT_WHOLESALE_MARGIN] ?: 0.0
    }

    override val isRoundingEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.IS_ROUNDING_ENABLED] ?: false
    }

    override val roundRetailPriceFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.ROUND_RETAIL_PRICE] ?: false
    }

    override val roundWholesalePriceFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.ROUND_WHOLESALE_PRICE] ?: false
    }

    override val roundTicketTotalFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.ROUND_TICKET_TOTAL] ?: false
    }

    override val disallowCardPaymentOnWholesaleFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.DISALLOW_CARD_PAYMENT_ON_WHOLESALE] ?: false
    }

    override val supabaseUrlFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.SUPABASE_URL] ?: ""
    }

    override val supabaseKeyFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.SUPABASE_KEY] ?: ""
    }

    override val lastSyncTimestampFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.LAST_SYNC_TIMESTAMP] ?: 0L
    }

    override val autoSyncEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_SYNC_ENABLED] ?: true
    }

    override val autoBackupEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_BACKUP_ENABLED] ?: true
    }

    override val lastBackupTimestampFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.LAST_BACKUP_TIMESTAMP] ?: 0L
    }

    override val backupDirectoryPathFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.BACKUP_DIRECTORY_PATH] ?: ""
    }

    override val receiptSettingsFlow: Flow<ReceiptSettings> = combine(
        combine(
            dataStore.data.map { it[PreferenceKeys.STORE_NAME] ?: "" },
            dataStore.data.map { it[PreferenceKeys.STORE_ADDRESS] ?: "" },
            dataStore.data.map { it[PreferenceKeys.STORE_PHONE] ?: "" },
            dataStore.data.map { it[PreferenceKeys.TRANSFER_CLABE] ?: "" },
            dataStore.data.map { it[PreferenceKeys.TRANSFER_BENEFICIARY] ?: "" }
        ) { storeName, storeAddress, storePhone, transferClabe, transferBeneficiary ->
            listOf(storeName, storeAddress, storePhone, transferClabe, transferBeneficiary)
        },
        combine(
            dataStore.data.map {
                it[PreferenceKeys.PAPER_WIDTH_MM] ?: when (it[PreferenceKeys.PRINTER_TYPE]) {
                    "A4" -> 105
                    "LETTER" -> 105
                    else -> DEFAULT_PAPER_WIDTH_MM
                }
            },
            dataStore.data.map { it[PreferenceKeys.RECEIPT_FONT_SIZE] ?: 12 },
            dataStore.data.map { it[PreferenceKeys.RECEIPT_FEED_LINES] ?: 3 },
            dataStore.data.map { it[PreferenceKeys.RECEIPT_FOOTER] ?: "" },
            dataStore.data.map {
                it[PreferenceKeys.OPEN_CASH_DRAWER_ON_CASH_SALE]
                    ?: it[PreferenceKeys.OPEN_CASH_DRAWER_ON_RECEIPT]
                    ?: false
            }
        ) { paperWidthMm, fontSize, feedLines, footerMessage, openCashDrawerOnCashSale ->
            ReceiptSettings(
                paperWidthMm = paperWidthMm.coerceIn(MIN_PAPER_WIDTH_MM, MAX_PAPER_WIDTH_MM),
                fontSize = fontSize,
                feedLines = feedLines,
                footerMessage = footerMessage,
                openCashDrawerOnCashSale = openCashDrawerOnCashSale
            )
        },
        dataStore.data.map { it[PreferenceKeys.PRINTER_ID] }
    ) { storeInfoList, printerSettings, printerId ->
        printerSettings.copy(
            storeName = storeInfoList[0],
            storeAddress = storeInfoList[1],
            storePhone = storeInfoList[2],
            transferClabe = storeInfoList[3],
            transferBeneficiary = storeInfoList[4],
            printerId = printerId
        )
    }

    override suspend fun setUseDynamicColor(useDynamic: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.USE_DYNAMIC_COLOR] = useDynamic
        }
    }

    override suspend fun setSeedColor(color: Color) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SEED_COLOR] = color.toArgb()
        }
    }

    override suspend fun setIsAmoled(isAmoled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_AMOLED] = isAmoled
        }
    }

    override suspend fun setDarkModeConfig(config: DarkModeConfig) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DARK_MODE_CONFIG] = config.name
        }
    }

    override suspend fun setPaletteStyle(style: PaletteStyle) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PALETTE_STYLE] = style.name
        }
    }

    override suspend fun setAppScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.APP_SCALE] = scale
        }
    }

    override suspend fun setDefaultScreen(screen: Screen) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_SCREEN] = screen.name
        }
    }

    override suspend fun setIsChecadorDialog(isDialog: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_CHECADOR_DIALOG] = isDialog
        }
    }

    override suspend fun setShowExtraPricesChecador(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_EXTRA_PRICES_CHECADOR] = show
        }
    }

    override suspend fun setUseProductTableInCatalog(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.USE_PRODUCT_TABLE_IN_CATALOG] = enabled
        }
    }

    override suspend fun setDefaultRetailMargin(margin: Double) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_RETAIL_MARGIN] = margin
            preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] = currentTimeMillis()
        }
    }

    override suspend fun setDefaultWholesaleMargin(margin: Double) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_WHOLESALE_MARGIN] = margin
            preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] = currentTimeMillis()
        }
    }

    override suspend fun setIsRoundingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_ROUNDING_ENABLED] = enabled
            preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] = currentTimeMillis()
        }
    }

    override suspend fun setRoundRetailPrice(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ROUND_RETAIL_PRICE] = enabled
            preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] = currentTimeMillis()
        }
    }

    override suspend fun setRoundWholesalePrice(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ROUND_WHOLESALE_PRICE] = enabled
            preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] = currentTimeMillis()
        }
    }

    override suspend fun setRoundTicketTotal(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ROUND_TICKET_TOTAL] = enabled
            preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] = currentTimeMillis()
        }
    }

    override suspend fun setDisallowCardPaymentOnWholesale(disallow: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DISALLOW_CARD_PAYMENT_ON_WHOLESALE] = disallow
            preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] = currentTimeMillis()
        }
    }

    override suspend fun setBusinessSettings(
        defaultRetailMargin: Double,
        defaultWholesaleMargin: Double,
        isRoundingEnabled: Boolean,
        roundRetailPrice: Boolean,
        roundWholesalePrice: Boolean,
        roundTicketTotal: Boolean,
        disallowCardPaymentOnWholesale: Boolean,
        storeName: String,
        storeAddress: String,
        storePhone: String,
        transferClabe: String,
        transferBeneficiary: String,
        receiptFooter: String,
        updatedAt: Long
    ) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_RETAIL_MARGIN] = defaultRetailMargin
            preferences[PreferenceKeys.DEFAULT_WHOLESALE_MARGIN] = defaultWholesaleMargin
            preferences[PreferenceKeys.IS_ROUNDING_ENABLED] = isRoundingEnabled
            preferences[PreferenceKeys.ROUND_RETAIL_PRICE] = roundRetailPrice
            preferences[PreferenceKeys.ROUND_WHOLESALE_PRICE] = roundWholesalePrice
            preferences[PreferenceKeys.ROUND_TICKET_TOTAL] = roundTicketTotal
            preferences[PreferenceKeys.DISALLOW_CARD_PAYMENT_ON_WHOLESALE] = disallowCardPaymentOnWholesale
            preferences[PreferenceKeys.STORE_NAME] = storeName
            preferences[PreferenceKeys.STORE_ADDRESS] = storeAddress
            preferences[PreferenceKeys.STORE_PHONE] = storePhone
            preferences[PreferenceKeys.TRANSFER_CLABE] = transferClabe
            preferences[PreferenceKeys.TRANSFER_BENEFICIARY] = transferBeneficiary
            preferences[PreferenceKeys.RECEIPT_FOOTER] = receiptFooter
            preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] = updatedAt
        }
    }

    override suspend fun setSupabaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SUPABASE_URL] = url.trim()
        }
    }

    override suspend fun setSupabaseKey(key: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SUPABASE_KEY] = key.trim()
        }
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    override suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_SYNC_ENABLED] = enabled
        }
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_BACKUP_ENABLED] = enabled
        }
    }

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    override suspend fun setBackupDirectoryPath(path: String) {
        dataStore.edit { preferences ->
            val clean = path.trim()
            if (clean.isEmpty()) {
                preferences.remove(PreferenceKeys.BACKUP_DIRECTORY_PATH)
            } else {
                preferences[PreferenceKeys.BACKUP_DIRECTORY_PATH] = clean
            }
        }
    }

    override suspend fun setReceiptSettings(settings: ReceiptSettings) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.STORE_NAME] = settings.storeName
            preferences[PreferenceKeys.STORE_ADDRESS] = settings.storeAddress
            preferences[PreferenceKeys.STORE_PHONE] = settings.storePhone
            preferences[PreferenceKeys.TRANSFER_CLABE] = settings.transferClabe
            preferences[PreferenceKeys.TRANSFER_BENEFICIARY] = settings.transferBeneficiary
            preferences[PreferenceKeys.PAPER_WIDTH_MM] = settings.paperWidthMm.coerceIn(MIN_PAPER_WIDTH_MM, MAX_PAPER_WIDTH_MM)
            preferences[PreferenceKeys.PRINTER_TYPE] = settings.printerType.name
            settings.printerId?.trim()?.takeIf { it.isNotEmpty() }?.let {
                preferences[PreferenceKeys.PRINTER_ID] = it
            } ?: preferences.remove(PreferenceKeys.PRINTER_ID)
            preferences[PreferenceKeys.RECEIPT_FONT_SIZE] = settings.fontSize.coerceIn(8, 32)
            preferences[PreferenceKeys.RECEIPT_FEED_LINES] = settings.feedLines.coerceIn(0, 10)
            preferences[PreferenceKeys.RECEIPT_FOOTER] = settings.footerMessage
            preferences[PreferenceKeys.OPEN_CASH_DRAWER_ON_CASH_SALE] = settings.openCashDrawerOnCashSale
            preferences[PreferenceKeys.BUSINESS_SETTINGS_UPDATED_AT] = currentTimeMillis()
        }
    }
}
