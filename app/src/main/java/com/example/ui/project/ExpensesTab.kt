package com.example.ui.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.ExpenseAttachment
import com.example.data.model.ProjectMember
import com.example.data.util.formatDate
import com.example.data.util.formatIndianCurrency
import com.google.firebase.Timestamp

enum class ExpenseTabFilter(val label: String) {
    ALL("All"),
    YOU_GAVE("You Gave"),
    YOU_GOT("You Got")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesTab(
    projectId: String,
    expenses: List<Expense>,
    members: Map<String, ProjectMember>,
    currentUserId: String,
    currentUserName: String,
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
    onRemoveAttachment: (expense: Expense, attachment: ExpenseAttachment) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var initialFormType by remember { mutableStateOf("GAVE") }
    var selectedExpenseForDetail by remember { mutableStateOf<Expense?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    var isFabExpanded by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(ExpenseTabFilter.ALL) }

    val totalGave = remember(expenses) {
        expenses.filter { !it.isIncome }.sumOf { it.amount }
    }
    val totalGot = remember(expenses) {
        expenses.filter { it.isIncome }.sumOf { it.amount }
    }
    val netBalance = totalGot - totalGave

    val filteredExpenses = remember(expenses, activeFilter) {
        when (activeFilter) {
            ExpenseTabFilter.ALL -> expenses
            ExpenseTabFilter.YOU_GAVE -> expenses.filter { !it.isIncome }
            ExpenseTabFilter.YOU_GOT -> expenses.filter { it.isIncome }
        }
    }

    val partnerTotals = remember(expenses, members) {
        members.mapValues { (uid, _) ->
            val memberGave = expenses.filter { it.paidByUid == uid && !it.isIncome }.sumOf { it.amount }
            val memberGot = expenses.filter { it.paidByUid == uid && it.isIncome }.sumOf { it.amount }
            memberGave to memberGot
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Speed Dial Actions
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option 1: You Got (Received)
                        ExtendedFloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                initialFormType = "GOT"
                                showAddForm = true
                            },
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White,
                            icon = { Icon(Icons.Default.CallReceived, contentDescription = null) },
                            text = { Text("You Got (Received)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("fab_add_you_got")
                        )

                        // Option 2: You Gave (Paid)
                        ExtendedFloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                initialFormType = "GAVE"
                                showAddForm = true
                            },
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                            icon = { Icon(Icons.Default.CallMade, contentDescription = null) },
                            text = { Text("You Gave (Paid)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("fab_add_you_gave")
                        )
                    }
                }

                // Main FAB Button
                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isFabExpanded) "Close" else "Add Transaction"
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -------------------------------------------------------------
            // Summary Dashboard Card
            // -------------------------------------------------------------
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_summary_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Project Cashflow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${expenses.size} transaction${if (expenses.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Summary Statistics Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // You Got Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CallReceived,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "You Got",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Text(
                                        text = formatIndianCurrency(totalGot),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            // You Gave Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CallMade,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "You Gave",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                    Text(
                                        text = formatIndianCurrency(totalGave),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }

                        // Net Balance row
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Balance (In - Out)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val netColor = if (netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFDC2626)
                            val prefix = if (netBalance > 0) "+" else ""
                            Text(
                                text = "$prefix${formatIndianCurrency(netBalance)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = netColor
                            )
                        }

                        // Partner Breakdown
                        if (members.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Partner Breakdown",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    members.forEach { (uid, member) ->
                                        val isMe = uid == currentUserId
                                        val (memberGave, memberGot) = partnerTotals[uid] ?: (0.0 to 0.0)
                                        val displayName = member.name.ifBlank { member.email }
                                        val labelName = if (isMe) "$displayName (Me)" else displayName

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = labelName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Gave: ${formatIndianCurrency(memberGave)} | Got: ${formatIndianCurrency(memberGot)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Filter Chips (All, You Gave, You Got)
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseTabFilter.values().forEach { filter ->
                        val count = when (filter) {
                            ExpenseTabFilter.ALL -> expenses.size
                            ExpenseTabFilter.YOU_GAVE -> expenses.count { !it.isIncome }
                            ExpenseTabFilter.YOU_GOT -> expenses.count { it.isIncome }
                        }
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text("${filter.label} ($count)") },
                            modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // Transaction List Items
            // -------------------------------------------------------------
            if (filteredExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Log payments (You Gave) or money received (You Got). All partners can view and record entries.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    val isIncome = expense.isIncome
                    val handlerName = members[expense.paidByUid]?.name?.ifBlank { members[expense.paidByUid]?.email }
                        ?: "Partner"
                    val entererName = members[expense.enteredByUid]?.name?.ifBlank { members[expense.enteredByUid]?.email }
                        ?: "Partner"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedExpenseForDetail = expense }
                            .testTag("expense_item_${expense.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Transaction Type Icon Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFEE2E2)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isIncome) Icons.Default.CallReceived else Icons.Default.CallMade,
                                            contentDescription = null,
                                            tint = if (isIncome) Color(0xFF2E7D32) else Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = expense.supplierName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!expense.supplierPhone.isNullOrBlank()) {
                                            Text(
                                                text = expense.supplierPhone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Amount with + or -
                                Text(
                                    text = (if (isIncome) "+" else "-") + formatIndianCurrency(expense.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFDC2626),
                                    modifier = Modifier.testTag("expense_amount_${expense.id}")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val actionLabel = if (isIncome) "Received by $handlerName" else "Paid by $handlerName"
                                Text(
                                    text = "$actionLabel · by $entererName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )

                                Text(
                                    text = expense.date.formatDate(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Badges Row: Type Badge, Payment Mode, Attachments, Notes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Type Chip
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (isIncome) "YOU GOT" else "YOU GAVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFDC2626),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                // Payment Mode Chip
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        val modeIcon = when (expense.paymentMode) {
                                            "Cash" -> Icons.Default.Payments
                                            "Cheque" -> Icons.Default.ReceiptLong
                                            "Netbanking" -> Icons.Default.AccountBalance
                                            else -> Icons.Default.QrCode2
                                        }
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = expense.paymentMode.ifBlank { "UPI" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (expense.attachments.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${expense.attachments.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedExpenseForDetail?.let { expense ->
        ExpenseDetailDialog(
            expense = expense,
            members = members,
            onDismiss = { selectedExpenseForDetail = null },
            onEdit = {
                expenseToEdit = expense
                selectedExpenseForDetail = null
            },
            onDelete = {
                onDeleteExpense(expense)
                selectedExpenseForDetail = null
            },
            onRemoveAttachment = { att ->
                onRemoveAttachment(expense, att)
                selectedExpenseForDetail = null
            }
        )
    }

    // Add Form Dialog
    if (showAddForm) {
        val targetExpenseId = remember { onGenerateExpenseId() }
        ExpenseFormDialog(
            projectId = projectId,
            initialExpense = null,
            targetExpenseId = targetExpenseId,
            initialType = initialFormType,
            members = members,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onDismiss = { showAddForm = false },
            onSave = { name, phone, amount, date, paidBy, note, type, mode, attachments, expenseId ->
                onCreateExpense(name, phone, amount, date, paidBy, note, type, mode, attachments, expenseId)
                showAddForm = false
            }
        )
    }

    // Edit Form Dialog
    expenseToEdit?.let { expense ->
        ExpenseFormDialog(
            projectId = projectId,
            initialExpense = expense,
            targetExpenseId = expense.id,
            initialType = expense.type,
            members = members,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onDismiss = { expenseToEdit = null },
            onSave = { name, phone, amount, date, paidBy, note, type, mode, attachments, _ ->
                onUpdateExpense(expense, name, phone, amount, date, paidBy, note, type, mode, attachments)
                expenseToEdit = null
            }
        )
    }
}
