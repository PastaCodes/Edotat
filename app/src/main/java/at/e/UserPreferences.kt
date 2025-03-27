package at.e

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import at.e.ui.home.FindTable
import kotlinx.coroutines.flow.map

class UserPreferences(private val dataStore: DataStore<Preferences>) {
    companion object {
        private const val DATA_STORE_NAME = "user_preferences"
        val Context.dataStore by preferencesDataStore(name = DATA_STORE_NAME)
    }

    object Keys {
        val AuthToken = stringPreferencesKey("auth_token")
        val AuthTokenExpiration = longPreferencesKey("auth_token_expiration")
        val AutoLoginRequireBiometrics = booleanPreferencesKey("auto_login_require_biometrics")
        val FindTablePreferredMethod = intPreferencesKey("find_table_preferred_method")
    }

    val authToken =
        dataStore.data.map { it[Keys.AuthToken] }

    val authTokenExpiration =
        dataStore.data.map { it[Keys.AuthTokenExpiration] }

    val findTableMethodPreference =
        dataStore.data.map { it[Keys.FindTablePreferredMethod] ?: FindTable.Method.NO_PREFERENCE }

    val autoLoginRequireBiometrics =
        dataStore.data.map { it[Keys.AutoLoginRequireBiometrics] ?: false }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    suspend fun <T> delete(key: Preferences.Key<T>) {
        dataStore.edit { it.remove(key) }
    }
}
