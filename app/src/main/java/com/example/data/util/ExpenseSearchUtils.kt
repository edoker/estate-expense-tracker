package com.example.data.util

import com.example.data.model.Expense
import com.example.data.model.ProjectMember

/**
 * Searches across:
 * 1. Amount paid / received (raw digits, formatted Indian currency, substring)
 * 2. Paid to / Supplier / Party name
 * 3. Note field
 * Plus helpful secondary fields (supplier phone, partner name, payment mode)
 */
fun matchesExpenseSearch(
    expense: Expense,
    query: String,
    members: Map<String, ProjectMember> = emptyMap()
): Boolean {
    val q = query.trim()
    if (q.isBlank()) return true

    // 1. Paid to / Supplier / Party name
    if (expense.supplierName.contains(q, ignoreCase = true)) {
        return true
    }

    // 2. Note field
    if (expense.note?.contains(q, ignoreCase = true) == true) {
        return true
    }

    // 3. Amount paid / received
    if (matchesAmount(expense.amount, q)) {
        return true
    }

    // 4. Supplier phone
    if (expense.supplierPhone?.contains(q, ignoreCase = true) == true) {
        return true
    }

    // 5. Partner name (who recorded/paid)
    val member = members[expense.paidByUid]
    if (member != null) {
        if (member.name.contains(q, ignoreCase = true) || member.email.contains(q, ignoreCase = true)) {
            return true
        }
    }

    // 6. Payment mode
    if (expense.paymentMode.contains(q, ignoreCase = true)) {
        return true
    }

    return false
}

/**
 * Matches an amount against a search query.
 * Supports:
 * - Direct numeric match ("500", "1500")
 * - Formatted Indian currency match ("1,500", "₹1,500")
 * - Stripped currency symbols / commas ("₹ 5000" -> "5000")
 * - Substring matching ("50" matches "1500" or "500")
 * - Decimal matching ("500.5")
 */
fun matchesAmount(amount: Double, query: String): Boolean {
    val cleanQuery = query.trim()
    if (cleanQuery.isEmpty()) return false

    // Formatted Indian currency (e.g. "₹1,500" or "1,500")
    val formatted = formatIndianCurrency(amount)
    if (formatted.contains(cleanQuery, ignoreCase = true)) {
        return true
    }

    val rawIntStr = amount.toLong().toString()
    val rawDoubleStr = amount.toString()

    if (rawIntStr.contains(cleanQuery, ignoreCase = true) ||
        rawDoubleStr.contains(cleanQuery, ignoreCase = true)
    ) {
        return true
    }

    // Strip currency symbols, commas, plus/minus, and spaces from query
    val digitsAndDots = cleanQuery.filter { it.isDigit() || it == '.' }
    if (digitsAndDots.isNotEmpty()) {
        if (rawIntStr.contains(digitsAndDots) || rawDoubleStr.contains(digitsAndDots)) {
            return true
        }
    }

    return false
}
