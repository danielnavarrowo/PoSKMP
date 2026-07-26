package com.dnavarro.poskmp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path

internal const val DATASTORE_FILE_NAME = "poskmp_settings.preferences_pb"

fun createDataStore(producePath: () -> Path): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath() }
    )

expect fun getDataStore(): DataStore<Preferences>
