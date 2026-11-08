package com.superduperlover.finance.data

import com.superduperlover.finance.core.BACKUP_VERSION
import com.superduperlover.finance.core.validateBackup
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BackupData(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val txns: List<TxnEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
    val contributions: List<ContributionEntity>,
)

fun BackupData.toJson(exportedAt: String): String {
    val root = JSONObject()
    root.put("app", "ChiLi")
    root.put("version", BACKUP_VERSION)
    root.put("exportedAt", exportedAt)

    fun jsonNull(v: Any?): Any = v ?: JSONObject.NULL

    val accountsArr = JSONArray()
    for (a in accounts) {
        val obj = JSONObject()
        obj.put("id", a.id)
        obj.put("name", a.name)
        obj.put("type", a.type)
        obj.put("openingBalance", a.openingBalance)
        obj.put("archived", a.archived)
        accountsArr.put(obj)
    }
    root.put("accounts", accountsArr)

    val categoriesArr = JSONArray()
    for (c in categories) {
        val obj = JSONObject()
        obj.put("id", c.id)
        obj.put("name", c.name)
        obj.put("kind", c.kind)
        obj.put("icon", c.icon)
        obj.put("archived", c.archived)
        categoriesArr.put(obj)
    }
    root.put("categories", categoriesArr)

    val txnsArr = JSONArray()
    for (t in txns) {
        val obj = JSONObject()
        obj.put("id", t.id)
        obj.put("kind", t.kind)
        obj.put("amount", t.amount)
        obj.put("accountId", t.accountId)
        obj.put("toAccountId", jsonNull(t.toAccountId))
        obj.put("categoryId", jsonNull(t.categoryId))
        obj.put("date", t.date)
        obj.put("note", t.note)
        txnsArr.put(obj)
    }
    root.put("txns", txnsArr)

    val budgetsArr = JSONArray()
    for (b in budgets) {
        val obj = JSONObject()
        obj.put("id", b.id)
        obj.put("month", b.month)
        obj.put("accountId", jsonNull(b.accountId))
        obj.put("categoryId", jsonNull(b.categoryId))
        obj.put("limit", b.limitAmount)
        budgetsArr.put(obj)
    }
    root.put("budgets", budgetsArr)

    val goalsArr = JSONArray()
    for (g in goals) {
        val obj = JSONObject()
        obj.put("id", g.id)
        obj.put("name", g.name)
        obj.put("target", g.target)
        obj.put("deadline", jsonNull(g.deadline))
        obj.put("archived", g.archived)
        goalsArr.put(obj)
    }
    root.put("goals", goalsArr)

    val contributionsArr = JSONArray()
    for (c in contributions) {
        val obj = JSONObject()
        obj.put("id", c.id)
        obj.put("goalId", c.goalId)
        obj.put("amount", c.amount)
        obj.put("date", c.date)
        contributionsArr.put(obj)
    }
    root.put("goalContributions", contributionsArr)

    return root.toString(2)
}

fun jsonToTree(json: String): Map<String, Any?> {
    val clean = if (json.startsWith("\uFEFF")) json.substring(1) else json
    val obj = try {
        JSONObject(clean)
    } catch (e: JSONException) {
        throw IllegalArgumentException("Tệp không phải JSON hợp lệ")
    }
    return toMap(obj)
}

private fun toMap(obj: JSONObject): LinkedHashMap<String, Any?> {
    val map = LinkedHashMap<String, Any?>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = obj.get(key)
        map[key] = convertValue(value)
    }
    return map
}

private fun toList(arr: JSONArray): ArrayList<Any?> {
    val list = ArrayList<Any?>(arr.length())
    for (i in 0 until arr.length()) {
        list.add(convertValue(arr.get(i)))
    }
    return list
}

private fun convertValue(v: Any?): Any? = when (v) {
    null, JSONObject.NULL -> null
    is JSONObject -> toMap(v)
    is JSONArray -> toList(v)
    else -> v // preserves Int, Long, Double, String, Boolean
}

fun validateBackupJson(json: String): List<String> {
    val tree = try {
        jsonToTree(json)
    } catch (e: IllegalArgumentException) {
        return listOf("Tệp không phải JSON hợp lệ")
    }
    return validateBackup(tree)
}

fun parseBackup(root: Map<String, Any?>): BackupData {
    fun toLong(v: Any?): Long = (v as Number).toLong()
    fun toLongOrNull(v: Any?): Long? = (v as? Number)?.toLong()
    fun toStr(v: Any?): String = v as String
    fun toStrOrNull(v: Any?): String? = v as? String
    fun toBool(v: Any?): Boolean = v as Boolean

    val accounts = (root["accounts"] as List<*>).map {
        val r = it as Map<*, *>
        AccountEntity(
            id = toLong(r["id"]),
            name = toStr(r["name"]),
            type = toStr(r["type"]),
            openingBalance = toLong(r["openingBalance"]),
            archived = toBool(r["archived"]),
        )
    }
    val categories = (root["categories"] as List<*>).map {
        val r = it as Map<*, *>
        CategoryEntity(
            id = toLong(r["id"]),
            name = toStr(r["name"]),
            kind = toStr(r["kind"]),
            icon = toStr(r["icon"]),
            archived = toBool(r["archived"]),
        )
    }
    val txns = (root["txns"] as List<*>).map {
        val r = it as Map<*, *>
        TxnEntity(
            id = toLong(r["id"]),
            kind = toStr(r["kind"]),
            amount = toLong(r["amount"]),
            accountId = toLong(r["accountId"]),
            toAccountId = toLongOrNull(r["toAccountId"]),
            categoryId = toLongOrNull(r["categoryId"]),
            date = toStr(r["date"]),
            note = toStr(r["note"]),
        )
    }
    val budgets = (root["budgets"] as List<*>).map {
        val r = it as Map<*, *>
        BudgetEntity(
            id = toLong(r["id"]),
            month = toStr(r["month"]),
            accountId = toLongOrNull(r["accountId"]),
            categoryId = toLongOrNull(r["categoryId"]),
            limitAmount = toLong(r["limit"]),
        )
    }
    val goals = (root["goals"] as List<*>).map {
        val r = it as Map<*, *>
        GoalEntity(
            id = toLong(r["id"]),
            name = toStr(r["name"]),
            target = toLong(r["target"]),
            deadline = toStrOrNull(r["deadline"]),
            archived = toBool(r["archived"]),
        )
    }
    val contributions = (root["goalContributions"] as List<*>).map {
        val r = it as Map<*, *>
        ContributionEntity(
            id = toLong(r["id"]),
            goalId = toLong(r["goalId"]),
            amount = toLong(r["amount"]),
            date = toStr(r["date"]),
        )
    }
    return BackupData(accounts, categories, txns, budgets, goals, contributions)
}
