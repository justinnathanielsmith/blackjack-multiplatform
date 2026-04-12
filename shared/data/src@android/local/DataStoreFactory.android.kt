package io.github.smithjustinn.blackjack.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile

private var applicationContext: Context? = null

fun initDataStore(context: Context) {
    applicationContext = context.applicationContext
}

private val dataStoreInstance: DataStore<Preferences> by lazy {
    val context =
        checkNotNull(applicationContext) {
            "Call initDataStore(context) before createDataStore()"
        }
    PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("blackjack") }
    )
}

actual fun createDataStore(): DataStore<Preferences> = dataStoreInstance
