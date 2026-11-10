package com.superduperlover.finance.core

import java.time.LocalDate
import java.time.YearMonth

/**
 * Progress of [goal]. [contributions] must be the contributions of this goal; a SETTLEMENT record is not savings.
 * `perMonth` = ceil(remaining / max(1, monthsBetween(thisMonth, deadlineMonth) + 1)), in Long math;
 * null when there is no deadline or the goal is done. A passed deadline gives the whole remaining amount.
 */
fun goalProgress(goal: Goal, contributions: List<GoalContribution>, today: LocalDate): GoalProgress {
    val saved = jarSaved(contributions)
    val done = saved >= goal.target
    val remaining = if (done) 0L else goal.target - saved
    val ratio = if (goal.target > 0) saved.toDouble() / goal.target.toDouble() else 1.0
    val perMonth = if (done || goal.deadline == null) {
        null
    } else {
        val months = maxOf(
            1,
            monthsBetween(YearMonth.from(today), YearMonth.from(goal.deadline)) + 1,
        ).toLong()
        (remaining + months - 1) / months
    }
    return GoalProgress(saved = saved, remaining = remaining, ratio = ratio, done = done, perMonth = perMonth)
}

/**
 * True when a contribution just brought the saved amount to the target: it was below before, is at or above now,
 * and the goal is not completed yet. A reopened goal (saved already >= target) is not completed again by this.
 */
fun shouldAutoComplete(goal: Goal, savedBefore: Long, savedAfter: Long): Boolean =
    goal.completedAt == null && savedBefore < goal.target && savedAfter >= goal.target
