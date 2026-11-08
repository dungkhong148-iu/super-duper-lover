package com.superduperlover.finance.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * Written by export. Import also accepts version 1 (before currencies: no currency, colorKey or toAmount; VND)
 * and version 2 (budgets have `month` instead of period / startDate / rollover; goals have no completedAt).
 */
const val BACKUP_VERSION = 3

/** Value of the `app` field in the header of an exported file. */
const val BACKUP_APP_NAME = "ChiLi"
const val OLDEST_SUPPORTED_BACKUP_VERSION = 1
val BACKUP_TABLES = listOf("accounts", "categories", "txns", "budgets", "goals", "goalContributions")

private val TABLE_INTEGER_FIELDS = mapOf(
    "accounts" to listOf("id", "openingBalance"),
    "categories" to listOf("id"),
    "txns" to listOf("id", "amount", "toAmount", "accountId", "toAccountId", "categoryId"),
    "budgets" to listOf("id", "accountId", "categoryId", "limit"),
    "goals" to listOf("id", "target"),
    "goalContributions" to listOf("id", "goalId", "amount"),
)

/** Tables whose rows may carry a `currency` string (version 2). */
private val TABLES_WITH_CURRENCY = setOf("accounts", "budgets", "goals")

fun validateBackup(root: Map<String, Any?>): List<String> {
    val errors = mutableListOf<String>()

    // `app` is written by export since X0; older files have none and stay valid.
    val app = root["app"]
    if (app != null && app != BACKUP_APP_NAME) errors.add("Tệp này không phải bản sao lưu của ChiLi")

    val version = root["version"]
    val isVersionValid = when (version) {
        is Int -> version in OLDEST_SUPPORTED_BACKUP_VERSION..BACKUP_VERSION
        is Long -> version in OLDEST_SUPPORTED_BACKUP_VERSION.toLong()..BACKUP_VERSION.toLong()
        else -> false
    }
    if (!isVersionValid) {
        errors.add("Phiên bản sao lưu không được hỗ trợ")
    }

    for (tableName in BACKUP_TABLES) {
        val tableData = root[tableName]
        if (tableData !is List<*>) {
            errors.add("Thiếu bảng: $tableName")
        } else {
            val integerFields = TABLE_INTEGER_FIELDS[tableName] ?: emptyList()
            for (row in tableData) {
                if (row !is Map<*, *>) {
                    errors.add("Dòng không hợp lệ: $tableName")
                } else {
                    val currency = row["currency"]
                    if (tableName in TABLES_WITH_CURRENCY && currency != null &&
                        (currency !is String || Currency.entries.none { it.name == currency })
                    ) {
                        errors.add("Tiền tệ không hợp lệ: $tableName.currency")
                    }
                    if (tableName == "budgets") {
                        val period = row["period"]
                        if (period != null && (period !is String || BudgetPeriod.entries.none { it.name == period })) {
                            errors.add("Chu kỳ không hợp lệ: budgets.period")
                        }
                        // version 3 has startDate; versions 1-2 have month (yyyy-MM)
                        if (budgetStartDateFromBackup(row) == null) errors.add("Ngày bắt đầu không hợp lệ: budgets.startDate")
                    }
                    if (tableName == "goals") {
                        val completedAt = row["completedAt"]
                        if (completedAt != null && parseIsoDate(completedAt) == null) {
                            errors.add("Ngày không hợp lệ: goals.completedAt")
                        }
                    }
                    for (field in integerFields) {
                        val value = row[field]
                        if (value != null && value !is Int && value !is Long) {
                            errors.add("Số không hợp lệ: $tableName.$field")
                        }
                    }
                }
            }
        }
    }

    return errors.distinct()
}

private fun parseIsoDate(raw: Any?): LocalDate? =
    (raw as? String)?.let { text -> try { LocalDate.parse(text) } catch (e: DateTimeParseException) { null } }

/** Period of a backup budget row; missing or unknown (versions 1-2) -> MONTH. */
fun budgetPeriodFromBackup(raw: Any?): BudgetPeriod =
    (raw as? String)?.let { name -> BudgetPeriod.entries.firstOrNull { it.name == name } } ?: BudgetPeriod.MONTH

/**
 * Start date of a backup budget row: `startDate` (version 3), or the first day of `month` (versions 1-2,
 * "2026-11" -> 2026-11-01). null when neither is valid.
 */
fun budgetStartDateFromBackup(row: Map<*, *>): LocalDate? {
    parseIsoDate(row["startDate"])?.let { return it }
    val month = row["month"] as? String ?: return null
    return try { YearMonth.parse(month).atDay(1) } catch (e: DateTimeParseException) { null }
}
