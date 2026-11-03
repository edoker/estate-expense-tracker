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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import com.example.data.model.Audit
import com.example.data.model.Expense
import com.example.data.model.ExpenseAttachment
import com.example.data.model.Invite
import com.example.data.model.Project
import com.example.data.model.ProjectMember
import com.example.data.util.formatDate
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
    onDeleteProject: () -> Unit,
    onClearOperationMessage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    var showInviteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteProjectDialog by remember { mutableStateOf(false) }
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
                    title = { Text("Project") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val isAdmin = project.isAdmin(currentUserId)

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("project_detail_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (isAdmin) "Admin access" else "Partner access",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("project_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("project_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export PDF Report") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showExportDialog = true
                                },
                                modifier = Modifier.testTag("menu_export_pdf")
                            )

                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete Project",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteProjectDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_delete_project")
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().testTag("project_tabs")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    text = { Text("Expenses") },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_expenses")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    text = { Text("Audit Trail") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_audit")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    text = { Text("Members") },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_members")
                )
            }

            when (selectedTab) {
                0 -> ExpensesTab(
                    projectId = project.id,
                    expenses = expenses,
                    members = project.members,
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    onGenerateExpenseId = onGenerateExpenseId,
                    onCreateExpense = onCreateExpense,
                    onUpdateExpense = onUpdateExpense,
                    onDeleteExpense = onDeleteExpense,
                    onRemoveAttachment = onRemoveAttachment
                )
                1 -> AuditTrailTab(
                    auditTrail = auditTrail
                )
                2 -> MembersTab(
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    members = project.members,
                    pendingInvites = pendingInvites,
                    onOpenInviteDialog = { showInviteDialog = true },
                    onCancelInvite = onCancelInvite
                )
            }
        }
    }

    if (showInviteDialog) {
        InvitePartnerDialog(
            currentMembers = project.members,
            onDismiss = { showInviteDialog = false },
            onInvite = { email ->
                showInviteDialog = false
                onInvitePartner(email)
            }
        )
    }

    if (showDeleteProjectDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteProjectDialog = false },
            title = {
                Text(
                    text = "Delete Project?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${project.name}\"? All transactions, receipts, audit history, and partner invitations will be deleted for everyone. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteProjectDialog = false
                        onDeleteProject()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_project_button")
                ) {
                    Text("Delete Project")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteProjectDialog = false },
                    modifier = Modifier.testTag("cancel_delete_project_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportDialog) {
        ExportReportDialog(
            projectName = project.name,
            transactions = expenses,
            members = project.members,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun MembersTab(
    currentUserId: String,
    isAdmin: Boolean,
    members: Map<String, ProjectMember>,
    pendingInvites: List<Invite>,
    onOpenInviteDialog: () -> Unit,
    onCancelInvite: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Members Header & Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Project Partners (${members.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Authorized Google accounts on this project",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ONLY admin sees "Invite partner" button
                if (isAdmin) {
                    Button(
                        onClick = onOpenInviteDialog,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("invite_partner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Invite Partner")
                    }
                }
            }
        }

        // Active Members List
        items(members.toList(), key = { it.first }) { (uid, member) ->
            val isCurrent = uid == currentUserId
            val isMemberAdmin = member.role == "admin"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_item_${uid}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMemberAdmin) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = member.name.firstOrNull()?.uppercaseChar()?.toString()
                            ?: member.email.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isMemberAdmin) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.name.ifBlank { "Partner" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(You)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = member.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isMemberAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isMemberAdmin) "Admin" else "Member",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMemberAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Pending Invites Section
        if (pendingInvites.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pending Invitations (${pendingInvites.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(pendingInvites, key = { it.id }) { invite ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_pending_invite_${invite.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = invite.invitedEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Invited by ${invite.invitedByName} · ${invite.createdAt.formatDate()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Admin can cancel a pending invite
                        if (isAdmin) {
                            OutlinedButton(
                                onClick = { onCancelInvite(invite.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("cancel_invite_button_${invite.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Cancel Invite",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "Pending",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
