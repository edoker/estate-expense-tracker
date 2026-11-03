package com.example.ui.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.data.model.ProjectMember
import com.example.data.util.PdfReportGenerator
import com.example.data.util.formatDate
import com.example.data.util.formatIndianCurrency
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date

enum class DateRangePreset(val label: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    CUSTOM("Custom Range")
}

enum class TransactionTypeFilter(val label: String) {
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

    var selectedPreset by remember { mutableStateOf(DateRangePreset.THIS_MONTH) }
    var selectedTypeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }

    // Calculate dates for This Month
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

    // Filter transactions based on selection
    val filteredTransactions = remember(transactions, selectedPreset, customStartDate, customEndDate, selectedTypeFilter) {
        val (startTime, endTime) = when (selectedPreset) {
            DateRangePreset.ALL_TIME -> 0L to Long.MAX_VALUE
            DateRangePreset.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                start to System.currentTimeMillis()
            }
            DateRangePreset.LAST_30_DAYS -> {
                val start = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                start to System.currentTimeMillis()
            }
            DateRangePreset.CUSTOM -> {
                // Ensure end of day for custom end date
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
            val txnTime = txn.date?.toDate()?.time ?: 0L
            val dateMatches = txnTime in startTime..endTime
            val typeMatches = when (selectedTypeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.YOU_GAVE -> !txn.isIncome
                TransactionTypeFilter.YOU_GOT -> txn.isIncome
            }
            dateMatches && typeMatches
        }
    }

    val totalGave = remember(filteredTransactions) {
        filteredTransactions.filter { !it.isIncome }.sumOf { it.amount }
    }
    val totalGot = remember(filteredTransactions) {
        filteredTransactions.filter { it.isIncome }.sumOf { it.amount }
    }

    val dateRangeLabel = when (selectedPreset) {
        DateRangePreset.ALL_TIME -> "All Time"
        DateRangePreset.THIS_MONTH -> "This Month"
        DateRangePreset.LAST_30_DAYS -> "Last 30 Days"
        DateRangePreset.CUSTOM -> {
            "${Timestamp(Date(customStartDate)).formatDate()} - ${Timestamp(Date(customEndDate)).formatDate()}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Export PDF Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Date Range Section
                Text(
                    text = "Date Period",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DateRangePreset.values().forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset },
                            label = { Text(preset.label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                if (selectedPreset == DateRangePreset.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = Timestamp(Date(customStartDate)).formatDate(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("From") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Select Start Date",
                                    modifier = Modifier.clickable { showStartDatePicker = true }
                                )
                            },
                            modifier = Modifier.weight(1f).clickable { showStartDatePicker = true }
                        )

                        OutlinedTextField(
                            value = Timestamp(Date(customEndDate)).formatDate(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("To") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Select End Date",
                                    modifier = Modifier.clickable { showEndDatePicker = true }
                                )
                            },
                            modifier = Modifier.weight(1f).clickable { showEndDatePicker = true }
                        )
                    }
                }

                // Type Filter Section
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionTypeFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedTypeFilter == filter,
                            onClick = { selectedTypeFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }

                // Report Preview Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Report Summary Preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${filteredTransactions.size} transaction${if (filteredTransactions.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Got (In):", style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatIndianCurrency(totalGot),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Gave (Out):", style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatIndianCurrency(totalGave),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val file = PdfReportGenerator.generateReport(
                            context = context,
                            projectName = projectName,
                            dateRangeLabel = dateRangeLabel,
                            transactions = filteredTransactions,
                            members = members
                        )
                        PdfReportGenerator.sharePdf(context, file)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("share_pdf_report_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                Button(
                    onClick = {
                        val file = PdfReportGenerator.generateReport(
                            context = context,
                            projectName = projectName,
                            dateRangeLabel = dateRangeLabel,
                            transactions = filteredTransactions,
                            members = members
                        )
                        PdfReportGenerator.openPdf(context, file)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("open_pdf_report_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open PDF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Custom Date Pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customStartDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { customStartDate = it }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
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
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
