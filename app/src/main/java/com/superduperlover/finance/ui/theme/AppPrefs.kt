package com.superduperlover.finance.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.TxnKind
import java.time.LocalDate

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

    /**
     * Account used by the last saved new transaction of [kind] (for a transfer: the source account), or null.
     * The caller checks that it still exists and is not archived.
     */
    fun lastAccount(kind: TxnKind): Long? = prefs.getLong(lastAccountKey(kind), NONE).takeIf { it != NONE }

    /** Target account of the last saved transfer, or null. */
    fun lastToAccount(): Long? = prefs.getLong(KEY_LAST_TO_ACCOUNT, NONE).takeIf { it != NONE }

    fun rememberAccount(kind: TxnKind, accountId: Long, toAccountId: Long? = null) {
        prefs.edit().apply {
            putLong(lastAccountKey(kind), accountId)
            if (kind == TxnKind.TRANSFER && toAccountId != null) putLong(KEY_LAST_TO_ACCOUNT, toAccountId)
        }.apply()
    }

    private val _lastBackupEpochDay = mutableStateOf(prefs.getLong(KEY_LAST_BACKUP, NONE))
    private val _txnsSinceBackup = mutableStateOf(prefs.getInt(KEY_TXNS_SINCE_BACKUP, 0))

    /** Day of the last successful export, or null if never. Observable: Home recomposes when it changes. */
    val lastBackupAt: LocalDate?
        get() = _lastBackupEpochDay.value.takeIf { it != NONE }?.let { LocalDate.ofEpochDay(it) }

    /** Transactions saved (new or edited) since the last successful export. Observable. */
    val txnsSinceBackup: Int get() = _txnsSinceBackup.value

    fun noteTxnSaved() {
        _txnsSinceBackup.value += 1
        prefs.edit().putInt(KEY_TXNS_SINCE_BACKUP, _txnsSinceBackup.value).apply()
    }

    /** Call after an export has been written successfully. */
    fun markBackedUp(today: LocalDate = LocalDate.now()) {
        _lastBackupEpochDay.value = today.toEpochDay()
        _txnsSinceBackup.value = 0
        prefs.edit().putLong(KEY_LAST_BACKUP, today.toEpochDay()).putInt(KEY_TXNS_SINCE_BACKUP, 0).apply()
    }

    private fun lastAccountKey(kind: TxnKind) = "last_account_${kind.name.lowercase()}"

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
        private const val KEY_LAST_TO_ACCOUNT = "last_to_account_transfer"
        private const val KEY_LAST_BACKUP = "last_backup_epoch_day"
        private const val KEY_TXNS_SINCE_BACKUP = "txns_since_backup"
        private const val NONE = -1L
    }
}

val LocalAppPrefs = staticCompositionLocalOf<AppPrefs> { error("AppPrefs not provided") }
