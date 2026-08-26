package com.dnavarro.poskmp.data.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipFile
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
            assets.firstOrNull { it.name.endsWith("-portable.zip", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".zip", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".msi", ignoreCase = true) }
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
            val updateWorkDir = File(System.getProperty("java.io.tmpdir", "."), "poskmp-update")
            if (updateWorkDir.exists()) {
                updateWorkDir.deleteRecursively()
            }
            updateWorkDir.mkdirs()

            val targetFile = File(updateWorkDir, asset.name)

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
                osName.contains("windows") && fileName.endsWith(".zip") -> {
                    applyWindowsZipUpdate(targetFile, updateWorkDir)
                }
                osName.contains("windows") && fileName.endsWith(".msi") -> {
                    ProcessBuilder("msiexec", "/i", targetFile.absolutePath, "/passive", "/norestart").start()
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

    private fun extractZip(zipFile: File, outputDir: File) {
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        val canonicalDestDirPath = outputDir.canonicalPath
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val destFile = File(outputDir, entry.name)
                val canonicalDestFilePath = destFile.canonicalPath
                if (!canonicalDestFilePath.startsWith(canonicalDestDirPath + File.separator) &&
                    canonicalDestFilePath != canonicalDestDirPath
                ) {
                    continue
                }
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private data class WindowsAppTarget(
        val installDir: File,
        val executable: File
    )

    private fun getWindowsAppInstallation(): WindowsAppTarget? {
        try {
            val command = ProcessHandle.current().info().command().orElse(null)
            if (!command.isNullOrBlank()) {
                val exeFile = File(command)
                if (exeFile.exists() && exeFile.isFile && exeFile.name.endsWith(".exe", ignoreCase = true)) {
                    return WindowsAppTarget(installDir = exeFile.parentFile, executable = exeFile)
                }
            }
        } catch (_: Exception) {}

        try {
            val codeSourceLoc = PlatformUpdater::class.java.protectionDomain?.codeSource?.location
            if (codeSourceLoc != null) {
                val jarFile = File(codeSourceLoc.toURI())
                val parent = jarFile.parentFile
                if (parent != null && parent.name.equals("app", ignoreCase = true)) {
                    val installDir = parent.parentFile
                    if (installDir != null) {
                        val exe = File(installDir, "PoSKMP.exe")
                        if (exe.exists()) {
                            return WindowsAppTarget(installDir = installDir, executable = exe)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val userDir = File(System.getProperty("user.dir", "."))
            val exe = File(userDir, "PoSKMP.exe")
            if (exe.exists()) {
                return WindowsAppTarget(installDir = userDir, executable = exe)
            }
        } catch (_: Exception) {}

        return null
    }

    private fun applyWindowsZipUpdate(zipFile: File, updateWorkDir: File) {
        val stagedDir = File(updateWorkDir, "staged")
        extractZip(zipFile, stagedDir)

        val sourceDir = if (File(stagedDir, "PoSKMP.exe").exists() || File(stagedDir, "app").exists()) {
            stagedDir
        } else {
            stagedDir.listFiles()?.firstOrNull { child ->
                child.isDirectory && (File(child, "PoSKMP.exe").exists() || File(child, "app").exists())
            } ?: stagedDir
        }

        val appTarget = getWindowsAppInstallation()
        if (appTarget == null) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(sourceDir)
            }
            return
        }

        val currentPid = ProcessHandle.current().pid()
        val scriptFile = File(updateWorkDir, "update.ps1")

        val psScript = """
            param(
                [int]${'$'}ProcessId,
                [string]${'$'}SourcePath,
                [string]${'$'}TargetPath,
                [string]${'$'}ExecutablePath,
                [string]${'$'}WorkDir
            )

            try {
                ${'$'}proc = Get-Process -Id ${'$'}ProcessId -ErrorAction SilentlyContinue
                if (${'$'}proc) {
                    ${'$'}proc.WaitForExit(15000)
                }
            } catch {}

            Start-Sleep -Milliseconds 600

            ${'$'}maxRetries = 10
            ${'$'}retryCount = 0
            ${'$'}copied = ${'$'}false

            while (-not ${'$'}copied -and ${'$'}retryCount -lt ${'$'}maxRetries) {
                try {
                    Copy-Item -Path "${'$'}SourcePath\*" -Destination "${'$'}TargetPath" -Recurse -Force -ErrorAction Stop
                    ${'$'}copied = ${'$'}true
                } catch {
                    ${'$'}retryCount++
                    Start-Sleep -Milliseconds 500
                }
            }

            if (Test-Path "${'$'}ExecutablePath") {
                Start-Process -FilePath "${'$'}ExecutablePath" -WorkingDirectory "${'$'}TargetPath"
            }

            Start-Sleep -Seconds 3
            try {
                Remove-Item -Path "${'$'}WorkDir" -Recurse -Force -ErrorAction SilentlyContinue
            } catch {}
        """.trimIndent()

        scriptFile.writeText(psScript, Charsets.UTF_8)

        ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-WindowStyle", "Hidden",
            "-File", scriptFile.absolutePath,
            "-ProcessId", currentPid.toString(),
            "-SourcePath", sourceDir.absolutePath,
            "-TargetPath", appTarget.installDir.absolutePath,
            "-ExecutablePath", appTarget.executable.absolutePath,
            "-WorkDir", updateWorkDir.absolutePath
        ).start()

        kotlin.concurrent.thread {
            Thread.sleep(600)
            exitProcess(0)
        }
    }
}
