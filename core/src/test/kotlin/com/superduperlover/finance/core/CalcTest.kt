package com.superduperlover.finance.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {
    private val vnd = Currency.VND
    private val twd = Currency.TWD
    private val usd = Currency.USD

    @Test
    fun vndAddsAndSubtracts() {
        assertEquals(80000L, evalAmountExpr("50k+30k", vnd))
        assertEquals(75000L, evalAmountExpr("100000-25000", vnd))
        assertEquals(1300000L, evalAmountExpr("1.250.000+50k", vnd))
    }

    @Test
    fun vndMultipliesAndDividesWithRoundingHalfUp() {
        assertEquals(36000L, evalAmountExpr("12000*3", vnd))
        assertEquals(33333L, evalAmountExpr("100000/3", vnd))
        assertEquals(66667L, evalAmountExpr("200000/3", vnd))
    }

    @Test
    fun multiplyAndDivideBindTighter() {
        assertEquals(16L, evalAmountExpr("10+2*3", vnd))
        assertEquals(94L, evalAmountExpr("100-2*3", vnd))
    }

    @Test
    fun acceptsPrettySymbols() {
        assertEquals(36000L, evalAmountExpr("12000×3", vnd))
        assertEquals(500L, evalAmountExpr("1000÷2", vnd))
        assertEquals(75000L, evalAmountExpr("100000−25000", vnd))
    }

    @Test
    fun plainNumberStillWorks() {
        assertEquals(50000L, evalAmountExpr("50k", vnd))
        assertEquals(1250000L, evalAmountExpr("1.250.000", vnd))
    }

    @Test
    fun intermediateNegativeIsFineIfFinalIsPositive() {
        assertEquals(15L, evalAmountExpr("5-10+20", vnd))
    }

    @Test
    fun rejectsMalformedAndNonPositive() {
        assertNull(evalAmountExpr("5-10", vnd))
        assertNull(evalAmountExpr("+5", vnd))
        assertNull(evalAmountExpr("5+", vnd))
        assertNull(evalAmountExpr("5/0", vnd))
        assertNull(evalAmountExpr("", vnd))
        assertNull(evalAmountExpr("abc", vnd))
        assertNull(evalAmountExpr("5++5", vnd))
        assertNull(evalAmountExpr("1.5+1", vnd))
        assertNull(evalAmountExpr("9223372036854775807+1", vnd))
    }

    @Test
    fun usdWorksInCents() {
        assertEquals(1275L, evalAmountExpr("10.50+2.25", usd))
        assertEquals(333L, evalAmountExpr("10/3", usd))
        assertEquals(2100L, evalAmountExpr("10.50*2", usd))
        assertEquals(1000000L, evalAmountExpr("1k*10", usd))
        assertNull(evalAmountExpr("10.555+1", usd))
    }

    @Test
    fun twdWorksInWholeDollars() {
        assertEquals(33L, evalAmountExpr("100/3", twd))
        assertEquals(1500L, evalAmountExpr("1,000+500", twd))
        assertNull(evalAmountExpr("1.5+1", twd))
    }

    @Test
    fun factorAfterTimesOrDivideIsAPlainNumber() {
        assertEquals(1500L, evalAmountExpr("1000*1.5", vnd))
        assertEquals(400L, evalAmountExpr("1000/2.5", vnd))
        assertEquals(7500L, evalAmountExpr("50k/2*3/10", vnd))
    }
}
