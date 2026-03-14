package io.github.smithjustinn.blackjack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private val dataStoreInstance: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val docDir =
                NSFileManager.defaultManager
                    .URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)!!
                    .path!!
            "$docDir/$DATASTORE_FILE_NAME".toPath()
        }
    )
}

actual fun createDataStore(): DataStore<Preferences> = dataStoreInstance
