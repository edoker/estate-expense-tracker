package com.example.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.data.model.ProjectMember
import com.example.data.util.ExcelReportGenerator
import com.example.data.util.PdfReportGenerator
import com.example.data.util.formatDate
import com.example.data.util.formatIndianCurrency
import com.example.ui.components.PartnerBottomSheet
import com.example.ui.components.PartnerCard
import com.example.ui.components.PartnerChip
import com.example.ui.components.PartnerPrimaryButton
import com.example.ui.theme.PartnerTheme
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date

enum class ExportFormat(val label: String) {
    PDF("PDF Document"),
    EXCEL("Excel Sheet (.xlsx)")
}

enum class DateRangePreset(val label: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    CUSTOM("Custom Range")
}

enum class ExportTypeFilter(val label: String) {
    ALL("All"),
    YOU_GAVE("You Gave"),
    YOU_GOT("You Got")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportDialog(
    projectName: String,
    transactions: List<Expense>,
    members: Map<String, ProjectMember>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedPreset by remember { mutableStateOf(DateRangePreset.THIS_MONTH) }
    var selectedTypeFilter by remember { mutableStateOf(ExportTypeFilter.ALL) }
    var selectedMemberUid by remember { mutableStateOf<String?>(null) }
    var memberFilterMenuExpanded by remember { mutableStateOf(false) }

    val now = Calendar.getInstance()
    val startOfMonth = (now.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    var customStartDate by remember { mutableLongStateOf(startOfMonth) }
    var customEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, selectedPreset, customStartDate, customEndDate, selectedTypeFilter, selectedMemberUid) {
        val (startTime, endTime) = when (selectedPreset) {
            DateRangePreset.ALL_TIME -> 0L to Long.MAX_VALUE
            DateRangePreset.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis to System.currentTimeMillis()
            }
            DateRangePreset.LAST_30_DAYS -> {
                val start = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                start to System.currentTimeMillis()
            }
            DateRangePreset.CUSTOM -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = customEndDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                customStartDate to cal.timeInMillis
            }
        }

        transactions.filter { txn ->
            val txnTime = (txn.date ?: txn.createdAt ?: Timestamp.now()).toDate().time
            val inDateRange = txnTime in startTime..endTime
            val inType = when (selectedTypeFilter) {
                ExportTypeFilter.ALL -> true
                ExportTypeFilter.YOU_GAVE -> !txn.isIncome
                ExportTypeFilter.YOU_GOT -> txn.isIncome
            }
            val inMember = selectedMemberUid == null || txn.paidByUid == selectedMemberUid
            inDateRange && inType && inMember
        }
    }

    val totalGiven = remember(filteredTransactions) {
        filteredTransactions.filter { !it.isIncome }.sumOf { it.amount }
    }
    val totalReceived = remember(filteredTransactions) {
        filteredTransactions.filter { it.isIncome }.sumOf { it.amount }
    }
    val netBalance = totalReceived - totalGiven

    PartnerBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        testTag = "export_report_dialog"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Export Report",
                    style = typography.screenTitle,
                    color = colors.text
                )
                Text(
                    text = "CANCEL",
                    style = typography.smallLabel,
                    color = colors.textMuted,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .testTag("cancel_export_button")
                )
            }

            // 1. Format Selection: PDF / Excel chips
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "REPORT FORMAT",
                    style = typography.smallLabel,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PartnerChip(
                        label = "PDF Document",
                        selected = selectedFormat == ExportFormat.PDF,
                        onClick = { selectedFormat = ExportFormat.PDF },
                        modifier = Modifier.weight(1f),
                        testTag = "format_pdf_chip"
                    )

                    PartnerChip(
                        label = "Excel Sheet (.xlsx)",
                        selected = selectedFormat == ExportFormat.EXCEL,
                        onClick = { selectedFormat = ExportFormat.EXCEL },
                        modifier = Modifier.weight(1f),
                        testTag = "format_excel_chip"
                    )
                }
            }

            // 2. Date Range: Preset chips + Custom Range as full-width row
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DATE PERIOD",
                    style = typography.smallLabel,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PartnerChip(
                        label = "All Time",
                        selected = selectedPreset == DateRangePreset.ALL_TIME,
                        onClick = { selectedPreset = DateRangePreset.ALL_TIME },
                        modifier = Modifier.weight(1f)
                    )
                    PartnerChip(
                        label = "This Month",
                        selected = selectedPreset == DateRangePreset.THIS_MONTH,
                        onClick = { selectedPreset = DateRangePreset.THIS_MONTH },
                        modifier = Modifier.weight(1f)
                    )
                    PartnerChip(
                        label = "Last 30 Days",
                        selected = selectedPreset == DateRangePreset.LAST_30_DAYS,
                        onClick = { selectedPreset = DateRangePreset.LAST_30_DAYS },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Range as full-width row per mockup specification
                PartnerChip(
                    label = "Custom Date Range",
                    selected = selectedPreset == DateRangePreset.CUSTOM,
                    onClick = { selectedPreset = DateRangePreset.CUSTOM },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "preset_custom_chip"
                )

                if (selectedPreset == DateRangePreset.CUSTOM) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FROM", style = typography.smallLabel, color = colors.textMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(dimens.buttonRadius))
                                    .background(colors.surface)
                                    .border(dimens.standardBorder, colors.border, RoundedCornerShape(dimens.buttonRadius))
                                    .clickable { showStartDatePicker = true }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = Timestamp(Date(customStartDate)).formatDate(),
                                    style = typography.bodyBold,
                                    color = colors.text
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("TO", style = typography.smallLabel, color = colors.textMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(dimens.buttonRadius))
                                    .background(colors.surface)
                                    .border(dimens.standardBorder, colors.border, RoundedCornerShape(dimens.buttonRadius))
                                    .clickable { showEndDatePicker = true }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = Timestamp(Date(customEndDate)).formatDate(),
                                    style = typography.bodyBold,
                                    color = colors.text
                                )
                            }
                        }
                    }
                }
            }

            // 3. Type & Member Filter Chips
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TRANSACTION TYPE & MEMBER",
                    style = typography.smallLabel,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PartnerChip(
                        label = "All",
                        selected = selectedTypeFilter == ExportTypeFilter.ALL,
                        onClick = { selectedTypeFilter = ExportTypeFilter.ALL }
                    )
                    PartnerChip(
                        label = "Gave",
                        selected = selectedTypeFilter == ExportTypeFilter.YOU_GAVE,
                        onClick = { selectedTypeFilter = ExportTypeFilter.YOU_GAVE }
                    )
                    PartnerChip(
                        label = "Got",
                        selected = selectedTypeFilter == ExportTypeFilter.YOU_GOT,
                        onClick = { selectedTypeFilter = ExportTypeFilter.YOU_GOT }
                    )

                    // Member selector chip
                    Box {
                        val memberLabel = if (selectedMemberUid == null) "All Members ⌵" else {
                            val name = members[selectedMemberUid]?.name?.ifBlank { members[selectedMemberUid]?.email } ?: "Partner"
                            "${name.take(6)} ⌵"
                        }

                        PartnerChip(
                            label = memberLabel,
                            selected = selectedMemberUid != null,
                            onClick = { memberFilterMenuExpanded = true }
                        )

                        DropdownMenu(
                            expanded = memberFilterMenuExpanded,
                            onDismissRequest = { memberFilterMenuExpanded = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Members", style = typography.body, color = colors.text) },
                                onClick = {
                                    selectedMemberUid = null
                                    memberFilterMenuExpanded = false
                                }
                            )
                            members.forEach { (uid, member) ->
                                val name = member.name.ifBlank { member.email }
                                DropdownMenuItem(
                                    text = { Text(name, style = typography.body, color = colors.text) },
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

            // 4. Summary Preview
            PartnerCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ENTRIES TO EXPORT", style = typography.smallLabel, color = colors.textMuted)
                        Text("${filteredTransactions.size} records", style = typography.bodyBold, color = colors.text)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL GIVEN", style = typography.smallLabel, color = colors.textMuted)
                        Text(formatIndianCurrency(totalGiven), style = typography.bodyBold, color = colors.dangerRed)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL RECEIVED", style = typography.smallLabel, color = colors.textMuted)
                        Text(formatIndianCurrency(totalReceived), style = typography.bodyBold, color = colors.accent)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NET BALANCE", style = typography.smallLabel, color = colors.textMuted)
                        val netColor = if (netBalance >= 0) colors.accent else colors.dangerRed
                        val prefix = if (netBalance >= 0) "+ " else "− "
                        Text("$prefix${formatIndianCurrency(kotlin.math.abs(netBalance))}", style = typography.bodyBold, color = netColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary action: Export Report
            val selectedMemberName = selectedMemberUid?.let { members[it]?.name?.ifBlank { members[it]?.email } }
            PartnerPrimaryButton(
                text = "EXPORT ${if (selectedFormat == ExportFormat.PDF) "PDF" else "EXCEL"} REPORT",
                onClick = {
                    val dateRangeString = when (selectedPreset) {
                        DateRangePreset.ALL_TIME -> "All Time"
                        DateRangePreset.THIS_MONTH -> "This Month"
                        DateRangePreset.LAST_30_DAYS -> "Last 30 Days"
                        DateRangePreset.CUSTOM -> "${Timestamp(Date(customStartDate)).formatDate()} - ${Timestamp(Date(customEndDate)).formatDate()}"
                    }

                    if (selectedFormat == ExportFormat.PDF) {
                        val file = PdfReportGenerator.generateReport(
                            context = context,
                            projectName = projectName,
                            dateRangeLabel = dateRangeString,
                            transactions = filteredTransactions,
                            members = members,
                            memberFilterLabel = selectedMemberName
                        )
                        PdfReportGenerator.sharePdf(context, file)
                    } else {
                        val file = ExcelReportGenerator.generateReport(
                            context = context,
                            projectName = projectName,
                            dateRangeLabel = dateRangeString,
                            transactions = filteredTransactions,
                            members = members,
                            memberFilterLabel = selectedMemberName
                        )
                        ExcelReportGenerator.shareExcel(context, file)
                    }
                    onDismiss()
                },
                enabled = filteredTransactions.isNotEmpty(),
                testTag = "confirm_export_button"
            )
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customStartDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { customStartDate = it }
                    showStartDatePicker = false
                }) {
                    Text("OK", style = typography.bodyBold, color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", style = typography.body, color = colors.textMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customEndDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { customEndDate = it }
                    showEndDatePicker = false
                }) {
                    Text("OK", style = typography.bodyBold, color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel", style = typography.body, color = colors.textMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
