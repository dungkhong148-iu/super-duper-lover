package com.superduperlover.finance.core

// Integer part: plain digits, or a first group of 1-3 digits followed by groups of exactly 3 digits.
private val plainDigits = Regex("""\d+""")
private val groupsDot = Regex("""\d{1,3}(?:[.,\s]\d{3})+""") // VND: "." "," and space
private val groupsComma = Regex("""\d{1,3}(?:[,\s]\d{3})+""") // TWD: "," and space
private val groupsCommaOnly = Regex("""\d{1,3}(?:,\d{3})+""") // USD: ","

private fun integerPart(text: String, currency: Currency): Long? {
    val valid = plainDigits.matches(text) || when (currency) {
        Currency.VND -> groupsDot.matches(text)
        Currency.TWD -> groupsComma.matches(text)
        Currency.USD -> groupsCommaOnly.matches(text)
    }
    if (!valid) return null
    return text.filter { it.isDigit() }.toLongOrNull()
}

/**
 * Parses what the user typed into an amount in MINOR units of [currency] (đồng, NT dollars, US cents).
 * - VND: `1250000`, `1.250.000`, `1,250,000`, `1 250 000`.
 * - TWD: `1250`, `1,250`, `1 250`; no decimals.
 * - USD: `12`, `12.5`, `12.50`, `1,250.75`; `.` is the decimal point, at most 2 digits.
 * A `k`/`K` suffix multiplies by 1000 (`50k`, USD `1.5k` = $1,500.00).
 * Returns null for empty, non-numeric, zero, negative or badly formatted input.
 */
fun parseAmount(text: String, currency: Currency): Long? {
    var s = text.trim()
    var multiplier = 1L
    if (s.endsWith("k") || s.endsWith("K")) {
        multiplier = 1000L
        s = s.dropLast(1).trim()
    }
    val (whole, fraction) = if (currency.decimals > 0 && s.contains('.')) {
        val dot = s.indexOf('.')
        s.substring(0, dot) to s.substring(dot + 1)
    } else {
        s to ""
    }
    if (fraction.length > currency.decimals || (fraction.isNotEmpty() && !fraction.all { it.isDigit() })) return null
    val wholeValue = integerPart(whole, currency) ?: return null
    var scale = 1L
    repeat(currency.decimals) { scale *= 10 }
    val fractionValue = if (fraction.isEmpty()) 0L else fraction.padEnd(currency.decimals, '0').toLong()
    return try {
        val minor = Math.addExact(Math.multiplyExact(wholeValue, scale), fractionValue)
        val result = Math.multiplyExact(minor, multiplier)
        if (result > 0) result else null
    } catch (e: ArithmeticException) {
        null
    }
}

private fun group(digits: String, separator: Char): String =
    digits.reversed().chunked(3).joinToString(separator.toString()).reversed()

/**
 * Display text: VND `1.250.000 ₫`, TWD `NT$1,250`, USD `$1,250.75`. Negative: sign before the symbol (`-NT$50`).
 * [amount] is in minor units.
 */
fun formatMoney(amount: Long, currency: Currency): String {
    val text = amount.toString()
    val negative = text.startsWith("-")
    val digits = if (negative) text.substring(1) else text
    val sign = if (negative) "-" else ""
    return when (currency) {
        Currency.VND -> sign + group(digits, '.') + " ₫"
        Currency.TWD -> sign + "NT$" + group(digits, ',')
        Currency.USD -> {
            val padded = digits.padStart(3, '0')
            sign + "$" + group(padded.dropLast(2), ',') + "." + padded.takeLast(2)
        }
    }
}

/** Symbol used in labels: `₫`, `NT$`, `$`. */
fun currencySymbol(currency: Currency): String = when (currency) {
    Currency.VND -> "₫"
    Currency.TWD -> "NT$"
    Currency.USD -> "$"
}

/**
 * [amount] (minor units) as text for an input field that [parseAmount] accepts back:
 * `1250` for VND/TWD, USD `1250` cents -> `12.5`, `1200` -> `12`, `5` -> `0.05`.
 */
fun amountToInput(amount: Long, currency: Currency): String {
    if (currency.decimals == 0) return amount.toString()
    val negative = amount < 0
    val padded = amount.toString().removePrefix("-").padStart(3, '0')
    val whole = padded.dropLast(2)
    val fraction = padded.takeLast(2).trimEnd('0')
    return (if (negative) "-" else "") + whole + (if (fraction.isEmpty()) "" else ".$fraction")
}

/** Currency named in a backup file; missing, unknown or not a string -> VND (the app was VND-only before currencies). */
fun currencyFromBackup(raw: Any?): Currency =
    (raw as? String)?.let { name -> Currency.entries.firstOrNull { it.name == name } } ?: Currency.VND
