package com.dnavarro.poskmp

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dnavarro.poskmp.di.initKoin
import org.jetbrains.compose.resources.painterResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.app_icon

fun main() {
    val userHome = System.getProperty("user.home") ?: "."
    val appDir = java.io.File(userHome, ".poskmp").apply { if (!exists()) mkdirs() }
    val logFile = java.io.File(appDir, "app.log")

    // Allow only one instance of the application to run at a time
    if (!SingleInstanceManager.acquireLock(appDir, logFile)) {
        return
    }

    // Check for user-configured renderApi override from settings
    val renderApiOverrideFile = java.io.File(appDir, "render_api.txt")
    if (renderApiOverrideFile.exists()) {
        val overrideApi = renderApiOverrideFile.readText().trim().uppercase()
        if (overrideApi.isNotEmpty() && overrideApi != "AUTO" && overrideApi != "DEFAULT") {
            System.setProperty("skiko.renderApi", overrideApi)
        }
    }

    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val timestamp = java.time.LocalDateTime.now()
        val errorMsg = "[$timestamp] [CRASH] Uncaught exception on thread ${thread.name}:\n" + throwable.stackTraceToString() + "\n\n"
        println(errorMsg)
        try {
            logFile.appendText(errorMsg)
        } catch (_: Exception) {
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    initKoin()

    application {
        val windowState = rememberWindowState(
            placement = WindowPlacement.Floating
        )
        var isClosing by remember { mutableStateOf(false) }

        Window(
            onCloseRequest = {
                isClosing = true
            },
            state = windowState,
            title = "Punto de Venta",
            icon = painterResource(Res.drawable.app_icon),
            undecorated = true
        ) {
            DisposableEffect(Unit) {
                SingleInstanceManager.onBringToFront = {
                    javax.swing.SwingUtilities.invokeLater {
                        windowState.isMinimized = false
                        window.extendedState = java.awt.Frame.NORMAL
                        window.toFront()
                        window.requestFocus()
                    }
                }
                onDispose {
                    SingleInstanceManager.onBringToFront = null
                }
            }

            App(
                isExiting = isClosing,
                onCancelExit = { isClosing = false },
                onExitCompleted = ::exitApplication,
                onMinimize = {
                    windowState.isMinimized = true
                    window.extendedState = java.awt.Frame.ICONIFIED
                },
                onClose = {
                    isClosing = true
                }
            )
        }
    }
}