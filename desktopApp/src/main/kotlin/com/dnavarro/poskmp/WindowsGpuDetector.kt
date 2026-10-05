package com.dnavarro.poskmp

import java.util.concurrent.TimeUnit

object WindowsGpuDetector {
    /**
     * Attempts to query the display adapter name from Windows registry.
     * Takes typically 10-25 ms.
     * Returns null if not running on Windows or if query fails.
     */
    fun detectGpuName(): String? {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        if (!os.contains("win")) return null

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

    /**
     * Determines whether the detected GPU is a legacy Intel GPU lacking stable DirectX 12 hardware support
     * (e.g. Intel HD Graphics on Bay Trail, Haswell, Ivy Bridge, Sandy Bridge),
     * which must use OPENGL to prevent falling back to CPU-based WARP software emulation.
     */
    fun isLegacyIntelGpu(gpuName: String?): Boolean {
        if (gpuName == null) return false
        val lower = gpuName.lowercase()
        val isHdGraphics = lower.contains("hd graphics") && !lower.contains("uhd")
        val isOldMediaAccelerator = lower.contains("graphics media accelerator") || lower.contains("express chipset")
        return isHdGraphics || isOldMediaAccelerator
    }
}
