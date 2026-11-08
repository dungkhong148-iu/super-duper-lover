package com.superduperlover.finance.core

/** The 8 account colours a user can pick. The hex values live in the UI theme (docs/ui-layout.md section 3). */
val ACCOUNT_COLOR_KEYS = listOf("emerald", "teal", "sky", "indigo", "violet", "rose", "amber", "slate")

/** Colour an account gets when the user has not picked one. */
fun defaultAccountColorKey(type: AccountType): String = when (type) {
    AccountType.CASH -> "emerald"
    AccountType.BANK -> "sky"
    AccountType.CARD -> "indigo"
    AccountType.EWALLET -> "violet"
}

/** [raw] if it is one of [ACCOUNT_COLOR_KEYS], otherwise the default for [type] (missing, null, wrong type or unknown key). */
fun accountColorKeyOrDefault(raw: Any?, type: AccountType): String =
    (raw as? String)?.takeIf { it in ACCOUNT_COLOR_KEYS } ?: defaultAccountColorKey(type)
