package com.dnavarro.poskmp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import okio.Path.Companion.toPath

actual fun getDataStore(): DataStore<Preferences> {
    val context = DatabaseDriverFactory.appContext
        ?: throw IllegalStateException("DatabaseDriverFactory.appContext must be initialized on Android before accessing DataStore")
    return createDataStore(
        producePath = {
            context.filesDir.resolve(DATASTORE_FILE_NAME).absolutePath.toPath()
        }
    )
}
