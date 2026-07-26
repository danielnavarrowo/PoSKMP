package com.dnavarro.poskmp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

actual fun getDataStore(): DataStore<Preferences> {
    val userHome = System.getProperty("user.home") ?: "."
    val appDir = File(userHome, ".poskmp").apply { if (!exists()) mkdirs() }
    return createDataStore(
        producePath = {
            File(appDir, DATASTORE_FILE_NAME).absolutePath.toPath()
        }
    )
}
