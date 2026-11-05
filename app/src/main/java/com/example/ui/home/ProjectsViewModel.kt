package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Invite
import com.example.data.model.Project
import com.example.data.model.User
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
    private val currentUser: User
) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository
        .observeUserProjects(currentUser.uid)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val pendingInvites: StateFlow<List<Invite>> = projectRepository
        .observePendingInvitesForEmail(currentUser.email)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun createProject(name: String, onSuccess: (String) -> Unit) {
        if (name.isBlank()) {
            _operationMessage.value = "Project name cannot be empty"
            return
        }
        _isProcessing.value = true
        viewModelScope.launch {
            val result = projectRepository.createProject(name, currentUser)
            _isProcessing.value = false
            result.onSuccess { projectId ->
                // Clear any pending message before immediate navigation
                _operationMessage.value = null
                onSuccess(projectId)
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to create project"
            }
        }
    }

    fun notifyProjectDeleted() {
        _operationMessage.value = "Project deleted successfully"
    }

    fun acceptInvite(invite: Invite) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = projectRepository.acceptInvite(invite, currentUser)
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = "Joined project '${invite.projectName}'"
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to accept invite"
            }
        }
    }

    fun declineInvite(invite: Invite) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = projectRepository.declineInvite(invite.id)
            _isProcessing.value = false
            result.onSuccess {
                _operationMessage.value = "Declined invitation to '${invite.projectName}'"
            }.onFailure { error ->
                _operationMessage.value = error.localizedMessage ?: "Failed to decline invite"
            }
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    fun postOperationMessage(message: String) {
        _operationMessage.value = message
    }
}
