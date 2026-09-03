package com.dnavarro.poskmp.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.dnavarro.poskmp.ui.Screen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Type-safe navigation destinations for PoSKMP using Navigation 3.
 */
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Venta : AppRoute

    @Serializable
    data object Checador : AppRoute

    @Serializable
    data object Productos : AppRoute

    @Serializable
    data object Ventas : AppRoute

    @Serializable
    data object Clientes : AppRoute

    @Serializable
    data class Ajustes(val category: com.dnavarro.poskmp.ui.ajustes.AjustesCategory? = null) : AppRoute
}

fun Screen.toRoute(): AppRoute = when (this) {
    Screen.VENTA -> AppRoute.Venta
    Screen.CHECADOR -> AppRoute.Checador
    Screen.PRODUCTOS -> AppRoute.Productos
    Screen.VENTAS -> AppRoute.Ventas
    Screen.CLIENTES -> AppRoute.Clientes
    Screen.AJUSTES -> AppRoute.Ajustes()
}

fun AppRoute.toScreen(): Screen = when (this) {
    is AppRoute.Venta -> Screen.VENTA
    is AppRoute.Checador -> Screen.CHECADOR
    is AppRoute.Productos -> Screen.PRODUCTOS
    is AppRoute.Ventas -> Screen.VENTAS
    is AppRoute.Clientes -> Screen.CLIENTES
    is AppRoute.Ajustes -> Screen.AJUSTES
}

val navSavedStateConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Venta::class, AppRoute.Venta.serializer())
            subclass(AppRoute.Checador::class, AppRoute.Checador.serializer())
            subclass(AppRoute.Productos::class, AppRoute.Productos.serializer())
            subclass(AppRoute.Ventas::class, AppRoute.Ventas.serializer())
            subclass(AppRoute.Clientes::class, AppRoute.Clientes.serializer())
            subclass(AppRoute.Ajustes::class, AppRoute.Ajustes.serializer())
        }
    }
}

/**
 * Navigates to a top-level route in the main navigation suite.
 * Top-level destinations do not accumulate on top of each other in the backstack.
 * Only the configured start destination stays at index 0, and any non-start destination
 * sits at index 1 so that pressing back always returns straight to the start destination.
 */
fun MutableList<NavKey>.navigateToTopLevel(route: AppRoute, defaultRoute: AppRoute) {
    if (lastOrNull() == route) return

    if (isEmpty()) {
        add(defaultRoute)
    } else if (get(0) != defaultRoute) {
        set(0, defaultRoute)
    }

    if (route == defaultRoute) {
        while (size > 1) {
            removeAt(lastIndex)
        }
    } else {
        while (size > 1) {
            removeAt(lastIndex)
        }
        add(route)
    }
}
