package com.superduperlover.finance.core

private val zeroAmount = Regex("""0+(?:\.0+)?""")
private val scalarNumber = Regex("""\d+(?:\.\d{1,6})?""")

/** A plain number written after × or ÷, as numerator / denominator (`2.5` -> 25 / 10). */
private fun parseScalar(token: String): Pair<Long, Long>? {
    val t = token.trim()
    if (!scalarNumber.matches(t)) return null
    val dot = t.indexOf('.')
    return if (dot < 0) {
        (t.toLongOrNull() ?: return null) to 1L
    } else {
        val fraction = t.substring(dot + 1)
        var den = 1L
        repeat(fraction.length) { den *= 10 }
        val num = (t.substring(0, dot) + fraction).toLongOrNull() ?: return null
        num to den
    }
}

/** Round half up for non-negative [a] and positive [d]. */
private fun divideRoundHalfUp(a: Long, d: Long): Long =
    Math.addExact(Math.multiplyExact(a, 2L), d) / Math.multiplyExact(d, 2L)

/**
 * Evaluates what the user typed on the calculator pad, e.g. `50k+30k` -> 80000, in MINOR units of [currency].
 * Amounts follow [parseAmount] for [currency] (groups, USD decimals, `k` suffix); a lone zero is allowed.
 * Operators: `+`, `-` (or `−`), `*` (or `×`), `/` (or `÷`). × and ÷ bind tighter than + and −, otherwise left to right.
 * The number after × or ÷ is a plain factor (`10.50*2` = $21.00, `10/3` = $3.33), not an amount.
 * ÷ rounds half up to the minor unit. Returns null when malformed (`+5`, `5+`), on division by zero, on overflow,
 * or when the result is not greater than 0.
 */
fun evalAmountExpr(text: String, currency: Currency): Long? {
    val operands = mutableListOf<String>()
    val ops = mutableListOf<Char>()
    val current = StringBuilder()
    for (ch in text.trim()) {
        val op = when (ch) {
            '+' -> '+'
            '-', '−' -> '-'
            '*', '×' -> '*'
            '/', '÷' -> '/'
            else -> null
        }
        if (op == null) {
            current.append(ch)
        } else {
            operands += current.toString().trim()
            current.clear()
            ops += op
        }
    }
    operands += current.toString().trim()

    fun amount(token: String): Long? = if (zeroAmount.matches(token)) 0L else parseAmount(token, currency)

    return try {
        var sum = 0L
        var sign = 1L
        var term = amount(operands[0]) ?: return null
        for (i in ops.indices) {
            val token = operands[i + 1]
            when (ops[i]) {
                '*' -> {
                    val (num, den) = parseScalar(token) ?: return null
                    term = divideRoundHalfUp(Math.multiplyExact(term, num), den)
                }
                '/' -> {
                    val (num, den) = parseScalar(token) ?: return null
                    if (num == 0L) return null
                    term = divideRoundHalfUp(Math.multiplyExact(term, den), num)
                }
                else -> {
                    sum = Math.addExact(sum, Math.multiplyExact(sign, term))
                    sign = if (ops[i] == '+') 1L else -1L
                    term = amount(token) ?: return null
                }
            }
        }
        sum = Math.addExact(sum, Math.multiplyExact(sign, term))
        if (sum > 0) sum else null
    } catch (e: ArithmeticException) {
        null
    }
}
