package com.example.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Audit
import com.example.data.model.Expense
import com.example.data.model.ExpenseAttachment
import com.example.data.model.Invite
import com.example.data.model.Project
import com.example.data.model.User
import com.example.data.repository.AuditRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ProjectRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectDetailViewModel(
    val projectId: String,
    private val projectRepository: ProjectRepository,
    private val expenseRepository: ExpenseRepository,
    private val auditRepository: AuditRepository,
    initialUser: User
) : ViewModel() {

    var currentUser: User = initialUser
        private set

    fun updateCurrentUser(user: User) {
        currentUser = user
    }

    fun generateExpenseId(): String {
        return expenseRepository.generateExpenseId(projectId)
    }

    val project: StateFlow<Project?> = projectRepository
        .observeProject(projectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val pendingInvites: StateFlow<List<Invite>> = projectRepository
        .observePendingInvitesForProject(projectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val expenses: StateFlow<List<Expense>> = expenseRepository
        .observeProjectExpenses(projectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val auditTrail: StateFlow<List<Audit>> = auditRepository
        .observeProjectAudit(projectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(0) // 0: Expenses, 1: Audit Trail, 2: Members
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun invitePartner(email: String, onSuccess: () -> Unit) {
        val currentProj = project.value ?: return
        _isProcessing.value = true
        viewModelScope.launch {
            val result = projectRepository.invitePartner(
                projectId = currentProj.id,
                projectName = currentProj.name,
                invitedEmail = email,
                inviter = currentUser,
                currentMembers = currentProj.members
            )
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = "Invitation sent to $email"
                onSuccess()
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to send invitation"
            }
        }
    }

    fun cancelInvite(inviteId: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = projectRepository.cancelInvite(inviteId)
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = "Invitation cancelled"
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to cancel invitation"
            }
        }
    }

    fun createExpense(
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
        onSuccess: () -> Unit
    ) {
        val rawMembers = project.value?.members ?: emptyMap()
        val members = if (currentUser.displayName.isNotBlank()) {
            rawMembers.mapValues { (uid, member) ->
                if (uid == currentUser.uid) member.copy(name = currentUser.displayName) else member
            }
        } else {
            rawMembers
        }
        val actorName = currentUser.displayName.ifBlank { "Partner" }

        _isProcessing.value = true
        viewModelScope.launch {
            val result = expenseRepository.createExpense(
                projectId = projectId,
                supplierName = supplierName,
                supplierPhone = supplierPhone,
                amount = amount,
                date = date,
                paidByUid = paidByUid,
                note = note,
                type = type,
                paymentMode = paymentMode,
                attachments = attachments,
                expenseId = expenseId,
                currentUserId = currentUser.uid,
                currentUserName = actorName,
                members = members
            )
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = if (type == "GOT") "Income recorded successfully" else "Payment logged successfully"
                onSuccess()
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to log transaction"
            }
        }
    }

    fun updateExpense(
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
        onSuccess: () -> Unit
    ) {
        val rawMembers = project.value?.members ?: emptyMap()
        val members = if (currentUser.displayName.isNotBlank()) {
            rawMembers.mapValues { (uid, member) ->
                if (uid == currentUser.uid) member.copy(name = currentUser.displayName) else member
            }
        } else {
            rawMembers
        }
        val actorName = currentUser.displayName.ifBlank { "Partner" }

        _isProcessing.value = true
        viewModelScope.launch {
            val result = expenseRepository.updateExpense(
                projectId = projectId,
                oldExpense = oldExpense,
                supplierName = supplierName,
                supplierPhone = supplierPhone,
                amount = amount,
                date = date,
                paidByUid = paidByUid,
                note = note,
                type = type,
                paymentMode = paymentMode,
                attachments = attachments,
                currentUserId = currentUser.uid,
                currentUserName = actorName,
                members = members
            )
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = "Transaction updated successfully"
                onSuccess()
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to update transaction"
            }
        }
    }

    fun removeAttachment(
        expense: Expense,
        attachment: ExpenseAttachment,
        onSuccess: () -> Unit
    ) {
        val actorName = currentUser.displayName.ifBlank { "Partner" }

        _isProcessing.value = true
        viewModelScope.launch {
            val result = expenseRepository.removeAttachment(
                projectId = projectId,
                expense = expense,
                attachmentToRemove = attachment,
                currentUserId = currentUser.uid,
                currentUserName = actorName
            )
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = "Attachment removed"
                onSuccess()
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to remove attachment"
            }
        }
    }

    fun softDeleteExpense(
        expense: Expense,
        onSuccess: () -> Unit
    ) {
        val actorName = currentUser.displayName.ifBlank { "Partner" }

        _isProcessing.value = true
        viewModelScope.launch {
            val result = expenseRepository.softDeleteExpense(
                projectId = projectId,
                expense = expense,
                currentUserId = currentUser.uid,
                currentUserName = actorName
            )
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = "Transaction deleted"
                onSuccess()
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to delete transaction"
            }
        }
    }

    fun deleteProject(onSuccess: () -> Unit) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = projectRepository.deleteProject(projectId)
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = "Project deleted successfully"
                onSuccess()
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to delete project"
            }
        }
    }

    fun removeMember(targetUid: String, memberName: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = projectRepository.removeMember(projectId, targetUid)
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = "$memberName removed from project"
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to remove member"
            }
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }
}
