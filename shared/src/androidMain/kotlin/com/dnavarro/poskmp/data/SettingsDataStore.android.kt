package com.dnavarro.poskmp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import okio.Path.Companion.toPath

private val singletonDataStore: DataStore<Preferences> by lazy {
    val context = DatabaseDriverFactory.appContext
        ?: throw IllegalStateException("DatabaseDriverFactory.appContext must be initialized on Android before accessing DataStore")
    createDataStore(
        producePath = {
            context.filesDir.resolve(DATASTORE_FILE_NAME).absolutePath.toPath()
        }
    )
}

actual fun getDataStore(): DataStore<Preferences> = singletonDataStore
