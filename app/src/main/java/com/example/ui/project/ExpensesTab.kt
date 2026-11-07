package com.example.ui.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.ExpenseAttachment
import com.example.data.model.ProjectMember
import com.example.data.util.formatDate
import com.example.data.util.formatIndianCurrency
import com.example.ui.components.PartnerCard
import com.example.ui.components.PartnerChip
import com.example.ui.components.PartnerChipVariant
import com.example.ui.components.PartnerDateDivider
import com.example.ui.components.PartnerIcon
import com.example.ui.components.PartnerIconType
import com.example.ui.theme.PartnerTheme
import com.google.firebase.Timestamp

enum class LedgerFilter {
    ALL,
    GAVE,
    GOT
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
    onRemoveAttachment: (expense: Expense, attachment: ExpenseAttachment) -> Unit,
    onExpenseClick: (Expense) -> Unit = {}
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    var showAddSheet by remember { mutableStateOf(false) }

    var activeFilter by remember { mutableStateOf(LedgerFilter.ALL) }
    var selectedMemberUid by remember { mutableStateOf<String?>(null) }
    var memberFilterMenuExpanded by remember { mutableStateOf(false) }
    var partnerBreakdownExpanded by remember { mutableStateOf(false) }

    val totalGave = remember(expenses) {
        expenses.filter { !it.isIncome }.sumOf { it.amount }
    }
    val totalGot = remember(expenses) {
        expenses.filter { it.isIncome }.sumOf { it.amount }
    }
    val netBalance = totalGot - totalGave

    val filteredExpenses = remember(expenses, activeFilter, selectedMemberUid) {
        expenses.filter { txn ->
            val matchesType = when (activeFilter) {
                LedgerFilter.ALL -> true
                LedgerFilter.GAVE -> !txn.isIncome
                LedgerFilter.GOT -> txn.isIncome
            }
            val matchesMember = selectedMemberUid == null || txn.paidByUid == selectedMemberUid
            matchesType && matchesMember
        }
    }

    // Group filtered expenses by formatted date string
    val groupedExpenses = remember(filteredExpenses) {
        filteredExpenses.groupBy {
            (it.date ?: it.createdAt ?: Timestamp.now()).formatDate()
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
            // Single "+" button bottom right that opens Add sheet directly
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = colors.accentButtonFill,
                contentColor = colors.accentButtonText,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_transaction_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Entry",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = colors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = dimens.screenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -----------------------------------------------------------------
            // Summary Dashboard Card: Net balance large, Got/Gave, collapsible partner breakdown
            // -----------------------------------------------------------------
            item {
                PartnerCard(
                    modifier = Modifier.testTag("transaction_summary_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "NET BALANCE",
                            style = typography.smallLabel,
                            color = colors.textMuted
                        )

                        // Net Balance large (30px/700)
                        val netSign = when {
                            netBalance > 0 -> "+ "
                            netBalance < 0 -> "− "
                            else -> ""
                        }
                        val netColor = when {
                            netBalance > 0 -> colors.gotGreen
                            netBalance < 0 -> colors.gaveRed
                            else -> colors.text
                        }
                        Text(
                            text = "$netSign${formatIndianCurrency(kotlin.math.abs(netBalance))}",
                            style = typography.netBalance,
                            color = netColor,
                            modifier = Modifier.testTag("net_balance_text")
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Got, Gave, and Expandable Partners row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GOT",
                                    style = typography.smallLabel,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = formatIndianCurrency(totalGot),
                                    style = typography.cardName,
                                    color = colors.gotGreen
                                )
                            }

                            Column {
                                Text(
                                    text = "GAVE",
                                    style = typography.smallLabel,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = formatIndianCurrency(totalGave),
                                    style = typography.cardName,
                                    color = colors.gaveRed
                                )
                            }

                            // Collapsible "X PARTNERS ›" row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { partnerBreakdownExpanded = !partnerBreakdownExpanded }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("toggle_partner_breakdown")
                            ) {
                                Text(
                                    text = "${members.size} PARTNERS ${if (partnerBreakdownExpanded) "⌵" else "›"}",
                                    style = typography.smallLabel,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Collapsed partner breakdown expanded view
                        AnimatedVisibility(
                            visible = partnerBreakdownExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.divider)
                                )

                                members.forEach { (uid, member) ->
                                    val isMe = uid == currentUserId
                                    val (memberGave, memberGot) = partnerTotals[uid] ?: (0.0 to 0.0)
                                    val displayName = member.name.ifBlank { member.email }
                                    val labelName = if (isMe) "$displayName (Me)" else displayName

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = labelName,
                                            style = typography.body,
                                            color = colors.text
                                        )
                                        Text(
                                            text = "Gave ${formatIndianCurrency(memberGave)} · Got ${formatIndianCurrency(memberGot)}",
                                            style = typography.notes,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // Filter Chips: All, Gave, Got, Member dropdown with counts
            // -----------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countAll = if (selectedMemberUid == null) expenses.size else expenses.count { it.paidByUid == selectedMemberUid }
                    val countGave = if (selectedMemberUid == null) expenses.count { !it.isIncome } else expenses.count { !it.isIncome && it.paidByUid == selectedMemberUid }
                    val countGot = if (selectedMemberUid == null) expenses.count { it.isIncome } else expenses.count { it.isIncome && it.paidByUid == selectedMemberUid }

                    PartnerChip(
                        label = "All $countAll",
                        selected = activeFilter == LedgerFilter.ALL,
                        onClick = { activeFilter = LedgerFilter.ALL },
                        variant = PartnerChipVariant.DEFAULT,
                        testTag = "filter_chip_all"
                    )

                    PartnerChip(
                        label = "Gave $countGave",
                        selected = activeFilter == LedgerFilter.GAVE,
                        onClick = { activeFilter = LedgerFilter.GAVE },
                        variant = PartnerChipVariant.GAVE,
                        testTag = "filter_chip_gave"
                    )

                    PartnerChip(
                        label = "Got $countGot",
                        selected = activeFilter == LedgerFilter.GOT,
                        onClick = { activeFilter = LedgerFilter.GOT },
                        variant = PartnerChipVariant.GOT,
                        testTag = "filter_chip_got"
                    )

                    // Member Dropdown Chip
                    Box {
                        val memberLabel = if (selectedMemberUid == null) {
                            "Member: All ⌵"
                        } else {
                            val name = members[selectedMemberUid]?.name?.ifBlank { members[selectedMemberUid]?.email } ?: "Partner"
                            "${name.take(7)} ⌵"
                        }

                        PartnerChip(
                            label = memberLabel,
                            selected = selectedMemberUid != null,
                            onClick = { memberFilterMenuExpanded = true },
                            variant = PartnerChipVariant.DEFAULT,
                            testTag = "filter_chip_member"
                        )

                        DropdownMenu(
                            expanded = memberFilterMenuExpanded,
                            onDismissRequest = { memberFilterMenuExpanded = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Member: All (${expenses.size})", style = typography.body, color = colors.text) },
                                onClick = {
                                    selectedMemberUid = null
                                    memberFilterMenuExpanded = false
                                }
                            )

                            members.forEach { (uid, member) ->
                                val name = member.name.ifBlank { member.email }
                                val count = expenses.count { it.paidByUid == uid }
                                DropdownMenuItem(
                                    text = { Text("$name ($count)", style = typography.body, color = colors.text) },
                                    onClick = {
                                        selectedMemberUid = uid
                                        memberFilterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // Grouped Transaction Cards with Date Dividers
            // -----------------------------------------------------------------
            if (filteredExpenses.isEmpty()) {
                item {
                    PartnerCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No transactions found",
                                style = typography.cardName,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the + button to record payments or receipts.",
                                style = typography.body,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            } else {
                groupedExpenses.forEach { (dateStr, txnsOnDate) ->
                    item(key = "divider_$dateStr") {
                        PartnerDateDivider(dateText = dateStr)
                    }

                    items(txnsOnDate.size, key = { txnsOnDate[it].id }) { index ->
                        val expense = txnsOnDate[index]
                        val isIncome = expense.isIncome
                        val partnerName = members[expense.paidByUid]?.name?.ifBlank { members[expense.paidByUid]?.email } ?: "Partner"
                        val actionLabel = if (isIncome) "Received by $partnerName" else "Paid by $partnerName"

                        PartnerCard(
                            onClick = { onExpenseClick(expense) },
                            modifier = Modifier.testTag("expense_item_${expense.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 48px rounded square with tinted fill and bold direction arrow
                                val iconBoxBg = if (isIncome) colors.gotSoft else colors.gaveSoft
                                val iconBoxBorder = if (isIncome) colors.gotGreen else colors.gaveRed
                                val iconTint = if (isIncome) colors.gotGreen else colors.gaveRed

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(dimens.iconSquareRadius))
                                        .background(iconBoxBg)
                                        .border(dimens.emphasizedBorder, iconBoxBorder, RoundedCornerShape(dimens.iconSquareRadius)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PartnerIcon(
                                        type = if (isIncome) PartnerIconType.ARROW_IN else PartnerIconType.ARROW_OUT,
                                        tint = iconTint,
                                        size = 24.dp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = expense.supplierName,
                                            style = typography.cardName,
                                            color = colors.text,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Signed amount: "− ₹1,25,000" in gaveRed, "+ ₹10,004" in gotGreen
                                        val prefix = if (isIncome) "+ " else "− "
                                        val amountColor = if (isIncome) colors.gotGreen else colors.gaveRed
                                        Text(
                                            text = "$prefix${formatIndianCurrency(expense.amount)}",
                                            style = typography.amountList,
                                            color = amountColor,
                                            modifier = Modifier.testTag("expense_amount_${expense.id}")
                                        )
                                    }

                                    if (!expense.note.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = expense.note,
                                            style = typography.notes,
                                            color = colors.textMuted,
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Chips row: "PAID BY <MEMBER>" and payment mode
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Paid/Received by chip
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.surface)
                                                .border(dimens.standardBorder, colors.border, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = actionLabel.uppercase(),
                                                style = typography.smallLabel.copy(fontSize = 10.sp),
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Payment mode chip
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.surface)
                                                .border(dimens.standardBorder, colors.border, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = expense.paymentMode.ifBlank { "UPI" }.uppercase(),
                                                style = typography.smallLabel.copy(fontSize = 10.sp),
                                                color = colors.textMuted,
                                                maxLines = 1
                                            )
                                        }

                                        if (expense.attachments.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier
                                                    .padding(start = 2.dp)
                                                    .testTag("expense_attachment_indicator_${expense.id}")
                                            ) {
                                                PartnerIcon(
                                                    type = PartnerIconType.PAPERCLIP,
                                                    tint = colors.textMuted,
                                                    size = 13.dp
                                                )
                                                Text(
                                                    text = "${expense.attachments.size}",
                                                    style = typography.smallLabel.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = colors.textMuted
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

            item {
                Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
            }
        }
    }

    // Add entry bottom sheet
    if (showAddSheet) {
        ExpenseFormDialog(
            projectId = projectId,
            initialExpense = null,
            targetExpenseId = onGenerateExpenseId(),
            initialType = "GAVE",
            members = members,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onDismiss = { showAddSheet = false },
            onSave = { supplier, phone, amount, date, paidBy, note, type, mode, attachments, expenseId ->
                showAddSheet = false
                onCreateExpense(supplier, phone, amount, date, paidBy, note, type, mode, attachments, expenseId)
            }
        )
    }
}
