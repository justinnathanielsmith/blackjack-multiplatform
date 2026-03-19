package io.github.smithjustinn.blackjack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions

private val dataStoreInstance: DataStore<Preferences> by lazy {
    val dirPath = Paths.get(System.getProperty("user.home"), ".blackjack")
    try {
        val perms = PosixFilePermissions.fromString("rwx------")
        val attr = PosixFilePermissions.asFileAttribute(perms)
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath, attr)
        }
    } catch (e: UnsupportedOperationException) {
        // Fallback for non-POSIX filesystems like Windows
        val dir = dirPath.toFile()
        if (!dir.exists()) {
            dir.mkdirs()
            dir.setReadable(false, false)
            dir.setWritable(false, false)
            dir.setExecutable(false, false)
            dir.setReadable(true, true)
            dir.setWritable(true, true)
            dir.setExecutable(true, true)
        }
    }

    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val file = dirPath.resolve(DATASTORE_FILE_NAME)
            try {
                val filePerms = PosixFilePermissions.fromString("rw-------")
                val fileAttr = PosixFilePermissions.asFileAttribute(filePerms)
                if (!Files.exists(file)) {
                    Files.createFile(file, fileAttr)
                }
            } catch (e: UnsupportedOperationException) {
                // Fallback for non-POSIX filesystems
                val fileObj = file.toFile()
                if (!fileObj.exists()) {
                    fileObj.createNewFile()
                    fileObj.setReadable(false, false)
                    fileObj.setWritable(false, false)
                    fileObj.setExecutable(false, false)
                    fileObj.setReadable(true, true)
                    fileObj.setWritable(true, true)
                }
            }
            file.toAbsolutePath().toString().toPath()
        }
    )
}

actual fun createDataStore(): DataStore<Preferences> = dataStoreInstance
