package com.dnavarro.poskmp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.dnavarro.poskmp.di.appModule
import com.dnavarro.poskmp.theme.AppTheme
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.AjustesScreen
import com.dnavarro.poskmp.ui.ChecadorDialog
import com.dnavarro.poskmp.ui.ChecadorScreen
import com.dnavarro.poskmp.ui.ProductosScreen
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.ui.VentaScreen
import com.dnavarro.poskmp.ui.VentasScreen
import com.dnavarro.poskmp.ui.ajustes.AjustesViewModel
import com.dnavarro.poskmp.ui.productos.ProductosViewModel
import com.dnavarro.poskmp.ui.venta.VentaViewModel
import com.dnavarro.poskmp.ui.ventas.VentasViewModel
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.analytics
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
import poskmp.shared.generated.resources.tab_ventas_historial
import poskmp.shared.generated.resources.tab_ventas_historial_desktop
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

internal fun navigationSuiteTypeForWidth(width: Dp): NavigationSuiteType = when {
    width >= 1200.dp -> NavigationSuiteType.WideNavigationRailExpanded
    width >= 800.dp -> NavigationSuiteType.WideNavigationRailCollapsed
    else -> NavigationSuiteType.None
}

private fun formatCurrentDateTime(dateTime: LocalDateTime = LocalDateTime.now()): String {
    val locale = Locale.forLanguageTag("es-MX")
    val dayOfWeek = dateTime.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    val dayOfMonth = dateTime.dayOfMonth
    val month = dateTime.month.getDisplayName(TextStyle.FULL, locale)

    val hour12 = dateTime.format(DateTimeFormatter.ofPattern("h:mm", locale))
    val amPm = dateTime.format(DateTimeFormatter.ofPattern("a", locale))

    return "$dayOfWeek, $dayOfMonth de $month\n$hour12 $amPm"
}

private data class ToolbarItem(
    val label: String,
    val icon: DrawableResource,
    val isSelected: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun App(
    modifier: Modifier = Modifier
) {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(appModule) }),
        content = {
            val repository = koinInject<ProductRepository>()
            val ventaViewModel = koinViewModel<VentaViewModel>()
            val productosViewModel = koinViewModel<ProductosViewModel>()
            val ajustesViewModel = koinViewModel<AjustesViewModel>()
            val ventasViewModel = koinViewModel<VentasViewModel>()

            LaunchedEffect(repository) {
                repository.insertDummyDataIfEmpty()
            }

            val ajustesUiState by ajustesViewModel.uiState.collectAsStateWithLifecycle()
            val defaultScreen = ajustesUiState.defaultScreen

            // 2. Navigation State
            var selectedScreen by rememberSaveable { mutableStateOf<Screen?>(null) }
            val currentScreen = selectedScreen ?: defaultScreen
            var showPriceCheckerDialog by rememberSaveable { mutableStateOf(false) }

            var currentDateTimeText by remember { mutableStateOf(formatCurrentDateTime()) }
            LaunchedEffect(Unit) {
                while (isActive) {
                    currentDateTimeText = formatCurrentDateTime()
                    delay(1.seconds)
                }
            }

            val isDesktop = !isAndroid()
            val tabVentaLabel =
                stringResource(if (isDesktop) Res.string.tab_venta_desktop else Res.string.tab_venta)
            val tabProductosLabel =
                stringResource(if (isDesktop) Res.string.tab_productos_desktop else Res.string.tab_productos)
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

            val appScale = ajustesUiState.appScale

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
                    darkTheme = darkTheme
                ) {
                    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                    val isCompact = maxWidth < 600.dp
                    val showNavLayout = !(currentScreen == Screen.CHECADOR && !isChecadorDialog)
                    val navigationLayoutType = if (showNavLayout) navigationSuiteTypeForWidth(maxWidth) else NavigationSuiteType.None

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
                                                    style = MaterialTheme.typography.labelSmall,
                                                    textAlign = TextAlign.Center,
                                                    fontSize = if (isExpanded) 11.sp else 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    lineHeight = 13.sp
                                                )
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
                                            viewModel = ventaViewModel,
                                            isCompact = isCompact
                                        )

                                        Screen.PRODUCTOS -> ProductosScreen(viewModel = productosViewModel)
                                        Screen.VENTAS -> VentasScreen(viewModel = ventasViewModel)
                                        Screen.AJUSTES -> AjustesScreen(viewModel = ajustesViewModel)
                                        Screen.CHECADOR -> {
                                            if (isChecadorDialog) {
                                                VentaScreen(
                                                    viewModel = ventaViewModel,
                                                    isCompact = isCompact
                                                )
                                            } else {
                                                ChecadorScreen(repository = repository)
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
                        repository = repository
                    )
                }
            }
        }
    }
)
}