package com.dnavarro.poskmp.util

import poskmp.shared.generated.resources.Res

object SoundManager {
    private var cachedErrorBytes: ByteArray? = null
    private var cachedInfoBytes: ByteArray? = null
    private var cachedBeepBytes: ByteArray? = null

    suspend fun playErrorSound() {
        try {
            val bytes = cachedErrorBytes ?: Res.readBytes("drawable/error.wav").also { cachedErrorBytes = it }
            playSoundAlert(bytes)
        } catch (_: Exception) {}
    }

    suspend fun playInfoSound() {
        try {
            val bytes = cachedInfoBytes ?: Res.readBytes("drawable/audioinfo.wav").also { cachedInfoBytes = it }
            playSoundAlert(bytes)
        } catch (_: Exception) {}
    }

    suspend fun playBeepSound() {
        try {
            val bytes = cachedBeepBytes ?: Res.readBytes("drawable/beep.wav").also { cachedBeepBytes = it }
            playSoundAlert(bytes)
        } catch (_: Exception) {}
    }
}
