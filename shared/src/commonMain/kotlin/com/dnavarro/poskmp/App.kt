package com.dnavarro.poskmp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
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
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.theme.AppTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    AppTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 600.dp

            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Content Area (occupies full screen)
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (currentScreen) {
                        Screen.VENTA -> VentaScreen(repository = repository, isCompact = isCompact)
                        Screen.PRODUCTOS -> ProductosScreen(repository = repository)
                        Screen.AJUSTES -> AjustesScreen()
                    }
                }

                // 2. Floating Toolbar overlay (at bottom center)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        modifier = Modifier.zIndex(1f)
                    ) {
                        // Item: Venta
                        val isVentaSelected = currentScreen == Screen.VENTA
                        ToggleButton(
                            checked = isVentaSelected,
                            onCheckedChange = { if (it) currentScreen = Screen.VENTA },
                            shapes = ToggleButtonDefaults.shapes(CircleShape, CircleShape, CircleShape),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = "Venta"
                                )
                                AnimatedVisibility(
                                    visible = isVentaSelected,
                                    enter = expandHorizontally(),
                                    exit = shrinkHorizontally()
                                ) {
                                    Text(
                                        text = "Venta",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Item: Productos
                        val isProductosSelected = currentScreen == Screen.PRODUCTOS
                        ToggleButton(
                            checked = isProductosSelected,
                            onCheckedChange = { if (it) currentScreen = Screen.PRODUCTOS },
                            shapes = ToggleButtonDefaults.shapes(CircleShape, CircleShape, CircleShape),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Productos"
                                )
                                AnimatedVisibility(
                                    visible = isProductosSelected,
                                    enter = expandHorizontally(),
                                    exit = shrinkHorizontally()
                                ) {
                                    Text(
                                        text = "Productos",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Item: Ajustes
                        val isAjustesSelected = currentScreen == Screen.AJUSTES
                        ToggleButton(
                            checked = isAjustesSelected,
                            onCheckedChange = { if (it) currentScreen = Screen.AJUSTES },
                            shapes = ToggleButtonDefaults.shapes(CircleShape, CircleShape, CircleShape),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Ajustes"
                                )
                                AnimatedVisibility(
                                    visible = isAjustesSelected,
                                    enter = expandHorizontally(),
                                    exit = shrinkHorizontally()
                                ) {
                                    Text(
                                        text = "Ajustes",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
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