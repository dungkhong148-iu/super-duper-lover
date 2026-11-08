package com.superduperlover.finance.core

import java.time.YearMonth

/** Whole months from [from] to [to]: 2026-11 -> 2027-01 = 2. Negative when [to] is earlier. */
fun monthsBetween(from: YearMonth, to: YearMonth): Int =
    (to.year - from.year) * 12 + (to.monthValue - from.monthValue)
