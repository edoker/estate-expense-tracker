package com.example

import com.example.data.model.Expense
import com.example.data.model.ProjectMember
import com.example.data.util.matchesAmount
import com.example.data.util.matchesExpenseSearch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseSearchTest {

    private val sampleExpense = Expense(
        id = "exp_1",
        supplierName = "Ultratech Cement Suppliers",
        supplierPhone = "9876543210",
        amount = 125000.0,
        paidByUid = "user_partner_1",
        enteredByUid = "user_partner_1",
        note = "Pillar foundation concrete mix load 1",
        type = "GAVE",
        paymentMode = "Netbanking"
    )

    private val sampleIncome = Expense(
        id = "exp_2",
        supplierName = "Client Advance Sharma",
        supplierPhone = null,
        amount = 50000.0,
        paidByUid = "user_partner_2",
        enteredByUid = "user_partner_2",
        note = "Token advance received for plumbing phase",
        type = "GOT",
        paymentMode = "UPI"
    )

    private val members = mapOf(
        "user_partner_1" to ProjectMember(
            email = "partner1@example.com",
            name = "Ramesh Gupta",
            role = "admin"
        ),
        "user_partner_2" to ProjectMember(
            email = "partner2@example.com",
            name = "Suresh Patel",
            role = "member"
        )
    )

    @Test
    fun search_emptyQuery_matchesAll() {
        assertTrue(matchesExpenseSearch(sampleExpense, "", members))
        assertTrue(matchesExpenseSearch(sampleExpense, "   ", members))
        assertTrue(matchesExpenseSearch(sampleIncome, "", members))
    }

    @Test
    fun search_byAmount_matchesExactAndSubstring() {
        // Plain numbers
        assertTrue(matchesExpenseSearch(sampleExpense, "125000", members))
        assertTrue(matchesExpenseSearch(sampleExpense, "125", members))
        assertTrue(matchesExpenseSearch(sampleIncome, "50000", members))
        assertTrue(matchesExpenseSearch(sampleIncome, "500", members))

        // Formatted Indian currency matching
        assertTrue(matchesExpenseSearch(sampleExpense, "1,25,000", members))
        assertTrue(matchesExpenseSearch(sampleExpense, "₹1,25,000", members))
        assertTrue(matchesExpenseSearch(sampleIncome, "₹50,000", members))

        // Non-matching amount
        assertFalse(matchesExpenseSearch(sampleExpense, "99999", members))
    }

    @Test
    fun search_byPaidTo_matchesSupplierNameCaseInsensitive() {
        assertTrue(matchesExpenseSearch(sampleExpense, "Ultratech", members))
        assertTrue(matchesExpenseSearch(sampleExpense, "ultratech", members))
        assertTrue(matchesExpenseSearch(sampleExpense, "CEMENT", members))
        assertTrue(matchesExpenseSearch(sampleExpense, "Suppliers", members))

        assertTrue(matchesExpenseSearch(sampleIncome, "sharma", members))
        assertTrue(matchesExpenseSearch(sampleIncome, "Client Advance", members))

        assertFalse(matchesExpenseSearch(sampleExpense, "Hardware Store", members))
    }

    @Test
    fun search_byNote_matchesDescriptionCaseInsensitive() {
        assertTrue(matchesExpenseSearch(sampleExpense, "foundation", members))
        assertTrue(matchesExpenseSearch(sampleExpense, "PILLAR", members))
        assertTrue(matchesExpenseSearch(sampleExpense, "concrete mix", members))

        assertTrue(matchesExpenseSearch(sampleIncome, "plumbing", members))
        assertTrue(matchesExpenseSearch(sampleIncome, "token advance", members))

        assertFalse(matchesExpenseSearch(sampleExpense, "electrical wiring", members))
    }

    @Test
    fun search_byNote_handlesNullNoteSafely() {
        val expenseWithoutNote = sampleExpense.copy(note = null)
        assertFalse(matchesExpenseSearch(expenseWithoutNote, "nonexistent note query", members))
        // Still matches by supplier name or amount
        assertTrue(matchesExpenseSearch(expenseWithoutNote, "Ultratech", members))
        assertTrue(matchesExpenseSearch(expenseWithoutNote, "125000", members))
    }

    @Test
    fun search_bySupplierPhone_matches() {
        assertTrue(matchesExpenseSearch(sampleExpense, "9876543210", members))
        assertTrue(matchesExpenseSearch(sampleExpense, "9876", members))
    }

    @Test
    fun search_byPartnerName_matches() {
        assertTrue(matchesExpenseSearch(sampleExpense, "Ramesh", members))
        assertTrue(matchesExpenseSearch(sampleIncome, "Suresh", members))
    }

    @Test
    fun search_byPaymentMode_matches() {
        assertTrue(matchesExpenseSearch(sampleExpense, "Netbanking", members))
        assertTrue(matchesExpenseSearch(sampleIncome, "UPI", members))
    }

    @Test
    fun test_matchesAmountUtility() {
        assertTrue(matchesAmount(500.0, "500"))
        assertTrue(matchesAmount(1500.0, "1,500"))
        assertTrue(matchesAmount(1500.0, "₹ 1,500"))
        assertTrue(matchesAmount(250.75, "250.75"))
        assertFalse(matchesAmount(500.0, "800"))
    }
}
