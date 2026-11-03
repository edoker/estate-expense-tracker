package com.example.ui.project

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.data.model.ExpenseAttachment
import com.example.data.model.ProjectMember
import com.example.data.util.AttachmentManager
import com.example.data.util.formatDate
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

enum class UploadStatus {
    UPLOADING,
    SUCCESS,
    FAILED
}

data class UploadTaskItem(
    val id: String = UUID.randomUUID().toString(),
    val preparedFile: AttachmentManager.PreparedFile,
    val isImage: Boolean,
    var progress: Int = 0,
    var status: UploadStatus = UploadStatus.UPLOADING,
    var error: String? = null,
    var uploadedAttachment: ExpenseAttachment? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    projectId: String,
    initialExpense: Expense? = null,
    targetExpenseId: String = "",
    initialType: String = initialExpense?.type ?: "GAVE",
    members: Map<String, ProjectMember>,
    currentUserId: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Determine the stable expense ID for storage path
    val effectiveExpenseId = remember {
        initialExpense?.id?.ifBlank { null } ?: targetExpenseId.ifBlank { UUID.randomUUID().toString() }
    }

    var type by remember { mutableStateOf(initialExpense?.type ?: initialType) }
    var paymentMode by remember { mutableStateOf(initialExpense?.paymentMode ?: "UPI") }
    var paymentModeDropdownExpanded by remember { mutableStateOf(false) }

    var supplierName by remember { mutableStateOf(initialExpense?.supplierName ?: "") }
    var supplierPhone by remember { mutableStateOf(initialExpense?.supplierPhone ?: "") }
    var amountInput by remember {
        mutableStateOf(if (initialExpense != null && initialExpense.amount > 0) initialExpense.amount.toString() else "")
    }
    var selectedDateMillis by remember {
        mutableLongStateOf(initialExpense?.date?.toDate()?.time ?: System.currentTimeMillis())
    }
    var paidByUid by remember {
        mutableStateOf(initialExpense?.paidByUid?.ifBlank { null } ?: currentUserId)
    }
    var note by remember { mutableStateOf(initialExpense?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var memberDropdownExpanded by remember { mutableStateOf(false) }
    var attachMenuExpanded by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    var supplierNameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var attachmentError by remember { mutableStateOf<String?>(null) }

    // Existing saved attachments (can be removed)
    val existingAttachments = remember {
        mutableStateListOf<ExpenseAttachment>().apply {
            if (initialExpense != null) {
                addAll(initialExpense.attachments)
            }
        }
    }

    // Active upload tasks
    val activeUploads = remember { mutableStateListOf<UploadTaskItem>() }

    fun getTotalAttachmentsCount(): Int {
        return existingAttachments.size + activeUploads.count { it.status != UploadStatus.FAILED }
    }

    fun startUpload(prepared: AttachmentManager.PreparedFile, isImage: Boolean) {
        val task = UploadTaskItem(
            preparedFile = prepared,
            isImage = isImage,
            progress = 0,
            status = UploadStatus.UPLOADING
        )
        activeUploads.add(task)

        coroutineScope.launch {
            val result = AttachmentManager.uploadAttachment(
                projectId = projectId,
                expenseId = effectiveExpenseId,
                preparedFile = prepared,
                uploadedByUid = currentUserId,
                onProgress = { p ->
                    val idx = activeUploads.indexOfFirst { it.id == task.id }
                    if (idx != -1) {
                        activeUploads[idx] = activeUploads[idx].copy(progress = p)
                    }
                }
            )

            val idx = activeUploads.indexOfFirst { it.id == task.id }
            if (idx != -1) {
                result.fold(
                    onSuccess = { att ->
                        activeUploads[idx] = activeUploads[idx].copy(
                            progress = 100,
                            status = UploadStatus.SUCCESS,
                            uploadedAttachment = att
                        )
                    },
                    onFailure = { err ->
                        activeUploads[idx] = activeUploads[idx].copy(
                            status = UploadStatus.FAILED,
                            error = err.localizedMessage ?: "Upload failed"
                        )
                    }
                )
            }
        }
    }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentError = null
            if (getTotalAttachmentsCount() >= 5) {
                attachmentError = "Maximum 5 attachments allowed per expense"
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                val prepResult = AttachmentManager.prepareAttachment(context, uri, isImage = true)
                prepResult.fold(
                    onSuccess = { prepared ->
                        startUpload(prepared, isImage = true)
                    },
                    onFailure = { err ->
                        attachmentError = err.localizedMessage ?: "Failed to process image"
                    }
                )
            }
        }
    }

    // System Document Picker (PDFs only)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentError = null
            if (getTotalAttachmentsCount() >= 5) {
                attachmentError = "Maximum 5 attachments allowed per expense"
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                val prepResult = AttachmentManager.prepareAttachment(context, uri, isImage = false)
                prepResult.fold(
                    onSuccess = { prepared ->
                        startUpload(prepared, isImage = false)
                    },
                    onFailure = { err ->
                        attachmentError = err.localizedMessage ?: "Failed to read document"
                    }
                )
            }
        }
    }

    // System File Picker for images (for screenshots, downloads, and storage files)
    val imageFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentError = null
            if (getTotalAttachmentsCount() >= 5) {
                attachmentError = "Maximum 5 attachments allowed per expense"
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                val prepResult = AttachmentManager.prepareAttachment(context, uri, isImage = true)
                prepResult.fold(
                    onSuccess = { prepared ->
                        startUpload(prepared, isImage = true)
                    },
                    onFailure = { err ->
                        attachmentError = err.localizedMessage ?: "Failed to process image"
                    }
                )
            }
        }
    }

    // System Contact Picker (ActivityResultContracts.PickContact) - requires zero permissions
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            try {
                val contentResolver = context.contentResolver
                contentResolver.query(
                    contactUri,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                supplierName = name
                                supplierNameError = null
                            }
                        }

                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val id = if (idIndex != -1) cursor.getString(idIndex) else null
                        val hasPhone = if (hasPhoneIndex != -1) cursor.getInt(hasPhoneIndex) > 0 else false

                        if (hasPhone && id != null) {
                            try {
                                contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                    arrayOf(id),
                                    null
                                )?.use { phoneCursor ->
                                    if (phoneCursor.moveToFirst()) {
                                        val phoneIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        if (phoneIndex != -1) {
                                            val phone = phoneCursor.getString(phoneIndex)
                                            if (!phone.isNullOrBlank()) {
                                                supplierPhone = phone
                                            }
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                // Gracefully ignore
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore gracefully
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialExpense == null) {
                    if (type == "GOT") "Add Income (You Got)" else "Add Payment (You Gave)"
                } else {
                    if (type == "GOT") "Edit Income" else "Edit Payment"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // -------------------------------------------------------------
                // Transaction Type Selector (You gave / You got)
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isGave = type == "GAVE"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGave) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { type = "GAVE" }
                            .testTag("type_you_gave_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallMade,
                                contentDescription = null,
                                tint = if (isGave) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You Gave",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isGave) FontWeight.Bold else FontWeight.Medium,
                                color = if (isGave) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isGot = type == "GOT"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGot) Color(0xFFE8F5E9) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { type = "GOT" }
                            .testTag("type_you_got_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallReceived,
                                contentDescription = null,
                                tint = if (isGot) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You Got",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isGot) FontWeight.Bold else FontWeight.Medium,
                                color = if (isGot) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Supplier / Party input & Contact Picker Button
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (type == "GOT") "Received From (Party / Client)" else "Paid To (Supplier / Vendor)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedButton(
                            onClick = { contactPickerLauncher.launch(null) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pick_contact_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Contact", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = {
                            supplierName = it
                            if (supplierNameError != null) supplierNameError = null
                        },
                        label = { Text("Supplier Name *") },
                        placeholder = { Text("e.g. UltraTech Cement, Sharma Transport") },
                        isError = supplierNameError != null,
                        supportingText = if (supplierNameError != null) {
                            { Text(supplierNameError!!) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("expense_supplier_name_input")
                    )

                    OutlinedTextField(
                        value = supplierPhone,
                        onValueChange = { supplierPhone = it },
                        label = { Text("Supplier Phone (Optional)") },
                        placeholder = { Text("e.g. +91 98765 43210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("expense_supplier_phone_input")
                    )
                }

                // Amount in INR
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        if (amountError != null) amountError = null
                    },
                    label = { Text("Amount (₹) *") },
                    placeholder = { Text("e.g. 125000") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError != null,
                    supportingText = if (amountError != null) {
                        { Text(amountError!!) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                )

                // Date Picker field
                OutlinedTextField(
                    value = Date(selectedDateMillis).formatDate(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Date *") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("expense_date_input")
                )

                // Paid By Dropdown (defaults to current user)
                ExposedDropdownMenuBox(
                    expanded = memberDropdownExpanded,
                    onExpandedChange = { memberDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedMember = members[paidByUid]
                    val selectedMemberText = selectedMember?.name?.ifBlank { selectedMember.email } ?: "Partner"

                    OutlinedTextField(
                        value = selectedMemberText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (type == "GOT") "Received By *" else "Paid By *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberDropdownExpanded)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                            .testTag("expense_paid_by_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = memberDropdownExpanded,
                        onDismissRequest = { memberDropdownExpanded = false }
                    ) {
                        members.forEach { (uid, member) ->
                            val isMe = uid == currentUserId
                            val memberName = member.name.ifBlank { member.email }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = if (isMe) "$memberName (Me)" else memberName,
                                            fontWeight = if (uid == paidByUid) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "${member.email} · ${member.role.replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    paidByUid = uid
                                    memberDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("paid_by_item_$uid")
                            )
                        }
                    }
                }

                // Payment Mode Dropdown (UPI, Cash, Cheque, Netbanking)
                val paymentModes = listOf("UPI", "Cash", "Cheque", "Netbanking")
                ExposedDropdownMenuBox(
                    expanded = paymentModeDropdownExpanded,
                    onExpandedChange = { paymentModeDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth().testTag("expense_payment_mode_dropdown")
                ) {
                    OutlinedTextField(
                        value = paymentMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Mode *") },
                        leadingIcon = {
                            val modeIcon = when (paymentMode) {
                                "Cash" -> Icons.Default.Payments
                                "Cheque" -> Icons.Default.ReceiptLong
                                "Netbanking" -> Icons.Default.AccountBalance
                                else -> Icons.Default.QrCode2
                            }
                            Icon(modeIcon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentModeDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = paymentModeDropdownExpanded,
                        onDismissRequest = { paymentModeDropdownExpanded = false }
                    ) {
                        paymentModes.forEach { mode ->
                            val modeIcon = when (mode) {
                                "Cash" -> Icons.Default.Payments
                                "Cheque" -> Icons.Default.ReceiptLong
                                "Netbanking" -> Icons.Default.AccountBalance
                                else -> Icons.Default.QrCode2
                            }
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(modeIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                text = {
                                    Text(
                                        text = mode,
                                        fontWeight = if (mode == paymentMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    paymentMode = mode
                                    paymentModeDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("payment_mode_item_$mode")
                            )
                        }
                    }
                }

                // Note field (Optional, Multi-line)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Memo (Optional)") },
                    placeholder = { Text("e.g. Invoice #204, 200 bags of cement delivered to site") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("expense_note_input")
                )

                // -------------------------------------------------------------
                // Attachments Section (Cloud Storage integration)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row - clickable anywhere to add attachment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (getTotalAttachmentsCount() < 5) {
                                    showAttachmentSheet = true
                                } else {
                                    attachmentError = "Maximum 5 attachments reached"
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Attachments (${getTotalAttachmentsCount()}/5)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Images (JPEG/PNG) & PDFs · max 10 MB each",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Attach Button
                        FilledTonalButton(
                            onClick = {
                                if (getTotalAttachmentsCount() < 5) {
                                    showAttachmentSheet = true
                                } else {
                                    attachmentError = "Maximum 5 attachments reached"
                                }
                            },
                            enabled = getTotalAttachmentsCount() < 5,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("attach_button")
                        ) {
                            Text("+ Attach")
                        }
                    }

                    // Direct Quick Action Buttons (Photo vs PDF)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (getTotalAttachmentsCount() < 5) {
                                    attachmentError = null
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    attachmentError = "Maximum 5 attachments reached"
                                }
                            },
                            enabled = getTotalAttachmentsCount() < 5,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("attach_photo_quick_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Photo", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = {
                                if (getTotalAttachmentsCount() < 5) {
                                    attachmentError = null
                                    documentPickerLauncher.launch(arrayOf("application/pdf"))
                                } else {
                                    attachmentError = "Maximum 5 attachments reached"
                                }
                            },
                            enabled = getTotalAttachmentsCount() < 5,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("attach_pdf_quick_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add PDF", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Empty state dropzone helper
                    if (getTotalAttachmentsCount() == 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAttachmentSheet = true }
                                .testTag("empty_attachments_dropzone")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "No files attached yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap 'Add Photo' or 'Add PDF' above to upload bills or receipts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (attachmentError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = attachmentError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Existing saved attachments
                    existingAttachments.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (item.isImage) Icons.Default.Image else Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.readableSize,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { existingAttachments.remove(item) },
                                    modifier = Modifier.size(24.dp).testTag("remove_existing_attachment_${item.name}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Active upload items
                    activeUploads.forEach { uploadItem ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uploadItem.isImage) Icons.Default.Image else Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = when (uploadItem.status) {
                                            UploadStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                                            UploadStatus.FAILED -> MaterialTheme.colorScheme.error
                                            UploadStatus.UPLOADING -> MaterialTheme.colorScheme.secondary
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uploadItem.preparedFile.fileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        val statusText = when (uploadItem.status) {
                                            UploadStatus.UPLOADING -> "Uploading (${uploadItem.progress}%)"
                                            UploadStatus.SUCCESS -> "Uploaded (${uploadItem.uploadedAttachment?.readableSize ?: ""})"
                                            UploadStatus.FAILED -> (uploadItem.error ?: "Upload failed")
                                        }
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (uploadItem.status) {
                                                UploadStatus.FAILED -> MaterialTheme.colorScheme.error
                                                UploadStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }

                                    // Action buttons for upload
                                    if (uploadItem.status == UploadStatus.FAILED) {
                                        IconButton(
                                            onClick = {
                                                uploadItem.status = UploadStatus.UPLOADING
                                                uploadItem.error = null
                                                startUpload(uploadItem.preparedFile, uploadItem.isImage)
                                            },
                                            modifier = Modifier.size(28.dp).testTag("retry_upload_button")
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            // Cancel or remove from active uploads
                                            activeUploads.remove(uploadItem)
                                            uploadItem.uploadedAttachment?.let { att ->
                                                coroutineScope.launch {
                                                    AttachmentManager.deleteAttachment(att.storagePath)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp).testTag("cancel_upload_button")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (uploadItem.status == UploadStatus.UPLOADING) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { uploadItem.progress / 100f },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // Info badge: enteredByUid cannot be changed
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Entered by: $currentUserName (You)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            val hasPendingUploads = activeUploads.any { it.status == UploadStatus.UPLOADING }

            Button(
                onClick = {
                    val cleanSupplier = supplierName.trim()
                    if (cleanSupplier.isBlank()) {
                        supplierNameError = "Please enter supplier name"
                        return@Button
                    }
                    val amountVal = amountInput.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0.0) {
                        amountError = "Please enter a valid amount greater than zero"
                        return@Button
                    }
                    if (paidByUid.isBlank()) {
                        paidByUid = currentUserId
                    }

                    // Consolidate attachments: existing + successfully uploaded items
                    val finalAttachments = existingAttachments.toMutableList()
                    activeUploads.forEach { item ->
                        if (item.status == UploadStatus.SUCCESS && item.uploadedAttachment != null) {
                            finalAttachments.add(item.uploadedAttachment!!)
                        }
                    }

                    val dateTimestamp = Timestamp(Date(selectedDateMillis))
                    onSave(
                        cleanSupplier,
                        supplierPhone.trim().ifBlank { null },
                        amountVal,
                        dateTimestamp,
                        paidByUid,
                        note.trim().ifBlank { null },
                        type,
                        paymentMode,
                        finalAttachments,
                        effectiveExpenseId
                    )
                },
                enabled = !hasPendingUploads,
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text(
                    if (hasPendingUploads) "Uploading Attachments..."
                    else if (initialExpense == null) (if (type == "GOT") "Save Income" else "Save Payment")
                    else "Update"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_expense_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("expense_form_dialog")
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAttachmentSheet) {
        AlertDialog(
            onDismissRequest = { showAttachmentSheet = false },
            title = {
                Text("Add Attachment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentSheet = false
                                attachmentError = null
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("choose_photo_attachment_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Photo / Image",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Camera photos, gallery receipts, invoices",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentSheet = false
                                attachmentError = null
                                imageFilePickerLauncher.launch(
                                    arrayOf("image/jpeg", "image/png", "image/webp", "image/*")
                                )
                            }
                            .testTag("choose_image_file_attachment_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Image from Files / Screenshots",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Screenshots, downloads, internal storage files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentSheet = false
                                attachmentError = null
                                documentPickerLauncher.launch(arrayOf("application/pdf"))
                            }
                            .testTag("choose_pdf_attachment_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PDF Document",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "PDF bills, GST invoices, statements",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showAttachmentSheet = false },
                    modifier = Modifier.testTag("cancel_attachment_chooser_button")
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
