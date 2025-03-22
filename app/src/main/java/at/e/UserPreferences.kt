package at.e

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object UserPreferences {
    private const val DATA_STORE_NAME = "user_preferences"

    private val Context.dataStore by preferencesDataStore(name = DATA_STORE_NAME)

    object Keys {
        val FindTablePreferredMethod = intPreferencesKey("find_table_preferred_method")
    }

    context(Context, CoroutineScope)
    fun <T> load(key: Preferences.Key<T>, defaultValue: T? = null, consumer: (T?) -> Unit) {
        launch {
            val value = dataStore.data.first()[key] ?: defaultValue
            @Suppress("UNCHECKED_CAST")
            consumer(value as T)
        }
    }

    context(Context, CoroutineScope)
    fun <T> save(key: Preferences.Key<T>, value: T) {
        launch {
            dataStore.edit { it[key] = value }
        }
    }
}
