package com.dnavarro.poskmp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.db.createDatabase
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.ui.VentaScreen
import com.dnavarro.poskmp.ui.ProductosScreen
import com.dnavarro.poskmp.ui.AjustesScreen
import com.dnavarro.poskmp.ui.ChecadorDialog
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.theme.AppTheme
import androidx.compose.material.icons.filled.Search
import com.dnavarro.poskmp.util.isAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable



@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
    var showPriceCheckerDialog by remember { mutableStateOf(false) }

    val toolbarItems = remember(currentScreen) {
        val isDesktop = !isAndroid()
        listOf(
            ToolbarItem(
                label = if (isDesktop) "Venta (F1)" else "Venta",
                icon = Icons.Default.ShoppingCart,
                isSelected = currentScreen == Screen.VENTA,
                onCheckedChange = { if (it) currentScreen = Screen.VENTA }
            ),
            ToolbarItem(
                label = if (isDesktop) "Productos (F3)" else "Productos",
                icon = Icons.AutoMirrored.Filled.List,
                isSelected = currentScreen == Screen.PRODUCTOS,
                onCheckedChange = { if (it) currentScreen = Screen.PRODUCTOS }
            ),
            ToolbarItem(
                label = if (isDesktop) "Checador (F2)" else "Checador",
                icon = Icons.Default.Search,
                isSelected = false,
                onCheckedChange = { showPriceCheckerDialog = true }
            ),
            ToolbarItem(
                label = "Ajustes",
                icon = Icons.Default.Settings,
                isSelected = currentScreen == Screen.AJUSTES,
                onCheckedChange = { if (it) currentScreen = Screen.AJUSTES }
            )
        )
    }



    AppTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 600.dp

            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                if (!isAndroid()) {
                    focusRequester.requestFocus()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize().background(MaterialTheme.colorScheme.background)
                    .then(
                        if (!isAndroid()) {
                            Modifier
                                .focusRequester(focusRequester)
                                .focusable()
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
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
                                    } else false
                                }
                        } else Modifier.statusBarsPadding()
                    )
            ) {
                // 1. Floating Toolbar overlay (at top center)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        modifier = Modifier.zIndex(1f)
                    ) {
                        val showLabels = !isAndroid()

                        var index = 0
                        toolbarItems.fastForEach { item ->
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Below
                                ),
                                tooltip = { PlainTooltip { Text(item.label) } },
                                state = rememberTooltipState(),
                            ) {
                                ToggleButton(
                                    checked = item.isSelected,
                                    onCheckedChange = item.onCheckedChange,
                                    shapes = ToggleButtonDefaults.shapes(CircleShape, CircleShape, CircleShape),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            item.icon,
                                            contentDescription = item.label
                                        )
                                        AnimatedVisibility(
                                            visible = showLabels,
                                            enter = expandHorizontally(),
                                            exit = shrinkHorizontally()
                                        ) {
                                            Text(
                                                text = item.label,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
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

                // 2. Content Area (occupies remaining screen space)
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    when (currentScreen) {
                        Screen.VENTA -> VentaScreen(repository = repository, isCompact = isCompact)
                        Screen.PRODUCTOS -> ProductosScreen(repository = repository)
                        Screen.AJUSTES -> AjustesScreen()
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

private data class ToolbarItem(
    val label: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)