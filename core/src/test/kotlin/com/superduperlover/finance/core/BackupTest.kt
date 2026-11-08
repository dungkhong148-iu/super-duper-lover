package com.superduperlover.finance.core

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
    fun version3Fails() {
        val errors = validateBackup(minimalValidBackup(version = 3))
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
            put("budgets", listOf(mapOf("id" to 1L, "limit" to 5L, "accountId" to null, "categoryId" to null, "currency" to "USD")))
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
}
