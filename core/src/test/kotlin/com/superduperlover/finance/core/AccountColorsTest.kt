package com.superduperlover.finance.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountColorsTest {
    @Test
    fun defaultColourFollowsType() {
        assertEquals("emerald", defaultAccountColorKey(AccountType.CASH))
        assertEquals("sky", defaultAccountColorKey(AccountType.BANK))
        assertEquals("indigo", defaultAccountColorKey(AccountType.CARD))
        assertEquals("violet", defaultAccountColorKey(AccountType.EWALLET))
        assertTrue(AccountType.entries.all { defaultAccountColorKey(it) in ACCOUNT_COLOR_KEYS })
    }

    @Test
    fun newAccountGetsDefaultColourOfItsType() {
        assertEquals("sky", Account(1, "Bank", AccountType.BANK, 0).colorKey)
        assertEquals("rose", Account(1, "Bank", AccountType.BANK, 0, colorKey = "rose").colorKey)
    }

    @Test
    fun backupValueIsKeptWhenValid() {
        assertEquals("amber", accountColorKeyOrDefault("amber", AccountType.CARD))
    }

    @Test
    fun backupValueMissingOrBadFallsBackToDefault() {
        assertEquals("indigo", accountColorKeyOrDefault(null, AccountType.CARD))
        assertEquals("indigo", accountColorKeyOrDefault("pink", AccountType.CARD))
        assertEquals("indigo", accountColorKeyOrDefault(5L, AccountType.CARD))
    }
}
