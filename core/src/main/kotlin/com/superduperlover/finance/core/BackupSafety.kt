package com.superduperlover.finance.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Default name of the exported file, e.g. `chili_backup_2026-11-08.json`. */
fun backupFileName(date: LocalDate): String = "chili_backup_$date.json"

/** At least this many days since the last backup (or never) before the reminder shows. */
const val BACKUP_REMINDER_DAYS = 7

/** At least this many new transactions since the last backup before the reminder shows. */
const val BACKUP_REMINDER_TXNS = 5

/** [daysSince] = days since the last backup, null = never backed up. */
data class BackupReminder(val daysSince: Long?, val newTxns: Int)

/**
 * The passive backup reminder of the Home screen: non-null only when the last backup is 7+ days old (or there was
 * none) AND 5+ transactions were saved since.
 */
fun backupReminder(today: LocalDate, lastBackupAt: LocalDate?, txnsSinceBackup: Int): BackupReminder? {
    if (txnsSinceBackup < BACKUP_REMINDER_TXNS) return null
    if (lastBackupAt == null) return BackupReminder(null, txnsSinceBackup)
    val days = ChronoUnit.DAYS.between(lastBackupAt, today).coerceAtLeast(0)
    return if (days >= BACKUP_REMINDER_DAYS) BackupReminder(days, txnsSinceBackup) else null
}
