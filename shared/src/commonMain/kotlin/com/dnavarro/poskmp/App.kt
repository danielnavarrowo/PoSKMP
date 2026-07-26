package com.dnavarro.poskmp

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.db.createDatabase
import com.dnavarro.poskmp.theme.AppTheme
import com.dnavarro.poskmp.ui.*
import com.dnavarro.poskmp.util.isAndroid
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import poskmp.shared.generated.resources.*


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
        val repo = ProductRepository(db)
        repo.insertDummyDataIfEmpty()
        repo
    }

    // 2. Navigation State
    var currentScreen by remember { mutableStateOf(Screen.VENTA) }
    val cartItems = remember { mutableStateListOf<CartItem>() }
    var showPriceCheckerDialog by remember { mutableStateOf(false) }

    val toolbarItems = remember(currentScreen) {
        val isDesktop = !isAndroid()
        listOf(
            ToolbarItem(
                label = if (isDesktop) "Venta (F1)" else "Venta",
                icon = Res.drawable.point_of_sale,
                isSelected = currentScreen == Screen.VENTA,
                onCheckedChange = { if (it) currentScreen = Screen.VENTA }
            ),
            ToolbarItem(
                label = if (isDesktop) "Productos (F3)" else "Productos",
                icon = Res.drawable.products,
                isSelected = currentScreen == Screen.PRODUCTOS,
                onCheckedChange = { if (it) currentScreen = Screen.PRODUCTOS }
            ),
            ToolbarItem(
                label = if (isDesktop) "Checador (F2)" else "Checador",
                icon = Res.drawable.price,
                isSelected = false,
                onCheckedChange = { showPriceCheckerDialog = true }
            ),
            ToolbarItem(
                label = "Ajustes",
                icon = Res.drawable.settings,
                isSelected = currentScreen == Screen.AJUSTES,
                onCheckedChange = { if (it) currentScreen = Screen.AJUSTES }
            )
        )
    }



    AppTheme {
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
                                repository = repository,
                                isCompact = isCompact,
                                cartItems = cartItems
                            )
                            Screen.PRODUCTOS -> ProductosScreen(repository = repository)
                            Screen.AJUSTES -> AjustesScreen()
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