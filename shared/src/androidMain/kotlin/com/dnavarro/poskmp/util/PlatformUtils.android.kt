package com.dnavarro.poskmp.util

import android.content.Intent
import android.os.Build
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
import androidx.annotation.ChecksSdkIntAtLeast
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

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
actual fun isDynamicColorSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun rememberPlatformSystemDarkTheme(): Boolean =
    androidx.compose.foundation.isSystemInDarkTheme()

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

private object AndroidSoundPlayer {
    private class ParsedWav(
        val sampleRate: Int,
        val channels: Int,
        val bitsPerSample: Int,
        val pcmOffset: Int,
        val pcmLength: Int
    )

    private val tracks = java.util.concurrent.atomic.AtomicReferenceArray<android.media.AudioTrack?>(SoundType.entries.size)
    private val playbackLock = Any()
    @Volatile
    private var activeTrack: android.media.AudioTrack? = null

    private val audioAttributes: android.media.AudioAttributes by lazy {
        android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private fun parseWavHeader(bytes: ByteArray): ParsedWav? {
        if (bytes.size < 44) return null
        if (bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
            bytes[2] != 'F'.code.toByte() || bytes[3] != 'F'.code.toByte()
        ) return null

        var pos = 12
        var audioFormat = 1
        var channels = 1
        var sampleRate = 44100
        var bitsPerSample = 16
        var dataOffset = -1
        var dataLength = 0

        while (pos + 8 <= bytes.size) {
            val chunkId0 = bytes[pos].toInt().toChar()
            val chunkId1 = bytes[pos + 1].toInt().toChar()
            val chunkId2 = bytes[pos + 2].toInt().toChar()
            val chunkId3 = bytes[pos + 3].toInt().toChar()
            val chunkSize = (bytes[pos + 4].toInt() and 0xFF) or
                    ((bytes[pos + 5].toInt() and 0xFF) shl 8) or
                    ((bytes[pos + 6].toInt() and 0xFF) shl 16) or
                    ((bytes[pos + 7].toInt() and 0xFF) shl 24)

            val contentStart = pos + 8
            if (chunkId0 == 'f' && chunkId1 == 'm' && chunkId2 == 't' && chunkId3 == ' ' && contentStart + 16 <= bytes.size) {
                audioFormat = (bytes[contentStart].toInt() and 0xFF) or ((bytes[contentStart + 1].toInt() and 0xFF) shl 8)
                channels = (bytes[contentStart + 2].toInt() and 0xFF) or ((bytes[contentStart + 3].toInt() and 0xFF) shl 8)
                sampleRate = (bytes[contentStart + 4].toInt() and 0xFF) or
                        ((bytes[contentStart + 5].toInt() and 0xFF) shl 8) or
                        ((bytes[contentStart + 6].toInt() and 0xFF) shl 16) or
                        ((bytes[contentStart + 7].toInt() and 0xFF) shl 24)
                bitsPerSample = (bytes[contentStart + 14].toInt() and 0xFF) or ((bytes[contentStart + 15].toInt() and 0xFF) shl 8)
            } else if (chunkId0 == 'd' && chunkId1 == 'a' && chunkId2 == 't' && chunkId3 == 'a') {
                dataOffset = contentStart
                dataLength = chunkSize.coerceAtMost(bytes.size - contentStart)
                break
            }

            if (chunkSize < 0) break
            pos = contentStart + chunkSize + (chunkSize and 1)
        }

        if (audioFormat != 1 || dataOffset < 0 || dataLength <= 0) return null
        return ParsedWav(sampleRate, channels, bitsPerSample, dataOffset, dataLength)
    }

    private fun getOrCreateTrack(type: SoundType, bytes: ByteArray): android.media.AudioTrack? {
        tracks.get(type.ordinal)?.let { return it }
        synchronized(this) {
            tracks.get(type.ordinal)?.let { return it }
            val wav = parseWavHeader(bytes) ?: return null
            val channelMask = if (wav.channels == 1) {
                android.media.AudioFormat.CHANNEL_OUT_MONO
            } else {
                android.media.AudioFormat.CHANNEL_OUT_STEREO
            }
            val encoding = if (wav.bitsPerSample == 8) {
                android.media.AudioFormat.ENCODING_PCM_8BIT
            } else {
                android.media.AudioFormat.ENCODING_PCM_16BIT
            }
            val format = android.media.AudioFormat.Builder()
                .setSampleRate(wav.sampleRate)
                .setEncoding(encoding)
                .setChannelMask(channelMask)
                .build()

            val builder = android.media.AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(wav.pcmLength)
                .setTransferMode(android.media.AudioTrack.MODE_STATIC)
            builder.setPerformanceMode(android.media.AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            val track = builder.build().apply {
                write(bytes, wav.pcmOffset, wav.pcmLength)
            }
            tracks.set(type.ordinal, track)
            return track
        }
    }

    fun preload(type: SoundType, bytes: ByteArray) {
        try {
            getOrCreateTrack(type, bytes)
        } catch (_: Exception) {}
    }

    fun play(type: SoundType, bytes: ByteArray) {
        try {
            val track = tracks.get(type.ordinal) ?: getOrCreateTrack(type, bytes) ?: return
            synchronized(playbackLock) {
                val previous = activeTrack
                if (previous != null && previous.playState == android.media.AudioTrack.PLAYSTATE_PLAYING) {
                    try {
                        previous.pause()
                        previous.flush()
                        previous.stop()
                    } catch (_: Exception) {}
                }
                if (track.playState == android.media.AudioTrack.PLAYSTATE_PLAYING) {
                    try {
                        track.stop()
                    } catch (_: Exception) {}
                }
                track.reloadStaticData()
                track.play()
                activeTrack = track
            }
        } catch (_: Exception) {}
    }
}

actual fun preloadSoundAlert(type: SoundType, bytes: ByteArray) {
    AndroidSoundPlayer.preload(type, bytes)
}

actual fun playSoundAlert(type: SoundType, bytes: ByteArray) {
    AndroidSoundPlayer.play(type, bytes)
}

actual fun pickFile(
    allowedExtensions: List<String>,
    onFilePicked: (fileName: String, content: ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    onError("Importación de archivos no disponible en Android.")
}

actual fun pickSingleFile(
    allowedExtensions: List<String>,
    onFilePicked: (filePath: String) -> Unit,
    onError: (String) -> Unit
) {
    onError("Selección de archivos no disponible en Android.")
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

actual suspend fun parseImportFile(
    fileName: String,
    content: ByteArray
): List<Products> {
    throw UnsupportedOperationException("Importación de archivos no disponible en Android.")
}

