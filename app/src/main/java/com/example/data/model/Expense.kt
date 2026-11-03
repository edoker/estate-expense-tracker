package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val supplierName: String = "",
    val supplierPhone: String? = null,
    val amount: Double = 0.0,
    val date: Timestamp? = null,
    val paidByUid: String = "",
    val enteredByUid: String = "",
    val note: String? = null,
    val type: String = "GAVE", // "GAVE" = You gave / expense; "GOT" = You got / received / income
    val paymentMode: String = "UPI", // "UPI", "Cash", "Cheque", "Netbanking"
    val attachments: List<ExpenseAttachment> = emptyList(),
    val isDeleted: Boolean = false,
    val deletedByUid: String? = null,
    val deletedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val updatedByUid: String? = null
) {
    val isIncome: Boolean
        get() = type.equals("GOT", ignoreCase = true)

    val isExpense: Boolean
        get() = !isIncome
}
