package com.superduperlover.finance.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parseAcceptsPlainAndGroupedNumbers() {
        assertEquals(1250000L, parseVndInput("1250000"))
        assertEquals(1250000L, parseVndInput("1.250.000"))
        assertEquals(1250000L, parseVndInput("1,250,000"))
        assertEquals(1250000L, parseVndInput("1 250 000"))
    }

    @Test
    fun parseAcceptsKSuffix() {
        assertEquals(50000L, parseVndInput("50k"))
        assertEquals(50000L, parseVndInput("50K"))
    }

    @Test
    fun parseRejectsBadInput() {
        assertNull(parseVndInput(""))
        assertNull(parseVndInput("abc"))
        assertNull(parseVndInput("0"))
        assertNull(parseVndInput("-5"))
        assertNull(parseVndInput("1.5"))
        assertNull(parseVndInput("12.50.000"))
    }

    @Test
    fun formatUsesDotGroupsAndDongSign() {
        assertEquals("1.250.000 ₫", formatVnd(1250000))
        assertEquals("0 ₫", formatVnd(0))
        assertEquals("-50.000 ₫", formatVnd(-50000))
    }
}
