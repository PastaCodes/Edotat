package at.e

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LoadPreference {
    private const val PREFERENCES_FILE_KEY = "at.e.PREFERENCE_FILE_KEY"

    context(Context)
    private suspend fun <T> base(key: String, defaultValue: T?, consumer: (T?) -> Unit, method: SharedPreferences.(String, T?) -> T?) {
        withContext(Dispatchers.IO) {
            val preferences = getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE)
            consumer(preferences.method(key, defaultValue))
        }
    }

    context(Context)
    suspend fun string(key: String, defaultValue: String? = null, consumer: (String?) -> Unit) =
        base(key, defaultValue, consumer, SharedPreferences::getString)
}
