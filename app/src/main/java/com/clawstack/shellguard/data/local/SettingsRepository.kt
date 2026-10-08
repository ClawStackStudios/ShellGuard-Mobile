package com.clawstack.shellguard.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clawstack.shellguard.crypto.LockTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shellguard_settings")

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColors: Boolean = false,
    val showFavicons: Boolean = true,
    val compactView: Boolean = false,
    val lockTimeout: LockTimeout = LockTimeout.FIVE_MINUTES,
    val allowScreenCapture: Boolean = false,
    val clipboardClearSeconds: Int = 30,
    val panicWipeCountdownSeconds: Int = 15,
    val autofillInlineChips: Boolean = true,
    val syncOverCellular: Boolean = true,
    val pullToRefreshEnabled: Boolean = true
)

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColors(enabled: Boolean)
    suspend fun setShowFavicons(enabled: Boolean)
    suspend fun setCompactView(enabled: Boolean)
    suspend fun setLockTimeout(timeout: LockTimeout)
    suspend fun setAllowScreenCapture(enabled: Boolean)
    suspend fun setClipboardClearSeconds(seconds: Int)
    suspend fun setPanicWipeCountdownSeconds(seconds: Int)
    suspend fun setAutofillInlineChips(enabled: Boolean)
    suspend fun setSyncOverCellular(enabled: Boolean)
    suspend fun setPullToRefreshEnabled(enabled: Boolean)
    suspend fun clearAll()
}

class SettingsRepositoryImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("pref_theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("pref_dynamic_colors")
        val SHOW_FAVICONS = booleanPreferencesKey("pref_show_favicons")
        val COMPACT_VIEW = booleanPreferencesKey("pref_compact_view")
        val LOCK_TIMEOUT = stringPreferencesKey("pref_lock_timeout")
        val ALLOW_SCREEN_CAPTURE = booleanPreferencesKey("pref_allow_screen_capture")
        val CLIPBOARD_CLEAR_SECONDS = intPreferencesKey("pref_clipboard_clear_seconds")
        val PANIC_WIPE_COUNTDOWN_SECONDS = intPreferencesKey("pref_panic_wipe_countdown_seconds")
        val AUTOFILL_INLINE_CHIPS = booleanPreferencesKey("pref_autofill_inline_chips")
        val SYNC_OVER_CELLULAR = booleanPreferencesKey("pref_sync_over_cellular")
        val PULL_TO_REFRESH_ENABLED = booleanPreferencesKey("pref_pull_to_refresh_enabled")
    }

    override val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeStr = preferences[PreferencesKeys.THEME_MODE]
            val themeMode = try {
                if (themeModeStr != null) ThemeMode.valueOf(themeModeStr) else ThemeMode.SYSTEM
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }

            val lockTimeoutStr = preferences[PreferencesKeys.LOCK_TIMEOUT]
            val lockTimeout = LockTimeout.fromName(lockTimeoutStr)

            AppSettings(
                themeMode = themeMode,
                dynamicColors = preferences[PreferencesKeys.DYNAMIC_COLORS] ?: false,
                showFavicons = preferences[PreferencesKeys.SHOW_FAVICONS] ?: true,
                compactView = preferences[PreferencesKeys.COMPACT_VIEW] ?: false,
                lockTimeout = lockTimeout,
                allowScreenCapture = preferences[PreferencesKeys.ALLOW_SCREEN_CAPTURE] ?: false,
                clipboardClearSeconds = preferences[PreferencesKeys.CLIPBOARD_CLEAR_SECONDS] ?: 30,
                panicWipeCountdownSeconds = preferences[PreferencesKeys.PANIC_WIPE_COUNTDOWN_SECONDS] ?: 15,
                autofillInlineChips = preferences[PreferencesKeys.AUTOFILL_INLINE_CHIPS] ?: true,
                syncOverCellular = preferences[PreferencesKeys.SYNC_OVER_CELLULAR] ?: true,
                pullToRefreshEnabled = preferences[PreferencesKeys.PULL_TO_REFRESH_ENABLED] ?: true
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DYNAMIC_COLORS] = enabled }
    }

    override suspend fun setShowFavicons(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_FAVICONS] = enabled }
    }

    override suspend fun setCompactView(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.COMPACT_VIEW] = enabled }
    }

    override suspend fun setLockTimeout(timeout: LockTimeout) {
        dataStore.edit { it[PreferencesKeys.LOCK_TIMEOUT] = timeout.name }
    }

    override suspend fun setAllowScreenCapture(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ALLOW_SCREEN_CAPTURE] = enabled }
    }

    override suspend fun setClipboardClearSeconds(seconds: Int) {
        dataStore.edit { it[PreferencesKeys.CLIPBOARD_CLEAR_SECONDS] = seconds }
    }

    override suspend fun setPanicWipeCountdownSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(5, 60)
        dataStore.edit { it[PreferencesKeys.PANIC_WIPE_COUNTDOWN_SECONDS] = clamped }
    }

    override suspend fun setAutofillInlineChips(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTOFILL_INLINE_CHIPS] = enabled }
    }

    override suspend fun setSyncOverCellular(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SYNC_OVER_CELLULAR] = enabled }
    }

    override suspend fun setPullToRefreshEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.PULL_TO_REFRESH_ENABLED] = enabled }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
