package com.dnavarro.poskmp.util

data class RenderApiInfo(
    val detectedGpu: String?,
    val isLegacyGpu: Boolean,
    val currentActiveApi: String,
    val savedPreference: String,
    val isAvailable: Boolean
)

expect fun getRenderApiInfo(): RenderApiInfo
expect fun setSavedRenderApi(api: String)
