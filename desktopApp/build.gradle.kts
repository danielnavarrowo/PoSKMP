import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.components.resources)
}

val appVersion = libs.versions.app.version.get()

val commonJvmArgs = listOf(
    "-Dapp.version=$appVersion",
    "-XX:+UseParallelGC",
    "-XX:CICompilerCount=2",
    "-Xms256m",
    "-Xmx768m"
)

compose.desktop {
    application {
        mainClass = "com.dnavarro.poskmp.MainKt"
        jvmArgs += commonJvmArgs

        buildTypes.release.proguard {
            isEnabled.set(false)
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules("java.desktop", "java.instrument", "java.sql", "jdk.unsupported")
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage, TargetFormat.Msi)
            packageName = "poskmp"
            packageVersion = appVersion
            jvmArgs += commonJvmArgs
            windows {
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
                perUserInstall = true
                menu = true
                shortcut = true
                dirChooser = true
                menuGroup = "Punto de Venta"
                upgradeUuid = "d7b2a9e1-6c3f-4b8a-9e12-3456789abcde"
            }
            linux {
                iconFile.set(project.file("src/main/resources/icons/icon.png"))
                shortcut = true
                menuGroup = "Office"
            }
        }
    }
}

val packagePkgTarGz = tasks.register("packagePkgTarGz") {
    group = "compose desktop"
    description = "Packages the desktop application as an Arch Linux .pkg.tar.gz package"
    dependsOn("createReleaseDistributable")

    val archOutputDir = layout.buildDirectory.dir("compose/binaries/main-release/arch")
    val appDistDir = layout.buildDirectory.dir("compose/binaries/main-release/app/poskmp")
    val packagingDir = rootProject.layout.projectDirectory.dir("packaging/archlinux")
    val artifactsDir = rootProject.layout.projectDirectory.dir("artifacts/desktop-linux")
    val iconFile = layout.projectDirectory.file("src/main/resources/icons/icon.png")
    val currentAppVersion = appVersion

    outputs.dir(archOutputDir)

    doLast {
        val appDir = appDistDir.get().asFile
        if (!appDir.exists()) {
            throw GradleException("Application distributable not found at: ${appDir.absolutePath}")
        }
        val outDir = archOutputDir.get().asFile
        outDir.mkdirs()

        val packagingDirFile = packagingDir.asFile
        val isMakepkgAvailable = try {
            val process = ProcessBuilder("which", "makepkg").start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }

        val pkgFileName = "poskmp-$currentAppVersion-1-x86_64.pkg.tar.gz"
        val targetPkgFile = File(outDir, pkgFileName)

        if (isMakepkgAvailable && packagingDirFile.exists()) {
            println("Building package with makepkg...")
            val pkgbuildFile = File(packagingDirFile, "PKGBUILD")
            if (pkgbuildFile.exists()) {
                val updatedContent = pkgbuildFile.readText()
                    .replace(Regex("pkgver=.*"), "pkgver=$currentAppVersion")
                pkgbuildFile.writeText(updatedContent)
            }
            val pb = ProcessBuilder("makepkg", "-f", "--nodeps")
                .directory(packagingDirFile)
                .redirectErrorStream(true)
            pb.environment()["PKGEXT"] = ".pkg.tar.gz"
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                throw GradleException("makepkg failed with exit code $exitCode:\n$output")
            }
            val generatedFile = packagingDirFile.listFiles()?.firstOrNull {
                it.name.startsWith("poskmp-$currentAppVersion-") && it.name.endsWith(".pkg.tar.gz")
            }
            if (generatedFile != null && generatedFile.exists()) {
                generatedFile.copyTo(targetPkgFile, overwrite = true)
                generatedFile.delete()
                File(packagingDirFile, "src").deleteRecursively()
                File(packagingDirFile, "pkg").deleteRecursively()
            } else {
                throw GradleException("Expected package file not found in ${packagingDirFile.absolutePath}")
            }
        } else {
            println("makepkg not found, building package with tar and .PKGINFO fallback...")
            val stagingDir = File(outDir, "tmp-staging")
            stagingDir.deleteRecursively()
            val pkgRoot = File(stagingDir, "pkg")
            pkgRoot.mkdirs()

            // /opt/poskmp
            val optDir = File(pkgRoot, "opt/poskmp")
            optDir.mkdirs()
            val hasCp = try { ProcessBuilder("which", "cp").start().waitFor() == 0 } catch (_: Exception) { false }
            if (hasCp) {
                ProcessBuilder("cp", "-a", "${appDir.absolutePath}/.", optDir.absolutePath).start().waitFor()
            } else {
                appDir.copyRecursively(optDir, overwrite = true)
            }

            // Ensure directories are 755 and executables have +x permissions preserved
            try { ProcessBuilder("chmod", "-R", "a+rX", pkgRoot.absolutePath).start().waitFor() } catch (_: Exception) {}
            File(optDir, "bin").walkTopDown().forEach {
                if (it.isFile) {
                    it.setExecutable(true, false)
                    try { ProcessBuilder("chmod", "755", it.absolutePath).start().waitFor() } catch (_: Exception) {}
                }
            }
            File(optDir, "lib").listFiles()?.filter { it.extension == "so" }?.forEach {
                it.setExecutable(true, false)
                try { ProcessBuilder("chmod", "755", it.absolutePath).start().waitFor() } catch (_: Exception) {}
            }
            listOf("jspawnhelper", "jexec").forEach { binaryName ->
                val f = File(optDir, "lib/runtime/lib/$binaryName")
                if (f.exists()) {
                    f.setExecutable(true, false)
                    try { ProcessBuilder("chmod", "755", f.absolutePath).start().waitFor() } catch (_: Exception) {}
                }
            }

            // /usr/bin launchers
            val binDir = File(pkgRoot, "usr/bin")
            binDir.mkdirs()
            val launcherScript = File(binDir, "poskmp")
            launcherScript.writeText("#!/bin/sh\nexec \"/opt/poskmp/bin/poskmp\" \"$@\"\n")
            launcherScript.setExecutable(true, false)
            try { ProcessBuilder("chmod", "755", launcherScript.absolutePath).start().waitFor() } catch (_: Exception) {}

            try { ProcessBuilder("ln", "-sf", "poskmp", File(binDir, "punto-de-venta").absolutePath).start().waitFor() } catch (_: Exception) {}

            // Desktop entry - user sees "Punto de Venta" in desktop launcher and menu
            val appsDir = File(pkgRoot, "usr/share/applications")
            appsDir.mkdirs()
            File(appsDir, "poskmp.desktop").writeText(
                """[Desktop Entry]
Type=Application
Name=Punto de Venta
GenericName=Sistema Punto de Venta
Comment=PoSKMP - Sistema Punto de Venta
Exec=/usr/bin/poskmp
Icon=poskmp
Terminal=false
Categories=Office;Utility;
StartupWMClass=poskmp
""".trimIndent()
            )

            // Icons
            val iconSrc = iconFile.asFile
            if (iconSrc.exists()) {
                val icon512Dir = File(pkgRoot, "usr/share/icons/hicolor/512x512/apps")
                icon512Dir.mkdirs()
                iconSrc.copyTo(File(icon512Dir, "poskmp.png"), overwrite = true)

                val pixmapDir = File(pkgRoot, "usr/share/pixmaps")
                pixmapDir.mkdirs()
                iconSrc.copyTo(File(pixmapDir, "poskmp.png"), overwrite = true)
            }

            // .PKGINFO
            val totalSize = pkgRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val buildDate = System.currentTimeMillis() / 1000
            File(pkgRoot, ".PKGINFO").writeText(
                """# Generated by PoSKMP Gradle build
pkgname = poskmp
pkgbase = poskmp
pkgver = $currentAppVersion-1
pkgdesc = Punto de Venta - Sistema Punto de Venta (Compose Multiplatform)
url = https://github.com/daniel-navarro-pos/poskmp
builddate = $buildDate
packager = PoSKMP Build System
size = $totalSize
arch = x86_64
license = custom
depend = glibc
depend = hicolor-icon-theme
""".trimIndent() + "\n"
            )

            val topLevelEntries = (pkgRoot.list() ?: emptyArray<String>())
                .sortedWith { a, b ->
                    if (a == ".PKGINFO") -1 else if (b == ".PKGINFO") 1 else a.compareTo(b)
                }

            val tarCmd = mutableListOf("tar", "-czf", targetPkgFile.absolutePath, "--owner=0", "--group=0")
            tarCmd.addAll(topLevelEntries)

            val pb = ProcessBuilder(tarCmd)
                .directory(pkgRoot)
                .redirectErrorStream(true)
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                throw GradleException("tar packaging failed with exit code $exitCode:\n$output")
            }
            stagingDir.deleteRecursively()
        }

        val artifactsDirFile = artifactsDir.asFile
        artifactsDirFile.mkdirs()
        targetPkgFile.copyTo(File(artifactsDirFile, pkgFileName), overwrite = true)

        println("Package generated successfully:")
        println("  -> ${targetPkgFile.absolutePath}")
        println("  -> ${File(artifactsDirFile, pkgFileName).absolutePath}")
    }
}

tasks.register("packageArch") {
    group = "compose desktop"
    description = "Alias for packagePkgTarGz"
    dependsOn(packagePkgTarGz)
}