package com.example.ui.project

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Audit
import com.example.data.model.Expense
import com.example.data.model.ExpenseAttachment
import com.example.data.model.Invite
import com.example.data.model.Project
import com.example.ui.components.DangerousHoldButton
import com.example.ui.components.PartnerBottomSheet
import com.example.ui.components.PartnerIcon
import com.example.ui.components.PartnerIconType
import com.example.ui.components.PartnerInputField
import com.example.ui.components.PartnerOutlinedButton
import com.example.ui.components.PartnerSquareIconButton
import com.example.ui.components.PartnerTabBar
import com.example.ui.theme.PartnerTheme
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    currentUserId: String,
    currentUserName: String,
    project: Project?,
    expenses: List<Expense>,
    auditTrail: List<Audit>,
    pendingInvites: List<Invite>,
    selectedTab: Int,
    operationMessage: String?,
    onSelectTab: (Int) -> Unit,
    onGenerateExpenseId: () -> String,
    onCreateExpense: (
        supplierName: String,
        supplierPhone: String?,
        amount: Double,
        date: Timestamp,
        paidByUid: String,
        note: String?,
        type: String,
        paymentMode: String,
        attachments: List<ExpenseAttachment>,
        expenseId: String
    ) -> Unit,
    onUpdateExpense: (
        oldExpense: Expense,
        supplierName: String,
        supplierPhone: String?,
        amount: Double,
        date: Timestamp,
        paidByUid: String,
        note: String?,
        type: String,
        paymentMode: String,
        attachments: List<ExpenseAttachment>
    ) -> Unit,
    onDeleteExpense: (expense: Expense) -> Unit,
    onRemoveAttachment: (expense: Expense, attachment: ExpenseAttachment) -> Unit,
    onInvitePartner: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onRemoveMember: (targetUid: String, memberName: String) -> Unit,
    onDeleteProject: () -> Unit,
    onClearOperationMessage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    var showInviteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteProjectSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationMessage) {
        if (!operationMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(operationMessage)
            onClearOperationMessage()
        }
    }

    if (project == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Project", style = typography.screenTitle, color = colors.text) },
                    navigationIcon = {
                        PartnerSquareIconButton(
                            iconType = PartnerIconType.BACK,
                            onClick = onNavigateBack,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                )
            },
            containerColor = colors.background
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
        }
        return
    }

    val isAdmin = project.isAdmin(currentUserId)

    // Ensure the current user's latest display name is always reflected in the members map
    val resolvedMembers = remember(project.members, currentUserId, currentUserName) {
        if (currentUserId.isNotBlank() && currentUserName.isNotBlank()) {
            project.members.mapValues { (uid, member) ->
                if (uid == currentUserId) {
                    member.copy(name = currentUserName)
                } else {
                    member
                }
            }
        } else {
            project.members
        }
    }

    var selectedExpenseForDetailId by remember { mutableStateOf<String?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    val selectedExpense = remember(selectedExpenseForDetailId, expenses) {
        if (selectedExpenseForDetailId == null) null
        else expenses.find { it.id == selectedExpenseForDetailId }
    }

    if (selectedExpense != null) {
        ExpenseDetailDialog(
            expense = selectedExpense,
            members = resolvedMembers,
            onDismiss = { selectedExpenseForDetailId = null },
            onEdit = {
                val toEdit = selectedExpense
                selectedExpenseForDetailId = null
                expenseToEdit = toEdit
            },
            onDelete = {
                selectedExpenseForDetailId = null
                onDeleteExpense(selectedExpense)
            },
            onRemoveAttachment = { att ->
                onRemoveAttachment(selectedExpense, att)
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize().testTag("project_detail_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = project.name,
                            style = typography.screenTitle,
                            color = colors.text,
                            maxLines = 1
                        )
                        Text(
                            text = if (isAdmin) "ADMIN ACCESS" else "PARTNER ACCESS",
                            style = typography.smallLabel,
                            color = colors.textMuted
                        )
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        PartnerSquareIconButton(
                            iconType = PartnerIconType.BACK,
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("project_back_button")
                        )
                    }
                },
                actions = {
                    // Export Download Tray Icon
                    PartnerSquareIconButton(
                        iconType = PartnerIconType.EXPORT_TRAY,
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_report_button")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // More Options Icon
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        PartnerSquareIconButton(
                            iconType = PartnerIconType.MORE_VERT,
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("project_menu_button")
                        )

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Report", style = typography.body, color = colors.text) },
                                leadingIcon = {
                                    PartnerIcon(
                                        type = PartnerIconType.EXPORT_TRAY,
                                        tint = colors.accent,
                                        size = 18.dp
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showExportDialog = true
                                },
                                modifier = Modifier.testTag("menu_export_pdf")
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete Project",
                                        style = typography.bodyBold,
                                        color = colors.dangerRed
                                    )
                                },
                                leadingIcon = {
                                    PartnerIcon(
                                        type = PartnerIconType.AUDIT_DELETED,
                                        tint = colors.dangerRed,
                                        size = 18.dp
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteProjectSheet = true
                                },
                                modifier = Modifier.testTag("menu_delete_project")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text
                )
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab bar with icons above labels
            PartnerTabBar(
                selectedTab = selectedTab,
                onTabSelected = onSelectTab
            )

            when (selectedTab) {
                0 -> ExpensesTab(
                    projectId = project.id,
                    expenses = expenses,
                    members = resolvedMembers,
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    onGenerateExpenseId = onGenerateExpenseId,
                    onCreateExpense = onCreateExpense,
                    onUpdateExpense = onUpdateExpense,
                    onDeleteExpense = onDeleteExpense,
                    onRemoveAttachment = onRemoveAttachment,
                    onExpenseClick = { selectedExpenseForDetailId = it.id }
                )
                1 -> AuditTrailTab(
                    auditTrail = auditTrail
                )
                2 -> MembersTab(
                    projectName = project.name,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    members = resolvedMembers,
                    pendingInvites = pendingInvites,
                    onOpenInviteDialog = { showInviteDialog = true },
                    onCancelInvite = onCancelInvite,
                    onRemoveMember = onRemoveMember
                )
            }
        }
    }
}

    if (expenseToEdit != null) {
        ExpenseFormDialog(
            projectId = project.id,
            initialExpense = expenseToEdit,
            targetExpenseId = expenseToEdit!!.id,
            initialType = expenseToEdit!!.type,
            members = resolvedMembers,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onDismiss = { expenseToEdit = null },
            onSave = { supplier, phone, amount, date, paidBy, note, type, mode, attachments, _ ->
                val old = expenseToEdit!!
                expenseToEdit = null
                onUpdateExpense(old, supplier, phone, amount, date, paidBy, note, type, mode, attachments)
            }
        )
    }

    if (showInviteDialog) {
        InvitePartnerDialog(
            currentMembers = resolvedMembers,
            onDismiss = { showInviteDialog = false },
            onInvite = { email ->
                showInviteDialog = false
                onInvitePartner(email)
            }
        )
    }

    // Danger Zone: Delete Project (Type project name + hold 3 sec)
    if (showDeleteProjectSheet) {
        DeleteProjectBottomSheet(
            projectName = project.name,
            onDismiss = { showDeleteProjectSheet = false },
            onConfirmDelete = {
                showDeleteProjectSheet = false
                onDeleteProject()
            }
        )
    }

    if (showExportDialog) {
        ExportReportDialog(
            projectName = project.name,
            transactions = expenses,
            members = resolvedMembers,
            onDismiss = { showExportDialog = false }
        )
    }
}

/**
 * Delete Project Bottom Sheet: Danger zone requiring typing exact name and holding for 3 seconds
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteProjectBottomSheet(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var typedName by remember { mutableStateOf("") }
    val isMatch = typedName.trim().equals(projectName.trim(), ignoreCase = true)

    PartnerBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        testTag = "delete_project_sheet"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Delete $projectName?",
                style = typography.screenTitle,
                color = colors.dangerRed
            )

            Text(
                text = "All transactions, receipts, audit history, and partner invitations will be permanently deleted for all partners. This action cannot be undone.",
                style = typography.body,
                color = colors.textMuted
            )

            PartnerInputField(
                value = typedName,
                onValueChange = { typedName = it },
                label = "TYPE $projectName TO CONFIRM",
                placeholder = "Project name",
                singleLine = true,
                testTag = "delete_project_input"
            )

            Spacer(modifier = Modifier.height(4.dp))

            DangerousHoldButton(
                actionLabel = if (isMatch) "HOLD 3 SEC TO DELETE" else "TYPE NAME TO UNLOCK",
                onHoldComplete = onConfirmDelete,
                enabled = isMatch,
                testTag = "confirm_delete_project_button"
            )

            PartnerOutlinedButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "cancel_delete_project_button"
            )
        }
    }
}
