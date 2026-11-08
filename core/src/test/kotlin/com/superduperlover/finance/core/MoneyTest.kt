package com.superduperlover.finance.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    private val vnd = Currency.VND
    private val twd = Currency.TWD
    private val usd = Currency.USD

    @Test
    fun vndAcceptsPlainAndGroupedNumbers() {
        assertEquals(1250000L, parseAmount("1250000", vnd))
        assertEquals(1250000L, parseAmount("1.250.000", vnd))
        assertEquals(1250000L, parseAmount("1,250,000", vnd))
        assertEquals(1250000L, parseAmount("1 250 000", vnd))
    }

    @Test
    fun vndAcceptsKSuffix() {
        assertEquals(50000L, parseAmount("50k", vnd))
        assertEquals(50000L, parseAmount("50K", vnd))
    }

    @Test
    fun vndRejectsBadInput() {
        assertNull(parseAmount("", vnd))
        assertNull(parseAmount("abc", vnd))
        assertNull(parseAmount("0", vnd))
        assertNull(parseAmount("-5", vnd))
        assertNull(parseAmount("1.5", vnd))
        assertNull(parseAmount("12.50.000", vnd))
    }

    @Test
    fun twdUsesCommaOrSpaceGroupsAndNoDecimals() {
        assertEquals(1250L, parseAmount("1,250", twd))
        assertEquals(1250L, parseAmount("1 250", twd))
        assertEquals(1250L, parseAmount("1250", twd))
        assertEquals(5000L, parseAmount("5k", twd))
        assertNull(parseAmount("12.5", twd))
        assertNull(parseAmount("1.250", twd))
        assertNull(parseAmount("0", twd))
    }

    @Test
    fun usdKeepsCentsInMinorUnits() {
        assertEquals(1250L, parseAmount("12.50", usd))
        assertEquals(1250L, parseAmount("12.5", usd))
        assertEquals(1200L, parseAmount("12", usd))
        assertEquals(125075L, parseAmount("1,250.75", usd))
        assertEquals(150000L, parseAmount("1.5k", usd))
        assertNull(parseAmount("12.345", usd))
        assertNull(parseAmount("0.00", usd))
        assertNull(parseAmount("", usd))
        assertNull(parseAmount("-1", usd))
        assertNull(parseAmount("1.250.000", usd))
    }

    @Test
    fun formatPerCurrency() {
        assertEquals("1.250.000 ₫", formatMoney(1250000, vnd))
        assertEquals("0 ₫", formatMoney(0, vnd))
        assertEquals("-50.000 ₫", formatMoney(-50000, vnd))
        assertEquals("NT$1,250", formatMoney(1250, twd))
        assertEquals("-NT$50", formatMoney(-50, twd))
        assertEquals("$1,250.75", formatMoney(125075, usd))
        assertEquals("$0.05", formatMoney(5, usd))
        assertEquals("$0.00", formatMoney(0, usd))
        assertEquals("-$12.50", formatMoney(-1250, usd))
    }

    @Test
    fun currencyFromBackupDefaultsToVnd() {
        assertEquals(Currency.USD, currencyFromBackup("USD"))
        assertEquals(Currency.VND, currencyFromBackup(null))
        assertEquals(Currency.VND, currencyFromBackup("EUR"))
        assertEquals(Currency.VND, currencyFromBackup(3L))
    }

    @Test
    fun amountToInputRoundTripsThroughParse() {
        assertEquals("1250", amountToInput(1250, vnd))
        assertEquals("1250", amountToInput(1250, twd))
        assertEquals("12.5", amountToInput(1250, usd))
        assertEquals("12", amountToInput(1200, usd))
        assertEquals("0.05", amountToInput(5, usd))
        assertEquals("1250.75", amountToInput(125075, usd))
        for (c in Currency.entries) {
            for (a in listOf(1L, 5L, 99L, 1250L, 125075L, 500000L)) {
                assertEquals(a, parseAmount(amountToInput(a, c), c))
            }
        }
    }

    @Test
    fun symbols() {
        assertEquals("₫", currencySymbol(vnd))
        assertEquals("NT$", currencySymbol(twd))
        assertEquals("$", currencySymbol(usd))
    }
}
