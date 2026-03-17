package io.github.smithjustinn.blackjack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

private val dataStoreInstance: DataStore<Preferences> by lazy {
    val dir =
        File(System.getProperty("user.home"), ".blackjack").apply {
            mkdirs()
            // Secure directory permissions (owner only)
            setReadable(false, false)
            setWritable(false, false)
            setExecutable(false, false)
            setReadable(true, true)
            setWritable(true, true)
            setExecutable(true, true)
        }
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val file = File(dir, DATASTORE_FILE_NAME)
            // Secure file permissions (owner only) if it exists/when created
            if (!file.exists()) {
                file.createNewFile()
                file.setReadable(false, false)
                file.setWritable(false, false)
                file.setExecutable(false, false)
                file.setReadable(true, true)
                file.setWritable(true, true)
            }
            file.absolutePath.toPath()
        }
    )
}

actual fun createDataStore(): DataStore<Preferences> = dataStoreInstance
