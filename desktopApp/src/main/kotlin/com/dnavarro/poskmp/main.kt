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
import com.dnavarro.poskmp.di.performEmergencyShutdownBackup
import com.dnavarro.poskmp.util.configureSkikoGraphicsEnvironment
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

    // Configure optimal graphics backend and Skiko renderApi
    configureSkikoGraphicsEnvironment(appDir)

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

    // Register JVM shutdown hook to ensure local database backup runs even when the app is terminated
    // by OS shutdown, SIGTERM/WM_ENDSESSION, or abrupt external process termination, before releasing the lock.
    Runtime.getRuntime().addShutdownHook(
        Thread(
            {
                try {
                    val result = performEmergencyShutdownBackup()
                    result.onSuccess { backupPath ->
                        if (backupPath != null) {
                            val timestamp = java.time.LocalDateTime.now()
                            val msg = "[$timestamp] [SHUTDOWN] Local backup created on exit: $backupPath\n"
                            println(msg)
                            try {
                                logFile.appendText(msg)
                            } catch (_: Exception) {
                            }
                        }
                    }.onFailure { error ->
                        val timestamp = java.time.LocalDateTime.now()
                        val msg = "[$timestamp] [SHUTDOWN] Failed to create local backup on exit: ${error.message}\n"
                        println(msg)
                        try {
                            logFile.appendText(msg)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Throwable) {
                } finally {
                    SingleInstanceManager.releaseLock()
                }
            },
            "PosKmp-ShutdownBackupHook"
        )
    )

    try {
        if (java.awt.Desktop.isDesktopSupported()) {
            val desktop = java.awt.Desktop.getDesktop()
            if (desktop.isSupported(java.awt.Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitHandler { _, response ->
                    try {
                        performEmergencyShutdownBackup()
                    } catch (_: Throwable) {
                    }
                    response.performQuit()
                }
            }
        }
    } catch (_: Throwable) {
    }

    application {
        val windowState = rememberWindowState(
            placement = WindowPlacement.Fullscreen
        )
        var isClosing by remember { mutableStateOf(false) }
        var isExternalClose by remember { mutableStateOf(false) }

        Window(
            onCloseRequest = {
                isExternalClose = true
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

                val keyEventDispatcher = java.awt.KeyEventDispatcher { event ->
                    if (event.id == java.awt.event.KeyEvent.KEY_PRESSED &&
                        event.keyCode == java.awt.event.KeyEvent.VK_F4 &&
                        event.isAltDown
                    ) {
                        javax.swing.SwingUtilities.invokeLater {
                            isExternalClose = true
                            isClosing = true
                        }
                        true
                    } else {
                        false
                    }
                }
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .addKeyEventDispatcher(keyEventDispatcher)

                onDispose {
                    SingleInstanceManager.onBringToFront = null
                    java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .removeKeyEventDispatcher(keyEventDispatcher)
                }
            }

            App(
                isExiting = isClosing,
                isExternalExit = isExternalClose,
                onCancelExit = {
                    isClosing = false
                    isExternalClose = false
                },
                onExitCompleted = ::exitApplication,
                onMinimize = {
                    windowState.isMinimized = true
                    window.extendedState = java.awt.Frame.ICONIFIED
                },
                onClose = {
                    isExternalClose = false
                    isClosing = true
                }
            )
        }
    }
}