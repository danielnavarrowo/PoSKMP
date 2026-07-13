package com.dnavarro.poskmp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.db.createDatabase
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.ui.VentaScreen
import com.dnavarro.poskmp.ui.ProductosScreen
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.theme.AppTheme

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

            if (isCompact) {
                // Mobile layout with bottom navigation
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentScreen) {
                            Screen.VENTA -> VentaScreen(repository = repository, isCompact = true)
                            Screen.PRODUCTOS -> ProductosScreen(repository = repository)
                        }
                    }

                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.VENTA,
                            onClick = { currentScreen = Screen.VENTA },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Venta") },
                            label = { Text("Venta") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.PRODUCTOS,
                            onClick = { currentScreen = Screen.PRODUCTOS },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Productos") },
                            label = { Text("Productos") }
                        )
                    }
                }
            } else {
                // Desktop layout with permanent sidebar
                Row(modifier = Modifier.fillMaxSize()) {
                    // SIDEBAR (Left Navigation)
                    Column(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant) // Sleek adaptive M3 surfaceVariant background
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top section: Branding & Navigation
                        Column {
                            // Branding / Logo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.primary), // Dynamic M3 primary
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Antigravity",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        "Punto de Venta",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Navigation Links
                            Text(
                                "MENÚ PRINCIPAL",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                            )

                            // Nav Item: Venta
                            SidebarNavItem(
                                title = "Venta",
                                icon = Icons.Default.ShoppingCart,
                                isSelected = currentScreen == Screen.VENTA,
                                onClick = { currentScreen = Screen.VENTA }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Nav Item: Productos
                            SidebarNavItem(
                                title = "Productos",
                                icon = Icons.AutoMirrored.Filled.List,
                                isSelected = currentScreen == Screen.PRODUCTOS,
                                onClick = { currentScreen = Screen.PRODUCTOS }
                            )
                        }

                        // Bottom section: Connection Status (Offline First indicator)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "ESTADO DEL SISTEMA",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Offline OK",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Base Local: Conectada",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Supabase Offline",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Sincronización: Pendiente",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // MAIN AREA (Right Content)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (currentScreen) {
                            Screen.VENTA -> VentaScreen(repository = repository, isCompact = false)
                            Screen.PRODUCTOS -> ProductosScreen(repository = repository)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SidebarNavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}