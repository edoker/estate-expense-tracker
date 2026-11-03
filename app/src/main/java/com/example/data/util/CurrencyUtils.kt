package com.example.data.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

fun formatIndianCurrency(amount: Double): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        val formatted = format.format(amount)
        if (formatted.startsWith("₹")) {
            formatted
        } else {
            "₹" + formatted.replace("INR", "").replace("₹", "").trim()
        }
    } catch (e: Exception) {
        val df = DecimalFormat("#,##,##0.##")
        "₹" + df.format(amount)
    }
}
