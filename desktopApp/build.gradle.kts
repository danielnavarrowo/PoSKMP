import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
            packageName = "Punto de Venta"
            packageVersion = appVersion
            jvmArgs += commonJvmArgs
            windows {
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
                perUserInstall = true
                menu = true
                shortcut = true
                dirChooser = true
                menuGroup = "PoSKMP"
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
    val appDistDir = layout.buildDirectory.dir("compose/binaries/main-release/app/Punto de Venta")
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
        val altName = "punto-de-venta-$currentAppVersion-1-x86_64.pkg.tar.gz"
        val targetPkgFile = File(outDir, pkgFileName)

        if (isMakepkgAvailable && packagingDirFile.exists()) {
            println("Building package with makepkg...")
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
            val generatedFile = File(packagingDirFile, pkgFileName)
            if (generatedFile.exists()) {
                generatedFile.copyTo(targetPkgFile, overwrite = true)
                File(packagingDirFile, "src").deleteRecursively()
                File(packagingDirFile, "pkg").deleteRecursively()
            } else {
                throw GradleException("Expected package file not found: ${generatedFile.absolutePath}")
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
            appDir.copyRecursively(optDir, overwrite = true)

            // /usr/bin launchers
            val binDir = File(pkgRoot, "usr/bin")
            binDir.mkdirs()
            val launcherScript = File(binDir, "poskmp")
            launcherScript.writeText("#!/bin/sh\nexec \"/opt/poskmp/bin/Punto de Venta\" \"\$@\"\n")
            launcherScript.setExecutable(true, false)

            val altLauncher = File(binDir, "punto-de-venta")
            altLauncher.writeText("#!/bin/sh\nexec \"/opt/poskmp/bin/Punto de Venta\" \"\$@\"\n")
            altLauncher.setExecutable(true, false)

            // Desktop entry
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
StartupWMClass=Punto de Venta
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
            val totalSize = pkgRoot.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            val buildDate = System.currentTimeMillis() / 1000
            File(pkgRoot, ".PKGINFO").writeText(
                """# Generated by PoSKMP Gradle build
pkgname = poskmp
pkgbase = poskmp
pkgver = $currentAppVersion-1
pkgdesc = PoSKMP - Sistema Punto de Venta (Compose Multiplatform)
url = https://github.com/daniel-navarro-pos/poskmp
builddate = $buildDate
packager = PoSKMP Build System
size = $totalSize
arch = x86_64
license = custom
depend = glibc
depend = hicolor-icon-theme
provides = punto-de-venta
conflicts = punto-de-venta
""".trimIndent() + "\n"
            )

            val pb = ProcessBuilder("tar", "-czf", targetPkgFile.absolutePath, "--owner=0", "--group=0", ".")
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

        // Alternate name copy
        targetPkgFile.copyTo(File(outDir, altName), overwrite = true)
        targetPkgFile.copyTo(File(artifactsDirFile, altName), overwrite = true)

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