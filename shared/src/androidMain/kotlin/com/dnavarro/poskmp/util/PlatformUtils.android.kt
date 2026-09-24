package com.dnavarro.poskmp.util

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldPredictiveBackHandler
import androidx.compose.runtime.Composable
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.db.Products
import java.util.UUID
import android.view.View
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun generateUUID(): String = UUID.randomUUID().toString()

@Suppress("SameReturnValue")
actual fun isAndroid(): Boolean = true

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    val view = LocalView.current

    val dialogWindow = remember(view) {
        var v: View? = view
        var w: Window? = null
        while (v != null) {
            if (v is DialogWindowProvider) {
                w = v.window
                break
            }
            v = v.parent as? View
        }
        w
    }

    if (dialogWindow != null) {
        val dispatcherOwner = remember(dialogWindow, view) {
            dialogWindow.decorView.findViewTreeOnBackPressedDispatcherOwner()
                ?: (dialogWindow.callback as? OnBackPressedDispatcherOwner)
                ?: view.findViewTreeOnBackPressedDispatcherOwner()
        }

        val callback = remember(dispatcherOwner) {
            object : OnBackPressedCallback(enabled) {
                override fun handleOnBackPressed() {
                    currentOnBack()
                }
            }
        }

        SideEffect {
            callback.isEnabled = enabled
        }

        DisposableEffect(dispatcherOwner, callback) {
            dispatcherOwner?.onBackPressedDispatcher?.addCallback(callback)
            onDispose {
                callback.remove()
            }
        }
    } else {
        BackHandler(enabled = enabled, onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
actual fun <T> AdaptiveScaffoldPredictiveBackHandler(
    navigator: ThreePaneScaffoldNavigator<T>,
    backBehavior: BackNavigationBehavior,
) {
    ThreePaneScaffoldPredictiveBackHandler(
        navigator = navigator,
        backBehavior = backBehavior
    )
}

actual fun playSoundAlert(bytes: ByteArray) {
    kotlin.concurrent.thread(isDaemon = true) {
        try {
            val tempFile = java.io.File.createTempFile("sound_alert", ".mp3")
            tempFile.deleteOnExit()
            tempFile.writeBytes(bytes)

            val mediaPlayer = android.media.MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
                try { tempFile.delete() } catch (_: Exception) {}
            }
            mediaPlayer.start()
        } catch (_: Exception) {}
    }
}

actual fun pickFile(
    allowedExtensions: List<String>,
    onFilePicked: (fileName: String, content: ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    onError("Importación de archivos no disponible en Android.")
}

actual fun pickDirectory(
    initialPath: String,
    onDirectoryPicked: (path: String) -> Unit,
    onError: (String) -> Unit
) {
    onError("Selección de carpetas no disponible en Android.")
}

object AndroidSaveFileHandler {
    var content: String? = null
    var defaultFileName: String? = null
    var onSuccess: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
}

class SaveFileHelperActivity : ComponentActivity() {

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/comma-separated-values")
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val contentStr = AndroidSaveFileHandler.content ?: ""
                    outputStream.write(contentStr.toByteArray(Charsets.UTF_8))
                }
                AndroidSaveFileHandler.onSuccess?.invoke()
            } catch (e: Exception) {
                AndroidSaveFileHandler.onError?.invoke("Error al guardar archivo: ${e.message.orEmpty()}")
            }
        } else {
            AndroidSaveFileHandler.onError?.invoke("Operación cancelada por el usuario.")
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val defaultName = AndroidSaveFileHandler.defaultFileName ?: "productos_exportados.csv"
        createDocumentLauncher.launch(defaultName)
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidSaveFileHandler.content = null
        AndroidSaveFileHandler.defaultFileName = null
        AndroidSaveFileHandler.onSuccess = null
        AndroidSaveFileHandler.onError = null
    }
}

actual fun saveFile(
    defaultFileName: String,
    content: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val context = DatabaseDriverFactory.appContext
    if (context == null) {
        onError("Contexto de Android no inicializado.")
        return
    }

    AndroidSaveFileHandler.content = content
    AndroidSaveFileHandler.defaultFileName = defaultFileName
    AndroidSaveFileHandler.onSuccess = onSuccess
    AndroidSaveFileHandler.onError = onError

    val intent = Intent(context, SaveFileHelperActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

actual fun parseImportFile(
    fileName: String,
    content: ByteArray
): List<Products> {
    throw UnsupportedOperationException("Importación de archivos no disponible en Android.")
}

