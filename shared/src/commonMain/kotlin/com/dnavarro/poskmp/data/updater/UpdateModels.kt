package com.dnavarro.poskmp.data.updater

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val publishedAt: String,
    val assets: List<ReleaseAsset>
) {
    val cleanVersion: String
        get() = tagName.trim().trimStart('v', 'V')
}

sealed interface UpdateCheckResult {
    object UpToDate : UpdateCheckResult
    data class UpdateAvailable(val releaseInfo: ReleaseInfo, val matchingAsset: ReleaseAsset?) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

sealed interface UpdateDownloadState {
    object Idle : UpdateDownloadState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState
    object Installing : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}
