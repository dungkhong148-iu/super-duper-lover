package com.superduperlover.finance.core

const val BACKUP_VERSION = 1
val BACKUP_TABLES = listOf("accounts", "categories", "txns", "budgets", "goals", "goalContributions")

private val TABLE_INTEGER_FIELDS = mapOf(
    "accounts" to listOf("id", "openingBalance"),
    "categories" to listOf("id"),
    "txns" to listOf("id", "amount", "accountId", "toAccountId", "categoryId"),
    "budgets" to listOf("id", "accountId", "categoryId", "limit"),
    "goals" to listOf("id", "target"),
    "goalContributions" to listOf("id", "goalId", "amount"),
)

fun validateBackup(root: Map<String, Any?>): List<String> {
    val errors = mutableListOf<String>()

    val version = root["version"]
    val isVersionValid = when (version) {
        is Int -> version == BACKUP_VERSION
        is Long -> version == BACKUP_VERSION.toLong()
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
