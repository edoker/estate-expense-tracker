package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.AuditRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ProjectRepository
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.SignInScreen
import com.example.ui.components.LoadingScreen
import com.example.ui.components.NotificationPermissionHandler
import com.example.ui.home.HomeScreen
import com.example.ui.home.ProjectsViewModel
import com.example.ui.project.ProjectDetailScreen
import com.example.ui.project.ProjectDetailViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    credentialManager: CredentialManager,
    targetProjectIdFlow: StateFlow<String?> = MutableStateFlow(null)
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        authViewModel.attemptAutoSignIn(context, credentialManager)
    }

    when (val state = authState) {
        is AuthState.Loading -> {
            LoadingScreen(message = "Checking partner credentials...")
        }
        is AuthState.Unauthenticated -> {
            SignInScreen(
                errorMessage = state.errorMessage,
                onSignInClick = {
                    authViewModel.signInWithGoogle(context, credentialManager)
                },
                onDismissError = {
                    authViewModel.dismissError()
                }
            )
        }
        is AuthState.Authenticated -> {
            val navController = rememberNavController()
            val projectRepository = rememberProjectRepository(context)
            val expenseRepository = rememberExpenseRepository(context)
            val auditRepository = rememberAuditRepository(context)

            // Request POST_NOTIFICATIONS permission on Android 13+ with explanation and sync FCM token
            NotificationPermissionHandler(
                onPermissionGranted = {
                    authViewModel.syncFcmToken(context, state.user.uid)
                }
            )

            // Deep-link navigation when opened from notification
            val targetProjectId by targetProjectIdFlow.collectAsStateWithLifecycle()
            LaunchedEffect(targetProjectId) {
                if (!targetProjectId.isNullOrBlank()) {
                    navController.navigate("project/$targetProjectId")
                }
            }

            val projectsViewModel: ProjectsViewModel = viewModel(
                key = "projects_${state.user.uid}",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ProjectsViewModel(projectRepository, state.user) as T
                    }
                }
            )

            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    val projects by projectsViewModel.projects.collectAsStateWithLifecycle()
                    val pendingInvites by projectsViewModel.pendingInvites.collectAsStateWithLifecycle()
                    val operationMessage by projectsViewModel.operationMessage.collectAsStateWithLifecycle()

                    HomeScreen(
                        currentUserId = state.user.uid,
                        displayName = state.user.displayName.ifBlank { "Partner" },
                        email = state.user.email,
                        projects = projects,
                        pendingInvites = pendingInvites,
                        operationMessage = operationMessage,
                        onClearOperationMessage = { projectsViewModel.clearOperationMessage() },
                        onProjectClick = { projectId ->
                            navController.navigate("project/$projectId")
                        },
                        onCreateProject = { name ->
                            projectsViewModel.createProject(name) { newId ->
                                navController.navigate("project/$newId")
                            }
                        },
                        onAcceptInvite = { invite ->
                            projectsViewModel.acceptInvite(invite)
                        },
                        onDeclineInvite = { invite ->
                            projectsViewModel.declineInvite(invite)
                        },
                        onUpdateDisplayName = { newName ->
                            authViewModel.updateDisplayName(newName, projectRepository) { success, errorMsg ->
                                if (success) {
                                    projectsViewModel.postOperationMessage("Name updated to '$newName'")
                                } else if (!errorMsg.isNullOrBlank()) {
                                    projectsViewModel.postOperationMessage(errorMsg)
                                }
                            }
                        },
                        onSignOutClick = {
                            authViewModel.signOut(context, credentialManager)
                        }
                    )
                }

                composable(
                    route = "project/{projectId}",
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val projectId = backStackEntry.arguments?.getString("projectId") ?: ""

                    val detailViewModel: ProjectDetailViewModel = viewModel(
                        key = "project_detail_$projectId",
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return ProjectDetailViewModel(
                                    projectId,
                                    projectRepository,
                                    expenseRepository,
                                    auditRepository,
                                    state.user
                                ) as T
                            }
                        }
                    )

                    LaunchedEffect(state.user) {
                        detailViewModel.updateCurrentUser(state.user)
                    }

                    val project by detailViewModel.project.collectAsStateWithLifecycle()
                    val expenses by detailViewModel.expenses.collectAsStateWithLifecycle()
                    val auditTrail by detailViewModel.auditTrail.collectAsStateWithLifecycle()
                    val pendingInvites by detailViewModel.pendingInvites.collectAsStateWithLifecycle()
                    val selectedTab by detailViewModel.selectedTab.collectAsStateWithLifecycle()
                    val operationMessage by detailViewModel.operationMessage.collectAsStateWithLifecycle()

                    ProjectDetailScreen(
                        currentUserId = state.user.uid,
                        currentUserName = state.user.displayName.ifBlank { "Partner" },
                        project = project,
                        expenses = expenses,
                        auditTrail = auditTrail,
                        pendingInvites = pendingInvites,
                        selectedTab = selectedTab,
                        operationMessage = operationMessage,
                        onSelectTab = { detailViewModel.selectTab(it) },
                        onGenerateExpenseId = { detailViewModel.generateExpenseId() },
                        onCreateExpense = { supplier, phone, amount, date, paidBy, note, type, mode, attachments, expenseId ->
                            detailViewModel.createExpense(supplier, phone, amount, date, paidBy, note, type, mode, attachments, expenseId) {}
                        },
                        onUpdateExpense = { oldExpense, supplier, phone, amount, date, paidBy, note, type, mode, attachments ->
                            detailViewModel.updateExpense(oldExpense, supplier, phone, amount, date, paidBy, note, type, mode, attachments) {}
                        },
                        onDeleteExpense = { expense ->
                            detailViewModel.softDeleteExpense(expense) {}
                        },
                        onRemoveAttachment = { expense, attachment ->
                            detailViewModel.removeAttachment(expense, attachment) {}
                        },
                        onInvitePartner = { email ->
                            detailViewModel.invitePartner(email) {}
                        },
                        onCancelInvite = { inviteId ->
                            detailViewModel.cancelInvite(inviteId)
                        },
                        onRemoveMember = { targetUid, name ->
                            detailViewModel.removeMember(targetUid, name)
                        },
                        onDeleteProject = {
                            detailViewModel.deleteProject {
                                projectsViewModel.notifyProjectDeleted()
                                android.widget.Toast.makeText(context, "Project deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        },
                        onClearOperationMessage = { detailViewModel.clearOperationMessage() },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberProjectRepository(context: android.content.Context): ProjectRepository {
    return androidx.compose.runtime.remember(context) {
        ProjectRepository(context)
    }
}

@Composable
private fun rememberExpenseRepository(context: android.content.Context): ExpenseRepository {
    return androidx.compose.runtime.remember(context) {
        ExpenseRepository(context)
    }
}

@Composable
private fun rememberAuditRepository(context: android.content.Context): AuditRepository {
    return androidx.compose.runtime.remember(context) {
        AuditRepository(context)
    }
}
