package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodsTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun r(s: LocalDate, e: LocalDate) = PeriodRange(s, e)

    @Test
    fun monthStartingOn25th() {
        val start = d(2026, 11, 25)
        assertEquals(r(d(2026, 11, 25), d(2026, 12, 24)), periodRange(BudgetPeriod.MONTH, start, 0))
        assertEquals(r(d(2026, 12, 25), d(2027, 1, 24)), periodRange(BudgetPeriod.MONTH, start, 1))
    }

    @Test
    fun monthStartingOn31stClampsToShortMonths() {
        val start = d(2026, 1, 31)
        assertEquals(r(d(2026, 1, 31), d(2026, 2, 27)), periodRange(BudgetPeriod.MONTH, start, 0))
        assertEquals(r(d(2026, 2, 28), d(2026, 3, 30)), periodRange(BudgetPeriod.MONTH, start, 1))
        assertEquals(r(d(2026, 3, 31), d(2026, 4, 29)), periodRange(BudgetPeriod.MONTH, start, 2))
        assertEquals(d(2026, 4, 30), periodRange(BudgetPeriod.MONTH, start, 3).start)
    }

    @Test
    fun weekIsSevenDaysFromStart() {
        val start = d(2026, 11, 5)
        assertEquals(r(d(2026, 11, 5), d(2026, 11, 11)), periodRange(BudgetPeriod.WEEK, start, 0))
        assertEquals(r(d(2026, 11, 12), d(2026, 11, 18)), periodRange(BudgetPeriod.WEEK, start, 1))
    }

    @Test
    fun dayPeriodIsOneDay() {
        assertEquals(r(d(2026, 11, 8), d(2026, 11, 8)), periodRange(BudgetPeriod.DAY, d(2026, 11, 5), 3))
    }

    @Test
    fun quarterIsThreeMonths() {
        assertEquals(r(d(2026, 11, 15), d(2027, 2, 14)), periodRange(BudgetPeriod.QUARTER, d(2026, 11, 15), 0))
    }

    @Test
    fun yearIsTwelveMonths() {
        assertEquals(r(d(2026, 3, 1), d(2027, 2, 28)), periodRange(BudgetPeriod.YEAR, d(2026, 3, 1), 0))
    }

    @Test
    fun periodsTileTheTimelineWithoutGaps() {
        for (period in BudgetPeriod.entries) {
            for (start in listOf(d(2026, 1, 31), d(2026, 11, 25), d(2024, 2, 29))) {
                for (i in 0..14) {
                    val a = periodRange(period, start, i)
                    val b = periodRange(period, start, i + 1)
                    assertEquals("$period $start $i", a.endInclusive.plusDays(1), b.start)
                }
            }
        }
    }

    @Test
    fun indexBeforeStartIsNull() {
        assertNull(periodIndexAt(BudgetPeriod.MONTH, d(2026, 11, 25), d(2026, 11, 24)))
        assertNull(periodIndexAt(BudgetPeriod.DAY, d(2026, 11, 25), d(2026, 11, 1)))
    }

    @Test
    fun indexOfDayInsideAndAtTheBordersOfAPeriod() {
        val start = d(2026, 11, 25)
        assertEquals(0, periodIndexAt(BudgetPeriod.MONTH, start, start))
        assertEquals(0, periodIndexAt(BudgetPeriod.MONTH, start, d(2026, 12, 24)))
        assertEquals(1, periodIndexAt(BudgetPeriod.MONTH, start, d(2026, 12, 25)))
        assertEquals(1, periodIndexAt(BudgetPeriod.WEEK, d(2026, 11, 5), d(2026, 11, 12)))
        assertEquals(0, periodIndexAt(BudgetPeriod.WEEK, d(2026, 11, 5), d(2026, 11, 11)))
        assertEquals(3, periodIndexAt(BudgetPeriod.DAY, d(2026, 11, 5), d(2026, 11, 8)))
        assertEquals(0, periodIndexAt(BudgetPeriod.YEAR, d(2026, 3, 1), d(2027, 2, 28)))
        assertEquals(1, periodIndexAt(BudgetPeriod.YEAR, d(2026, 3, 1), d(2027, 3, 1)))
    }

    @Test
    fun indexAgreesWithRangeEverywhere() {
        for (period in BudgetPeriod.entries) {
            val start = d(2026, 1, 31)
            var day = start
            repeat(500) {
                val i = periodIndexAt(period, start, day)!!
                val range = periodRange(period, start, i)
                assertEquals("$period $day", true, day >= range.start && day <= range.endInclusive)
                day = day.plusDays(1)
            }
        }
    }
}
