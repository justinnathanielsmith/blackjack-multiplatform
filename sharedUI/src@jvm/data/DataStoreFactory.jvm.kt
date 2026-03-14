package io.github.smithjustinn.blackjack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

private val dataStoreInstance: DataStore<Preferences> by lazy {
    val dir = File(System.getProperty("user.home"), ".blackjack").apply { mkdirs() }
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { File(dir, DATASTORE_FILE_NAME).absolutePath.toPath() }
    )
}

actual fun createDataStore(): DataStore<Preferences> = dataStoreInstance
