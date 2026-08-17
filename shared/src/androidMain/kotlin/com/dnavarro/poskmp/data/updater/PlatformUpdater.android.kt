package com.dnavarro.poskmp.data.updater

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import com.dnavarro.poskmp.util.AppConstants
import androidx.core.net.toUri

actual object PlatformUpdater {

    actual fun getAppVersion(): String {
        val context = DatabaseDriverFactory.appContext ?: return AppConstants.APP_VERSION
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: AppConstants.APP_VERSION
        } catch (_: Exception) {
            AppConstants.APP_VERSION
        }
    }

    actual fun findMatchingAsset(assets: List<ReleaseAsset>): ReleaseAsset? {
        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        for (abi in supportedAbis) {
            val match = assets.firstOrNull {
                it.name.contains(abi, ignoreCase = true) && it.name.endsWith(".apk", ignoreCase = true)
            }
            if (match != null) return match
        }
        return assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
    }

    actual suspend fun downloadAndInstall(
        asset: ReleaseAsset,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val context = DatabaseDriverFactory.appContext
            ?: return@withContext Result.failure(IllegalStateException("Android Context not initialized"))

        if (!context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri()
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            return@withContext Result.failure(Exception("Por favor concede el permiso para instalar aplicaciones desconocidas y vuelve a pulsar en Actualizar."))
        }

        try {
            val apkFile = File(context.cacheDir, "update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            var currentUrl = asset.downloadUrl
            var connection: HttpURLConnection
            var redirects = 0
            val maxRedirects = 10

            while (true) {
                val url = java.net.URI.create(currentUrl).toURL()
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
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
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

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
