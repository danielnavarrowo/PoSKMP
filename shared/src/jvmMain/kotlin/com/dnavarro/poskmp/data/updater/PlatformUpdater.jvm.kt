package com.dnavarro.poskmp.data.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import kotlin.system.exitProcess
import com.dnavarro.poskmp.util.AppConstants

actual object PlatformUpdater {

    actual fun getAppVersion(): String {
        return System.getProperty("app.version")
            ?: PlatformUpdater::class.java.`package`?.implementationVersion
            ?: AppConstants.APP_VERSION
    }

    actual fun findMatchingAsset(assets: List<ReleaseAsset>): ReleaseAsset? {
        val osName = System.getProperty("os.name", "").lowercase()
        val isLinux = osName.contains("linux")
        val isWindows = osName.contains("windows")

        return if (isLinux) {
            assets.firstOrNull { it.name.endsWith(".AppImage", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".deb", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".rpm", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".tar.gz", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.contains("linux", ignoreCase = true) }
        } else if (isWindows) {
            assets.firstOrNull { it.name.endsWith(".msi", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".zip", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.contains("windows", ignoreCase = true) }
        } else {
            assets.firstOrNull()
        }
    }

    actual suspend fun downloadAndInstall(
        asset: ReleaseAsset,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userHome = System.getProperty("user.home", ".")
            val downloadsDir = File(userHome, "Downloads").takeIf { it.exists() && it.isDirectory }
                ?: File(System.getProperty("java.io.tmpdir", "."))

            val targetFile = File(downloadsDir, asset.name)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            var currentUrl = asset.downloadUrl
            var connection: HttpURLConnection
            var redirects = 0
            val maxRedirects = 10

            while (true) {
                val url = URI.create(currentUrl).toURL()
                connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "PoSKMP-App")
                connection.instanceFollowRedirects = true
                connection.connect()

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl != null && redirects < maxRedirects) {
                        currentUrl = newUrl
                        redirects++
                        continue
                    }
                }
                break
            }

            val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else asset.sizeBytes
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                    output.flush()
                }
            }
            connection.disconnect()

            val osName = System.getProperty("os.name", "").lowercase()
            val fileName = targetFile.name.lowercase()

            when {
                osName.contains("windows") && fileName.endsWith(".msi") -> {
                    ProcessBuilder("msiexec", "/i", targetFile.absolutePath).start()
                    kotlin.concurrent.thread {
                        Thread.sleep(1000)
                        exitProcess(0)
                    }
                }
                osName.contains("linux") && (fileName.endsWith(".deb") || fileName.endsWith(".rpm")) -> {
                    try {
                        ProcessBuilder("xdg-open", targetFile.absolutePath).start()
                    } catch (_: Exception) {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                            Desktop.getDesktop().open(targetFile)
                        }
                    }
                }
                osName.contains("linux") && fileName.endsWith(".appimage") -> {
                    targetFile.setExecutable(true)
                    ProcessBuilder(targetFile.absolutePath).start()
                    kotlin.concurrent.thread {
                        Thread.sleep(1000)
                        exitProcess(0)
                    }
                }
                else -> {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(targetFile.parentFile ?: targetFile)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
