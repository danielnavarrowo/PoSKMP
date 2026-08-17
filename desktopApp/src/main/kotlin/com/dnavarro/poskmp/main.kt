package com.dnavarro.poskmp

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dnavarro.poskmp.di.initKoin

fun main() {
    val userHome = System.getProperty("user.home") ?: "."
    val appDir = java.io.File(userHome, ".poskmp").apply { if (!exists()) mkdirs() }
    val logFile = java.io.File(appDir, "app.log")

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val timestamp = java.time.LocalDateTime.now()
        val errorMsg = "[$timestamp] [CRASH] Uncaught exception on thread ${thread.name}:\n" + throwable.stackTraceToString() + "\n\n"
        println(errorMsg)
        try {
            logFile.appendText(errorMsg)
        } catch (_: Exception) {
        }
    }

    initKoin()

    application {
        val windowState = rememberWindowState(
            width = 1280.dp,
            height = 800.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Antigravity POS"
        ) {
            App()
        }
    }
}