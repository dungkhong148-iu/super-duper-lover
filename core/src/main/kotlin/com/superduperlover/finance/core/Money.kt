package com.superduperlover.finance.core

// Plain digits, or groups of exactly 3 digits after the first (1-3 digits) group.
// Separators ".", "," and space are valid only between such groups.
private val plainNumber = Regex("""\d+""")
private val groupedNumber = Regex("""\d{1,3}(?:[.,\s]\d{3})+""")

/**
 * Parses what the user typed into an amount in đồng.
 * Accepts `1250000`, `1.250.000`, `1,250,000`, `1 250 000` and a `k`/`K` suffix (x1000).
 * Returns null for empty, non-numeric, zero, negative, decimal or badly grouped input.
 */
fun parseVndInput(text: String): Long? {
    var s = text.trim()
    var multiplier = 1L
    if (s.endsWith("k") || s.endsWith("K")) {
        multiplier = 1000L
        s = s.dropLast(1).trim()
    }
    if (!plainNumber.matches(s) && !groupedNumber.matches(s)) return null
    val value = s.filter { it.isDigit() }.toLongOrNull() ?: return null
    val result = try {
        Math.multiplyExact(value, multiplier)
    } catch (e: ArithmeticException) {
        return null
    }
    return if (result > 0) result else null
}

/** `1250000` -> `1.250.000 ₫`, `-50000` -> `-50.000 ₫`. */
fun formatVnd(amount: Long): String {
    val text = amount.toString()
    val negative = text.startsWith("-")
    val digits = if (negative) text.substring(1) else text
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return (if (negative) "-" else "") + grouped + " ₫"
}
