package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {

    private fun minimalValidBackup(version: Any? = 1L): MutableMap<String, Any?> = mutableMapOf(
        "version" to version,
        "accounts" to emptyList<Any?>(),
        "categories" to emptyList<Any?>(),
        "txns" to emptyList<Any?>(),
        "budgets" to emptyList<Any?>(),
        "goals" to emptyList<Any?>(),
        "goalContributions" to emptyList<Any?>(),
    )

    @Test
    fun minimalValidBackupHasNoErrors() {
        val errors = validateBackup(minimalValidBackup())
        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun oneValidRowPerTableHasNoErrors() {
        val validRowBackup = mapOf(
            "version" to 1,
            "accounts" to listOf(
                mapOf("id" to 1L, "name" to "Cash", "type" to "CASH", "openingBalance" to 0L, "archived" to false),
            ),
            "categories" to listOf(
                mapOf("id" to 10, "name" to "Food", "kind" to "EXPENSE", "icon" to "food", "archived" to false),
            ),
            "txns" to listOf(
                mapOf(
                    "id" to 100L,
                    "kind" to "EXPENSE",
                    "amount" to 50000,
                    "accountId" to 1,
                    "toAccountId" to null,
                    "categoryId" to 10,
                    "date" to "2026-11-08",
                    "note" to "",
                ),
            ),
            "budgets" to listOf(
                mapOf("id" to 1, "month" to "2026-11", "accountId" to null, "categoryId" to 10, "limit" to 500000L),
            ),
            "goals" to listOf(
                mapOf("id" to 2L, "name" to "Car", "target" to 100000000L, "deadline" to null, "archived" to false),
            ),
            "goalContributions" to listOf(
                mapOf("id" to 20, "goalId" to 2L, "amount" to 500000L, "date" to "2026-11-08"),
            ),
        )
        val errors = validateBackup(validRowBackup)
        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun version5Fails() {
        val errors = validateBackup(minimalValidBackup(version = 5))
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Phiên bản") })
    }

    @Test
    fun versionMissingFails() {
        val map = minimalValidBackup()
        map.remove("version")
        val errors = validateBackup(map)
        assertTrue(errors.isNotEmpty())
    }

    @Test
    fun versionDoubleFails() {
        val errors = validateBackup(minimalValidBackup(version = 1.0))
        assertTrue(errors.isNotEmpty())
    }

    @Test
    fun tableTxnsMissingFails() {
        val map = minimalValidBackup()
        map.remove("txns")
        val errors = validateBackup(map)
        assertTrue(errors.any { it.contains("Thiếu bảng: txns") })
    }

    @Test
    fun tableTxnsStringFails() {
        val map = minimalValidBackup()
        map["txns"] = "not a list"
        val errors = validateBackup(map)
        assertTrue(errors.any { it.contains("Thiếu bảng: txns") })
    }

    @Test
    fun txnsAmountDoubleFails() {
        val map = minimalValidBackup()
        map["txns"] = listOf(mapOf("id" to 1L, "amount" to 12.5, "accountId" to 1L))
        val errors = validateBackup(map)
        assertTrue(errors.any { it.contains("txns.amount") })
    }

    @Test
    fun txnsAmountStringFails() {
        val map = minimalValidBackup()
        map["txns"] = listOf(mapOf("id" to 1L, "amount" to "50000", "accountId" to 1L))
        val errors = validateBackup(map)
        assertTrue(errors.any { it.contains("txns.amount") })
    }

    @Test
    fun budgetsLimitDoubleFails() {
        val map = minimalValidBackup()
        map["budgets"] = listOf(mapOf("id" to 1L, "limit" to 1000.0))
        val errors = validateBackup(map)
        assertTrue(errors.any { it.contains("budgets.limit") })
    }

    @Test
    fun rowStringInsteadOfMapFails() {
        val map = minimalValidBackup()
        map["accounts"] = listOf("string instead of map")
        val errors = validateBackup(map)
        assertTrue(errors.any { it.contains("Dòng không hợp lệ") })
    }

    @Test
    fun twoBadFieldsCollectsBothErrors() {
        val map = minimalValidBackup()
        map["txns"] = listOf(mapOf("id" to 1.5, "amount" to 12.5))
        val errors = validateBackup(map)
        assertEquals(2, errors.size)
        assertTrue(errors.any { it.contains("txns.id") })
        assertTrue(errors.any { it.contains("txns.amount") })
    }

    @Test
    fun accountRowWithOrWithoutColourKeyIsValid() {
        val withColour = minimalValidBackup().apply {
            put("accounts", listOf(mapOf("id" to 1L, "name" to "Cash", "type" to "CASH", "openingBalance" to 0L, "colorKey" to "rose")))
        }
        val withoutColour = minimalValidBackup().apply {
            put("accounts", listOf(mapOf("id" to 1L, "name" to "Cash", "type" to "CASH", "openingBalance" to 0L)))
        }
        assertEquals(emptyList<String>(), validateBackup(withColour))
        assertEquals(emptyList<String>(), validateBackup(withoutColour))
    }

    @Test
    fun versionOneAndTwoAreAccepted() {
        assertEquals(emptyList<String>(), validateBackup(minimalValidBackup(version = 1L)))
        assertEquals(emptyList<String>(), validateBackup(minimalValidBackup(version = 2L)))
    }

    @Test
    fun version2RowsWithCurrencyAndReceivedAmountAreValid() {
        val backup = minimalValidBackup(version = 2L).apply {
            put("accounts", listOf(mapOf("id" to 1L, "name" to "A", "type" to "BANK", "openingBalance" to 0L, "currency" to "TWD", "colorKey" to "sky")))
            put("txns", listOf(mapOf("id" to 1L, "kind" to "TRANSFER", "amount" to 1_000_000L, "accountId" to 1L, "toAccountId" to 2L, "toAmount" to 1_250L, "categoryId" to null)))
            put("budgets", listOf(mapOf("id" to 1L, "month" to "2026-11", "limit" to 5L, "accountId" to null, "categoryId" to null, "currency" to "USD")))
            put("goals", listOf(mapOf("id" to 1L, "target" to 5L, "currency" to "VND")))
        }
        assertEquals(emptyList<String>(), validateBackup(backup))
    }

    @Test
    fun unknownCurrencyOrNonIntegerReceivedAmountFails() {
        val badCurrency = minimalValidBackup(version = 2L).apply {
            put("accounts", listOf(mapOf("id" to 1L, "openingBalance" to 0L, "currency" to "EUR")))
        }
        assertTrue(validateBackup(badCurrency).any { it.contains("accounts.currency") })
        val badReceived = minimalValidBackup(version = 2L).apply {
            put("txns", listOf(mapOf("id" to 1L, "amount" to 5L, "toAmount" to 12.5)))
        }
        assertTrue(validateBackup(badReceived).any { it.contains("txns.toAmount") })
    }

    @Test
    fun version3BudgetsWithPeriodStartDateAndRolloverAreValid() {
        val backup = minimalValidBackup(version = 3L).apply {
            put(
                "budgets",
                listOf(
                    mapOf(
                        "id" to 1L, "period" to "WEEK", "startDate" to "2026-11-05", "rollover" to true,
                        "accountId" to null, "categoryId" to null, "limit" to 5L, "currency" to "TWD",
                    ),
                ),
            )
            put("goals", listOf(mapOf("id" to 1L, "target" to 5L, "currency" to "VND", "completedAt" to "2026-11-08")))
        }
        assertEquals(emptyList<String>(), validateBackup(backup))
    }

    @Test
    fun budgetNeedsAStartDateOrAMonth() {
        val backup = minimalValidBackup(version = 3L).apply {
            put("budgets", listOf(mapOf("id" to 1L, "limit" to 5L)))
        }
        assertTrue(validateBackup(backup).any { it.contains("budgets.startDate") })
        val badDate = minimalValidBackup(version = 3L).apply {
            put("budgets", listOf(mapOf("id" to 1L, "startDate" to "2026-13-40", "limit" to 5L)))
        }
        assertTrue(validateBackup(badDate).any { it.contains("budgets.startDate") })
    }

    @Test
    fun unknownPeriodAndBadCompletedAtFail() {
        val badPeriod = minimalValidBackup(version = 3L).apply {
            put("budgets", listOf(mapOf("id" to 1L, "month" to "2026-11", "period" to "DECADE", "limit" to 5L)))
        }
        assertTrue(validateBackup(badPeriod).any { it.contains("budgets.period") })
        val badCompleted = minimalValidBackup(version = 3L).apply {
            put("goals", listOf(mapOf("id" to 1L, "target" to 5L, "completedAt" to "yesterday")))
        }
        assertTrue(validateBackup(badCompleted).any { it.contains("goals.completedAt") })
    }

    @Test
    fun oldMonthBudgetsBecomeMonthlyFromTheFirst() {
        val old = mapOf("id" to 1L, "month" to "2026-11", "limit" to 5L)
        assertEquals(LocalDate.of(2026, 11, 1), budgetStartDateFromBackup(old))
        assertEquals(BudgetPeriod.MONTH, budgetPeriodFromBackup(old["period"]))
        val v3 = mapOf("id" to 1L, "startDate" to "2026-11-25", "period" to "QUARTER")
        assertEquals(LocalDate.of(2026, 11, 25), budgetStartDateFromBackup(v3))
        assertEquals(BudgetPeriod.QUARTER, budgetPeriodFromBackup(v3["period"]))
        assertEquals(null, budgetStartDateFromBackup(mapOf("id" to 1L)))
    }

    @Test
    fun version4JarFieldsAreValidatedAndOldGoalsStayPlain() {
        val v4 = minimalValidBackup(version = 4L).apply {
            put(
                "goals",
                listOf(
                    mapOf(
                        "id" to 1L, "target" to 5L, "kind" to "SPENDING", "status" to "PENDING_SETTLEMENT",
                        "sourceAccountId" to 2L, "categoryId" to 3L, "autoDeduct" to true, "durationCount" to 4,
                        "durationUnit" to "MONTH", "planStart" to "2026-11-10", "planBase" to 1L,
                        "lastEvaluatedDate" to "2026-11-10",
                    ),
                ),
            )
            put(
                "goalContributions",
                listOf(mapOf("id" to 1L, "goalId" to 1L, "amount" to 5L, "type" to "AUTO_SCHEDULED", "periodIndex" to 1)),
            )
        }
        assertEquals(emptyList<String>(), validateBackup(v4))
        val bad = minimalValidBackup(version = 4L).apply {
            put("goals", listOf(mapOf("id" to 1L, "target" to 5L, "kind" to "BOX", "planStart" to "soon")))
            put("goalContributions", listOf(mapOf("id" to 1L, "goalId" to 1L, "amount" to 5L, "type" to "GIFT")))
        }
        val errors = validateBackup(bad)
        assertTrue(errors.any { it.contains("goals.kind") })
        assertTrue(errors.any { it.contains("goals.planStart") })
        assertTrue(errors.any { it.contains("goalContributions.type") })
        // version 3 rows have none of these fields
        assertEquals(JarKind.SAVING, jarKindFromBackup(null))
        assertEquals(JarStatus.ACTIVE, jarStatusFromBackup(null, null))
        assertEquals(JarStatus.COMPLETED, jarStatusFromBackup(null, "2026-11-08"))
        assertEquals(ContributionType.MANUAL, contributionTypeFromBackup(null))
    }
}
