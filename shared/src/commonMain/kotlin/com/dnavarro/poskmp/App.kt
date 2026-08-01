package com.dnavarro.poskmp

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.db.createDatabase
import com.dnavarro.poskmp.theme.AppTheme
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.AjustesScreen
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.ui.ChecadorDialog
import com.dnavarro.poskmp.ui.ProductosScreen
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.ui.VentaScreen
import com.dnavarro.poskmp.ui.ajustes.AjustesViewModel
import com.dnavarro.poskmp.ui.productos.ProductosViewModel
import com.dnavarro.poskmp.ui.venta.VentaViewModel
import com.dnavarro.poskmp.util.isAndroid
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.point_of_sale
import poskmp.shared.generated.resources.products
import poskmp.shared.generated.resources.settings
import poskmp.shared.generated.resources.tab_ajustes
import poskmp.shared.generated.resources.tab_checador
import poskmp.shared.generated.resources.tab_checador_desktop
import poskmp.shared.generated.resources.tab_productos
import poskmp.shared.generated.resources.tab_productos_desktop
import poskmp.shared.generated.resources.tab_venta
import poskmp.shared.generated.resources.tab_venta_desktop

internal fun navigationSuiteTypeForWidth(width: Dp): NavigationSuiteType = when {
    width >= 1200.dp -> NavigationSuiteType.WideNavigationRailExpanded
    width >= 800.dp -> NavigationSuiteType.WideNavigationRailCollapsed
    else -> NavigationSuiteType.None
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun App() {
    // 1. Initialize Database & Repository
    val repository = remember {
        val factory = DatabaseDriverFactory()
        val db = createDatabase(factory)
        ProductRepository(db)
    }

    val settingsRepository = remember { SettingsRepository() }

    val ventaViewModel = remember(repository) { VentaViewModel(repository) }
    val productosViewModel = remember(repository) { ProductosViewModel(repository) }
    val ajustesViewModel = remember(settingsRepository) { AjustesViewModel(settingsRepository) }

    LaunchedEffect(repository) {
        repository.insertDummyDataIfEmpty()
    }

    // 2. Navigation State
    var currentScreen by remember { mutableStateOf(Screen.VENTA) }
    val cartItems = remember { mutableStateListOf<CartItem>() }
    var showPriceCheckerDialog by remember { mutableStateOf(false) }

    val isDesktop = !isAndroid()
    val tabVentaLabel = stringResource(if (isDesktop) Res.string.tab_venta_desktop else Res.string.tab_venta)
    val tabProductosLabel = stringResource(if (isDesktop) Res.string.tab_productos_desktop else Res.string.tab_productos)
    val tabChecadorLabel = stringResource(if (isDesktop) Res.string.tab_checador_desktop else Res.string.tab_checador)
    val tabAjustesLabel = stringResource(Res.string.tab_ajustes)

    val toolbarItems = remember(currentScreen, isDesktop, tabVentaLabel, tabProductosLabel, tabChecadorLabel, tabAjustesLabel) {
        listOf(
            ToolbarItem(
                label = tabVentaLabel,
                icon = Res.drawable.point_of_sale,
                isSelected = currentScreen == Screen.VENTA,
                onCheckedChange = { if (it) currentScreen = Screen.VENTA }
            ),
            ToolbarItem(
                label = tabProductosLabel,
                icon = Res.drawable.products,
                isSelected = currentScreen == Screen.PRODUCTOS,
                onCheckedChange = { if (it) currentScreen = Screen.PRODUCTOS }
            ),
            ToolbarItem(
                label = tabChecadorLabel,
                icon = Res.drawable.barcode_scanner,
                isSelected = false,
                onCheckedChange = { showPriceCheckerDialog = true }
            ),
            ToolbarItem(
                label = tabAjustesLabel,
                icon = Res.drawable.settings,
                isSelected = currentScreen == Screen.AJUSTES,
                onCheckedChange = { if (it) currentScreen = Screen.AJUSTES }
            )
        )
    }



    val ajustesUiState by ajustesViewModel.uiState.collectAsState()
    val useDynamicColor = ajustesUiState.useDynamicColor
    val seedColor = ajustesUiState.seedColor
    val isAmoled = ajustesUiState.isAmoled
    val darkModeConfig = ajustesUiState.darkModeConfig

    val systemInDark = isSystemInDarkTheme()
    val darkTheme = when (darkModeConfig) {
        DarkModeConfig.SYSTEM -> systemInDark
        DarkModeConfig.LIGHT -> false
        DarkModeConfig.DARK -> true
    }

    AppTheme(
        seedColor = seedColor,
        useDynamicColor = useDynamicColor,
        isAmoled = isAmoled,
        darkTheme = darkTheme
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 600.dp
            val navigationLayoutType = navigationSuiteTypeForWidth(maxWidth)

            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(currentScreen) {
                if (!isAndroid() && currentScreen != Screen.VENTA) {
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }

            NavigationSuiteScaffold(
                layoutType = navigationLayoutType,
                navigationSuiteItems = {
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
                                                currentScreen = Screen.VENTA
                                                true
                                            }

                                            Key.F2 -> {
                                                showPriceCheckerDialog = true
                                                true
                                            }

                                            Key.F3 -> {
                                                currentScreen = Screen.PRODUCTOS
                                                true
                                            }

                                            else -> false
                                        }
                                    }
                            } else Modifier.statusBarsPadding()
                        ),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (navigationLayoutType == NavigationSuiteType.None) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .navigationBarsPadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                HorizontalFloatingToolbar(
                                    expanded = false,
                                    modifier = Modifier.zIndex(1f)
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
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    ) {
                        when (currentScreen) {
                            Screen.VENTA -> VentaScreen(
                                viewModel = ventaViewModel,
                                isCompact = isCompact,
                                cartItems = cartItems
                            )
                            Screen.PRODUCTOS -> ProductosScreen(viewModel = productosViewModel)
                            Screen.AJUSTES -> AjustesScreen(viewModel = ajustesViewModel)
                        }
                    }
                }
            }

            ChecadorDialog(
                showDialog = showPriceCheckerDialog,
                onDismiss = { showPriceCheckerDialog = false },
                repository = repository
            )
        }
    }
}

private data class ToolbarItem(
    val label: String,
    val icon: DrawableResource,
    val isSelected: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)