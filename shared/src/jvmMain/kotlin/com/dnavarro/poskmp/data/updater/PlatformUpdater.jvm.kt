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

    private fun isRunningAppImage(): Boolean {
        val appImageEnv = System.getenv("APPIMAGE")
        val appDirEnv = System.getenv("APPDIR")
        if (!appImageEnv.isNullOrBlank() || !appDirEnv.isNullOrBlank()) {
            return true
        }
        val exe = getLinuxExecutablePath()
        return exe != null && (exe.contains("/.mount_") || exe.endsWith(".appimage", ignoreCase = true))
    }

    private fun getRunningAppImageFile(): File? {
        val appImagePath = System.getenv("APPIMAGE")
        if (!appImagePath.isNullOrBlank()) {
            val file = File(appImagePath)
            if (file.exists() && file.isFile) {
                return file
            }
        }
        return null
    }

    private fun getLinuxExecutablePath(): String? {
        try {
            val procExe = File("/proc/self/exe")
            if (procExe.exists()) {
                return procExe.canonicalPath
            }
        } catch (_: Exception) {}

        try {
            val command = ProcessHandle.current().info().command().orElse(null)
            if (!command.isNullOrBlank()) {
                return command
            }
        } catch (_: Exception) {}

        return null
    }

    actual fun findMatchingAsset(assets: List<ReleaseAsset>): ReleaseAsset? {
        val osName = System.getProperty("os.name", "").lowercase()
        val isLinux = osName.contains("linux")
        val isWindows = osName.contains("windows")

        return if (isLinux) {
            val isCurrentAppImage = isRunningAppImage()

            if (isCurrentAppImage) {
                // Si la app se está ejecutando como AppImage, debe actualizarse con el AppImage
                assets.firstOrNull { it.name.endsWith(".AppImage", ignoreCase = true) }
                    ?: assets.firstOrNull { it.name.endsWith(".tar.gz", ignoreCase = true) && !it.name.contains(".pkg.tar", ignoreCase = true) }
                    ?: assets.firstOrNull { it.name.contains("linux", ignoreCase = true) }
            } else {
                // No es AppImage: detectar si el sistema o instalación usa .pkg.tar (Arch), .deb o .rpm
                val isDebian = File("/etc/debian_version").exists() || File("/usr/bin/dpkg").exists()
                val isRedHat = File("/etc/redhat-release").exists() || File("/etc/fedora-release").exists() || (File("/usr/bin/rpm").exists() && !isDebian)
                val isArch = File("/etc/arch-release").exists() || File("/etc/cachyos-release").exists() || File("/usr/bin/pacman").exists()
                val isInstalledArchPkg = getLinuxExecutablePath()?.let { it.startsWith("/opt/poskmp") || it.startsWith("/opt/punto-de-venta") } == true

                when {
                    isArch || isInstalledArchPkg -> {
                        assets.firstOrNull { it.name.contains(".pkg.tar", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.endsWith(".AppImage", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.contains("linux", ignoreCase = true) }
                    }
                    isDebian -> {
                        assets.firstOrNull { it.name.endsWith(".deb", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.endsWith(".AppImage", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.endsWith(".tar.gz", ignoreCase = true) && !it.name.contains(".pkg.tar", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.contains("linux", ignoreCase = true) }
                    }
                    isRedHat -> {
                        assets.firstOrNull { it.name.endsWith(".rpm", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.endsWith(".AppImage", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.endsWith(".tar.gz", ignoreCase = true) && !it.name.contains(".pkg.tar", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.contains("linux", ignoreCase = true) }
                    }
                    else -> {
                        assets.firstOrNull { it.name.contains(".pkg.tar", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.endsWith(".AppImage", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.endsWith(".deb", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.endsWith(".rpm", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.endsWith(".tar.gz", ignoreCase = true) && !it.name.contains(".pkg.tar", ignoreCase = true) }
                            ?: assets.firstOrNull { it.name.contains("linux", ignoreCase = true) }
                    }
                }
            }
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
            try {
                updateWorkDir.setReadable(true, false)
                updateWorkDir.setExecutable(true, false)
            } catch (_: Exception) {}

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
            try {
                targetFile.setReadable(true, false)
            } catch (_: Exception) {}

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
                osName.contains("linux") && (
                    fileName.endsWith(".deb") ||
                    fileName.endsWith(".rpm") ||
                    fileName.contains(".pkg.tar")
                ) -> {
                    applyLinuxPackageUpdate(targetFile, updateWorkDir)
                }
                osName.contains("linux") && fileName.endsWith(".appimage") -> {
                    applyLinuxAppImageUpdate(targetFile, updateWorkDir)
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

        val psScript = $$"""
            param(
                [int]$ProcessId,
                [string]$SourcePath,
                [string]$TargetPath,
                [string]$ExecutablePath,
                [string]$WorkDir
            )

            try {
                $proc = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
                if ($proc) {
                    $proc.WaitForExit(15000)
                }
            } catch {}

            Start-Sleep -Milliseconds 600

            $maxRetries = 10
            $retryCount = 0
            $copied = $false

            while (-not $copied -and $retryCount -lt $maxRetries) {
                try {
                    Copy-Item -Path "$SourcePath\*" -Destination "$TargetPath" -Recurse -Force -ErrorAction Stop
                    $copied = $true
                } catch {
                    $retryCount++
                    Start-Sleep -Milliseconds 500
                }
            }

            if (Test-Path "$ExecutablePath") {
                Start-Process -FilePath "$ExecutablePath" -WorkingDirectory "$TargetPath"
            }

            Start-Sleep -Seconds 3
            try {
                Remove-Item -Path "$WorkDir" -Recurse -Force -ErrorAction SilentlyContinue
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

    private fun applyLinuxAppImageUpdate(targetFile: File, updateWorkDir: File) {
        val targetAppImage = getRunningAppImageFile()
            ?: File(System.getProperty("user.home"), "Applications/${targetFile.name}").let {
                if (it.parentFile.exists() || it.parentFile.mkdirs()) it else File(System.getProperty("user.home"), targetFile.name)
            }

        targetFile.setExecutable(true, false)

        val currentPid = ProcessHandle.current().pid()
        val scriptFile = File(updateWorkDir, "update_appimage.sh")

        val shScript = $$"""
            #!/bin/sh
            OLD_PID="$1"
            NEW_FILE="$2"
            TARGET_FILE="$3"
            WORK_DIR="$4"

            # 1. Wait for old AppImage process to fully exit
            if [ -n "$OLD_PID" ] && [ "$OLD_PID" -gt 0 ] 2>/dev/null; then
                while kill -0 "$OLD_PID" 2>/dev/null; do
                    sleep 0.2
                done
            fi
            sleep 0.6

            # 2. Atomic replacement of target AppImage
            TARGET_DIR="$(dirname "$TARGET_FILE")"
            mkdir -p "$TARGET_DIR" 2>/dev/null

            FINAL_EXEC="$TARGET_FILE"
            if [ -w "$TARGET_DIR" ]; then
                TMP_TARGET="${TARGET_FILE}.new.$$"
                if cp -f "$NEW_FILE" "$TMP_TARGET" 2>/dev/null || cat "$NEW_FILE" > "$TMP_TARGET" 2>/dev/null; then
                    chmod +x "$TMP_TARGET" 2>/dev/null
                    mv -f "$TMP_TARGET" "$TARGET_FILE" 2>/dev/null
                    chmod +x "$TARGET_FILE" 2>/dev/null
                else
                    chmod +x "$NEW_FILE"
                    FINAL_EXEC="$NEW_FILE"
                fi
            else
                USER_APP_DIR="$HOME/Applications"
                mkdir -p "$USER_APP_DIR" 2>/dev/null
                if [ -w "$USER_APP_DIR" ]; then
                    DEST="$USER_APP_DIR/$(basename "$TARGET_FILE")"
                    cp -f "$NEW_FILE" "$DEST" 2>/dev/null && chmod +x "$DEST" 2>/dev/null && FINAL_EXEC="$DEST"
                else
                    chmod +x "$NEW_FILE"
                    FINAL_EXEC="$NEW_FILE"
                fi
            fi

            # 3. CRUCIAL: Unset parent AppImage environment variables so child AppImage runs its own mount cleanly
            unset APPIMAGE
            unset APPDIR
            unset ARGV0
            unset OWD

            if [ -n "$LD_LIBRARY_PATH" ]; then
                CLEANED_LD="$(echo "$LD_LIBRARY_PATH" | tr ':' '\n' | grep -v '\.mount_' | tr '\n' ':' | sed 's/:$//')"
                export LD_LIBRARY_PATH="$CLEANED_LD"
            fi

            # 4. Launch the updated AppImage in the background, fully detached
            nohup "$FINAL_EXEC" >/dev/null 2>&1 &

            # 5. Cleanup temporary work directory
            sleep 3
            if [ -n "$WORK_DIR" ] && [ -d "$WORK_DIR" ] && [ "$FINAL_EXEC" != "$NEW_FILE" ]; then
                rm -rf "$WORK_DIR" 2>/dev/null
            fi
        """.trimIndent()

        scriptFile.writeText(shScript, Charsets.UTF_8)
        scriptFile.setExecutable(true, false)

        ProcessBuilder(
            "sh",
            scriptFile.absolutePath,
            currentPid.toString(),
            targetFile.absolutePath,
            targetAppImage.absolutePath,
            updateWorkDir.absolutePath
        ).start()

        kotlin.concurrent.thread {
            Thread.sleep(600)
            exitProcess(0)
        }
    }

    private fun applyLinuxPackageUpdate(packageFile: File, updateWorkDir: File) {
        val currentPid = ProcessHandle.current().pid()
        val currentExe = getLinuxExecutablePath() ?: ""
        val scriptFile = File(updateWorkDir, "update_package.sh")

        val shScript = $$"""
            #!/bin/sh
            OLD_PID="$1"
            PACKAGE_FILE="$2"
            APP_COMMAND="$3"
            WORK_DIR="$4"

            # 1. Wait for old process to exit
            if [ -n "$OLD_PID" ] && [ "$OLD_PID" -gt 0 ] 2>/dev/null; then
                while kill -0 "$OLD_PID" 2>/dev/null; do
                    sleep 0.2
                done
            fi
            sleep 0.6

            INSTALLED=0

            # 2. Try pkexec with system package manager for seamless GUI authentication
            if command -v pkexec >/dev/null 2>&1; then
                case "$PACKAGE_FILE" in
                    *.pkg.tar.*|*.pkg.tar.gz|*.pkg.tar.zst)
                        if command -v pacman >/dev/null 2>&1; then
                            pkexec pacman -U --noconfirm "$PACKAGE_FILE" && INSTALLED=1
                        fi
                        ;;
                    *.deb)
                        if command -v apt-get >/dev/null 2>&1; then
                            pkexec apt-get install -y --reinstall "$PACKAGE_FILE" && INSTALLED=1
                        elif command -v dpkg >/dev/null 2>&1; then
                            pkexec dpkg -i "$PACKAGE_FILE" && INSTALLED=1
                        fi
                        ;;
                    *.rpm)
                        if command -v dnf >/dev/null 2>&1; then
                            pkexec dnf reinstall -y "$PACKAGE_FILE" || pkexec dnf install -y "$PACKAGE_FILE" && INSTALLED=1
                        elif command -v rpm >/dev/null 2>&1; then
                            pkexec rpm -Uvh --replacepkgs "$PACKAGE_FILE" && INSTALLED=1
                        elif command -v zypper >/dev/null 2>&1; then
                            pkexec zypper --non-interactive install "$PACKAGE_FILE" && INSTALLED=1
                        fi
                        ;;
                esac
            fi

            # 3. If installed successfully, relaunch the app from system path
            if [ "$INSTALLED" -eq 1 ]; then
                sleep 1
                if [ -n "$APP_COMMAND" ] && [ -x "$APP_COMMAND" ]; then
                    nohup "$APP_COMMAND" >/dev/null 2>&1 &
                elif [ -x "/usr/bin/poskmp" ]; then
                    nohup "/usr/bin/poskmp" >/dev/null 2>&1 &
                elif [ -x "/usr/bin/punto-de-venta" ]; then
                    nohup "/usr/bin/punto-de-venta" >/dev/null 2>&1 &
                elif [ -x "/opt/poskmp/bin/Punto de Venta" ]; then
                    nohup "/opt/poskmp/bin/Punto de Venta" >/dev/null 2>&1 &
                elif [ -x "/opt/punto-de-venta/bin/Punto de Venta" ]; then
                    nohup "/opt/punto-de-venta/bin/Punto de Venta" >/dev/null 2>&1 &
                elif [ -x "/opt/poskmp/bin/PoSKMP" ]; then
                    nohup "/opt/poskmp/bin/PoSKMP" >/dev/null 2>&1 &
                elif command -v gtk-launch >/dev/null 2>&1; then
                    gtk-launch "poskmp" >/dev/null 2>&1 || gtk-launch "punto-de-venta" >/dev/null 2>&1 &
                fi

                # Cleanup work directory
                sleep 2
                if [ -n "$WORK_DIR" ] && [ -d "$WORK_DIR" ]; then
                    rm -rf "$WORK_DIR" 2>/dev/null
                fi
            else
                # Fallback to desktop GUI package installer
                if command -v pamac-installer >/dev/null 2>&1; then
                    pamac-installer "$PACKAGE_FILE"
                elif command -v gdebi-gtk >/dev/null 2>&1; then
                    gdebi-gtk "$PACKAGE_FILE"
                elif command -v xdg-open >/dev/null 2>&1; then
                    xdg-open "$PACKAGE_FILE"
                fi
            fi
        """.trimIndent()

        scriptFile.writeText(shScript, Charsets.UTF_8)
        scriptFile.setExecutable(true, false)

        ProcessBuilder(
            "sh",
            scriptFile.absolutePath,
            currentPid.toString(),
            packageFile.absolutePath,
            currentExe,
            updateWorkDir.absolutePath
        ).start()

        kotlin.concurrent.thread {
            Thread.sleep(600)
            exitProcess(0)
        }
    }
}
