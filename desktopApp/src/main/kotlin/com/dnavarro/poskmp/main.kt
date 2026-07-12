package com.dnavarro.poskmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Punto de Venta",
    ) {
        App()
    }
}