package at.e

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

object PreferenceKeys {
    val FindTablePreferredMethod = intPreferencesKey("find_table_preferred_method")
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

suspend fun <T> Context.loadPreference(key: Preferences.Key<T>, defaultValue: T? = null, consumer: (T?) -> Unit) {
    dataStore.data.map { it[key] ?: defaultValue }.collect(consumer)
}
