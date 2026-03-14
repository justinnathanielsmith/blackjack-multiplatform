package io.github.smithjustinn.blackjack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

internal const val DATASTORE_FILE_NAME = "blackjack.preferences_pb"

expect fun createDataStore(): DataStore<Preferences>
