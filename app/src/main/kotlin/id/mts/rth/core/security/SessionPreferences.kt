package id.mts.rth.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "simrth_session")

/**
 * Model status sesi pengguna di memori.
 */
data class UserSession(
    val serverUrl: String,
    val authToken: String?,
    val userId: String?,
    val userName: String?,
    val activeRole: String,
    val pinHash: String?,
    val pinSalt: String?,
    val failedPinCount: Int,
    val lastSyncTimestamp: Long
) {
    val isLoggedIn: Boolean get() = !authToken.isNullOrBlank()
    val hasPinConfigured: Boolean get() = !pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()
    val isLockedOut: Boolean get() = failedPinCount >= SessionPreferences.MAX_FAILED_PIN_ATTEMPTS
}

/**
 * Penyimpanan sesi dan preferensi keamanan berbasis Jetpack DataStore<Preferences>.
 */
class SessionPreferences(private val context: Context) {

    companion object {
        const val DEFAULT_SERVER_URL = "http://simrth.local:8090"
        const val MAX_FAILED_PIN_ATTEMPTS = 5

        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_ACTIVE_ROLE = stringPreferencesKey("active_role")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_FAILED_PIN_COUNT = intPreferencesKey("failed_pin_count")
        private val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val sessionFlow: Flow<UserSession> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserSession(
                serverUrl = prefs[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL,
                authToken = prefs[KEY_AUTH_TOKEN],
                userId = prefs[KEY_USER_ID],
                userName = prefs[KEY_USER_NAME],
                activeRole = prefs[KEY_ACTIVE_ROLE] ?: "GURU",
                pinHash = prefs[KEY_PIN_HASH],
                pinSalt = prefs[KEY_PIN_SALT],
                failedPinCount = prefs[KEY_FAILED_PIN_COUNT] ?: 0,
                lastSyncTimestamp = prefs[KEY_LAST_SYNC_TIMESTAMP] ?: 0L
            )
        }

    suspend fun saveServerUrl(url: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = url
        }
    }

    suspend fun saveLoginSession(
        token: String,
        userId: String,
        userName: String,
        role: String,
        serverUrl: String = DEFAULT_SERVER_URL
    ) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_NAME] = userName
            prefs[KEY_ACTIVE_ROLE] = role
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_FAILED_PIN_COUNT] = 0
        }
    }

    suspend fun savePin(pinHash: String, pinSalt: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = pinHash
            prefs[KEY_PIN_SALT] = pinSalt
            prefs[KEY_FAILED_PIN_COUNT] = 0
        }
    }

    /**
     * Mencatat percobaan PIN yang salah.
     * Jika mencapai 5 kali berturut-turut, sesi login dihapus untuk mencegah brute-force.
     */
    suspend fun recordFailedPinAttempt(): Int {
        var newCount = 0
        context.sessionDataStore.edit { prefs ->
            val current = prefs[KEY_FAILED_PIN_COUNT] ?: 0
            newCount = current + 1
            prefs[KEY_FAILED_PIN_COUNT] = newCount

            if (newCount >= MAX_FAILED_PIN_ATTEMPTS) {
                // Wipe sensitive session token
                prefs.remove(KEY_AUTH_TOKEN)
                prefs.remove(KEY_PIN_HASH)
                prefs.remove(KEY_PIN_SALT)
            }
        }
        return newCount
    }

    suspend fun resetFailedPinAttempts() {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_FAILED_PIN_COUNT] = 0
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            val serverUrl = prefs[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL
            prefs.clear()
            prefs[KEY_SERVER_URL] = serverUrl
        }
    }
}
