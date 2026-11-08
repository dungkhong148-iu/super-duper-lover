package com.superduperlover.finance.core

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthsTest {
    @Test
    fun countsMonthsAcrossYearEnd() {
        assertEquals(2, monthsBetween(YearMonth.of(2026, 11), YearMonth.of(2027, 1)))
    }

    @Test
    fun sameMonthIsZero() {
        assertEquals(0, monthsBetween(YearMonth.of(2026, 11), YearMonth.of(2026, 11)))
    }

    @Test
    fun earlierTargetIsNegative() {
        assertEquals(-3, monthsBetween(YearMonth.of(2026, 11), YearMonth.of(2026, 8)))
    }
}
