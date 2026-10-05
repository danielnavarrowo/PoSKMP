package com.dnavarro.poskmp.util

import java.io.File
import java.util.concurrent.TimeUnit

actual fun getRenderApiInfo(): RenderApiInfo {
    val osName = System.getProperty("os.name")?.lowercase() ?: ""
    val isWindows = osName.contains("win")

    val userHome = System.getProperty("user.home") ?: "."
    val appDir = File(userHome, ".poskmp")
    val renderApiFile = File(appDir, "render_api.txt")
    val saved = if (renderApiFile.exists()) {
        val content = renderApiFile.readText().trim().uppercase()
        if (content in listOf("DIRECT3D", "OPENGL", "SOFTWARE")) content else "AUTO"
    } else {
        "AUTO"
    }

    val currentActive = System.getProperty("skiko.renderApi") ?: if (isWindows) "DIRECT3D" else "OPENGL"
    val detectedGpu = if (isWindows) detectWindowsGpu() else null
    val isLegacy = isLegacyIntelGpu(detectedGpu)

    return RenderApiInfo(
        detectedGpu = detectedGpu,
        isLegacyGpu = isLegacy,
        currentActiveApi = currentActive,
        savedPreference = saved,
        isAvailable = isWindows
    )
}

actual fun setSavedRenderApi(api: String) {
    val userHome = System.getProperty("user.home") ?: "."
    val appDir = File(userHome, ".poskmp").apply { if (!exists()) mkdirs() }
    val renderApiFile = File(appDir, "render_api.txt")

    if (api == "AUTO" || api == "DEFAULT") {
        if (renderApiFile.exists()) {
            renderApiFile.delete()
        }
    } else {
        renderApiFile.writeText(api.uppercase())
    }
}

private fun detectWindowsGpu(): String? {
    return try {
        val process = ProcessBuilder(
            "reg", "query",
            "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}",
            "/s", "/v", "DriverDesc"
        ).redirectErrorStream(true).start()

        val finished = process.waitFor(500, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            return null
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val gpuLine = output.lines().firstOrNull { it.contains("DriverDesc") && it.contains("REG_SZ") }
        gpuLine?.substringAfter("REG_SZ")?.trim()
    } catch (_: Exception) {
        null
    }
}

private fun isLegacyIntelGpu(gpuName: String?): Boolean {
    if (gpuName == null) return false
    val lower = gpuName.lowercase()
    val isHdGraphics = lower.contains("hd graphics") && !lower.contains("uhd")
    val isOldMediaAccelerator = lower.contains("graphics media accelerator") || lower.contains("express chipset")
    return isHdGraphics || isOldMediaAccelerator
}
