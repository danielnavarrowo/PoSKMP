package com.dnavarro.poskmp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

private val singletonDataStore: DataStore<Preferences> by lazy {
    val userHome = System.getProperty("user.home") ?: "."
    val appDir = File(userHome, ".poskmp").apply { if (!exists()) mkdirs() }
    createDataStore(
        producePath = {
            File(appDir, DATASTORE_FILE_NAME).absolutePath.toPath()
        }
    )
}

actual fun getDataStore(): DataStore<Preferences> = singletonDataStore
