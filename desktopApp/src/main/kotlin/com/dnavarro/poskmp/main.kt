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

    // Optional FPS profiling toggle via fps.txt
    val fpsFile = java.io.File(appDir, "fps.txt")
    if (fpsFile.exists()) {
        System.setProperty("skiko.fps.enabled", "true")
        System.setProperty("skiko.fps.longFrames.show", "true")
        System.setProperty("skiko.fps.longFrames.millis", "16.6")
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

    try {
        val timestamp = java.time.LocalDateTime.now()
        val runtime = Runtime.getRuntime()
        val startupMsg = buildString {
            appendLine("[$timestamp] [STARTUP] Starting Punto de Venta v${System.getProperty("app.version") ?: "unknown"}")
            appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")
            appendLine("Java: ${System.getProperty("java.version")} by ${System.getProperty("java.vendor")}")
            appendLine("Processors: ${runtime.availableProcessors()} cores, Max Memory: ${runtime.maxMemory() / (1024 * 1024)} MB")
            appendLine("Skiko Render API: ${System.getProperty("skiko.renderApi") ?: "Por defecto (Sistema)"}")
            if (System.getProperty("skiko.fps.enabled") == "true") {
                appendLine("FPS Profiling: ENABLED (logging long frames > 16.6ms)")
            }
        }
        println(startupMsg)
        logFile.appendText(startupMsg + "\n")
    } catch (_: Exception) {
    }

    initKoin()

    application {
        val windowState = rememberWindowState(
            placement = WindowPlacement.Fullscreen
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