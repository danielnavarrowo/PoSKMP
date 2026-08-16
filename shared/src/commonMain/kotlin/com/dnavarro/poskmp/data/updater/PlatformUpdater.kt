package com.dnavarro.poskmp.data.updater

expect object PlatformUpdater {
    fun getAppVersion(): String
    fun findMatchingAsset(assets: List<ReleaseAsset>): ReleaseAsset?
    suspend fun downloadAndInstall(
        asset: ReleaseAsset,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit>
}
