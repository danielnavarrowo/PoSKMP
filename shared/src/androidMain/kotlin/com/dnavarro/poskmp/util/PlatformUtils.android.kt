package com.dnavarro.poskmp.util

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.db.Products
import java.util.UUID

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun generateUUID(): String = UUID.randomUUID().toString()
actual fun isAndroid(): Boolean = true

actual fun pickFile(
    allowedExtensions: List<String>,
    onFilePicked: (fileName: String, content: ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    onError("La importación no está soportada en Android.")
}

// Callback holder for Android file saving
object AndroidSaveFileHandler {
    var content: String? = null
    var defaultFileName: String? = null
    var onSuccess: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
}

// Transparent activity to launch the SAF CreateDocument chooser
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
                AndroidSaveFileHandler.onError?.invoke("Error al guardar archivo: ${e.message}")
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
    throw UnsupportedOperationException("La importación no está soportada en Android.")
}
