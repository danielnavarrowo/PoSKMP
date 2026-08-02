package com.dnavarro.poskmp.util

import poskmp.shared.generated.resources.Res

object SoundManager {
    private var cachedAudioBytes: ByteArray? = null

    suspend fun playErrorSound() {
        try {
            val bytes = cachedAudioBytes ?: Res.readBytes("drawable/error.mp3").also { cachedAudioBytes = it }
            playSoundAlert(bytes)
        } catch (_: Exception) {}
    }
}
