package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalsTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val goal = Goal(1, "Du lịch", 12_000_000, LocalDate.of(2027, 4, 30))

    private fun contribs(vararg amounts: Long) =
        amounts.mapIndexed { i, a -> GoalContribution(i + 1L, 1, a, today) }

    @Test
    fun progressWithDeadlineSplitsRemainingOverMonths() {
        val p = goalProgress(goal, contribs(2_000_000, 1_000_000), today)
        assertEquals(GoalProgress(3_000_000, 9_000_000, 0.25, false, 1_500_000), p)
    }

    @Test
    fun perMonthRoundsUp() {
        val g = goal.copy(target = 10_000_000)
        // remaining 10_000_000 over 6 months = 1_666_666.67 -> 1_666_667
        assertEquals(1_666_667L, goalProgress(g, emptyList(), today).perMonth)
    }

    @Test
    fun passedDeadlineNeedsWholeRemainingNow() {
        val g = goal.copy(deadline = LocalDate.of(2026, 9, 30))
        val p = goalProgress(g, contribs(2_000_000), today)
        assertEquals(10_000_000L, p.perMonth)
    }

    @Test
    fun noDeadlineHasNoPerMonth() {
        assertNull(goalProgress(goal.copy(deadline = null), contribs(1_000_000), today).perMonth)
    }

    @Test
    fun reachedGoalIsDone() {
        val p = goalProgress(goal, contribs(12_000_000, 500_000), today)
        assertTrue(p.done)
        assertEquals(0L, p.remaining)
        assertNull(p.perMonth)
    }
}
