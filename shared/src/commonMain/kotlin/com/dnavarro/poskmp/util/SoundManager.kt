package com.dnavarro.poskmp.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import poskmp.shared.generated.resources.Res

enum class SoundType(val resourcePath: String) {
    BEEP("drawable/beep.wav"),
    INFO("drawable/audioinfo.wav"),
    ERROR("drawable/error.wav")
}

object SoundManager {
    private val cachedBytes = arrayOfNulls<ByteArray>(SoundType.entries.size)
    private val loadMutex = Mutex()

    /**
     * Preloads and decodes all POS sound effects into hardware-ready static audio buffers
     * so the very first barcode scan has zero cold-start latency.
     */
    suspend fun warmup() {
        for (type in SoundType.entries) {
            try {
                val bytes = getOrLoadBytes(type)
                preloadSoundAlert(type, bytes)
            } catch (_: Exception) {}
        }
    }

    private suspend fun getOrLoadBytes(type: SoundType): ByteArray {
        cachedBytes[type.ordinal]?.let { return it }
        return loadMutex.withLock {
            cachedBytes[type.ordinal] ?: Res.readBytes(type.resourcePath).also {
                cachedBytes[type.ordinal] = it
            }
        }
    }

    private suspend fun play(type: SoundType) {
        try {
            val bytes = cachedBytes[type.ordinal] ?: getOrLoadBytes(type)
            playSoundAlert(type, bytes)
        } catch (_: Exception) {}
    }

    suspend fun playErrorSound() = play(SoundType.ERROR)

    suspend fun playInfoSound() = play(SoundType.INFO)

    suspend fun playBeepSound() = play(SoundType.BEEP)
}
