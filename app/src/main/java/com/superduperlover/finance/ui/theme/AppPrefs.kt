package com.superduperlover.finance.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.superduperlover.finance.core.Currency

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Wraps SharedPreferences for user-selected theme and currency preferences. */
class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = mutableStateOf(readThemeMode())
    var themeMode: ThemeMode
        get() = _themeMode.value
        set(value) {
            _themeMode.value = value
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    private val _summaryCurrency = mutableStateOf<Currency?>(readSummaryCurrency())
    var summaryCurrency: Currency?
        get() = _summaryCurrency.value
        set(value) {
            _summaryCurrency.value = value
            if (value == null) {
                prefs.edit().remove(KEY_SUMMARY_CURRENCY).apply()
            } else {
                prefs.edit().putString(KEY_SUMMARY_CURRENCY, value.name).apply()
            }
        }

    private fun readThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private fun readSummaryCurrency(): Currency? {
        val stored = prefs.getString(KEY_SUMMARY_CURRENCY, null) ?: return null
        return runCatching { Currency.valueOf(stored) }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "chili_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SUMMARY_CURRENCY = "summary_currency"
    }
}

val LocalAppPrefs = staticCompositionLocalOf<AppPrefs> { error("AppPrefs not provided") }
