package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.ExpenseAttachment
import com.example.data.model.ProjectMember
import com.example.data.util.AttachmentManager
import com.example.data.util.OperationType
import com.example.data.util.formatDate
import com.example.data.util.formatIndianCurrency
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ExpenseRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun generateExpenseId(projectId: String): String {
        return db.collection("projects").document(projectId).collection("expenses").document().id
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing expenses.")
    }

    fun observeProjectExpenses(projectId: String): Flow<List<Expense>> = flow {
        if (auth.currentUser == null || projectId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "projects/$projectId/expenses"
        db.collection("projects").document(projectId).collection("expenses")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val supplierName = doc.getString("supplierName") ?: ""
                        val supplierPhone = doc.getString("supplierPhone")
                        val amount = doc.getDouble("amount") ?: (doc.getLong("amount")?.toDouble() ?: 0.0)
                        val date = doc.getTimestamp("date")
                        val paidByUid = doc.getString("paidByUid") ?: ""
                        val enteredByUid = doc.getString("enteredByUid") ?: ""
                        val note = doc.getString("note")
                        val attachmentsRaw = doc.get("attachments") as? List<*>
                        val attachments = attachmentsRaw?.mapNotNull { item ->
                            when (item) {
                                is Map<*, *> -> ExpenseAttachment.fromMap(item)
                                else -> null
                            }
                        } ?: emptyList()
                        val isDeleted = doc.getBoolean("isDeleted") ?: false
                        val deletedByUid = doc.getString("deletedByUid")
                        val deletedAt = doc.getTimestamp("deletedAt")
                        val createdAt = doc.getTimestamp("createdAt")
                        val updatedAt = doc.getTimestamp("updatedAt")
                        val updatedByUid = doc.getString("updatedByUid")

                        val type = doc.getString("type") ?: "GAVE"
                        val paymentMode = doc.getString("paymentMode") ?: "UPI"

                        Expense(
                            id = doc.id,
                            supplierName = supplierName,
                            supplierPhone = supplierPhone,
                            amount = amount,
                            date = date,
                            paidByUid = paidByUid,
                            enteredByUid = enteredByUid,
                            note = note,
                            type = type,
                            paymentMode = paymentMode,
                            attachments = attachments,
                            isDeleted = isDeleted,
                            deletedByUid = deletedByUid,
                            deletedAt = deletedAt,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            updatedByUid = updatedByUid
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .filter { !it.isDeleted }
                .sortedWith(
                    compareByDescending<Expense> { it.date?.seconds ?: 0L }
                        .thenByDescending { it.createdAt?.seconds ?: 0L }
                )
            }
            .catch { error ->
                if (auth.currentUser != null && error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun createExpense(
        projectId: String,
        supplierName: String,
        supplierPhone: String?,
        amount: Double,
        date: Timestamp,
        paidByUid: String,
        note: String?,
        type: String = "GAVE",
        paymentMode: String = "UPI",
        attachments: List<ExpenseAttachment> = emptyList(),
        expenseId: String? = null,
        currentUserId: String,
        currentUserName: String,
        members: Map<String, ProjectMember>
    ): Result<String> {
        requireUserId()
        val cleanSupplier = supplierName.trim()
        if (cleanSupplier.isBlank()) {
            return Result.failure(IllegalArgumentException("Party / supplier name is required"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        if (paidByUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Please select who handled this transaction"))
        }

        val projectRef = db.collection("projects").document(projectId)
        val newDoc = if (!expenseId.isNullOrBlank()) {
            projectRef.collection("expenses").document(expenseId)
        } else {
            projectRef.collection("expenses").document()
        }

        val data = hashMapOf<String, Any?>(
            "supplierName" to cleanSupplier,
            "supplierPhone" to supplierPhone?.trim()?.ifBlank { null },
            "amount" to amount,
            "date" to date,
            "paidByUid" to paidByUid,
            "enteredByUid" to currentUserId,
            "note" to note?.trim()?.ifBlank { null },
            "type" to type,
            "paymentMode" to paymentMode,
            "attachments" to attachments.map { it.toMap() },
            "isDeleted" to false,
            "deletedByUid" to null,
            "deletedAt" to null,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByUid" to currentUserId
        )

        return try {
            newDoc.set(data).await()
            Result.success(newDoc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, newDoc.path)
            Result.failure(e)
        }
    }

    suspend fun updateExpense(
        projectId: String,
        oldExpense: Expense,
        supplierName: String,
        supplierPhone: String?,
        amount: Double,
        date: Timestamp,
        paidByUid: String,
        note: String?,
        type: String = oldExpense.type,
        paymentMode: String = oldExpense.paymentMode,
        attachments: List<ExpenseAttachment> = oldExpense.attachments,
        currentUserId: String,
        currentUserName: String = "",
        members: Map<String, ProjectMember> = emptyMap()
    ): Result<Unit> {
        requireUserId()
        val cleanSupplier = supplierName.trim()
        if (cleanSupplier.isBlank()) {
            return Result.failure(IllegalArgumentException("Party / supplier name is required"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        if (paidByUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Please select who handled this transaction"))
        }

        val projectRef = db.collection("projects").document(projectId)
        val docRef = projectRef.collection("expenses").document(oldExpense.id)

        val updates = hashMapOf<String, Any?>(
            "supplierName" to cleanSupplier,
            "supplierPhone" to supplierPhone?.trim()?.ifBlank { null },
            "amount" to amount,
            "date" to date,
            "paidByUid" to paidByUid,
            "note" to note?.trim()?.ifBlank { null },
            "type" to type,
            "paymentMode" to paymentMode,
            "attachments" to attachments.map { it.toMap() },
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByUid" to currentUserId
        )

        val removedAttachments = oldExpense.attachments.filter { oldAtt ->
            attachments.none { it.storagePath == oldAtt.storagePath }
        }

        return try {
            docRef.update(updates).await()

            // Clean up removed attachments from Firebase Storage
            for (removed in removedAttachments) {
                try {
                    AttachmentManager.deleteAttachment(removed.storagePath)
                } catch (_: Exception) {
                    // Non-fatal
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeAttachment(
        projectId: String,
        expense: Expense,
        attachmentToRemove: ExpenseAttachment,
        currentUserId: String,
        currentUserName: String = ""
    ): Result<Unit> {
        requireUserId()
        val projectRef = db.collection("projects").document(projectId)
        val docRef = projectRef.collection("expenses").document(expense.id)

        val newAttachments = expense.attachments.filter { it.storagePath != attachmentToRemove.storagePath }

        val updates = hashMapOf<String, Any?>(
            "attachments" to newAttachments.map { it.toMap() },
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByUid" to currentUserId
        )

        return try {
            docRef.update(updates).await()

            // Delete actual file from Firebase Storage
            AttachmentManager.deleteAttachment(attachmentToRemove.storagePath)

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addAttachment(
        projectId: String,
        expense: Expense,
        newAttachment: ExpenseAttachment,
        currentUserId: String,
        currentUserName: String = ""
    ): Result<Unit> {
        requireUserId()
        val projectRef = db.collection("projects").document(projectId)
        val docRef = projectRef.collection("expenses").document(expense.id)

        val updatedAttachments = expense.attachments + newAttachment

        val updates = hashMapOf<String, Any?>(
            "attachments" to updatedAttachments.map { it.toMap() },
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByUid" to currentUserId
        )

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun softDeleteExpense(
        projectId: String,
        expense: Expense,
        currentUserId: String,
        currentUserName: String = ""
    ): Result<Unit> {
        requireUserId()
        val projectRef = db.collection("projects").document(projectId)
        val docRef = projectRef.collection("expenses").document(expense.id)

        val updates = hashMapOf<String, Any?>(
            "isDeleted" to true,
            "deletedByUid" to currentUserId,
            "deletedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByUid" to currentUserId
        )

        return try {
            docRef.update(updates).await()

            // Delete all associated files from Firebase Storage
            for (att in expense.attachments) {
                try {
                    AttachmentManager.deleteAttachment(att.storagePath)
                } catch (_: Exception) {
                    // Non-fatal
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
