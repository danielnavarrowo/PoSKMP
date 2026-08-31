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

compose.desktop {
    application {
        mainClass = "com.dnavarro.poskmp.MainKt"
        jvmArgs += listOf(
            "-Dapp.version=$appVersion",
            "-XX:+TieredCompilation"
        )

        buildTypes.release.proguard {
            isEnabled.set(false)
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules("java.desktop", "java.instrument", "java.sql", "jdk.unsupported")
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage, TargetFormat.Msi)
            packageName = "Punto de Venta"
            packageVersion = appVersion
            jvmArgs += listOf(
                "-Dapp.version=$appVersion"
            )
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