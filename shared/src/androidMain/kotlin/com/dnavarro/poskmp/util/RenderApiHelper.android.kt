package com.dnavarro.poskmp.util

actual fun getRenderApiInfo(): RenderApiInfo = RenderApiInfo(
    detectedGpu = null,
    isLegacyGpu = false,
    currentActiveApi = "OPENGL",
    savedPreference = "AUTO",
    isAvailable = false
)

actual fun setSavedRenderApi(api: String) {
    // No-op on Android
}
