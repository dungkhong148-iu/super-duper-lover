package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSafetyTest {
    private val today = LocalDate.of(2026, 11, 8)

    @Test
    fun fileNameHasIsoDate() {
        assertEquals("chili_backup_2026-11-08.json", backupFileName(today))
    }

    @Test
    fun noReminderBeforeSevenDays() {
        assertNull(backupReminder(today, today.minusDays(6), txnsSinceBackup = 50))
    }

    @Test
    fun noReminderWithFewerThanFiveNewTransactions() {
        assertNull(backupReminder(today, today.minusDays(30), txnsSinceBackup = 4))
        assertNull(backupReminder(today, null, txnsSinceBackup = 0))
    }

    @Test
    fun reminderAtSevenDaysAndFiveTransactions() {
        val r = backupReminder(today, today.minusDays(7), txnsSinceBackup = 5)
        assertNotNull(r)
        assertEquals(7L, r!!.daysSince)
        assertEquals(5, r.newTxns)
    }

    @Test
    fun neverBackedUpRemindsWithNullDays() {
        val r = backupReminder(today, null, txnsSinceBackup = 9)
        assertNotNull(r)
        assertNull(r!!.daysSince)
        assertEquals(9, r.newTxns)
    }

    @Test
    fun futureBackupDateCountsAsZeroDays() {
        assertNull(backupReminder(today, today.plusDays(3), txnsSinceBackup = 20))
    }

    @Test
    fun backupFromAnotherAppFails() {
        val backup = mutableMapOf<String, Any?>(
            "app" to "Other", "version" to 3L,
            "accounts" to emptyList<Any>(), "categories" to emptyList<Any>(), "txns" to emptyList<Any>(),
            "budgets" to emptyList<Any>(), "goals" to emptyList<Any>(), "goalContributions" to emptyList<Any>(),
        )
        assertTrue(validateBackup(backup).any { it.contains("ChiLi") })
        backup["app"] = "ChiLi"
        assertEquals(emptyList<String>(), validateBackup(backup))
        backup.remove("app")
        assertEquals(emptyList<String>(), validateBackup(backup))
    }
}
