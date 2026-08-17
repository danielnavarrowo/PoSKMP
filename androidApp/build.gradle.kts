import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.dnavarro.poskmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.dnavarro.poskmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.app.versionCode.get().toInt()
        versionName = libs.versions.app.version.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    val localProps = Properties().apply {
        val propsFile = project.rootProject.file("local.properties")
        if (propsFile.exists()) {
            FileInputStream(propsFile).use { load(it) }
        }
    }

    val releaseKeystoreFile = System.getenv("KEYSTORE_PATH")?.let { file(it) }
        ?: project.rootProject.file("poskmp-release.jks").takeIf { it.exists() }
        ?: file("poskmp-release.jks").takeIf { it.exists() }

    val storePassword = System.getenv("KEYSTORE_PASSWORD")
        ?: localProps.getProperty("KEYSTORE_PASSWORD")
        ?: project.findProperty("KEYSTORE_PASSWORD") as? String

    val keyAlias = System.getenv("KEY_ALIAS")
        ?: localProps.getProperty("KEY_ALIAS")
        ?: project.findProperty("KEY_ALIAS") as? String

    val keyPassword = System.getenv("KEY_PASSWORD")
        ?: localProps.getProperty("KEY_PASSWORD")
        ?: project.findProperty("KEY_PASSWORD") as? String

    signingConfigs {
        create("release") {
            if (releaseKeystoreFile != null && releaseKeystoreFile.exists() && !storePassword.isNullOrBlank()) {
                storeFile = releaseKeystoreFile
                this.storePassword = storePassword
                this.keyAlias = keyAlias ?: "poskmp-key"
                this.keyPassword = keyPassword ?: storePassword
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt")
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}